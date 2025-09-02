package br.tec.facilitaservicos.batepapo.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;

/**
 * Configuração R2DBC específica para testes
 * Simplifica a configuração para usar H2 em memória sem pool de conexões
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@TestConfiguration
@Profile("test")
@EnableR2dbcRepositories(basePackages = "br.tec.facilitaservicos.batepapo.dominio.repositorio")
public class TestR2dbcConfig {

    /**
     * Transaction Manager simplificado para testes
     * Usa a ConnectionFactory padrão do Spring Boot para H2
     */
    @Bean
    @Primary
    public ReactiveTransactionManager testTransactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }
}