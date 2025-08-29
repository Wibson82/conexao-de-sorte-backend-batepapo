package br.tec.facilitaservicos.batepapo.configuracao;

import br.tec.facilitaservicos.batepapo.websocket.ChatWebSocketHandler;
import br.tec.facilitaservicos.batepapo.websocket.PresenceWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration for real-time chat functionality
 * Follows AGENTS.md guidelines - Java 24, Spring Boot 3.5.5, no hardcoded values
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    private final ChatWebSocketHandler chatHandler;
    private final PresenceWebSocketHandler presenceHandler;
    private final Environment environment;
    
    public WebSocketConfig(ChatWebSocketHandler chatHandler, 
                          PresenceWebSocketHandler presenceHandler,
                          Environment environment) {
        this.chatHandler = chatHandler;
        this.presenceHandler = presenceHandler;
        this.environment = environment;
    }
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] allowedOrigins = getAllowedOrigins();
        
        // Chat WebSocket endpoint
        registry.addHandler(chatHandler, "/ws/chat")
            .setAllowedOrigins(allowedOrigins)
            .withSockJS()
            .setHeartbeatTime(25000)
            .setDisconnectDelay(5000)
            .setStreamBytesLimit(128 * 1024) // 128KB
            .setHttpMessageCacheSize(1000);
            
        // Presence WebSocket endpoint  
        registry.addHandler(presenceHandler, "/ws/presence")
            .setAllowedOrigins(allowedOrigins)
            .withSockJS()
            .setHeartbeatTime(30000)
            .setDisconnectDelay(3000);
    }
    
    private String[] getAllowedOrigins() {
        String corsOrigins = environment.getProperty("CORS_ALLOWED_ORIGINS", "http://localhost:3000");
        return corsOrigins.split(",");
    }
}