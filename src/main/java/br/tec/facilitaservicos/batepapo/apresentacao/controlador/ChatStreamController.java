package br.tec.facilitaservicos.batepapo.apresentacao.controlador;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.tec.facilitaservicos.batepapo.aplicacao.servico.ChatStreamService;
import br.tec.facilitaservicos.batepapo.apresentacao.dto.ChatEventDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Flux;

/**
 * ============================================================================
 * 📡 CONTROLADOR SSE - STREAM DE CHAT EM TEMPO REAL
 * ============================================================================
 * 
 * Controlador 100% reativo para Server-Sent Events (SSE) do chat usando WebFlux
 * 
 * Endpoints disponíveis:
 * - GET /api/chat/stream/{sala} - Stream SSE de mensagens da sala
 * - GET /api/chat/stream/{sala}/eventos - Stream de todos os eventos
 * - GET /api/chat/heartbeat - Heartbeat para manter conexões ativas
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat Stream", description = "API para streaming em tempo real do chat via SSE")
@SecurityRequirement(name = "bearerAuth")
public class ChatStreamController {

    private final ChatStreamService chatStreamService;

    @Value("${chat.heartbeat-interval-seconds:30}")
    private int heartbeatInterval;

    public ChatStreamController(ChatStreamService chatStreamService) {
        this.chatStreamService = chatStreamService;
    }

    @Operation(summary = "Stream SSE de mensagens da sala", 
               description = "Conecta ao stream de mensagens em tempo real de uma sala específica")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stream conectado com sucesso",
                    content = @Content(mediaType = "text/event-stream")),
        @ApiResponse(responseCode = "401", description = "Não autorizado"),
        @ApiResponse(responseCode = "403", description = "Acesso negado à sala"),
        @ApiResponse(responseCode = "404", description = "Sala não encontrada")
    })
    @GetMapping(value = "/stream/{sala}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatEventDto>> streamMensagensSala(
            @Parameter(description = "Nome da sala", example = "geral")
            @PathVariable String sala,
            
            @Parameter(description = "Incluir histórico recente", example = "true")
            @RequestParam(defaultValue = "false") boolean incluirHistorico,
            
            Authentication authentication
    ) {
        String usuarioNome = authentication.getName();
        Long usuarioId = extrairUsuarioId(authentication);
        
        // Stream de mensagens da sala com heartbeat
        Flux<ChatEventDto> mensagens = chatStreamService.streamMensagensSala(sala, usuarioId, incluirHistorico);
        Flux<ChatEventDto> heartbeat = Flux.interval(Duration.ofSeconds(heartbeatInterval))
            .map(tick -> ChatEventDto.heartbeat(sala));
        
        return Flux.merge(mensagens, heartbeat)
            .map(evento -> ServerSentEvent.<ChatEventDto>builder()
                .id(evento.timestamp().toString())
                .event(evento.tipo().name().toLowerCase())
                .data(evento)
                .build())
            .doOnSubscribe(subscription -> 
                chatStreamService.adicionarConexaoSSE(sala, usuarioId, usuarioNome))
            .doOnCancel(() -> 
                chatStreamService.removerConexaoSSE(sala, usuarioId))
            .doOnTerminate(() -> 
                chatStreamService.removerConexaoSSE(sala, usuarioId));
    }

    @Operation(summary = "Stream completo de eventos da sala", 
               description = "Stream com todos os tipos de eventos: mensagens, usuários, etc.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stream de eventos conectado",
                    content = @Content(mediaType = "text/event-stream")),
        @ApiResponse(responseCode = "401", description = "Não autorizado")
    })
    @GetMapping(value = "/stream/{sala}/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatEventDto>> streamEventosSala(
            @Parameter(description = "Nome da sala", example = "geral")
            @PathVariable String sala,
            
            Authentication authentication
    ) {
        String usuarioNome = authentication.getName();
        Long usuarioId = extrairUsuarioId(authentication);
        
        // Stream completo de eventos com heartbeat
        Flux<ChatEventDto> eventos = chatStreamService.streamEventosSala(sala, usuarioId);
        Flux<ChatEventDto> heartbeat = Flux.interval(Duration.ofSeconds(heartbeatInterval))
            .map(tick -> ChatEventDto.heartbeat(sala));
        
        return Flux.merge(eventos, heartbeat)
            .map(evento -> ServerSentEvent.<ChatEventDto>builder()
                .id(evento.timestamp().toString())
                .event(evento.tipo().name().toLowerCase())
                .data(evento)
                .build())
            .doOnSubscribe(subscription -> 
                chatStreamService.adicionarConexaoSSE(sala, usuarioId, usuarioNome))
            .doOnCancel(() -> 
                chatStreamService.removerConexaoSSE(sala, usuarioId))
            .doOnTerminate(() -> 
                chatStreamService.removerConexaoSSE(sala, usuarioId));
    }

    @Operation(summary = "Stream global de presença", 
               description = "Stream de eventos de presença de usuários em todas as salas")
    @GetMapping(value = "/stream/presenca", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatEventDto>> streamPresencaGlobal(
            Authentication authentication
    ) {
        Long usuarioId = extrairUsuarioId(authentication);
        
        Flux<ChatEventDto> presenca = chatStreamService.streamPresencaGlobal(usuarioId);
        Flux<ChatEventDto> heartbeat = Flux.interval(Duration.ofSeconds(heartbeatInterval * 2))
            .map(tick -> ChatEventDto.heartbeat("global"));
        
        return Flux.merge(presenca, heartbeat)
            .map(evento -> ServerSentEvent.<ChatEventDto>builder()
                .id(evento.timestamp().toString())
                .event(evento.tipo().name().toLowerCase())
                .data(evento)
                .build());
    }

    @Operation(summary = "Heartbeat para conexão", 
               description = "Endpoint para manter conexões SSE ativas")
    @GetMapping("/heartbeat")
    public Flux<ServerSentEvent<String>> heartbeat() {
        return Flux.interval(Duration.ofSeconds(heartbeatInterval))
            .map(tick -> ServerSentEvent.<String>builder()
                .event("heartbeat")
                .data("ping")
                .build());
    }

    @Operation(summary = "Status das conexões", 
               description = "Retorna informações sobre conexões SSE ativas")
    @GetMapping("/stream/status")
    public Flux<ServerSentEvent<Object>> statusConexoes(Authentication authentication) {
        Long usuarioId = extrairUsuarioId(authentication);
        
        return chatStreamService.streamStatusConexoes(usuarioId)
            .map(status -> ServerSentEvent.builder()
                .event("status")
                .data(status)
                .build());
    }

    // Métodos auxiliares

    /**
     * Extrai ID do usuário do contexto de autenticação
     */
    private Long extrairUsuarioId(Authentication authentication) {
        try {
            // Assumindo que o subject do JWT contém o ID do usuário
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            // Se não conseguir converter, usar hash do nome como ID temporário
            return (long) authentication.getName().hashCode();
        }
    }
}