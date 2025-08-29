package br.tec.facilitaservicos.batepapo.infraestrutura.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuração de Circuit Breakers para o Chat Service
 * Implementa padrões de resiliência para integração com outros microserviços
 */
@Configuration
public class ChatCircuitBreakerConfig {

    private static final Logger logger = LoggerFactory.getLogger(ChatCircuitBreakerConfig.class);

    /**
     * Registros para Circuit Breakers via anotações
     */
    @Bean
    public String circuitBreakerInfo() {
        logger.info("🔧 Circuit Breakers configurados via anotações");
        logger.info("🎯 Verifique application.yml para configurações");
        return "Circuit Breakers enabled";
    }

    /**
     * Circuit Breaker para Database
     */
    @Bean 
    public CircuitBreaker databaseCircuitBreaker() {
        logger.info("🔧 Configurando Circuit Breaker para Database");
        
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .slidingWindowSize(10)
            .minimumNumberOfCalls(3)
            .failureRateThreshold(60.0f)
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .permittedNumberOfCallsInHalfOpenState(2)
            .slowCallRateThreshold(60.0f)
            .slowCallDurationThreshold(Duration.ofSeconds(5))
            .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("database", config);
        
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> 
                logger.warn("🔄 Database Circuit Breaker: {} -> {}", 
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState())
            );
            
        return circuitBreaker;
    }

    /**
     * Retry para Auth Service
     */
    @Bean
    public Retry authServiceRetry() {
        logger.info("🔧 Configurando Retry para Auth Service");
        
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(500))
            .retryExceptions(RuntimeException.class)
            .build();

        Retry retry = Retry.of("auth-service", config);
        
        retry.getEventPublisher()
            .onRetry(event -> 
                logger.debug("🔁 Auth Service retry attempt {} of {}", 
                    event.getNumberOfRetryAttempts(),
                    event.getLastThrowable().getMessage())
            );
            
        return retry;
    }

    /**
     * Time Limiter para Auth Service
     */
    @Bean
    public TimeLimiter authServiceTimeLimiter() {
        logger.info("🔧 Configurando Time Limiter para Auth Service");
        
        TimeLimiterConfig config = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(3))
            .cancelRunningFuture(true)
            .build();
            
        return TimeLimiter.of("auth-service", config);
    }

    /**
     * Registry para Circuit Breakers
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }

    /**
     * Registry para Retries
     */
    @Bean
    public RetryRegistry retryRegistry() {
        return RetryRegistry.ofDefaults();
    }

    /**
     * Registry para Time Limiters
     */
    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        return TimeLimiterRegistry.ofDefaults();
    }
}