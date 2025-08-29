package br.tec.facilitaservicos.batepapo.infraestrutura.eventos;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.stream.StreamReceiver;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Map;

/**
 * Consumer de eventos do Chat via Redis Streams
 * Processa eventos de outros microserviços para manter consistência
 */
@Component
public class ChatEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ChatEventConsumer.class);
    
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final StreamReceiver<String, MapRecord<String, String, String>> streamReceiver;
    
    // Consumer group e consumer name
    private static final String CONSUMER_GROUP = "batepapo-service";
    private static final String CONSUMER_NAME = "batepapo-consumer-1";
    
    // Streams para escutar
    private static final String AUTH_EVENTS_STREAM = "auth:events";
    private static final String NOTIFICATIONS_STREAM = "notifications:events";
    
    private boolean consuming = false;

    public ChatEventConsumer(ReactiveRedisTemplate<String, Object> redisTemplate, 
                           ObjectMapper objectMapper,
                           StreamReceiver<String, MapRecord<String, String, String>> streamReceiver) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.streamReceiver = streamReceiver;
    }

    @PostConstruct
    public void startConsuming() {
        createConsumerGroups()
            .then(Mono.fromRunnable(this::consumeAuthEvents))
            .then(Mono.fromRunnable(this::consumeNotificationEvents))
            .subscribe(
                unused -> logger.info("🚀 ChatEventConsumer iniciado com sucesso"),
                error -> logger.error("❌ Erro ao iniciar ChatEventConsumer: {}", error.getMessage())
            );
    }

    @PreDestroy
    public void stopConsuming() {
        consuming = false;
        logger.info("🛑 ChatEventConsumer parado");
    }

    /**
     * Cria consumer groups se não existirem
     */
    private Mono<Void> createConsumerGroups() {
        return createConsumerGroupIfNotExists(AUTH_EVENTS_STREAM, CONSUMER_GROUP)
            .then(createConsumerGroupIfNotExists(NOTIFICATIONS_STREAM, CONSUMER_GROUP))
            .doOnSuccess(unused -> logger.info("✅ Consumer groups verificados/criados"));
    }

    private Mono<Void> createConsumerGroupIfNotExists(String stream, String group) {
        return redisTemplate.opsForStream()
            .createGroup(stream, ReadOffset.from("0"), group)
            .onErrorResume(ex -> {
                // Ignora erro se o grupo já existe
                logger.debug("Consumer group {} já existe para stream {}", group, stream);
                return Mono.empty();
            })
            .then();
    }

    /**
     * Consome eventos do Auth Service
     */
    private void consumeAuthEvents() {
        if (consuming) return;
        consuming = true;
        
        logger.info("👂 Iniciando consumo de eventos do Auth Service...");
        
        streamReceiver
            .receive(Consumer.from(CONSUMER_GROUP, CONSUMER_NAME),
                    StreamOffset.create(AUTH_EVENTS_STREAM, ReadOffset.lastConsumed()))
            .delayElements(Duration.ofMillis(100))
            .doOnNext(record -> {
                try {
                    processAuthEvent(record.getValue());
                    
                    // ACK do evento
                    redisTemplate.opsForStream()
                        .acknowledge(AUTH_EVENTS_STREAM, CONSUMER_GROUP, record.getId())
                        .subscribe(
                            ackResult -> logger.debug("✅ ACK enviado para evento: {}", record.getId()),
                            ackError -> logger.warn("⚠️ Erro no ACK do evento {}: {}", record.getId(), ackError.getMessage())
                        );
                } catch (Exception e) {
                    logger.error("❌ Erro ao processar evento do Auth Service: {}", e.getMessage());
                }
            })
            .onErrorContinue((error, obj) -> {
                logger.error("❌ Erro no consumer de Auth Events: {}", error.getMessage());
            })
            .subscribe();
    }

    /**
     * Consome eventos de Notificações
     */
    private void consumeNotificationEvents() {
        logger.info("👂 Iniciando consumo de eventos de Notificações...");
        
        streamReceiver
            .receive(Consumer.from(CONSUMER_GROUP, CONSUMER_NAME),
                    StreamOffset.create(NOTIFICATIONS_STREAM, ReadOffset.lastConsumed()))
            .delayElements(Duration.ofMillis(200))
            .doOnNext(record -> {
                try {
                    processNotificationEvent(record.getValue());
                    
                    // ACK do evento
                    redisTemplate.opsForStream()
                        .acknowledge(NOTIFICATIONS_STREAM, CONSUMER_GROUP, record.getId())
                        .subscribe();
                } catch (Exception e) {
                    logger.error("❌ Erro ao processar evento de Notificação: {}", e.getMessage());
                }
            })
            .onErrorContinue((error, obj) -> {
                logger.error("❌ Erro no consumer de Notification Events: {}", error.getMessage());
            })
            .subscribe();
    }

    /**
     * Processa eventos do Auth Service
     */
    private void processAuthEvent(Map<String, String> eventData) {
        String eventType = eventData.get("eventType");
        String userId = eventData.get("userId");
        
        logger.debug("🔐 Processando evento do Auth: {} para usuário {}", eventType, userId);
        
        switch (eventType) {
            case "USER_LOGGED_IN":
                handleUserLoggedIn(Long.valueOf(userId), eventData);
                break;
            case "USER_LOGGED_OUT":
                handleUserLoggedOut(Long.valueOf(userId), eventData);
                break;
            case "USER_BANNED":
                handleUserBanned(Long.valueOf(userId), eventData);
                break;
            case "USER_SESSION_EXPIRED":
                handleUserSessionExpired(Long.valueOf(userId), eventData);
                break;
            default:
                logger.debug("⚪ Evento do Auth ignorado: {}", eventType);
        }
    }

    /**
     * Processa eventos de Notificações
     */
    private void processNotificationEvent(Map<String, String> eventData) {
        String eventType = eventData.get("eventType");
        String userId = eventData.get("userId");
        
        logger.debug("🔔 Processando evento de Notificação: {} para usuário {}", eventType, userId);
        
        switch (eventType) {
            case "CHAT_NOTIFICATION_SENT":
                handleChatNotificationSent(eventData);
                break;
            case "NOTIFICATION_DELIVERY_FAILED":
                handleNotificationFailed(eventData);
                break;
            default:
                logger.debug("⚪ Evento de Notificação ignorado: {}", eventType);
        }
    }

    // Event handlers
    private void handleUserLoggedIn(Long userId, Map<String, String> eventData) {
        logger.info("👤 Usuário {} fez login - atualizando presença no chat", userId);
        // Aqui poderia atualizar cache de usuários online
        // Publicar evento de presença online
    }

    private void handleUserLoggedOut(Long userId, Map<String, String> eventData) {
        logger.info("👤 Usuário {} fez logout - removendo presença do chat", userId);
        // Remover usuário dos usuários online
        // Publicar evento de presença offline
    }

    private void handleUserBanned(Long userId, Map<String, String> eventData) {
        logger.warn("🚫 Usuário {} foi banido - removendo do chat", userId);
        // Forçar desconexão do chat
        // Limpar mensagens pendentes
    }

    private void handleUserSessionExpired(Long userId, Map<String, String> eventData) {
        logger.warn("⏰ Sessão do usuário {} expirou - desconectando do chat", userId);
        // Similar ao logout
    }

    private void handleChatNotificationSent(Map<String, String> eventData) {
        String messageId = eventData.get("messageId");
        logger.debug("📧 Notificação enviada para mensagem {}", messageId);
        // Marcar mensagem como notificada
    }

    private void handleNotificationFailed(Map<String, String> eventData) {
        String messageId = eventData.get("messageId");
        String reason = eventData.get("reason");
        logger.warn("❌ Falha na notificação para mensagem {}: {}", messageId, reason);
        // Reagendar notificação ou marcar como falha
    }
}