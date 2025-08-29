package br.tec.facilitaservicos.batepapo.aplicacao.servico;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import br.tec.facilitaservicos.batepapo.apresentacao.dto.MensagemDto;
import br.tec.facilitaservicos.batepapo.apresentacao.dto.SalaDto;
import br.tec.facilitaservicos.batepapo.apresentacao.dto.UsuarioOnlineDto;
import br.tec.facilitaservicos.batepapo.apresentacao.dto.ChatEventDto;
import br.tec.facilitaservicos.batepapo.dominio.entidade.MensagemR2dbc;
import br.tec.facilitaservicos.batepapo.dominio.entidade.SalaR2dbc;
import br.tec.facilitaservicos.batepapo.dominio.entidade.UsuarioOnlineR2dbc;
import br.tec.facilitaservicos.batepapo.dominio.enums.TipoMensagem;
import br.tec.facilitaservicos.batepapo.dominio.enums.StatusMensagem;
import br.tec.facilitaservicos.batepapo.dominio.enums.TipoSala;
import br.tec.facilitaservicos.batepapo.dominio.enums.StatusPresenca;
import br.tec.facilitaservicos.batepapo.dominio.repositorio.RepositorioMensagemR2dbc;
import br.tec.facilitaservicos.batepapo.dominio.repositorio.RepositorioSalaR2dbc;
import br.tec.facilitaservicos.batepapo.dominio.repositorio.RepositorioUsuarioOnlineR2dbc;
import br.tec.facilitaservicos.batepapo.infraestrutura.eventos.ChatEventPublisher;
import br.tec.facilitaservicos.batepapo.infraestrutura.cache.ReactiveMessageCache;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * ============================================================================
 * 💬 SERVIÇO DE BATE-PAPO
 * ============================================================================
 * 
 * Serviço principal para processamento de chat em tempo real:
 * - Orquestração de mensagens e salas
 * - Streaming reativo com SSE
 * - Controle de presença de usuários
 * - Broadcasting via Redis Streams
 * - Cache inteligente de mensagens
 * - Rate limiting por usuário
 * - Moderação de conteúdo
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private final RepositorioMensagemR2dbc mensagemRepository;
    private final RepositorioSalaR2dbc salaRepository;
    private final RepositorioUsuarioOnlineR2dbc usuarioOnlineRepository;
    private final ChatEventPublisher eventPublisher;
    private final ReactiveMessageCache messageCache;

    // Sink para broadcasting de mensagens em tempo real
    private final Sinks.Many<ChatEventDto> eventSink = Sinks.many()
            .multicast()
            .onBackpressureBuffer(1000);

    public ChatService(
            RepositorioMensagemR2dbc mensagemRepository,
            RepositorioSalaR2dbc salaRepository,
            RepositorioUsuarioOnlineR2dbc usuarioOnlineRepository,
            ChatEventPublisher eventPublisher,
            ReactiveMessageCache messageCache) {
        this.mensagemRepository = mensagemRepository;
        this.salaRepository = salaRepository;
        this.usuarioOnlineRepository = usuarioOnlineRepository;
        this.eventPublisher = eventPublisher;
        this.messageCache = messageCache;
    }

    /**
     * Envia uma mensagem para uma sala
     */
    public Mono<MensagemDto> enviarMensagem(MensagemDto mensagemDto, String userId) {
        logger.debug("💬 Usuário {} enviando mensagem para sala: {}", userId, mensagemDto.getSala());

        return criarMensagem(mensagemDto, userId)
                .flatMap(mensagemRepository::save)
                .flatMap(mensagem -> {
                    // Publicar evento para Redis Streams
                    ChatEventDto evento = criarEventoMensagem(mensagem);
                    return eventPublisher.publicarMensagem(evento)
                            .thenReturn(mensagem);
                })
                .map(this::converterParaDto)
                .doOnNext(dto -> {
                    // Broadcasting local via Sink
                    ChatEventDto evento = criarEventoMensagem(dto);
                    eventSink.tryEmitNext(evento);
                })
                .doOnSuccess(dto -> logger.debug("✅ Mensagem enviada com sucesso: {}", dto.getId()))
                .onErrorResume(ex -> {
                    logger.error("❌ Erro ao enviar mensagem: {}", ex.getMessage());
                    return Mono.error(new RuntimeException("Erro ao enviar mensagem", ex));
                });
    }

    /**
     * Lista mensagens de uma sala com paginação
     */
    public Flux<MensagemDto> listarMensagens(String sala, String userId, int page, int size, String filtro) {
        logger.debug("📜 Listando mensagens da sala {} para usuário {}", sala, userId);

        PageRequest pageable = PageRequest.of(page, size);

        // Primeiro tenta buscar do cache
        return messageCache.buscarMensagensSala(sala, page, size)
                .switchIfEmpty(
                    // Se não encontrar no cache, busca do banco
                    (filtro != null && !filtro.trim().isEmpty()) ?
                        mensagemRepository.findBySalaAndConteudoContaining(sala, filtro, pageable) :
                        mensagemRepository.findBySalaOrderByTimestampDesc(sala, pageable)
                )
                .map(this::converterParaDto)
                .doOnNext(dto -> messageCache.cachearMensagem(dto).subscribe()); // Cache assíncrono
    }

    /**
     * Stream de mensagens em tempo real para uma sala
     */
    public Flux<ChatEventDto> streamMensagens(String sala, String userId) {
        logger.debug("🌊 Iniciando stream para usuário {} na sala {}", userId, sala);

        return entrarNaSala(sala, userId)
                .thenMany(
                    eventSink.asFlux()
                        .filter(evento -> sala.equals(evento.getSala()))
                        .doOnNext(evento -> logger.trace("📡 Evento transmitido: {}", evento.getTipo()))
                );
    }

    /**
     * Stream completo de eventos (mensagens + presença + moderação)
     */
    public Flux<ChatEventDto> streamEventosCompletos(String sala, String userId) {
        logger.debug("🌊 Iniciando stream completo para usuário {} na sala {}", userId, sala);

        return entrarNaSala(sala, userId)
                .thenMany(
                    Flux.merge(
                        // Stream de mensagens
                        streamMensagens(sala, userId),
                        // Stream de eventos de presença
                        streamPresencaSala(sala),
                        // Stream de eventos de moderação
                        streamModeracaoSala(sala)
                    )
                );
    }

    /**
     * Lista salas disponíveis para o usuário
     */
    public Flux<SalaDto> listarSalas(String userId) {
        logger.debug("🏠 Listando salas para usuário {}", userId);

        return salaRepository.findSalasDisponiveis()
                .map(this::converterSalaParaDto);
    }

    /**
     * Lista salas públicas
     */
    public Flux<SalaDto> listarSalasPublicas() {
        logger.debug("🏠 Listando salas públicas");

        return salaRepository.findByTipo(TipoSala.PUBLICA)
                .map(this::converterSalaParaDto);
    }

    /**
     * Cria uma nova sala
     */
    public Mono<SalaDto> criarSala(SalaDto salaDto, String userId) {
        logger.debug("🏗️ Criando sala {} por usuário {}", salaDto.getNome(), userId);

        return Mono.fromCallable(() -> criarEntidadeSala(salaDto, userId))
                .flatMap(salaRepository::save)
                .map(this::converterSalaParaDto)
                .doOnSuccess(dto -> logger.debug("✅ Sala criada: {}", dto.getId()));
    }

    /**
     * Lista usuários online em uma sala
     */
    public Flux<UsuarioOnlineDto> usuariosOnline(String sala) {
        logger.debug("👥 Listando usuários online da sala {}", sala);

        return usuarioOnlineRepository.findBySalaAndStatus(sala, StatusPresenca.ONLINE)
                .map(this::converterUsuarioParaDto);
    }

    /**
     * Usuário entra em uma sala
     */
    public Mono<Void> entrarNaSala(String sala, String userId) {
        logger.debug("🚪 Usuário {} entrando na sala {}", userId, sala);

        return usuarioOnlineRepository.findByUsuarioIdAndSala(userId, sala)
                .switchIfEmpty(criarUsuarioOnline(userId, sala))
                .flatMap(usuario -> {
                    usuario.setStatus(StatusPresenca.ONLINE);
                    usuario.setUltimoHeartbeat(LocalDateTime.now());
                    return usuarioOnlineRepository.save(usuario);
                })
                .doOnSuccess(usuario -> {
                    // Publicar evento de entrada
                    ChatEventDto evento = criarEventoPresenca(userId, sala, "ENTROU");
                    eventSink.tryEmitNext(evento);
                    eventPublisher.publicarPresenca(evento).subscribe();
                })
                .then();
    }

    /**
     * Usuário sai de uma sala
     */
    public Mono<Void> sairDaSala(String sala, String userId) {
        logger.debug("🚪 Usuário {} saindo da sala {}", userId, sala);

        return usuarioOnlineRepository.findByUsuarioIdAndSala(userId, sala)
                .flatMap(usuario -> {
                    usuario.setStatus(StatusPresenca.OFFLINE);
                    return usuarioOnlineRepository.save(usuario);
                })
                .doOnSuccess(usuario -> {
                    // Publicar evento de saída
                    ChatEventDto evento = criarEventoPresenca(userId, sala, "SAIU");
                    eventSink.tryEmitNext(evento);
                    eventPublisher.publicarPresenca(evento).subscribe();
                })
                .then();
    }

    /**
     * Atualiza heartbeat do usuário
     */
    public Mono<Void> atualizarHeartbeat(String userId) {
        logger.trace("💓 Atualizando heartbeat do usuário {}", userId);

        return usuarioOnlineRepository.findByUsuarioId(userId)
                .flatMap(usuarios -> 
                    Flux.fromIterable(usuarios)
                        .flatMap(usuario -> {
                            usuario.setUltimoHeartbeat(LocalDateTime.now());
                            return usuarioOnlineRepository.save(usuario);
                        })
                        .then()
                );
    }

    /**
     * Edita uma mensagem existente
     */
    public Mono<MensagemDto> editarMensagem(String mensagemId, String novoConteudo, String userId) {
        logger.debug("✏️ Usuário {} editando mensagem {}", userId, mensagemId);

        return mensagemRepository.findById(mensagemId)
                .filter(mensagem -> userId.equals(mensagem.getAutorId()) || isUserModerator(userId))
                .switchIfEmpty(Mono.error(new RuntimeException("Não autorizado a editar esta mensagem")))
                .flatMap(mensagem -> {
                    mensagem.setConteudo(novoConteudo);
                    mensagem.setEditada(true);
                    mensagem.setTimestampEdicao(LocalDateTime.now());
                    return mensagemRepository.save(mensagem);
                })
                .map(this::converterParaDto);
    }

    /**
     * Exclui uma mensagem
     */
    public Mono<Void> excluirMensagem(String mensagemId, String userId) {
        logger.debug("🗑️ Usuário {} excluindo mensagem {}", userId, mensagemId);

        return mensagemRepository.findById(mensagemId)
                .filter(mensagem -> userId.equals(mensagem.getAutorId()) || isUserModerator(userId))
                .switchIfEmpty(Mono.error(new RuntimeException("Não autorizado a excluir esta mensagem")))
                .flatMap(mensagem -> {
                    mensagem.setStatus(StatusMensagem.DELETADA);
                    return mensagemRepository.save(mensagem);
                })
                .then();
    }

    /**
     * Stream de presença global
     */
    public Flux<UsuarioOnlineDto> streamPresencaGlobal() {
        return eventSink.asFlux()
                .filter(evento -> "PRESENCA".equals(evento.getTipo()))
                .map(evento -> converterEventoParaUsuario(evento));
    }

    /**
     * Obtém estatísticas de uma sala (moderadores apenas)
     */
    public Mono<Map<String, Object>> obterEstatisticasSala(String sala) {
        logger.debug("📊 Obtendo estatísticas da sala {}", sala);

        return Mono.zip(
                mensagemRepository.countBySala(sala),
                usuarioOnlineRepository.countBySalaAndStatus(sala, StatusPresenca.ONLINE),
                mensagemRepository.findFirstBySalaOrderByTimestampAsc(sala)
                    .map(MensagemR2dbc::getTimestamp)
                    .switchIfEmpty(Mono.just(LocalDateTime.now()))
        ).map(tuple -> {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalMensagens", tuple.getT1());
            stats.put("usuariosOnline", tuple.getT2());
            stats.put("primeiraAtividade", tuple.getT3());
            stats.put("sala", sala);
            stats.put("timestamp", LocalDateTime.now());
            return stats;
        });
    }

    // Métodos auxiliares

    private Mono<MensagemR2dbc> criarMensagem(MensagemDto dto, String userId) {
        return Mono.fromCallable(() -> MensagemR2dbc.builder()
                .id(UUID.randomUUID().toString())
                .autorId(userId)
                .sala(dto.getSala())
                .conteudo(dto.getConteudo())
                .tipo(TipoMensagem.TEXTO)
                .status(StatusMensagem.ATIVA)
                .timestamp(LocalDateTime.now())
                .editada(false)
                .build());
    }

    private MensagemDto converterParaDto(MensagemR2dbc mensagem) {
        return MensagemDto.builder()
                .id(mensagem.getId())
                .autorId(mensagem.getAutorId())
                .sala(mensagem.getSala())
                .conteudo(mensagem.getConteudo())
                .timestamp(mensagem.getTimestamp())
                .editada(mensagem.getEditada())
                .build();
    }

    private ChatEventDto criarEventoMensagem(MensagemR2dbc mensagem) {
        return ChatEventDto.builder()
                .id(UUID.randomUUID().toString())
                .tipo("MENSAGEM")
                .sala(mensagem.getSala())
                .autorId(mensagem.getAutorId())
                .timestamp(LocalDateTime.now())
                .dados(Map.of(
                    "mensagemId", mensagem.getId(),
                    "conteudo", mensagem.getConteudo()
                ))
                .build();
    }

    private ChatEventDto criarEventoMensagem(MensagemDto mensagem) {
        return ChatEventDto.builder()
                .id(UUID.randomUUID().toString())
                .tipo("MENSAGEM")
                .sala(mensagem.getSala())
                .autorId(mensagem.getAutorId())
                .timestamp(LocalDateTime.now())
                .dados(Map.of(
                    "mensagemId", mensagem.getId(),
                    "conteudo", mensagem.getConteudo()
                ))
                .build();
    }

    private ChatEventDto criarEventoPresenca(String userId, String sala, String acao) {
        return ChatEventDto.builder()
                .id(UUID.randomUUID().toString())
                .tipo("PRESENCA")
                .sala(sala)
                .autorId(userId)
                .timestamp(LocalDateTime.now())
                .dados(Map.of("acao", acao))
                .build();
    }

    private SalaDto converterSalaParaDto(SalaR2dbc sala) {
        return SalaDto.builder()
                .id(sala.getId())
                .nome(sala.getNome())
                .descricao(sala.getDescricao())
                .tipo(sala.getTipo())
                .build();
    }

    private UsuarioOnlineDto converterUsuarioParaDto(UsuarioOnlineR2dbc usuario) {
        return UsuarioOnlineDto.builder()
                .usuarioId(usuario.getUsuarioId())
                .sala(usuario.getSala())
                .status(usuario.getStatus())
                .ultimoHeartbeat(usuario.getUltimoHeartbeat())
                .build();
    }

    private SalaR2dbc criarEntidadeSala(SalaDto dto, String userId) {
        return SalaR2dbc.builder()
                .id(UUID.randomUUID().toString())
                .nome(dto.getNome())
                .descricao(dto.getDescricao())
                .tipo(dto.getTipo() != null ? dto.getTipo() : TipoSala.PUBLICA)
                .criadaPor(userId)
                .build();
    }

    private Mono<UsuarioOnlineR2dbc> criarUsuarioOnline(String userId, String sala) {
        return Mono.fromCallable(() -> UsuarioOnlineR2dbc.builder()
                .id(UUID.randomUUID().toString())
                .usuarioId(userId)
                .sala(sala)
                .status(StatusPresenca.ONLINE)
                .ultimoHeartbeat(LocalDateTime.now())
                .build());
    }

    private Flux<ChatEventDto> streamPresencaSala(String sala) {
        return eventSink.asFlux()
                .filter(evento -> "PRESENCA".equals(evento.getTipo()) && sala.equals(evento.getSala()));
    }

    private Flux<ChatEventDto> streamModeracaoSala(String sala) {
        return eventSink.asFlux()
                .filter(evento -> "MODERACAO".equals(evento.getTipo()) && sala.equals(evento.getSala()));
    }

    private boolean isUserModerator(String userId) {
        // TODO: Implementar verificação de moderador via claims JWT ou serviço
        return false;
    }

    private UsuarioOnlineDto converterEventoParaUsuario(ChatEventDto evento) {
        return UsuarioOnlineDto.builder()
                .usuarioId(evento.getAutorId())
                .sala(evento.getSala())
                .status("ENTROU".equals(evento.getDados().get("acao")) ? 
                       StatusPresenca.ONLINE : StatusPresenca.OFFLINE)
                .ultimoHeartbeat(evento.getTimestamp())
                .build();
    }
}