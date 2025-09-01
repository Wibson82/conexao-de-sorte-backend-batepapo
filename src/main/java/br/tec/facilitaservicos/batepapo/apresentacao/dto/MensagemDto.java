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
 * DTO reativo para Mensagem de Chat
 * 
 * @param id Identificador único da mensagem
 * @param conteudo Conteúdo da mensagem
 * @param usuarioId ID do usuário que enviou
 * @param usuarioNome Nome do usuário que enviou
 * @param sala Nome da sala
 * @param tipo Tipo da mensagem
 * @param status Status da mensagem
 * @param respostaParaId ID da mensagem sendo respondida (opcional)
 * @param editada Se a mensagem foi editada
 * @param dataEnvio Data e hora do envio
 * @param dataEdicao Data e hora da edição (opcional)
 * @param dataCriacao Data de criação do registro
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Data Transfer Object for Chat Message")
public record MensagemDto(
    @Schema(description = "Unique identifier of the message", example = "1")
    Long id,
    
    @Schema(description = "Content of the message", required = true, example = "Hello, world!")
    @NotBlank(message = "Conteúdo da mensagem é obrigatório")
    @Size(max = 500, message = "Mensagem não pode exceder 500 caracteres")
    String conteudo,
    
    @Schema(description = "Identifier of the user who sent the message", required = true, example = "123")
    @NotNull(message = "ID do usuário é obrigatório")
    Long usuarioId,
    
    @Schema(description = "Name of the user who sent the message", required = true, example = "John Doe")
    @NotBlank(message = "Nome do usuário é obrigatório")
    String usuarioNome,
    
    @Schema(description = "Name of the room where the message was sent", required = true, example = "general")
    @NotBlank(message = "Sala é obrigatória")
    String sala,
    
    @Schema(description = "Type of the message", example = "TEXTO")
    TipoMensagem tipo,
    @Schema(description = "Status of the message", example = "ENVIADA")
    StatusMensagem status,
    @Schema(description = "Identifier of the message being replied to", example = "456")
    Long respostaParaId,
    @Schema(description = "Indicates if the message has been edited", example = "false")
    Boolean editada,
    
    @Schema(description = "Date and time when the message was sent", example = "2025-01-01T12:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime dataEnvio,
    
    @Schema(description = "Date and time when the message was edited", example = "2025-01-01T12:01:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime dataEdicao,
    
    @Schema(description = "Date and time when the message record was created", example = "2025-01-01T12:00:00")
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