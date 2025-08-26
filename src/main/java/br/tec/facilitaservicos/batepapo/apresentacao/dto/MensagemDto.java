package br.tec.facilitaservicos.batepapo.apresentacao.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import br.tec.facilitaservicos.batepapo.dominio.enums.StatusMensagem;
import br.tec.facilitaservicos.batepapo.dominio.enums.TipoMensagem;
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
public record MensagemDto(
    Long id,
    
    @NotBlank(message = "Conteúdo da mensagem é obrigatório")
    @Size(max = 500, message = "Mensagem não pode exceder 500 caracteres")
    String conteudo,
    
    @NotNull(message = "ID do usuário é obrigatório")
    Long usuarioId,
    
    @NotBlank(message = "Nome do usuário é obrigatório")
    String usuarioNome,
    
    @NotBlank(message = "Sala é obrigatória")
    String sala,
    
    TipoMensagem tipo,
    StatusMensagem status,
    Long respostaParaId,
    Boolean editada,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime dataEnvio,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime dataEdicao,
    
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