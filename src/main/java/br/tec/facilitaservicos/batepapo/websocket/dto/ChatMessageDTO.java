package br.tec.facilitaservicos.batepapo.websocket.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * DTO para mensagens de chat via WebSocket.
 * 
 * Utilizado para transmissão de mensagens em tempo real através
 * de conexões WebSocket, otimizado para baixa latência e alta
 * frequência de mensagens.
 * 
 * Principais casos de uso:
 * - Envio de mensagens em tempo real via WebSocket
 * - Notificações instantâneas de eventos de chat
 * - Broadcasting de mensagens para múltiplos usuários
 * - Sincronização de estado entre clientes conectados
 * 
 * Características de performance:
 * - Estrutura minimalista para reduzir overhead de rede
 * - Serialização otimizada para JSON
 * - Campos essenciais apenas para reduzir payload
 * - Timestamps precisos para sincronização
 * 
 * Restrições técnicas:
 * - Conteúdo limitado para evitar sobrecarga de rede
 * - Timestamps em formato padronizado
 * - Tipos de mensagem pré-definidos
 * - Validação mínima para performance
 * 
 * Relacionamentos:
 * - Converte para/de MensagemDto para persistência
 * - Integra com sistema de autenticação WebSocket
 * - Conecta com controle de salas e presença
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mensagem de chat otimizada para transmissão via WebSocket")
public class ChatMessageDTO {
    
    @Schema(description = "Conteúdo textual da mensagem", 
            example = "Olá pessoal!",
            required = true,
            maxLength = 500)
    @NotBlank(message = "Conteúdo da mensagem é obrigatório")
    @Size(max = 500, message = "Conteúdo não pode exceder 500 caracteres")
    private String conteudo;
    
    @Schema(description = "Identificador da sala de destino", 
            example = "sala-geral",
            required = true)
    @NotBlank(message = "ID da sala é obrigatório")
    private String salaId;
    
    @Schema(description = "Tipo da mensagem WebSocket", 
            example = "MESSAGE",
            allowableValues = {"MESSAGE", "JOIN", "LEAVE", "TYPING", "SYSTEM"})
    private String tipo;
    
    @Schema(description = "Timestamp preciso da mensagem", 
            example = "2025-09-01T16:30:45")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    public ChatMessageDTO() {
    }
    
    public ChatMessageDTO(String conteudo, String salaId, String tipo, LocalDateTime timestamp) {
        this.conteudo = conteudo;
        this.salaId = salaId;
        this.tipo = tipo;
        this.timestamp = timestamp;
    }
    
    public String getConteudo() {
        return conteudo;
    }
    
    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }
    
    public String getSalaId() {
        return salaId;
    }
    
    public void setSalaId(String salaId) {
        this.salaId = salaId;
    }
    
    public String getTipo() {
        return tipo;
    }
    
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}