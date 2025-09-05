
package br.tec.facilitaservicos.batepapo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.r2dbc.ConnectionFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;

import io.r2dbc.spi.ConnectionFactory;
import java.time.Duration;

/**
 * Configuração R2DBC usando Spring Boot ConnectionFactoryBuilder
 * para compatibilidade com Spring Boot 3.5.5 e options-capable ConnectionFactory
 */
@Configuration
@EnableR2dbcRepositories(basePackages = "br.tec.facilitaservicos.batepapo.dominio.repositorio")
public class R2dbcConfig {

    @Value("${spring.r2dbc.url}")
    private String url;

    @Value("${spring.r2dbc.username}")
    private String username;

    @Value("${spring.r2dbc.password}")
    private String password;

    @Value("${spring.r2dbc.pool.initial-size:5}")
    private int initialSize;

    @Value("${spring.r2dbc.pool.max-size:30}")
    private int maxSize;

    @Value("${spring.r2dbc.pool.max-idle-time:30m}")
    private Duration maxIdleTime;
    
    @Value("${spring.r2dbc.pool.max-acquire-time:60s}")
    private Duration maxAcquireTime;

    /**
     * ConnectionFactory using Spring Boot ConnectionFactoryBuilder
     * This ensures the ConnectionFactory is options-capable for Spring Boot 3.5.5
     */
    @Bean
    @Primary
    public ConnectionFactory connectionFactory() {
        return ConnectionFactoryBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .option("initialSize", String.valueOf(initialSize))
                .option("maxSize", String.valueOf(maxSize))
                .option("maxIdleTime", maxIdleTime.toString())
                .option("maxAcquireTime", maxAcquireTime.toString())
                .option("acquireRetry", "3")
                .option("validationQuery", "SELECT 1")
                .build();
    }

    @Bean
    @Primary
    public ReactiveTransactionManager transactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }
}
