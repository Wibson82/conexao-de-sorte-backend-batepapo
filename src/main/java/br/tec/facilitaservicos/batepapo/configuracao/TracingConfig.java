package br.tec.facilitaservicos.batepapo.configuracao;

import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração de Distributed Tracing para Chat Service
 */
@Configuration
public class TracingConfig {

    private static final Logger logger = LoggerFactory.getLogger(TracingConfig.class);

    /**
     * Repositório para armazenar exchanges HTTP para observabilidade
     */
    @Bean
    public HttpExchangeRepository httpExchangeRepository() {
        logger.info("🔍 Configurando HTTP Exchange Repository para tracing");
        return new InMemoryHttpExchangeRepository();
    }

    /**
     * Log quando o tracing é inicializado
     */
    @Bean
    public String tracingInfo() {
        logger.info("📊 Distributed Tracing configurado para Chat Service");
        logger.info("🎯 Traces serão enviados para: ${management.zipkin.tracing.endpoint:http://localhost:9411/api/v2/spans}");
        logger.info("🔧 Sampling rate: ${management.tracing.sampling.probability:0.1}");
        return "Tracing configured";
    }
}