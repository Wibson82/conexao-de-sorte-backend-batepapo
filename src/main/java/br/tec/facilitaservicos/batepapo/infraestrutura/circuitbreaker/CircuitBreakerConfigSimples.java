package br.tec.facilitaservicos.batepapo.infraestrutura.circuitbreaker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração simplificada de Circuit Breakers
 * Os circuit breakers são configurados via anotações e application.yml
 */
@Configuration
public class CircuitBreakerConfigSimples {

    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerConfigSimples.class);

    public CircuitBreakerConfigSimples() {
        logger.info("🔧 Circuit Breakers configurados via anotações (@CircuitBreaker, @Retry, @TimeLimiter)");
        logger.info("📋 Configurações definidas em application.yml - seção resilience4j");
    }
}