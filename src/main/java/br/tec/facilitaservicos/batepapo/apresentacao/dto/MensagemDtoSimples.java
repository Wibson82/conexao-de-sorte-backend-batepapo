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
 * DTO simplificado para Mensagem de Chat - Versão Mínima Viável
 * Foca apenas nas funcionalidades essenciais do chat.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MensagemDtoSimples(
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
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime dataEnvio
) {
    
    /**
     * Cria DTO com dados básicos para envio
     */
    public static MensagemDtoSimples paraEnvio(String conteudo, Long usuarioId, String usuarioNome, String sala) {
        return new MensagemDtoSimples(
            null, conteudo, usuarioId, usuarioNome, sala,
            TipoMensagem.TEXTO, StatusMensagem.ENVIADA, LocalDateTime.now()
        );
    }
    
    /**
     * Cria DTO de mensagem do sistema
     */
    public static MensagemDtoSimples sistema(String conteudo, String sala) {
        return new MensagemDtoSimples(
            null, conteudo, -1L, "Sistema", sala,
            TipoMensagem.SISTEMA, StatusMensagem.ENVIADA, LocalDateTime.now()
        );
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