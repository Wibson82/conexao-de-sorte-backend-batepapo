package br.tec.facilitaservicos.batepapo.apresentacao.controlador;

import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import br.tec.facilitaservicos.batepapo.aplicacao.servico.ChatService;
import br.tec.facilitaservicos.batepapo.apresentacao.dto.MensagemDto;
import br.tec.facilitaservicos.batepapo.apresentacao.dto.SalaDto;
import br.tec.facilitaservicos.batepapo.apresentacao.dto.UsuarioOnlineDto;
import br.tec.facilitaservicos.batepapo.apresentacao.dto.ChatEventDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * ============================================================================
 * 💬 CONTROLLER REATIVO DE BATE-PAPO
 * ============================================================================
 * 
 * Controller 100% reativo para gerenciamento de chat em tempo real:
 * - Envio e recebimento de mensagens
 * - Server-Sent Events para streaming em tempo real
 * - Gestão de salas de chat
 * - Controle de presença de usuários
 * - Rate limiting por usuário
 * - Validação JWT via JWKS
 * - Moderação de conteúdo
 * 
 * Endpoints principais:
 * - POST /api/chat/mensagem - Enviar mensagem
 * - GET /api/chat/mensagens/{sala} - Histórico paginado
 * - GET /api/chat/stream/{sala} - SSE stream de mensagens
 * - GET /api/chat/salas - Listar salas
 * - POST /api/chat/entrar/{sala} - Entrar em sala
 * - GET /api/chat/online/{sala} - Usuários online
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@RestController
@RequestMapping("/api/chat")
@Tag(name = "Bate-papo", description = "API para chat em tempo real")
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Envia uma nova mensagem de chat
     */
    @PostMapping("/mensagem")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Enviar mensagem", description = "Envia uma nova mensagem para o chat")
    public Mono<ResponseEntity<MensagemDto>> enviarMensagem(
            @Valid @RequestBody MensagemDto mensagem,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.debug("💬 Usuário {} enviando mensagem para sala: {}", userId, mensagem.getSala());
        
        return chatService.enviarMensagem(mensagem, userId)
                .map(ResponseEntity::ok)
                .onErrorResume(ex -> {
                    logger.error("❌ Erro ao enviar mensagem: {}", ex.getMessage());
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    /**
     * Lista mensagens de uma sala com paginação
     */
    @GetMapping("/mensagens/{sala}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Histórico de mensagens", description = "Lista mensagens de uma sala com paginação")
    public Flux<MensagemDto> listarMensagens(
            @PathVariable String sala,
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String filtro) {
        
        String userId = authentication.getName();
        logger.debug("📜 Usuário {} listando mensagens da sala: {}", userId, sala);
        
        return chatService.listarMensagens(sala, userId, page, size, filtro);
    }

    /**
     * Stream SSE de mensagens em tempo real
     */
    @GetMapping(value = "/stream/{sala}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Stream de mensagens", description = "Stream SSE de mensagens em tempo real")
    public Flux<ServerSentEvent<ChatEventDto>> streamMensagens(
            @PathVariable String sala,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.debug("🌊 Usuário {} conectando ao stream da sala: {}", userId, sala);
        
        return chatService.streamMensagens(sala, userId)
                .map(event -> ServerSentEvent.<ChatEventDto>builder()
                        .id(event.getId())
                        .event(event.getTipo())
                        .data(event)
                        .build())
                .onBackpressureBuffer(1000)
                .doOnCancel(() -> {
                    logger.debug("❌ Stream cancelado para usuário {} na sala {}", userId, sala);
                    chatService.sairDaSala(sala, userId).subscribe();
                })
                .timeout(Duration.ofMinutes(30)); // Timeout de 30 minutos
    }

    /**
     * Stream completo de eventos (mensagens + presença + moderação)
     */
    @GetMapping(value = "/stream/{sala}/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Stream completo de eventos", description = "Stream SSE com todos os eventos da sala")
    public Flux<ServerSentEvent<ChatEventDto>> streamEventos(
            @PathVariable String sala,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.debug("🌊 Usuário {} conectando ao stream completo da sala: {}", userId, sala);
        
        return chatService.streamEventosCompletos(sala, userId)
                .map(event -> ServerSentEvent.<ChatEventDto>builder()
                        .id(event.getId())
                        .event(event.getTipo())
                        .data(event)
                        .retry(Duration.ofSeconds(5))
                        .build())
                .onBackpressureBuffer(1000)
                .timeout(Duration.ofMinutes(60));
    }

    /**
     * Lista salas disponíveis
     */
    @GetMapping("/salas")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Listar salas", description = "Lista salas de chat disponíveis")
    public Flux<SalaDto> listarSalas(Authentication authentication) {
        
        String userId = authentication.getName();
        logger.debug("🏠 Usuário {} listando salas", userId);
        
        return chatService.listarSalas(userId);
    }

    /**
     * Lista salas públicas (sem autenticação)
     */
    @GetMapping("/salas/publicas")
    @Operation(summary = "Listar salas públicas", description = "Lista salas públicas disponíveis")
    public Flux<SalaDto> listarSalasPublicas() {
        
        logger.debug("🏠 Listando salas públicas");
        
        return chatService.listarSalasPublicas();
    }

    /**
     * Cria nova sala de chat
     */
    @PostMapping("/salas")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Criar sala", description = "Cria uma nova sala de chat")
    public Mono<ResponseEntity<SalaDto>> criarSala(
            @Valid @RequestBody SalaDto sala,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.debug("🏗️ Usuário {} criando sala: {}", userId, sala.getNome());
        
        return chatService.criarSala(sala, userId)
                .map(ResponseEntity::ok)
                .onErrorResume(ex -> {
                    logger.error("❌ Erro ao criar sala: {}", ex.getMessage());
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    /**
     * Lista usuários online em uma sala
     */
    @GetMapping("/online/{sala}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Usuários online", description = "Lista usuários online em uma sala")
    public Flux<UsuarioOnlineDto> usuariosOnline(
            @PathVariable String sala,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.debug("👥 Usuário {} listando usuários online da sala: {}", userId, sala);
        
        return chatService.usuariosOnline(sala);
    }

    /**
     * Entrar em uma sala de chat
     */
    @PostMapping("/entrar/{sala}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Entrar na sala", description = "Entrar em uma sala de chat")
    public Mono<ResponseEntity<Void>> entrarNaSala(
            @PathVariable String sala,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.debug("🚪 Usuário {} entrando na sala: {}", userId, sala);
        
        return chatService.entrarNaSala(sala, userId)
                .then(Mono.just(ResponseEntity.ok().<Void>build()))
                .onErrorResume(ex -> {
                    logger.error("❌ Erro ao entrar na sala: {}", ex.getMessage());
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    /**
     * Sair de uma sala de chat
     */
    @DeleteMapping("/sair/{sala}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Sair da sala", description = "Sair de uma sala de chat")
    public Mono<ResponseEntity<Void>> sairDaSala(
            @PathVariable String sala,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.debug("🚪 Usuário {} saindo da sala: {}", userId, sala);
        
        return chatService.sairDaSala(sala, userId)
                .then(Mono.just(ResponseEntity.ok().<Void>build()));
    }

    /**
     * Atualizar heartbeat (manter usuário online)
     */
    @PutMapping("/heartbeat")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Atualizar heartbeat", description = "Mantém o usuário como online")
    public Mono<ResponseEntity<Void>> atualizarHeartbeat(Authentication authentication) {
        
        String userId = authentication.getName();
        logger.trace("💓 Heartbeat do usuário: {}", userId);
        
        return chatService.atualizarHeartbeat(userId)
                .then(Mono.just(ResponseEntity.ok().<Void>build()));
    }

    /**
     * Editar mensagem (apenas próprio autor ou moderadores)
     */
    @PutMapping("/mensagem/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Editar mensagem", description = "Edita uma mensagem existente")
    public Mono<ResponseEntity<MensagemDto>> editarMensagem(
            @PathVariable String id,
            @Valid @RequestBody String novoConteudo,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.debug("✏️ Usuário {} editando mensagem: {}", userId, id);
        
        return chatService.editarMensagem(id, novoConteudo, userId)
                .map(ResponseEntity::ok)
                .onErrorResume(ex -> {
                    logger.error("❌ Erro ao editar mensagem: {}", ex.getMessage());
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    /**
     * Excluir mensagem (apenas próprio autor ou moderadores)
     */
    @DeleteMapping("/mensagem/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Excluir mensagem", description = "Exclui uma mensagem")
    public Mono<ResponseEntity<Void>> excluirMensagem(
            @PathVariable String id,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.debug("🗑️ Usuário {} excluindo mensagem: {}", userId, id);
        
        return chatService.excluirMensagem(id, userId)
                .then(Mono.just(ResponseEntity.ok().<Void>build()))
                .onErrorResume(ex -> {
                    logger.error("❌ Erro ao excluir mensagem: {}", ex.getMessage());
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    /**
     * Stream global de presença de usuários
     */
    @GetMapping(value = "/stream/presenca", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Stream de presença", description = "Stream global de usuários online/offline")
    public Flux<ServerSentEvent<UsuarioOnlineDto>> streamPresenca(Authentication authentication) {
        
        String userId = authentication.getName();
        logger.debug("👥 Usuário {} conectando ao stream de presença", userId);
        
        return chatService.streamPresencaGlobal()
                .map(presenca -> ServerSentEvent.<UsuarioOnlineDto>builder()
                        .id(presenca.getUsuarioId())
                        .event("user_" + presenca.getStatus().name().toLowerCase())
                        .data(presenca)
                        .build())
                .onBackpressureBuffer(500)
                .timeout(Duration.ofHours(1));
    }

    /**
     * Obtém estatísticas de uma sala (moderadores apenas)
     */
    @GetMapping("/salas/{sala}/estatisticas")
    @PreAuthorize("hasRole('CHAT_MODERATOR') or hasRole('ADMIN')")
    @Operation(summary = "Estatísticas da sala", description = "Obtém estatísticas detalhadas da sala")
    public Mono<ResponseEntity<Map<String, Object>>> obterEstatisticas(
            @PathVariable String sala,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.debug("📊 Moderador {} solicitando estatísticas da sala: {}", userId, sala);
        
        return chatService.obterEstatisticasSala(sala)
                .map(ResponseEntity::ok)
                .onErrorResume(ex -> {
                    logger.error("❌ Erro ao obter estatísticas: {}", ex.getMessage());
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    /**
     * Health check do serviço
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Verifica saúde do serviço")
    public Mono<ResponseEntity<Map<String, Object>>> healthCheck() {
        
        return Mono.just(ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "chat-microservice",
            "timestamp", java.time.Instant.now(),
            "version", "1.0-production-ready",
            "features", Map.of(
                "sse", true,
                "websocket", true,
                "redis_streams", true,
                "jwt_auth", true,
                "rate_limiting", true
            )
        )));
    }
}