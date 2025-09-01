package br.tec.facilitaservicos.batepapo.apresentacao.controlador;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import br.tec.facilitaservicos.batepapo.aplicacao.servico.ChatService;
import br.tec.facilitaservicos.batepapo.apresentacao.dto.MensagemDtoSimples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Controlador de Chat
 * 
 * Endpoints essenciais:
 * - POST /rest/v1/chat/mensagens - Enviar mensagem
 * - GET /rest/v1/chat/salas/{sala}/mensagens - Listar mensagens
 * - GET /rest/v1/chat/salas/{sala}/status - Status da sala
 */
@RestController
@RequestMapping("/rest/v1/chat")
@Tag(name = "Chat", description = "API para chat em tempo real")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Envia uma mensagem de chat
     */
    @PostMapping(value = "/mensagens", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Enviar mensagem", description = "Envia uma nova mensagem para o chat")
    public Mono<ResponseEntity<MensagemDtoSimples>> enviarMensagem(
            @Valid @RequestBody MensagemDtoSimples mensagem) {

        logger.debug("💬 Recebida solicitação para enviar mensagem: sala={}", mensagem.sala());

        return chatService.enviarMensagem(mensagem)
            .map(ResponseEntity::ok)
            .onErrorReturn(ResponseEntity.badRequest().build());
    }

    /**
     * Lista mensagens de uma sala
     */
    @GetMapping(value = "/salas/{sala}/mensagens", produces = MediaType.APPLICATION_NDJSON_VALUE)
    @Operation(summary = "Listar mensagens", description = "Lista mensagens recentes de uma sala")
    public Flux<MensagemDtoSimples> listarMensagens(
            @PathVariable String sala,
            @RequestParam(defaultValue = "50") int limite) {

        logger.debug("📜 Solicitação para listar mensagens: sala={}, limite={}", sala, limite);

        return chatService.listarMensagens(sala, limite);
    }

    /**
     * Obtém status de uma sala (quantidade de mensagens)
     */
    @GetMapping(value = "/salas/{sala}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Status da sala", description = "Obtém informações sobre a sala")
    public Mono<ResponseEntity<Object>> obterStatusSala(@PathVariable String sala) {

        logger.debug("📊 Solicitação de status da sala: {}", sala);

        return chatService.contarMensagens(sala)
            .map(count -> ResponseEntity.ok().body((Object) 
                java.util.Map.of(
                    "sala", sala,
                    "totalMensagens", count,
                    "status", "ativa",
                    "timestamp", java.time.Instant.now()
                )
            ))
            .onErrorReturn(ResponseEntity.internalServerError().build());
    }

    /**
     * Health check do serviço de chat
     */
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Health check", description = "Verifica saúde do serviço de chat")
    public Mono<ResponseEntity<Object>> healthCheck() {
        
        return Mono.just(ResponseEntity.ok().body(
            java.util.Map.of(
                "status", "UP",
                "service", "chat-simples",
                "timestamp", java.time.Instant.now(),
                "version", "1.0-mvp"
            )
        ));
    }
}