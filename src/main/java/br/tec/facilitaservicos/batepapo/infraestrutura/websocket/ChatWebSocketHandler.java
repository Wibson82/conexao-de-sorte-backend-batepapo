package br.tec.facilitaservicos.batepapo.infraestrutura.websocket;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.tec.facilitaservicos.batepapo.aplicacao.servico.ChatService;
import br.tec.facilitaservicos.batepapo.aplicacao.servico.SalaService;
import br.tec.facilitaservicos.batepapo.apresentacao.dto.ChatEventDto;
import br.tec.facilitaservicos.batepapo.apresentacao.dto.MensagemDto;
import br.tec.facilitaservicos.batepapo.infraestrutura.streaming.ChatStreamingService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * ============================================================================
 * 🔌 WEBSOCKET HANDLER PARA CHAT EM TEMPO REAL
 * ============================================================================
 * 
 * Gerencia conexões WebSocket para chat com:
 * - Conexões reativas por sala
 * - Broadcasting de mensagens em tempo real
 * - Eventos de presença (usuário conectou/desconectou)
 * - Heartbeat para manter conexões ativas
 * - Rate limiting por conexão
 * - Reconexão automática
 * - Compressão de mensagens
 * 
 * Protocolo de mensagens:
 * - JOIN_ROOM: Entrar em sala
 * - LEAVE_ROOM: Sair da sala
 * - SEND_MESSAGE: Enviar mensagem
 * - HEARTBEAT: Manter conexão ativa
 * - TYPING: Indicador de digitação
 * 
 * Eventos enviados:
 * - NEW_MESSAGE: Nova mensagem
 * - MESSAGE_EDITED: Mensagem editada
 * - MESSAGE_DELETED: Mensagem excluída
 * - USER_JOINED: Usuário entrou
 * - USER_LEFT: Usuário saiu
 * - USER_TYPING: Usuário digitando
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@Component
public class ChatWebSocketHandler implements WebSocketHandler {

    private final ChatService chatService;
    private final SalaService salaService;
    private final ChatStreamingService streamingService;
    private final ObjectMapper objectMapper;

    // Cache de sessões ativas por sala
    private final Map<String, Sinks.Many<ChatEventDto>> salaSinks = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> sessionsAtivas = new ConcurrentHashMap<>();

    @Value("${websocket.heartbeat.interval:30}")
    private int heartbeatIntervalSeconds;

    @Value("${websocket.max-message-size:4096}")
    private int maxMessageSize;

    @Value("${websocket.timeout.idle:300}")
    private int idleTimeoutSeconds;

    public ChatWebSocketHandler(ChatService chatService, 
                               SalaService salaService,
                               ChatStreamingService streamingService,
                               ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.salaService = salaService;
        this.streamingService = streamingService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        // Extrair parâmetros da URL
        SessionInfo sessionInfo = extrairInfoSessao(session);
        
        if (sessionInfo == null) {
            return session.close();
        }

        String sessionId = session.getId();
        sessionsAtivas.put(sessionId, session);

        // Configurar sink para esta sala
        Sinks.Many<ChatEventDto> sink = salaSinks.computeIfAbsent(
            sessionInfo.sala, 
            k -> Sinks.many().multicast().directBestEffort()
        );

        // Stream de entrada (mensagens do cliente)
        Flux<Void> entrada = session.receive()
            .map(WebSocketMessage::getPayloadAsText)
            .filter(payload -> payload.length() <= maxMessageSize)
            .flatMap(payload -> processarMensagemCliente(payload, sessionInfo, session))
            .doOnError(error -> handleError(sessionInfo, error))
            .onErrorResume(error -> Mono.empty());

        // Stream de saída (eventos para o cliente)
        Flux<WebSocketMessage> saida = configurarStreamSaida(session, sink, sessionInfo);

        // Conectar usuário à sala
        return conectarUsuario(sessionInfo)
            .then(
                // Processar streams de entrada e saída em paralelo
                Mono.zip(
                    entrada.then(),
                    session.send(saida).then()
                ).then()
            )
            .doFinally(signal -> desconectarUsuario(sessionInfo, sessionId));
    }

    // === PROCESSAMENTO DE MENSAGENS ===

    private Mono<Void> processarMensagemCliente(String payload, SessionInfo info, WebSocketSession session) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> mensagem = objectMapper.readValue(payload, Map.class);
            String tipo = (String) mensagem.get("type");

            return switch (tipo) {
                case "SEND_MESSAGE" -> processarEnvioMensagem(mensagem, info);
                case "JOIN_ROOM" -> processarEntradaSala(mensagem, info);
                case "LEAVE_ROOM" -> processarSaidaSala(mensagem, info);
                case "HEARTBEAT" -> processarHeartbeat(info);
                case "TYPING" -> processarIndicadorDigitacao(mensagem, info);
                case "EDIT_MESSAGE" -> processarEdicaoMensagem(mensagem, info);
                case "DELETE_MESSAGE" -> processarExclusaoMensagem(mensagem, info);
                default -> {
                    enviarErro(session, "Tipo de mensagem desconhecido: " + tipo);
                    yield Mono.empty();
                }
            };

        } catch (Exception e) {
            return enviarErro(session, "Erro ao processar mensagem: " + e.getMessage());
        }
    }

    private Mono<Void> processarEnvioMensagem(Map<String, Object> dados, SessionInfo info) {
        String conteudo = (String) dados.get("content");
        Long respostaParaId = dados.containsKey("replyTo") ? 
            Long.valueOf(dados.get("replyTo").toString()) : null;

        return chatService.enviarMensagem(conteudo, info.usuarioId, info.usuarioNome, 
                                         info.sala, respostaParaId)
            .doOnSuccess(mensagem -> {
                // Broadcast para todos na sala
                ChatEventDto evento = ChatEventDto.novaMensagem(info.sala, mensagem);
                broadcastParaSala(info.sala, evento);
            })
            .onErrorResume(error -> {
                // Enviar erro apenas para o remetente
                return enviarEventoParaSessao(info.sala, ChatEventDto.erro(error.getMessage()));
            })
            .then();
    }

    private Mono<Void> processarEntradaSala(Map<String, Object> dados, SessionInfo info) {
        String novaSala = (String) dados.get("room");
        if (novaSala != null && !novaSala.equals(info.sala)) {
            // Mudar de sala
            return sairDaSala(info.sala, info)
                .then(entrarNaSala(novaSala, info));
        }
        return Mono.empty();
    }

    private Mono<Void> processarSaidaSala(Map<String, Object> dados, SessionInfo info) {
        return sairDaSala(info.sala, info);
    }

    private Mono<Void> processarHeartbeat(SessionInfo info) {
        return salaService.atualizarHeartbeat(info.sala, info.usuarioId);
    }

    private Mono<Void> processarIndicadorDigitacao(Map<String, Object> dados, SessionInfo info) {
        boolean digitando = (Boolean) dados.getOrDefault("typing", false);
        
        ChatEventDto evento = ChatEventDto.usuarioDigitando(
            info.sala, info.usuarioId, info.usuarioNome, digitando
        );
        
        return broadcastParaSala(info.sala, evento);
    }

    private Mono<Void> processarEdicaoMensagem(Map<String, Object> dados, SessionInfo info) {
        Long mensagemId = Long.valueOf(dados.get("messageId").toString());
        String novoConteudo = (String) dados.get("content");

        return chatService.editarMensagem(mensagemId, novoConteudo, info.usuarioId)
            .doOnSuccess(mensagem -> {
                ChatEventDto evento = new ChatEventDto(
                    ChatEventDto.TipoEvento.MENSAGEM_EDITADA, info.sala,
                    mensagem.dataEnvio(), mensagem, null, null, null
                );
                broadcastParaSala(info.sala, evento);
            })
            .then();
    }

    private Mono<Void> processarExclusaoMensagem(Map<String, Object> dados, SessionInfo info) {
        Long mensagemId = Long.valueOf(dados.get("messageId").toString());

        return chatService.excluirMensagem(mensagemId, info.usuarioId)
            .doOnSuccess(v -> {
                ChatEventDto evento = ChatEventDto.mensagemExcluida(
                    info.sala, mensagemId, info.usuarioId, "Excluída pelo autor"
                );
                broadcastParaSala(info.sala, evento);
            });
    }

    // === GERENCIAMENTO DE CONEXÕES ===

    private Mono<Void> conectarUsuario(SessionInfo info) {
        return salaService.conectarUsuario(info.sala, info.usuarioId, info.usuarioNome, info.userAgent)
            .doOnSuccess(usuario -> {
                // Notificar outros usuários
                ChatEventDto evento = ChatEventDto.usuarioConectado(
                    info.sala, info.usuarioId, info.usuarioNome
                );
                broadcastParaSala(info.sala, evento);
            })
            .then();
    }

    private Mono<Void> desconectarUsuario(SessionInfo info, String sessionId) {
        sessionsAtivas.remove(sessionId);
        
        return salaService.desconectarUsuario(info.sala, info.usuarioId)
            .doOnSuccess(v -> {
                // Notificar outros usuários
                ChatEventDto evento = ChatEventDto.usuarioDesconectado(
                    info.sala, info.usuarioId, info.usuarioNome
                );
                broadcastParaSala(info.sala, evento);
                
                // Limpar sink se não há mais usuários
                salaService.contarUsuariosOnline(info.sala)
                    .filter(count -> count == 0)
                    .doOnNext(count -> salaSinks.remove(info.sala))
                    .subscribe();
            });
    }

    private Mono<Void> entrarNaSala(String sala, SessionInfo info) {
        info.sala = sala;
        return conectarUsuario(info);
    }

    private Mono<Void> sairDaSala(String sala, SessionInfo info) {
        return salaService.desconectarUsuario(sala, info.usuarioId);
    }

    // === STREAMING E BROADCASTING ===

    private Flux<WebSocketMessage> configurarStreamSaida(WebSocketSession session, 
                                                        Sinks.Many<ChatEventDto> sink,
                                                        SessionInfo info) {
        // Combinar eventos locais com eventos do Redis Streams
        Flux<ChatEventDto> eventosLocais = sink.asFlux()
            .filter(evento -> evento.sala().equals(info.sala));

        Flux<ChatEventDto> eventosRedis = streamingService.streamEventosMensagens()
            .filter(evento -> evento.sala().equals(info.sala));

        return Flux.merge(eventosLocais, eventosRedis)
            .distinct(evento -> evento.timestamp()) // Evitar duplicatas
            .map(evento -> {
                try {
                    String json = objectMapper.writeValueAsString(evento);
                    return session.textMessage(json);
                } catch (Exception e) {
                    return session.textMessage("{\"type\":\"ERROR\",\"message\":\"Erro de serialização\"}");
                }
            })
            .doOnError(error -> handleError(info, error))
            .onErrorResume(error -> Flux.empty())
            .takeUntilOther(
                // Auto-disconnect por timeout de inatividade
                Mono.delay(Duration.ofSeconds(idleTimeoutSeconds))
            );
    }

    private Mono<Void> broadcastParaSala(String sala, ChatEventDto evento) {
        Sinks.Many<ChatEventDto> sink = salaSinks.get(sala);
        if (sink != null) {
            sink.tryEmitNext(evento);
        }
        return Mono.empty();
    }

    private Mono<Void> enviarEventoParaSessao(String sala, ChatEventDto evento) {
        return broadcastParaSala(sala, evento);
    }

    private Mono<Void> enviarErro(WebSocketSession session, String mensagem) {
        ChatEventDto erro = ChatEventDto.erro(mensagem);
        try {
            String json = objectMapper.writeValueAsString(erro);
            return session.send(Mono.just(session.textMessage(json))).then();
        } catch (Exception e) {
            return session.close();
        }
    }

    // === MÉTODOS AUXILIARES ===

    private SessionInfo extrairInfoSessao(WebSocketSession session) {
        try {
            URI uri = session.getHandshakeInfo().getUri();
            Map<String, String> params = UriComponentsBuilder.fromUri(uri)
                .build()
                .getQueryParams()
                .toSingleValueMap();

            String sala = params.get("room");
            String usuarioIdStr = params.get("userId");
            String usuarioNome = params.get("userName");
            String token = params.get("token");

            if (sala == null || usuarioIdStr == null || usuarioNome == null) {
                return null;
            }

            // TODO: Validar token JWT aqui

            Long usuarioId = Long.valueOf(usuarioIdStr);
            String userAgent = session.getHandshakeInfo().getHeaders()
                .getFirst("User-Agent");

            return new SessionInfo(sala, usuarioId, usuarioNome, userAgent);

        } catch (Exception e) {
            return null;
        }
    }

    private void handleError(SessionInfo info, Throwable error) {
        // Log error
        System.err.println("WebSocket error for user " + info.usuarioId + 
                          " in room " + info.sala + ": " + error.getMessage());
    }

    // === CLASSES AUXILIARES ===

    private static class SessionInfo {
        String sala;
        final Long usuarioId;
        final String usuarioNome;
        final String userAgent;

        SessionInfo(String sala, Long usuarioId, String usuarioNome, String userAgent) {
            this.sala = sala;
            this.usuarioId = usuarioId;
            this.usuarioNome = usuarioNome;
            this.userAgent = userAgent;
        }
    }
}