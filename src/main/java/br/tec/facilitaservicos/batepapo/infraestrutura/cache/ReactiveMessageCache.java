package br.tec.facilitaservicos.batepapo.infraestrutura.cache;

import br.tec.facilitaservicos.batepapo.apresentacao.dto.MensagemDtoSimples;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Cache reativo para mensagens do chat com otimizações de performance
 */
@Component
public class ReactiveMessageCache {

    private static final Logger logger = LoggerFactory.getLogger(ReactiveMessageCache.class);
    
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    // Configurações de cache
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);
    private static final Duration PRESENCE_TTL = Duration.ofMinutes(5);
    private static final String MESSAGES_KEY_PREFIX = "chat:messages:";
    private static final String PRESENCE_KEY_PREFIX = "chat:presence:";
    private static final String ROOM_USERS_KEY_PREFIX = "chat:room:users:";

    public ReactiveMessageCache(ReactiveRedisTemplate<String, Object> redisTemplate, 
                               ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Cache de mensagens recentes por sala (últimas 50 mensagens)
     */
    public Mono<Void> cacheRecentMessages(String roomId, List<MensagemDtoSimples> mensagens) {
        String key = MESSAGES_KEY_PREFIX + roomId;
        
        return Flux.fromIterable(mensagens)
            .map(this::serializeMessage)
            .collectList()
            .flatMap(serializedMessages -> 
                redisTemplate.opsForList()
                    .delete(key) // Limpar cache anterior
                    .then(redisTemplate.opsForList().leftPushAll(key, serializedMessages))
                    .then(redisTemplate.expire(key, CACHE_TTL))
            )
            .doOnSuccess(result -> 
                logger.debug("✅ Cache atualizado para sala {}: {} mensagens", roomId, mensagens.size())
            )
            .doOnError(error -> 
                logger.error("❌ Erro ao cachear mensagens para sala {}: {}", roomId, error.getMessage())
            )
            .then();
    }

    /**
     * Recupera mensagens do cache
     */
    public Flux<MensagemDtoSimples> getCachedMessages(String roomId, int limit) {
        String key = MESSAGES_KEY_PREFIX + roomId;
        
        return redisTemplate.opsForList()
            .range(key, 0, limit - 1)
            .cast(String.class)
            .map(this::deserializeMessage)
            .filter(mensagem -> mensagem != null)
            .doOnComplete(() -> 
                logger.debug("📥 Mensagens recuperadas do cache para sala {}", roomId)
            );
    }

    /**
     * Adiciona nova mensagem ao cache (prepend)
     */
    public Mono<Void> addMessageToCache(String roomId, MensagemDtoSimples mensagem) {
        String key = MESSAGES_KEY_PREFIX + roomId;
        String serializedMessage = serializeMessage(mensagem);
        
        return redisTemplate.opsForList()
            .leftPush(key, serializedMessage)
            .then(redisTemplate.opsForList().trim(key, 0, 49)) // Manter apenas últimas 50
            .then(redisTemplate.expire(key, CACHE_TTL))
            .doOnSuccess(result -> 
                logger.debug("➕ Mensagem adicionada ao cache da sala {}", roomId)
            )
            .then();
    }

    /**
     * Cache de usuários online por sala
     */
    public Mono<Void> cacheUserPresence(String roomId, Long userId, String status) {
        String presenceKey = PRESENCE_KEY_PREFIX + userId;
        String roomUsersKey = ROOM_USERS_KEY_PREFIX + roomId;
        
        return Mono.zip(
            // Cache individual de presença
            redisTemplate.opsForValue()
                .set(presenceKey, status, PRESENCE_TTL),
            // Cache de usuários por sala
            redisTemplate.opsForSet()
                .add(roomUsersKey, userId.toString())
                .then(redisTemplate.expire(roomUsersKey, PRESENCE_TTL))
        )
        .doOnSuccess(result -> 
            logger.debug("👤 Presença cacheada: usuário {} está {} na sala {}", userId, status, roomId)
        )
        .then();
    }

    /**
     * Remove usuário da presença
     */
    public Mono<Void> removeUserPresence(String roomId, Long userId) {
        String presenceKey = PRESENCE_KEY_PREFIX + userId;
        String roomUsersKey = ROOM_USERS_KEY_PREFIX + roomId;
        
        return Mono.zip(
            redisTemplate.delete(presenceKey),
            redisTemplate.opsForSet().remove(roomUsersKey, userId.toString())
        )
        .doOnSuccess(result -> 
            logger.debug("👤 Presença removida: usuário {} da sala {}", userId, roomId)
        )
        .then();
    }

    /**
     * Obtém usuários online numa sala
     */
    public Flux<Long> getOnlineUsersInRoom(String roomId) {
        String roomUsersKey = ROOM_USERS_KEY_PREFIX + roomId;
        
        return redisTemplate.opsForSet()
            .members(roomUsersKey)
            .cast(String.class)
            .map(Long::valueOf)
            .onErrorContinue((error, obj) -> 
                logger.warn("⚠️ Erro ao converter userId para Long: {}", obj)
            );
    }

    /**
     * Verifica se usuário está online
     */
    public Mono<Boolean> isUserOnline(Long userId) {
        String presenceKey = PRESENCE_KEY_PREFIX + userId;
        
        return redisTemplate.hasKey(presenceKey);
    }

    /**
     * Cache de estatísticas da sala (performance)
     */
    public Mono<Void> cacheRoomStats(String roomId, int messageCount, int userCount) {
        String statsKey = "chat:stats:" + roomId;
        
        return redisTemplate.opsForHash()
            .putAll(statsKey, java.util.Map.of(
                "messageCount", messageCount,
                "userCount", userCount,
                "lastUpdate", LocalDateTime.now().toString()
            ))
            .then(redisTemplate.expire(statsKey, Duration.ofMinutes(10)))
            .doOnSuccess(result -> 
                logger.debug("📊 Stats cacheadas para sala {}: {} msgs, {} usuários", 
                           roomId, messageCount, userCount)
            )
            .then();
    }

    /**
     * Obtém estatísticas da sala do cache
     */
    public Mono<java.util.Map<String, Object>> getRoomStats(String roomId) {
        String statsKey = "chat:stats:" + roomId;
        
        return redisTemplate.opsForHash()
            .entries(statsKey)
            .collectMap(entry -> entry.getKey().toString(), 
                       entry -> entry.getValue());
    }

    /**
     * Invalidação de cache por padrão
     */
    public Mono<Long> invalidateCachePattern(String pattern) {
        return redisTemplate.keys(pattern)
            .collectList()
            .flatMap(keys -> {
                if (keys.isEmpty()) {
                    return Mono.just(0L);
                }
                return redisTemplate.delete(keys.toArray(new String[0]));
            })
            .doOnSuccess(count -> 
                logger.info("🧹 Cache invalidado: {} chaves removidas com padrão '{}'", count, pattern)
            );
    }

    /**
     * Cache warming - pré-aquecimento para salas ativas
     */
    public Mono<Void> warmupCache(List<String> activeRooms) {
        return Flux.fromIterable(activeRooms)
            .flatMap(roomId -> {
                // Aqui você faria consulta ao banco para carregar mensagens recentes
                // Por enquanto, apenas log
                logger.info("🔥 Warming up cache para sala: {}", roomId);
                return Mono.empty();
            })
            .then()
            .doOnSuccess(unused -> 
                logger.info("🔥 Cache warming concluído para {} salas", activeRooms.size())
            );
    }

    // Métodos auxiliares de serialização
    private String serializeMessage(MensagemDtoSimples mensagem) {
        try {
            return objectMapper.writeValueAsString(mensagem);
        } catch (JsonProcessingException e) {
            logger.error("❌ Erro ao serializar mensagem: {}", e.getMessage());
            return "{}";
        }
    }

    private MensagemDtoSimples deserializeMessage(String json) {
        try {
            return objectMapper.readValue(json, MensagemDtoSimples.class);
        } catch (Exception e) {
            logger.error("❌ Erro ao deserializar mensagem: {}", e.getMessage());
            return null;
        }
    }
}