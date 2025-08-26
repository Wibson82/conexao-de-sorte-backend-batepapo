package br.tec.facilitaservicos.batepapo.apresentacao.controlador;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.tec.facilitaservicos.batepapo.aplicacao.servico.ChatService;
import br.tec.facilitaservicos.batepapo.aplicacao.servico.SalaService;
import br.tec.facilitaservicos.batepapo.aplicacao.servico.UsuarioOnlineService;
import br.tec.facilitaservicos.batepapo.apresentacao.dto.ChatEventDto;
import br.tec.facilitaservicos.batepapo.apresentacao.dto.MensagemDto;
import br.tec.facilitaservicos.batepapo.apresentacao.dto.SalaDto;
import br.tec.facilitaservicos.batepapo.apresentacao.dto.UsuarioOnlineDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * ============================================================================
 * 💬 CONTROLADOR REATIVO - BATE-PAPO
 * ============================================================================
 * 
 * Controlador 100% reativo para operações de chat usando WebFlux
 * 
 * Endpoints disponíveis:
 * - POST /api/chat/mensagem - Enviar mensagem
 * - GET /api/chat/mensagens/{sala} - Histórico paginado
 * - GET /api/chat/salas - Listar salas disponíveis
 * - POST /api/chat/salas - Criar nova sala
 * - GET /api/chat/online/{sala} - Usuários online
 * - POST /api/chat/entrar/{sala} - Entrar em sala
 * - DELETE /api/chat/sair/{sala} - Sair de sala
 * - PUT /api/chat/heartbeat - Atualizar heartbeat
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat", description = "API para operações de bate-papo em tempo real")
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    private final ChatService chatService;
    private final SalaService salaService;
    private final UsuarioOnlineService usuarioOnlineService;

    @Value("${pagination.default-size:20}")
    private int tamanhoDefault;

    public ChatController(ChatService chatService, SalaService salaService, UsuarioOnlineService usuarioOnlineService) {
        this.chatService = chatService;
        this.salaService = salaService;
        this.usuarioOnlineService = usuarioOnlineService;
    }

    @Operation(summary = "Enviar mensagem", 
               description = "Envia uma nova mensagem para uma sala de chat")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Mensagem enviada com sucesso",
                    content = @Content(schema = @Schema(implementation = MensagemDto.class))),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Não autorizado"),
        @ApiResponse(responseCode = "403", description = "Acesso negado à sala"),
        @ApiResponse(responseCode = "429", description = "Muitas mensagens enviadas")
    })
    @PostMapping(value = "/mensagem", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<MensagemDto>> enviarMensagem(
            @RequestBody @Valid MensagemDto mensagemDto,
            Authentication authentication
    ) {
        Long usuarioId = extrairUsuarioId(authentication);
        String usuarioNome = authentication.getName();
        
        return chatService.enviarMensagem(mensagemDto.conteudo(), usuarioId, usuarioNome, 
                                         mensagemDto.sala(), mensagemDto.respostaParaId())
            .map(mensagem -> ResponseEntity.status(201).body(mensagem));
    }

    @Operation(summary = "Histórico de mensagens", 
               description = "Busca histórico paginado de mensagens de uma sala")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Histórico recuperado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Sala não encontrada")
    })
    @GetMapping(value = "/mensagens/{sala}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<MensagemDto> buscarMensagensSala(
            @Parameter(description = "Nome da sala", example = "geral")
            @PathVariable String sala,
            
            @Parameter(description = "Número da página", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            
            @Parameter(description = "Tamanho da página", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho,
            
            @Parameter(description = "Data de início (ISO)", example = "2024-01-01T00:00:00")
            @RequestParam(required = false) LocalDateTime dataInicio,
            
            @Parameter(description = "Data de fim (ISO)", example = "2024-01-31T23:59:59")
            @RequestParam(required = false) LocalDateTime dataFim
    ) {
        return chatService.buscarMensagensSala(sala, pagina, tamanho, dataInicio, dataFim);
    }

    @Operation(summary = "Listar salas disponíveis", 
               description = "Lista todas as salas de chat disponíveis para o usuário")
    @GetMapping(value = "/salas", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<SalaDto> listarSalas(
            @Parameter(description = "Filtro por tipo de sala")
            @RequestParam(required = false) String tipo,
            
            @Parameter(description = "Apenas salas ativas", example = "true")
            @RequestParam(defaultValue = "true") boolean apenasAtivas,
            
            @Parameter(description = "Incluir salas vazias", example = "false")
            @RequestParam(defaultValue = "false") boolean incluirVazias
    ) {
        return salaService.listarSalas(tipo, apenasAtivas, incluirVazias);
    }

    @Operation(summary = "Criar nova sala", 
               description = "Cria uma nova sala de chat")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Sala criada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "409", description = "Nome da sala já existe")
    })
    @PostMapping(value = "/salas", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<SalaDto>> criarSala(
            @RequestBody @Valid SalaDto salaDto,
            Authentication authentication
    ) {
        Long usuarioId = extrairUsuarioId(authentication);
        
        return salaService.criarSala(salaDto, usuarioId)
            .map(sala -> ResponseEntity.status(201).body(sala));
    }

    @Operation(summary = "Usuários online na sala", 
               description = "Lista usuários atualmente online em uma sala")
    @GetMapping(value = "/online/{sala}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<UsuarioOnlineDto> usuariosOnlineSala(
            @Parameter(description = "Nome da sala", example = "geral")
            @PathVariable String sala,
            
            @Parameter(description = "Incluir usuários ausentes", example = "false")
            @RequestParam(defaultValue = "false") boolean incluirAusentes
    ) {
        return usuarioOnlineService.buscarUsuariosOnlineSala(sala, incluirAusentes);
    }

    @Operation(summary = "Entrar em sala", 
               description = "Registra entrada do usuário em uma sala")
    @PostMapping(value = "/entrar/{sala}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<UsuarioOnlineDto>> entrarSala(
            @Parameter(description = "Nome da sala", example = "geral")
            @PathVariable String sala,
            
            Authentication authentication
    ) {
        Long usuarioId = extrairUsuarioId(authentication);
        String usuarioNome = authentication.getName();
        
        return usuarioOnlineService.entrarSala(usuarioId, usuarioNome, sala)
            .map(usuario -> ResponseEntity.ok(usuario));
    }

    @Operation(summary = "Sair de sala", 
               description = "Registra saída do usuário de uma sala")
    @DeleteMapping("/sair/{sala}")
    public Mono<ResponseEntity<Void>> sairSala(
            @Parameter(description = "Nome da sala", example = "geral")
            @PathVariable String sala,
            
            Authentication authentication
    ) {
        Long usuarioId = extrairUsuarioId(authentication);
        
        return usuarioOnlineService.sairSala(usuarioId, sala)
            .then(Mono.just(ResponseEntity.noContent().build()));
    }

    @Operation(summary = "Atualizar heartbeat", 
               description = "Atualiza heartbeat para manter conexão ativa")
    @PutMapping("/heartbeat")
    public Mono<ResponseEntity<Void>> atualizarHeartbeat(
            @Parameter(description = "Salas ativas do usuário")
            @RequestParam(required = false) String[] salas,
            
            Authentication authentication
    ) {
        Long usuarioId = extrairUsuarioId(authentication);
        
        return usuarioOnlineService.atualizarHeartbeat(usuarioId, salas)
            .then(Mono.just(ResponseEntity.ok().build()));
    }

    @Operation(summary = "Buscar mensagem específica", 
               description = "Busca uma mensagem específica por ID")
    @GetMapping(value = "/mensagem/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<MensagemDto>> buscarMensagem(
            @Parameter(description = "ID da mensagem")
            @PathVariable Long id
    ) {
        return chatService.buscarMensagemPorId(id)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Editar mensagem", 
               description = "Edita o conteúdo de uma mensagem existente")
    @PutMapping(value = "/mensagem/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<MensagemDto>> editarMensagem(
            @Parameter(description = "ID da mensagem")
            @PathVariable Long id,
            
            @RequestBody @Valid MensagemDto mensagemDto,
            Authentication authentication
    ) {
        Long usuarioId = extrairUsuarioId(authentication);
        
        return chatService.editarMensagem(id, mensagemDto.conteudo(), usuarioId)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Excluir mensagem", 
               description = "Exclui uma mensagem (apenas o autor ou moderador)")
    @DeleteMapping("/mensagem/{id}")
    public Mono<ResponseEntity<Void>> excluirMensagem(
            @Parameter(description = "ID da mensagem")
            @PathVariable Long id,
            
            Authentication authentication
    ) {
        Long usuarioId = extrairUsuarioId(authentication);
        
        return chatService.excluirMensagem(id, usuarioId)
            .then(Mono.just(ResponseEntity.noContent().build()))
            .onErrorReturn(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Status da sala", 
               description = "Obtém informações detalhadas sobre uma sala")
    @GetMapping(value = "/sala/{nome}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Object>> statusSala(
            @Parameter(description = "Nome da sala")
            @PathVariable String nome
    ) {
        return salaService.obterStatusSala(nome)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // Métodos auxiliares

    /**
     * Extrai ID do usuário do contexto de autenticação
     */
    private Long extrairUsuarioId(Authentication authentication) {
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            return (long) authentication.getName().hashCode();
        }
    }
}