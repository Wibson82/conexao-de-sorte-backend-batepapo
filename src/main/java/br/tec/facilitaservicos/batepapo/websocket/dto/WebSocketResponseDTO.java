package br.tec.facilitaservicos.batepapo.websocket.dto;

import br.tec.facilitaservicos.batepapo.apresentacao.dto.MensagemDtoSimples;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for WebSocket responses
 * Follows AGENTS.md guidelines - Java 24, Spring Boot 3.5.5
 */
public class WebSocketResponseDTO {
    
    private String type;
    private Object data;
    private String status;
    private String message;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    public WebSocketResponseDTO() {
    }
    
    private WebSocketResponseDTO(String type, Object data, String status, String message, LocalDateTime timestamp) {
        this.type = type;
        this.data = data;
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
    }
    
    public static WebSocketResponseDTO success(String message, Object data) {
        return new WebSocketResponseDTO("success", data, "ok", message, LocalDateTime.now());
    }
    
    public static WebSocketResponseDTO error(String message, LocalDateTime timestamp) {
        return new WebSocketResponseDTO("error", null, "error", message, timestamp);
    }
    
    public static WebSocketResponseDTO message(MensagemDtoSimples mensagem) {
        return new WebSocketResponseDTO("message", mensagem, "ok", "Nova mensagem", LocalDateTime.now());
    }
    
    public static WebSocketResponseDTO presence(String userId, String action, String roomId, LocalDateTime timestamp) {
        Map<String, Object> presenceData = Map.of(
            "userId", userId,
            "action", action,
            "roomId", roomId
        );
        return new WebSocketResponseDTO("presence", presenceData, "ok", "Presença atualizada", timestamp);
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Object getData() {
        return data;
    }
    
    public void setData(Object data) {
        this.data = data;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}