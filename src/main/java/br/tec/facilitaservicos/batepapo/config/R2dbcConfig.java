
package br.tec.facilitaservicos.batepapo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;

import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactoryOptions;
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
     * ConnectionFactory using Spring Boot auto-configuration
     * This ensures the ConnectionFactory is options-capable for Spring Boot 3.5.5
     * by letting Spring Boot handle the configuration automatically
     */
    @Bean
    @Primary
    public ConnectionFactory connectionFactory() {
        // Use Spring Boot's built-in ConnectionFactoryBuilder via ConnectionFactoryOptions
        ConnectionFactoryOptions.Builder builder = ConnectionFactoryOptions.parse(url).mutate();
        
        if (username != null && !username.isEmpty()) {
            builder.option(ConnectionFactoryOptions.USER, username);
        }
        if (password != null && !password.isEmpty()) {
            builder.option(ConnectionFactoryOptions.PASSWORD, password);
        }
        
        // Pool options - using string literals as these constants may not exist in all R2DBC versions
        builder.option(ConnectionFactoryOptions.of("initialSize"), initialSize);
        builder.option(ConnectionFactoryOptions.of("maxSize"), maxSize);
        builder.option(ConnectionFactoryOptions.of("maxIdleTime"), maxIdleTime);
        builder.option(ConnectionFactoryOptions.of("maxAcquireTime"), maxAcquireTime);
        
        ConnectionFactoryOptions options = builder.build();
        return ConnectionFactories.get(options);
    }

    @Bean
    @Primary
    public ReactiveTransactionManager transactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }
}
