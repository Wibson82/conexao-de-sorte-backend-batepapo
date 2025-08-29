package br.tec.facilitaservicos.batepapo.websocket.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * DTO for WebSocket chat messages
 * Follows AGENTS.md guidelines - Java 24, Spring Boot 3.5.5
 */
public class ChatMessageDTO {
    
    private String conteudo;
    private String salaId;
    private String tipo;
    
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