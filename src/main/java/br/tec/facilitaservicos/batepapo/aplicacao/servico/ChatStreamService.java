package br.tec.facilitaservicos.batepapo.aplicacao.servico;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import br.tec.facilitaservicos.batepapo.apresentacao.dto.ChatEventDto;
import br.tec.facilitaservicos.batepapo.infraestrutura.streaming.ChatStreamingService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * Serviço reativo para streams SSE do chat
 * Gerencia conexões em tempo real e broadcasting de eventos
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@Service
public class ChatStreamService {

    private final ChatStreamingService streamingService;
    private final ChatService chatService;
    
    // Mapa de conexões SSE ativas por sala
    private final ConcurrentMap<String, Sinks.Many<ChatEventDto>> salaStreams = new ConcurrentHashMap<>();
    
    // Mapa de usuários conectados por SSE
    private final ConcurrentMap<String, ConexaoSSE> conexoesSSE = new ConcurrentHashMap<>();

    @Value("${chat.heartbeat-interval-seconds:30}")
    private int heartbeatInterval;

    public ChatStreamService(ChatStreamingService streamingService, ChatService chatService) {
        this.streamingService = streamingService;
        this.chatService = chatService;
    }

    /**
     * Stream de mensagens de uma sala com histórico opcional
     */
    public Flux<ChatEventDto> streamMensagensSala(String sala, Long usuarioId, boolean incluirHistorico) {
        // Criar ou obter sink para a sala
        Sinks.Many<ChatEventDto> salaSink = salaStreams.computeIfAbsent(sala, 
            key -> Sinks.many().multicast().directBestEffort());

        Flux<ChatEventDto> streamAtual = salaSink.asFlux();

        // Incluir histórico recente se solicitado
        if (incluirHistorico) {
            Flux<ChatEventDto> historico = chatService.buscarMensagensRecentes(sala, 10)
                .map(mensagem -> ChatEventDto.novaMensagem(sala, mensagem));
                
            return Flux.concat(historico, streamAtual);
        }

        return streamAtual;
    }

    /**
     * Stream completo de eventos de uma sala (mensagens + usuários + sala)
     */
    public Flux<ChatEventDto> streamEventosSala(String sala, Long usuarioId) {
        // Stream de mensagens
        Flux<ChatEventDto> mensagens = streamMensagensSala(sala, usuarioId, false);
        
        // Stream de eventos de usuários (Redis Streams)
        Flux<ChatEventDto> eventosUsuarios = streamingService.streamEventosUsuarios(sala);
        
        // Stream de eventos de sala
        Flux<ChatEventDto> eventosSala = streamingService.streamEventosSala(sala);

        return Flux.merge(mensagens, eventosUsuarios, eventosSala);
    }

    /**
     * Stream global de presença de usuários
     */
    public Flux<ChatEventDto> streamPresencaGlobal(Long usuarioId) {
        return streamingService.streamPresencaGlobal()
            .filter(evento -> !evento.usuarioOnline().usuarioId().equals(usuarioId)); // Filtrar próprios eventos
    }

    /**
     * Stream de status das conexões
     */
    public Flux<Object> streamStatusConexoes(Long usuarioId) {
        return Flux.interval(Duration.ofSeconds(heartbeatInterval * 2))
            .map(tick -> criarStatusConexao(usuarioId));
    }

    /**
     * Adiciona conexão SSE
     */
    public void adicionarConexaoSSE(String sala, Long usuarioId, String usuarioNome) {
        String chaveConexao = gerarChaveConexao(usuarioId, sala);
        ConexaoSSE conexao = new ConexaoSSE(usuarioId, usuarioNome, sala, LocalDateTime.now());
        
        conexoesSSE.put(chaveConexao, conexao);
        
        // Notificar entrada do usuário via streaming
        streamingService.publicarEventoUsuario(
            ChatEventDto.usuarioEntrou(sala, conexao.toDto())
        );
    }

    /**
     * Remove conexão SSE
     */
    public void removerConexaoSSE(String sala, Long usuarioId) {
        String chaveConexao = gerarChaveConexao(usuarioId, sala);
        ConexaoSSE conexao = conexoesSSE.remove(chaveConexao);
        
        if (conexao != null) {
            // Notificar saída do usuário via streaming
            streamingService.publicarEventoUsuario(
                ChatEventDto.usuarioSaiu(sala, conexao.toDto())
            );
        }

        // Limpar sink da sala se não houver mais conexões
        limparSalaSeVazia(sala);
    }

    /**
     * Publica evento para todas as conexões de uma sala
     */
    public void publicarEventoSala(String sala, ChatEventDto evento) {
        Sinks.Many<ChatEventDto> salaSink = salaStreams.get(sala);
        if (salaSink != null) {
            salaSink.tryEmitNext(evento);
        }
    }

    /**
     * Lista conexões ativas de uma sala
     */
    public Flux<ConexaoSSE> listarConexoesSala(String sala) {
        return Flux.fromIterable(conexoesSSE.values())
            .filter(conexao -> conexao.sala.equals(sala));
    }

    /**
     * Conta conexões ativas
     */
    public Mono<Long> contarConexoesAtivas() {
        return Mono.just((long) conexoesSSE.size());
    }

    /**
     * Conta conexões por sala
     */
    public Mono<Long> contarConexoesSala(String sala) {
        return listarConexoesSala(sala).count();
    }

    // Métodos auxiliares

    private String gerarChaveConexao(Long usuarioId, String sala) {
        return String.format("%d:%s", usuarioId, sala);
    }

    private void limparSalaSeVazia(String sala) {
        boolean temConexoes = conexoesSSE.values().stream()
            .anyMatch(conexao -> conexao.sala.equals(sala));
            
        if (!temConexoes) {
            Sinks.Many<ChatEventDto> sink = salaStreams.remove(sala);
            if (sink != null) {
                sink.tryEmitComplete();
            }
        }
    }

    private Object criarStatusConexao(Long usuarioId) {
        long totalConexoes = conexoesSSE.size();
        long conexoesUsuario = conexoesSSE.values().stream()
            .mapToLong(conexao -> conexao.usuarioId.equals(usuarioId) ? 1 : 0)
            .sum();

        return new StatusConexao(usuarioId, conexoesUsuario, totalConexoes, LocalDateTime.now());
    }

    // Classes auxiliares

    public static class ConexaoSSE {
        public final Long usuarioId;
        public final String usuarioNome;
        public final String sala;
        public final LocalDateTime conectadoEm;

        public ConexaoSSE(Long usuarioId, String usuarioNome, String sala, LocalDateTime conectadoEm) {
            this.usuarioId = usuarioId;
            this.usuarioNome = usuarioNome;
            this.sala = sala;
            this.conectadoEm = conectadoEm;
        }

        public br.tec.facilitaservicos.batepapo.apresentacao.dto.UsuarioOnlineDto toDto() {
            return br.tec.facilitaservicos.batepapo.apresentacao.dto.UsuarioOnlineDto.paraEntrada(
                usuarioId, usuarioNome, sala, "sse-" + usuarioId
            );
        }
    }

    public record StatusConexao(
        Long usuarioId,
        Long conexoesUsuario,
        Long totalConexoes,
        LocalDateTime timestamp
    ) {}
}