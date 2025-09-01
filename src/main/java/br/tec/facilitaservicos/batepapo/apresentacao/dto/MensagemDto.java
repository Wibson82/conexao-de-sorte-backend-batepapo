package br.tec.facilitaservicos.batepapo.apresentacao.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import br.tec.facilitaservicos.batepapo.dominio.enums.StatusMensagem;
import br.tec.facilitaservicos.batepapo.dominio.enums.TipoMensagem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO reativo para Mensagem de Chat.
 * 
 * Representa uma mensagem enviada em uma sala de bate-papo, incluindo
 * todas as informações necessárias para exibição, processamento e
 * rastreamento de mensagens em tempo real.
 * 
 * Principais casos de uso:
 * - Envio e recebimento de mensagens via WebSocket
 * - Exibição de histórico de conversas
 * - Gerenciamento de respostas e threads
 * - Notificações em tempo real
 * 
 * Funcionalidades suportadas:
 * - Mensagens de texto, sistema e especiais
 * - Sistema de respostas e citações
 * - Edição de mensagens enviadas
 * - Tracking de status de entrega
 * - Timestamps para auditoria
 * 
 * Restrições de negócio:
 * - Conteúdo limitado a 500 caracteres para performance
 * - Apenas o autor pode editar mensagens
 * - Mensagens do sistema têm usuarioId = -1
 * - Status controlado automaticamente pelo sistema
 * 
 * Relacionamentos:
 * - Conecta com microserviço de usuários para validação
 * - Integra com sistema de notificações para alerts
 * - Vincula com salas para controle de acesso
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mensagem de bate-papo com informações completas para comunicação em tempo real")
public record MensagemDto(
    @Schema(description = "Identificador único da mensagem", 
            example = "15847")
    Long id,
    
    @Schema(description = "Conteúdo textual da mensagem", 
            example = "Olá pessoal! Como estão hoje?",
            requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = 500)
    @NotBlank(message = "Conteúdo da mensagem é obrigatório")
    @Size(max = 500, message = "Mensagem não pode exceder 500 caracteres")
    String conteudo,
    
    @Schema(description = "Identificador do usuário que enviou a mensagem", 
            example = "12345",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "ID do usuário é obrigatório")
    Long usuarioId,
    
    @Schema(description = "Nome de exibição do usuário remetente", 
            example = "João Silva",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Nome do usuário é obrigatório")
    String usuarioNome,
    
    @Schema(description = "Identificador da sala onde a mensagem foi enviada", 
            example = "sala-geral",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Sala é obrigatória")
    String sala,
    
    @Schema(description = "Tipo da mensagem", 
            example = "TEXTO",
            allowableValues = {"TEXTO", "SISTEMA", "IMAGEM", "ARQUIVO", "ENTRADA", "SAIDA"})
    TipoMensagem tipo,
    
    @Schema(description = "Status atual da mensagem", 
            example = "ENVIADA",
            allowableValues = {"ENVIADA", "ENTREGUE", "LIDA", "FALHOU", "PENDENTE"})
    StatusMensagem status,
    
    @Schema(description = "ID da mensagem que está sendo respondida (para threads)", 
            example = "15832")
    Long respostaParaId,
    
    @Schema(description = "Indica se a mensagem foi editada após o envio", 
            example = "false")
    Boolean editada,
    
    @Schema(description = "Data e hora de envio da mensagem", 
            example = "2025-09-01T14:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime dataEnvio,
    
    @Schema(description = "Data e hora da última edição (se aplicável)", 
            example = "2025-09-01T14:32:15")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime dataEdicao,
    
    @Schema(description = "Data e hora de criação do registro no banco", 
            example = "2025-09-01T14:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime dataCriacao
) {
    
    /**
     * Cria DTO com dados básicos para envio
     */
    public static MensagemDto paraEnvio(String conteudo, Long usuarioId, String usuarioNome, String sala) {
        return new MensagemDto(
            null, conteudo, usuarioId, usuarioNome, sala,
            TipoMensagem.TEXTO, StatusMensagem.ENVIADA, null, false,
            LocalDateTime.now(), null, LocalDateTime.now()
        );
    }
    
    /**
     * Cria DTO de mensagem do sistema
     */
    public static MensagemDto sistema(String conteudo, String sala) {
        return new MensagemDto(
            null, conteudo, -1L, "Sistema", sala,
            TipoMensagem.SISTEMA, StatusMensagem.ENVIADA, null, false,
            LocalDateTime.now(), null, LocalDateTime.now()
        );
    }
    
    /**
     * Cria DTO completo
     */
    public static MensagemDto completa(Long id, String conteudo, Long usuarioId, String usuarioNome, 
                                      String sala, TipoMensagem tipo, StatusMensagem status, 
                                      Long respostaParaId, Boolean editada, LocalDateTime dataEnvio,
                                      LocalDateTime dataEdicao, LocalDateTime dataCriacao) {
        return new MensagemDto(id, conteudo, usuarioId, usuarioNome, sala, tipo, status,
                              respostaParaId, editada, dataEnvio, dataEdicao, dataCriacao);
    }
    
    /**
     * Verifica se é mensagem do usuário (não sistema)
     */
    public boolean isMensagemUsuario() {
        return tipo != null && tipo.isMensagemUsuario();
    }
    
    /**
     * Verifica se é mensagem do sistema
     */
    public boolean isMensagemSistema() {
        return tipo != null && tipo.isMensagemSistema();
    }
    
    /**
     * Verifica se é uma resposta
     */
    public boolean isResposta() {
        return respostaParaId != null;
    }
    
    /**
     * Verifica se foi editada
     */
    public boolean foiEditada() {
        return Boolean.TRUE.equals(editada);
    }
    
    /**
     * Verifica se é mensagem recente (últimos 5 minutos)
     */
    public boolean isRecente() {
        return dataEnvio != null && 
               dataEnvio.isAfter(LocalDateTime.now().minusMinutes(5));
    }
    
    /**
     * Obtém ícone do tipo de mensagem
     */
    public String getIconeTipo() {
        return tipo != null ? tipo.getIcone() : "💬";
    }
    
    /**
     * Obtém ícone do status da mensagem
     */
    public String getIconeStatus() {
        return status != null ? status.getIcone() : "✓";
    }
}