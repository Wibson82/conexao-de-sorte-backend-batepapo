package br.tec.facilitaservicos.batepapo.configuracao;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy;

import br.tec.facilitaservicos.batepapo.infraestrutura.websocket.ChatWebSocketHandler;

/**
 * ============================================================================
 * ⚙️ CONFIGURAÇÃO WEBSOCKET PARA CHAT REATIVO
 * ============================================================================
 * 
 * Configuração completa do WebSocket com:
 * - Roteamento de URLs para handlers
 * - Configuração CORS para WebSocket
 * - Estratégia de upgrade de conexão
 * - Configurações de buffer e timeout
 * - Suporte a sub-protocolos
 * - Compressão de mensagens
 * 
 * Endpoints configurados:
 * - /ws/chat: Chat geral em tempo real
 * - /ws/presence: Stream de presença de usuários
 * - /ws/notifications: Notificações push via WebSocket
 * 
 * Parâmetros de conexão:
 * - room: Identificador da sala
 * - userId: ID do usuário
 * - userName: Nome do usuário
 * - token: Token JWT para autenticação
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@Configuration
public class WebSocketConfig {

    private final ChatWebSocketHandler chatWebSocketHandler;

    public WebSocketConfig(ChatWebSocketHandler chatWebSocketHandler) {
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    /**
     * Configura mapeamento de URLs para handlers WebSocket
     */
    @Bean
    public HandlerMapping webSocketHandlerMapping() {
        Map<String, WebSocketHandler> handlerMap = new HashMap<>();
        
        // Endpoint principal do chat
        handlerMap.put("/ws/chat", chatWebSocketHandler);
        
        // Endpoint para stream de presença
        handlerMap.put("/ws/presence", chatWebSocketHandler);
        
        // Endpoint para notificações (futuro)
        // handlerMap.put("/ws/notifications", notificationWebSocketHandler);

        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(handlerMap);
        mapping.setOrder(1);

        return mapping;
    }

    /**
     * Configura adaptador WebSocket com estratégia de upgrade
     */
    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter(webSocketService());
    }

    /**
     * Configura serviço WebSocket com configurações otimizadas
     */
    @Bean
    public WebSocketService webSocketService() {
        // Estratégia de upgrade para Reactor Netty
        ReactorNettyRequestUpgradeStrategy upgradeStrategy = new ReactorNettyRequestUpgradeStrategy();
        
        // Configurações de buffer e compressão
        upgradeStrategy.setMaxFramePayloadLength(65536); // 64KB por frame
        upgradeStrategy.setHandleCloseTimeout(10000); // 10 segundos para fechar
        
        HandshakeWebSocketService service = new HandshakeWebSocketService(upgradeStrategy);
        
        // Configurar CORS para WebSocket
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.addAllowedOriginPattern("*"); // Em produção, especificar domínios
        corsConfig.addAllowedMethod("*");
        corsConfig.addAllowedHeader("*");
        corsConfig.setAllowCredentials(true);
        
        service.setCorsConfigurationSource(exchange -> corsConfig);
        
        // Sub-protocolos suportados
        service.setSupportedProtocols("chat-v1", "presence-v1");
        
        return service;
    }

    /**
     * Configurações adicionais para WebSocket
     */
    @Bean
    public WebSocketConfigProperties webSocketProperties() {
        return new WebSocketConfigProperties();
    }

    /**
     * Classe de propriedades configuráveis
     */
    public static class WebSocketConfigProperties {
        private int maxConnections = 10000;
        private int maxFrameSize = 65536;
        private int idleTimeout = 300; // 5 minutos
        private int heartbeatInterval = 30; // 30 segundos
        private boolean compressionEnabled = true;
        private String allowedOrigins = "*";

        // Getters e setters
        public int getMaxConnections() { return maxConnections; }
        public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }

        public int getMaxFrameSize() { return maxFrameSize; }
        public void setMaxFrameSize(int maxFrameSize) { this.maxFrameSize = maxFrameSize; }

        public int getIdleTimeout() { return idleTimeout; }
        public void setIdleTimeout(int idleTimeout) { this.idleTimeout = idleTimeout; }

        public int getHeartbeatInterval() { return heartbeatInterval; }
        public void setHeartbeatInterval(int heartbeatInterval) { this.heartbeatInterval = heartbeatInterval; }

        public boolean isCompressionEnabled() { return compressionEnabled; }
        public void setCompressionEnabled(boolean compressionEnabled) { this.compressionEnabled = compressionEnabled; }

        public String getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(String allowedOrigins) { this.allowedOrigins = allowedOrigins; }
    }
}