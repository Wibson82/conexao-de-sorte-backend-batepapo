package br.tec.facilitaservicos.batepapo.infraestrutura.eventos;

import br.tec.facilitaservicos.batepapo.apresentacao.dto.MensagemDtoSimples;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Publisher de eventos do Chat para Redis Streams
 * Implementa event-driven architecture para comunicação entre microserviços
 */
@Component
public class ChatEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(ChatEventPublisher.class);
    
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    // Redis Streams
    private static final String CHAT_MESSAGES_STREAM = "chat:messages";
    private static final String USER_PRESENCE_STREAM = "chat:presence";
    private static final String ROOM_EVENTS_STREAM = "chat:rooms";

    public ChatEventPublisher(ReactiveRedisTemplate<String, Object> redisTemplate, 
                             ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publica evento de nova mensagem no Redis Stream
     */
    @CircuitBreaker(name = "redis", fallbackMethod = "fallbackPublishMessage")
    public Mono<String> publishMessageEvent(MensagemDtoSimples mensagem) {
        return Mono.fromCallable(() -> {
            Map<String, Object> eventData = Map.of(
                "eventType", "MESSAGE_SENT",
                "messageId", mensagem.id() != null ? mensagem.id() : "temp-id",
                "userId", mensagem.usuarioId(),
                "roomId", mensagem.sala() != null ? mensagem.sala() : "geral",
                "content", mensagem.conteudo(),
                "timestamp", mensagem.dataEnvio() != null ? mensagem.dataEnvio().toString() : LocalDateTime.now().toString(),
                "messageType", mensagem.tipo() != null ? mensagem.tipo() : "TEXT"
            );
            
            logger.debug("📢 Publicando evento MESSAGE_SENT: userId={}, roomId={}", 
                        mensagem.usuarioId(), mensagem.sala());
            
            return eventData;
        })
        .flatMap(eventData -> 
            redisTemplate.opsForStream()
                .add(CHAT_MESSAGES_STREAM, eventData)
                .map(recordId -> {
                    logger.debug("✅ Evento MESSAGE_SENT publicado: recordId={}", recordId);
                    return recordId.getValue();
                })
        )
        .onErrorResume(ex -> {
            logger.error("❌ Erro ao publicar evento MESSAGE_SENT: {}", ex.getMessage());
            return Mono.just("failed");
        });
    }

    /**
     * Publica evento de presença do usuário
     */
    @CircuitBreaker(name = "redis", fallbackMethod = "fallbackPublishPresence")
    public Mono<String> publishUserPresenceEvent(Long userId, String status, String roomId) {
        return Mono.fromCallable(() -> {
            Map<String, Object> eventData = Map.of(
                "eventType", "USER_PRESENCE",
                "userId", userId,
                "status", status, // "ONLINE", "OFFLINE", "TYPING"
                "roomId", roomId != null ? roomId : "geral",
                "timestamp", LocalDateTime.now().toString()
            );
            
            logger.debug("👤 Publicando evento USER_PRESENCE: userId={}, status={}", userId, status);
            
            return eventData;
        })
        .flatMap(eventData -> 
            redisTemplate.opsForStream()
                .add(USER_PRESENCE_STREAM, eventData)
                .map(recordId -> {
                    logger.debug("✅ Evento USER_PRESENCE publicado: recordId={}", recordId);
                    return recordId.getValue();
                })
        );
    }

    /**
     * Publica evento de sala (criação, join, leave)
     */
    @CircuitBreaker(name = "redis", fallbackMethod = "fallbackPublishRoom")
    public Mono<String> publishRoomEvent(String eventType, Long userId, String roomId, Object metadata) {
        return Mono.fromCallable(() -> {
            try {
                Map<String, Object> eventData = Map.of(
                    "eventType", eventType, // "ROOM_CREATED", "USER_JOINED", "USER_LEFT"
                    "userId", userId,
                    "roomId", roomId,
                    "metadata", objectMapper.writeValueAsString(metadata),
                    "timestamp", LocalDateTime.now().toString()
                );
                
                logger.debug("🏠 Publicando evento {}: userId={}, roomId={}", eventType, userId, roomId);
                
                return eventData;
            } catch (JsonProcessingException e) {
                logger.error("❌ Erro ao serializar metadata do evento {}: {}", eventType, e.getMessage());
                throw new RuntimeException("Falha na serialização do evento", e);
            }
        })
        .flatMap(eventData -> 
            redisTemplate.opsForStream()
                .add(ROOM_EVENTS_STREAM, eventData)
                .map(recordId -> {
                    logger.debug("✅ Evento {} publicado: recordId={}", eventData.get("eventType"), recordId);
                    return recordId.getValue();
                })
        );
    }

    // Fallback methods
    public Mono<String> fallbackPublishMessage(MensagemDtoSimples mensagem, Exception ex) {
        logger.warn("⚠️ Fallback: Não foi possível publicar MESSAGE_SENT para mensagem do usuário {}: {}", 
                   mensagem.usuarioId(), ex.getMessage());
        return Mono.just("fallback-message");
    }

    public Mono<String> fallbackPublishPresence(Long userId, String status, String roomId, Exception ex) {
        logger.warn("⚠️ Fallback: Não foi possível publicar USER_PRESENCE para usuário {}: {}", 
                   userId, ex.getMessage());
        return Mono.just("fallback-presence");
    }

    public Mono<String> fallbackPublishRoom(String eventType, Long userId, String roomId, Object metadata, Exception ex) {
        logger.warn("⚠️ Fallback: Não foi possível publicar {} para sala {}: {}", 
                   eventType, roomId, ex.getMessage());
        return Mono.just("fallback-room");
    }
}