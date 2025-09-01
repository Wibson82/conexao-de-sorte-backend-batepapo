package br.tec.facilitaservicos.batepapo.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

/**
 * Reactive WebSocket handler for chat functionality
 * Compatible with Spring WebFlux
 */
@Component
public class ReactiveWebSocketHandler implements WebSocketHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ReactiveWebSocketHandler.class);
    
    @Override
    public Mono<Void> handle(WebSocketSession session) {
        logger.info("🔗 WebSocket connection established: {}", session.getId());
        
        // Handle incoming messages
        Mono<Void> input = session.receive()
            .doOnNext(message -> {
                String payload = message.getPayloadAsText();
                logger.debug("📨 Received message: {} from session: {}", payload, session.getId());
                // Process message here
            })
            .then();

        // Send messages (echo for now)
        Mono<Void> output = session.send(
            session.receive()
                .map(message -> {
                    String response = "Echo: " + message.getPayloadAsText();
                    return session.textMessage(response);
                })
        );

        return Mono.zip(input, output).then()
            .doOnTerminate(() -> logger.info("🔌 WebSocket connection closed: {}", session.getId()));
    }
}