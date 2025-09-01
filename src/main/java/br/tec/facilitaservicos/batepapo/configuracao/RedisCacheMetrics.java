package br.tec.facilitaservicos.batepapo.configuracao;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Gauge;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Métricas customizadas para monitoramento do cache Redis.
 * Específico para padrões de acesso do bate-papo em tempo real.
 */
public class RedisCacheMetrics {

    private final RedisCacheManager cacheManager;
    private final MeterRegistry meterRegistry;
    private final String applicationName;
    
    // Contadores para métricas de chat
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Counter cacheEvictionCounter;
    private final Timer cacheAccessTimer;
    private final AtomicLong activeChatSessions;
    private final AtomicLong totalMessagesInCache;

    public RedisCacheMetrics(RedisCacheManager cacheManager, MeterRegistry meterRegistry, String applicationName) {
        this.cacheManager = cacheManager;
        this.meterRegistry = meterRegistry;
        this.applicationName = applicationName;
        
        // Inicializar contadores específicos do chat
        this.cacheHitCounter = Counter.builder("redis.cache.hit")
                .description("Número de cache hits")
                .tag("service", applicationName)
                .tag("type", "chat")
                .register(meterRegistry);
                
        this.cacheMissCounter = Counter.builder("redis.cache.miss")
                .description("Número de cache misses")
                .tag("service", applicationName)
                .tag("type", "chat")
                .register(meterRegistry);
                
        this.cacheEvictionCounter = Counter.builder("redis.cache.eviction")
                .description("Número de evictions do cache")
                .tag("service", applicationName)
                .tag("type", "chat")
                .register(meterRegistry);
                
        this.cacheAccessTimer = Timer.builder("redis.cache.access")
                .description("Tempo de acesso ao cache")
                .tag("service", applicationName)
                .tag("type", "chat")
                .register(meterRegistry);

        this.activeChatSessions = new AtomicLong(0);
        this.totalMessagesInCache = new AtomicLong(0);
        
        // Gauges para métricas em tempo real
        Gauge.builder("chat.sessions.active")
                .description("Número de sessões de chat ativas")
                .tag("service", applicationName)
                .register(meterRegistry, activeChatSessions, AtomicLong::get);
                
        Gauge.builder("chat.messages.cached")
                .description("Total de mensagens em cache")
                .tag("service", applicationName)
                .register(meterRegistry, totalMessagesInCache, AtomicLong::get);
    }

    public RedisCacheManager instrumentedCacheManager() {
        return cacheManager;
    }

    public void recordCacheHit(String cacheName) {
        cacheHitCounter.increment();
    }

    public void recordCacheMiss(String cacheName) {
        cacheMissCounter.increment();
    }

    public void recordCacheEviction(String cacheName) {
        cacheEvictionCounter.increment();
    }

    public Timer.Sample startCacheAccess() {
        return Timer.start(meterRegistry);
    }

    public void recordCacheAccess(Timer.Sample sample) {
        sample.stop(cacheAccessTimer);
    }

    public void updateActiveSessions(long count) {
        activeChatSessions.set(count);
    }

    public void updateMessagesInCache(long count) {
        totalMessagesInCache.set(count);
    }

    /**
     * Coleta métricas específicas do Redis para chat
     */
    public void collectChatSpecificMetrics(RedisTemplate<String, Object> redisTemplate) {
        try {
            // Contagem de chaves específicas do chat
            Long userOnlineKeys = redisTemplate.execute((RedisCallback<Long>) connection -> 
                connection.eval("return #redis.call('keys', ARGV[1])", 0, (applicationName + ":chat:usuarios-online:*").getBytes())
            );
            
            Long messageKeys = redisTemplate.execute((RedisCallback<Long>) connection -> 
                connection.eval("return #redis.call('keys', ARGV[1])", 0, (applicationName + ":chat:mensagens:*").getBytes())
            );
            
            if (userOnlineKeys != null) {
                updateActiveSessions(userOnlineKeys);
            }
            
            if (messageKeys != null) {
                updateMessagesInCache(messageKeys);
            }
            
        } catch (Exception e) {
            // Log do erro sem quebrar a aplicação
            System.err.println("Erro ao coletar métricas do Redis: " + e.getMessage());
        }
    }
}