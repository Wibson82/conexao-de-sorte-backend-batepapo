package br.tec.facilitaservicos.batepapo.websocket;

import br.tec.facilitaservicos.batepapo.aplicacao.servico.ChatSimplesService;
import br.tec.facilitaservicos.batepapo.apresentacao.dto.MensagemDtoSimples;
import br.tec.facilitaservicos.batepapo.infraestrutura.cache.ReactiveMessageCache;
import br.tec.facilitaservicos.batepapo.infraestrutura.eventos.ChatEventPublisher;
import br.tec.facilitaservicos.batepapo.websocket.dto.ChatMessageDTO;
import br.tec.facilitaservicos.batepapo.websocket.dto.WebSocketResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for real-time chat messages
 * Implements defensive programming and SOLID principles per AGENTS.md
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    
    private final ChatSimplesService chatService;
    private final ChatEventPublisher eventPublisher;
    private final ReactiveMessageCache messageCache;
    private final ObjectMapper objectMapper;
    
    // Active WebSocket sessions by userId
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    
    // Room membership: roomId -> Set<userId>
    private final Map<String, ConcurrentHashMap<String, String>> roomMemberships = new ConcurrentHashMap<>();
    
    public ChatWebSocketHandler(ChatSimplesService chatService,
                               ChatEventPublisher eventPublisher,
                               ReactiveMessageCache messageCache,
                               ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.eventPublisher = eventPublisher;
        this.messageCache = messageCache;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            String userId = extractUserIdFromSession(session);
            String roomId = extractRoomFromSession(session);
            
            if (userId == null || roomId == null) {
                logger.warn("🚫 WebSocket connection rejected - missing userId or roomId");
                session.close(CloseStatus.BAD_DATA);
                return;
            }
            
            // Store session
            activeSessions.put(userId, session);
            
            // Add to room
            roomMemberships.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
                          .put(userId, userId);
            
            // Update presence cache
            messageCache.cacheUserPresence(roomId, Long.valueOf(userId), "ONLINE")
                       .subscribe();
            
            // Publish presence event
            eventPublisher.publishUserPresenceEvent(Long.valueOf(userId), "ONLINE", roomId)
                         .subscribe();
            
            // Send connection confirmation
            sendToClient(session, WebSocketResponseDTO.success(
                "connected", 
                Map.of("userId", userId, "roomId", roomId, "timestamp", LocalDateTime.now())
            ));
            
            // Notify room members about new user
            broadcastToRoom(roomId, WebSocketResponseDTO.presence(
                userId, "joined", roomId, LocalDateTime.now()
            ), userId); // Exclude the joining user
            
            logger.info("👤 User {} connected to room {} via WebSocket", userId, roomId);
            
        } catch (Exception e) {
            logger.error("❌ Error establishing WebSocket connection: {}", e.getMessage(), e);
            session.close(CloseStatus.SERVER_ERROR);
        }
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String userId = extractUserIdFromSession(session);
            if (userId == null) {
                sendErrorToClient(session, "Unauthorized");
                return;
            }
            
            ChatMessageDTO messageDto = objectMapper.readValue(message.getPayload(), ChatMessageDTO.class);
            
            // Validate message
            if (messageDto.getConteudo() == null || messageDto.getConteudo().trim().isEmpty()) {
                sendErrorToClient(session, "Message content cannot be empty");
                return;
            }
            
            if (messageDto.getConteudo().length() > 500) {
                sendErrorToClient(session, "Message too long (max 500 characters)");
                return;
            }
            
            // Convert to simple DTO for sending
            MensagemDtoSimples simpleMessage = MensagemDtoSimples.paraEnvio(
                messageDto.getConteudo(),
                Long.valueOf(userId), 
                "User" + userId, // TODO: Get real user name from auth service
                messageDto.getSalaId()
            );
            
            // Save message via service
            chatService.enviarMensagem(simpleMessage)
                .doOnSuccess(savedMessage -> {
                    // Add to cache
                    messageCache.addMessageToCache(savedMessage.sala(), savedMessage)
                              .subscribe();
                    
                    // Publish event
                    eventPublisher.publishMessageEvent(savedMessage)
                                 .subscribe();
                    
                    // Broadcast to room members
                    broadcastToRoom(savedMessage.sala(), WebSocketResponseDTO.message(savedMessage));
                    
                    logger.debug("💬 Message sent by user {} to room {}", userId, savedMessage.sala());
                })
                .doOnError(error -> {
                    logger.error("❌ Error saving message from user {}: {}", userId, error.getMessage());
                    sendErrorToClient(session, "Failed to send message: " + error.getMessage());
                })
                .subscribe();
                
        } catch (Exception e) {
            logger.error("❌ Error processing WebSocket message: {}", e.getMessage(), e);
            sendErrorToClient(session, "Invalid message format");
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        try {
            String userId = extractUserIdFromSession(session);
            String roomId = extractRoomFromSession(session);
            
            if (userId != null) {
                // Remove session
                activeSessions.remove(userId);
                
                if (roomId != null) {
                    // Remove from room
                    Map<String, String> roomMembers = roomMemberships.get(roomId);
                    if (roomMembers != null) {
                        roomMembers.remove(userId);
                        if (roomMembers.isEmpty()) {
                            roomMemberships.remove(roomId);
                        }
                    }
                    
                    // Update presence cache
                    messageCache.removeUserPresence(roomId, Long.valueOf(userId))
                              .subscribe();
                    
                    // Publish presence event
                    eventPublisher.publishUserPresenceEvent(Long.valueOf(userId), "OFFLINE", roomId)
                                 .subscribe();
                    
                    // Notify room members about user leaving
                    broadcastToRoom(roomId, WebSocketResponseDTO.presence(
                        userId, "left", roomId, LocalDateTime.now()
                    ));
                }
                
                logger.info("👤 User {} disconnected from room {} (status: {})", userId, roomId, status);
            }
            
        } catch (Exception e) {
            logger.error("❌ Error handling WebSocket disconnection: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String userId = extractUserIdFromSession(session);
        logger.error("🔌 WebSocket transport error for user {}: {}", userId, exception.getMessage());
        
        // Close session gracefully
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }
    
    /**
     * Broadcast message to all users in a room
     */
    private void broadcastToRoom(String roomId, WebSocketResponseDTO response) {
        broadcastToRoom(roomId, response, null);
    }
    
    /**
     * Broadcast message to all users in a room, optionally excluding one user
     */
    private void broadcastToRoom(String roomId, WebSocketResponseDTO response, String excludeUserId) {
        Map<String, String> roomMembers = roomMemberships.get(roomId);
        if (roomMembers == null || roomMembers.isEmpty()) {
            return;
        }
        
        String messageJson;
        try {
            messageJson = objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            logger.error("❌ Error serializing broadcast message: {}", e.getMessage());
            return;
        }
        
        roomMembers.keySet().parallelStream()
            .filter(userId -> !userId.equals(excludeUserId))
            .forEach(userId -> {
                WebSocketSession session = activeSessions.get(userId);
                if (session != null && session.isOpen()) {
                    try {
                        synchronized (session) {
                            session.sendMessage(new TextMessage(messageJson));
                        }
                    } catch (Exception e) {
                        logger.warn("⚠️ Failed to send message to user {}: {}", userId, e.getMessage());
                    }
                }
            });
    }
    
    private void sendToClient(WebSocketSession session, WebSocketResponseDTO response) {
        try {
            String messageJson = objectMapper.writeValueAsString(response);
            synchronized (session) {
                session.sendMessage(new TextMessage(messageJson));
            }
        } catch (Exception e) {
            logger.error("❌ Error sending message to client: {}", e.getMessage());
        }
    }
    
    private void sendErrorToClient(WebSocketSession session, String errorMessage) {
        sendToClient(session, WebSocketResponseDTO.error(errorMessage, LocalDateTime.now()));
    }
    
    private String extractUserIdFromSession(WebSocketSession session) {
        // Extract from query parameters or headers
        String query = session.getUri().getQuery();
        if (query != null && query.contains("userId=")) {
            return query.split("userId=")[1].split("&")[0];
        }
        return null;
    }
    
    private String extractRoomFromSession(WebSocketSession session) {
        // Extract from query parameters
        String query = session.getUri().getQuery();
        if (query != null && query.contains("roomId=")) {
            return query.split("roomId=")[1].split("&")[0];
        }
        return "geral"; // Default room
    }
    
    /**
     * Get active users count for monitoring
     */
    public int getActiveUsersCount() {
        return activeSessions.size();
    }
    
    /**
     * Get rooms count for monitoring
     */
    public int getActiveRoomsCount() {
        return roomMemberships.size();
    }
}