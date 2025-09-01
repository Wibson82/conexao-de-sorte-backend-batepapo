package br.tec.facilitaservicos.batepapo.websocket.dto;

import br.tec.facilitaservicos.batepapo.apresentacao.dto.MensagemDtoSimples;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO para respostas WebSocket padronizadas.
 * 
 * Fornece uma estrutura consistente para todas as respostas
 * enviadas através de conexões WebSocket, facilitando o
 * tratamento no lado cliente e debugging.
 * 
 * Principais casos de uso:
 * - Respostas de sucesso/erro para ações WebSocket
 * - Notificações de eventos em tempo real
 * - Broadcasting de mudanças de presença
 * - Confirmações de recebimento de mensagens
 * 
 * Tipos de resposta suportados:
 * - success: Operação realizada com sucesso
 * - error: Erro durante processamento
 * - message: Nova mensagem recebida
 * - presence: Mudança de presença de usuário
 * - notification: Notificação geral
 * 
 * Estrutura padronizada:
 * - type: Identifica o tipo de evento
 * - data: Payload específico do evento
 * - status: Status da operação (ok, error)
 * - message: Mensagem legível para humanos
 * - timestamp: Momento exato do evento
 * 
 * Relacionamentos:
 * - Integra com todos os eventos do sistema de chat
 * - Conecta com sistema de notificações
 * - Suporta diferentes tipos de dados através de Object
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Resposta padronizada para comunicação via WebSocket")
public class WebSocketResponseDTO {
    
    @Schema(description = "Tipo do evento WebSocket", 
            example = "message",
            allowableValues = {"success", "error", "message", "presence", "notification", "typing"})
    private String type;
    
    @Schema(description = "Dados específicos do evento (estrutura varia por tipo)", 
            example = "{\"conteudo\": \"Hello!\", \"usuario\": \"João\"}")
    private Object data;
    
    @Schema(description = "Status da operação", 
            example = "ok",
            allowableValues = {"ok", "error", "warning"})
    private String status;
    
    @Schema(description = "Mensagem descritiva para o usuário", 
            example = "Mensagem enviada com sucesso")
    private String message;
    
    @Schema(description = "Timestamp preciso do evento", 
            example = "2025-09-01T16:30:45")
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