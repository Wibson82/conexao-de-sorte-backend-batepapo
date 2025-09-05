package br.tec.facilitaservicos.batepapo.configuracao;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuração de cache Redis otimizada para microserviço de bate-papo.
 * TTLs ajustados para chat em tempo real:
 * - Mensagens: 2 minutos (alta rotatividade)
 * - Salas: 15 minutos (moderada persistência)  
 * - Usuários online: 30 segundos (tempo real)
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${spring.application.name:batepapo}")
    private String applicationName;

    // Cache names específicos do domínio de bate-papo
    public static final String MENSAGENS_CACHE = "chat:mensagens";
    public static final String SALAS_CACHE = "chat:salas";
    public static final String USUARIOS_ONLINE_CACHE = "chat:usuarios-online";
    public static final String MENSAGENS_RECENTES_CACHE = "chat:mensagens-recentes";
    public static final String SALAS_ATIVAS_CACHE = "chat:salas-ativas";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, MeterRegistry meterRegistry) {
        // Configuração base com serialização otimizada
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5)) // TTL padrão conservador
                .computePrefixWith(cacheName -> applicationName + ":" + cacheName + ":")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        // TTLs diferenciados por criticidade e padrão de acesso
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Mensagens - TTL curto para alta rotatividade
        cacheConfigurations.put(MENSAGENS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(2)));
        cacheConfigurations.put(MENSAGENS_RECENTES_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(2)));
        
        // Salas - TTL moderado para equilíbrio performance/consistência  
        cacheConfigurations.put(SALAS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put(SALAS_ATIVAS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(15)));
        
        // Usuários online - TTL muito curto para dados em tempo real
        cacheConfigurations.put(USUARIOS_ONLINE_CACHE, defaultConfig.entryTtl(Duration.ofSeconds(30)));

        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();

        // Registrar métricas de cache personalizadas
        return new RedisCacheMetrics(cacheManager, meterRegistry, applicationName).instrumentedCacheManager();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Serialização otimizada para performance
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.setDefaultSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        
        return template;
    }

    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();

        RedisSerializationContext.RedisSerializationContextBuilder<String, Object> builder = 
            RedisSerializationContext.newSerializationContext(keySerializer);

        RedisSerializationContext<String, Object> context = builder
            .value(valueSerializer)
            .hashKey(keySerializer)
            .hashValue(valueSerializer)
            .build();

        return new ReactiveRedisTemplate<String, Object>(connectionFactory, context);
    }
}