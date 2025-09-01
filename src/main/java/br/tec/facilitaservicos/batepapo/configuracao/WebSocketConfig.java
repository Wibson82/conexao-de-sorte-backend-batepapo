package br.tec.facilitaservicos.batepapo.configuracao;

import br.tec.facilitaservicos.batepapo.websocket.ReactiveWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.HashMap;
import java.util.Map;

/**
 * Reactive WebSocket configuration for real-time chat functionality
 * Compatible with Spring WebFlux - Java 24, Spring Boot 3.5.5
 */
@Configuration
public class WebSocketConfig {
    
    private final ReactiveWebSocketHandler webSocketHandler;
    private final Environment environment;
    
    public WebSocketConfig(ReactiveWebSocketHandler webSocketHandler,
                          Environment environment) {
        this.webSocketHandler = webSocketHandler;
        this.environment = environment;
    }
    
    @Bean
    public HandlerMapping webSocketMapping() {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/ws/chat", webSocketHandler);
        map.put("/ws/presence", webSocketHandler);
        
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(map);
        mapping.setOrder(-1); // Before annotated controllers
        return mapping;
    }
    
    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}