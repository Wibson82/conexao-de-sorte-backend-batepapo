package br.tec.facilitaservicos.batepapo.websocket;

import br.tec.facilitaservicos.batepapo.infraestrutura.cache.ReactiveMessageCache;
import br.tec.facilitaservicos.batepapo.infraestrutura.eventos.ChatEventPublisher;
import br.tec.facilitaservicos.batepapo.websocket.dto.WebSocketResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for user presence tracking
 * Implements lightweight presence monitoring per AGENTS.md
 */
@Component
public class PresenceWebSocketHandler extends TextWebSocketHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(PresenceWebSocketHandler.class);
    
    private final ChatEventPublisher eventPublisher;
    private final ReactiveMessageCache messageCache;
    private final ObjectMapper objectMapper;
    
    // Active presence sessions by userId
    private final Map<String, WebSocketSession> presenceSessions = new ConcurrentHashMap<>();
    
    // Room presence: roomId -> Set<userId>
    private final Map<String, ConcurrentHashMap<String, LocalDateTime>> roomPresence = new ConcurrentHashMap<>();
    
    public PresenceWebSocketHandler(ChatEventPublisher eventPublisher,
                                  ReactiveMessageCache messageCache,
                                  ObjectMapper objectMapper) {
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
                logger.warn("🚫 Presence connection rejected - missing userId or roomId");
                session.close(CloseStatus.BAD_DATA);
                return;
            }
            
            // Store presence session
            presenceSessions.put(userId, session);
            
            // Update room presence
            roomPresence.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
                       .put(userId, LocalDateTime.now());
            
            // Update cache
            messageCache.cacheUserPresence(roomId, Long.valueOf(userId), "ONLINE")
                       .subscribe();
            
            // Publish presence event
            eventPublisher.publishUserPresenceEvent(Long.valueOf(userId), "ONLINE", roomId)
                         .subscribe();
            
            // Send presence confirmation
            sendPresenceUpdate(session, "connected", userId, roomId);
            
            // Broadcast user online status
            broadcastPresenceToRoom(roomId, userId, "joined", userId);
            
            logger.info("👁️ User {} presence connected to room {}", userId, roomId);
            
        } catch (Exception e) {
            logger.error("❌ Error establishing presence connection: {}", e.getMessage(), e);
            session.close(CloseStatus.SERVER_ERROR);
        }
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String userId = extractUserIdFromSession(session);
            String roomId = extractRoomFromSession(session);
            
            if (userId == null) {
                sendErrorToClient(session, "Unauthorized");
                return;
            }
            
            // Handle ping messages to keep connection alive
            String payload = message.getPayload();
            if ("ping".equals(payload)) {
                sendPresenceUpdate(session, "pong", userId, roomId);
                
                // Update last seen timestamp
                Map<String, LocalDateTime> roomUsers = roomPresence.get(roomId);
                if (roomUsers != null) {
                    roomUsers.put(userId, LocalDateTime.now());
                }
                return;
            }
            
            // Handle typing indicator
            if (payload.startsWith("{\"typing\":")) {
                Map<String, Object> typingData = objectMapper.readValue(payload, Map.class);
                boolean isTyping = (Boolean) typingData.get("typing");
                
                broadcastPresenceToRoom(roomId, 
                    WebSocketResponseDTO.presence(userId, 
                        isTyping ? "typing" : "stopped_typing", 
                        roomId, LocalDateTime.now()), 
                    userId);
                
                logger.debug("⌨️ User {} {} typing in room {}", 
                    userId, isTyping ? "started" : "stopped", roomId);
            }
            
        } catch (Exception e) {
            logger.error("❌ Error processing presence message: {}", e.getMessage(), e);
            sendErrorToClient(session, "Invalid presence message");
        }
    }
    
    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
        String userId = extractUserIdFromSession(session);
        String roomId = extractRoomFromSession(session);
        
        if (userId != null && roomId != null) {
            // Update last seen timestamp
            Map<String, LocalDateTime> roomUsers = roomPresence.get(roomId);
            if (roomUsers != null) {
                roomUsers.put(userId, LocalDateTime.now());
            }
            logger.debug("🏓 Pong received from user {} in room {}", userId, roomId);
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        try {
            String userId = extractUserIdFromSession(session);
            String roomId = extractRoomFromSession(session);
            
            if (userId != null) {
                // Remove presence session
                presenceSessions.remove(userId);
                
                if (roomId != null) {
                    // Remove from room presence
                    Map<String, LocalDateTime> roomUsers = roomPresence.get(roomId);
                    if (roomUsers != null) {
                        roomUsers.remove(userId);
                        if (roomUsers.isEmpty()) {
                            roomPresence.remove(roomId);
                        }
                    }
                    
                    // Update cache
                    messageCache.removeUserPresence(roomId, Long.valueOf(userId))
                              .subscribe();
                    
                    // Publish presence event
                    eventPublisher.publishUserPresenceEvent(Long.valueOf(userId), "OFFLINE", roomId)
                                 .subscribe();
                    
                    // Broadcast user offline status
                    broadcastPresenceToRoom(roomId, userId, "left", null);
                }
                
                logger.info("👁️ User {} presence disconnected from room {} (status: {})", 
                    userId, roomId, status);
            }
            
        } catch (Exception e) {
            logger.error("❌ Error handling presence disconnection: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String userId = extractUserIdFromSession(session);
        logger.error("🔌 Presence transport error for user {}: {}", userId, exception.getMessage());
        
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }
    
    private void sendPresenceUpdate(WebSocketSession session, String action, String userId, String roomId) {
        try {
            WebSocketResponseDTO response = WebSocketResponseDTO.presence(userId, action, roomId, LocalDateTime.now());
            String messageJson = objectMapper.writeValueAsString(response);
            
            synchronized (session) {
                session.sendMessage(new TextMessage(messageJson));
            }
        } catch (Exception e) {
            logger.error("❌ Error sending presence update: {}", e.getMessage());
        }
    }
    
    private void broadcastPresenceToRoom(String roomId, String userId, String action, String excludeUserId) {
        WebSocketResponseDTO response = WebSocketResponseDTO.presence(userId, action, roomId, LocalDateTime.now());
        broadcastPresenceToRoom(roomId, response, excludeUserId);
    }
    
    private void broadcastPresenceToRoom(String roomId, WebSocketResponseDTO response, String excludeUserId) {
        Map<String, LocalDateTime> roomUsers = roomPresence.get(roomId);
        if (roomUsers == null || roomUsers.isEmpty()) {
            return;
        }
        
        String messageJson;
        try {
            messageJson = objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            logger.error("❌ Error serializing presence broadcast: {}", e.getMessage());
            return;
        }
        
        roomUsers.keySet().parallelStream()
            .filter(userId -> !userId.equals(excludeUserId))
            .forEach(userId -> {
                WebSocketSession session = presenceSessions.get(userId);
                if (session != null && session.isOpen()) {
                    try {
                        synchronized (session) {
                            session.sendMessage(new TextMessage(messageJson));
                        }
                    } catch (Exception e) {
                        logger.warn("⚠️ Failed to send presence to user {}: {}", userId, e.getMessage());
                    }
                }
            });
    }
    
    private void sendErrorToClient(WebSocketSession session, String errorMessage) {
        try {
            WebSocketResponseDTO response = WebSocketResponseDTO.error(errorMessage, LocalDateTime.now());
            String messageJson = objectMapper.writeValueAsString(response);
            
            synchronized (session) {
                session.sendMessage(new TextMessage(messageJson));
            }
        } catch (Exception e) {
            logger.error("❌ Error sending presence error: {}", e.getMessage());
        }
    }
    
    private String extractUserIdFromSession(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("userId=")) {
            return query.split("userId=")[1].split("&")[0];
        }
        return null;
    }
    
    private String extractRoomFromSession(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("roomId=")) {
            return query.split("roomId=")[1].split("&")[0];
        }
        return "geral";
    }
    
    public int getActivePresenceCount() {
        return presenceSessions.size();
    }
    
    public int getActiveRoomsCount() {
        return roomPresence.size();
    }
}