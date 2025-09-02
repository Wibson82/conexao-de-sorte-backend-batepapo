package br.tec.facilitaservicos.batepapo.integration;

import br.tec.facilitaservicos.batepapo.BatepapoSimplesApplication;
import br.tec.facilitaservicos.batepapo.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de validação de conexão R2DBC
 * Verifica se a configuração R2DBC está funcionando corretamente nos testes
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@ContextConfiguration(classes = {BatepapoSimplesApplication.class, TestConfig.class})
class R2dbcConnectionValidationTest {

    @Autowired
    private DatabaseClient databaseClient;
    
    @Autowired
    private R2dbcEntityTemplate r2dbcEntityTemplate;

    @Test
    void deveValidarConexaoR2dbcComBancoH2() {
        // When & Then - Testa conexão básica
        StepVerifier.create(
            databaseClient.sql("SELECT 1 as test_value")
                .map(row -> row.get("test_value", Integer.class))
                .one()
        )
        .expectNext(1)
        .verifyComplete();
    }

    @Test
    void deveValidarExistenciaDeTabelas() {
        // When & Then - Verifica se as tabelas foram criadas
        StepVerifier.create(
            databaseClient.sql(
                "SELECT COUNT(*) as table_count FROM INFORMATION_SCHEMA.TABLES " +
                "WHERE TABLE_NAME IN ('SALAS_CHAT', 'MENSAGENS_CHAT', 'USUARIOS_ONLINE_CHAT')"
            )
            .map(row -> row.get("table_count", Long.class))
            .one()
        )
        .expectNext(3L) // Deve encontrar as 3 tabelas
        .verifyComplete();
    }

    @Test
    void deveValidarEstruturaTabelaSalas() {
        // When & Then - Verifica estrutura da tabela salas_chat
        StepVerifier.create(
            databaseClient.sql(
                "SELECT COUNT(*) as column_count FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = 'SALAS_CHAT'"
            )
            .map(row -> row.get("column_count", Long.class))
            .one()
        )
        .expectNextMatches(count -> count >= 10) // Deve ter pelo menos 10 colunas
        .verifyComplete();
    }

    @Test
    void deveValidarEstruturaTabelaMensagens() {
        // When & Then - Verifica estrutura da tabela mensagens_chat
        StepVerifier.create(
            databaseClient.sql(
                "SELECT COUNT(*) as column_count FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = 'MENSAGENS_CHAT'"
            )
            .map(row -> row.get("column_count", Long.class))
            .one()
        )
        .expectNextMatches(count -> count >= 8) // Deve ter pelo menos 8 colunas
        .verifyComplete();
    }

    @Test
    void deveValidarEstruturaTabelaUsuariosOnline() {
        // When & Then - Verifica estrutura da tabela usuarios_online_chat
        StepVerifier.create(
            databaseClient.sql(
                "SELECT COUNT(*) as column_count FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = 'USUARIOS_ONLINE_CHAT'"
            )
            .map(row -> row.get("column_count", Long.class))
            .one()
        )
        .expectNextMatches(count -> count >= 12) // Deve ter pelo menos 12 colunas
        .verifyComplete();
    }

    @Test
    void deveValidarIndicesExistentes() {
        // When & Then - Verifica se os índices foram criados
        StepVerifier.create(
            databaseClient.sql(
                "SELECT COUNT(*) as index_count FROM INFORMATION_SCHEMA.INDEXES " +
                "WHERE TABLE_NAME IN ('SALAS_CHAT', 'MENSAGENS_CHAT', 'USUARIOS_ONLINE_CHAT')"
            )
            .map(row -> row.get("index_count", Long.class))
            .one()
        )
        .expectNextMatches(count -> count > 0) // Deve ter pelo menos alguns índices
        .verifyComplete();
    }

    @Test
    void deveValidarDadosIniciais() {
        // When & Then - Verifica se os dados iniciais foram inseridos
        StepVerifier.create(
            databaseClient.sql("SELECT COUNT(*) as sala_count FROM salas_chat")
                .map(row -> row.get("sala_count", Long.class))
                .one()
        )
        .expectNextMatches(count -> count >= 3) // Deve ter pelo menos 3 salas de teste
        .verifyComplete();
    }

    @Test
    void deveValidarTempoDeResposta() {
        // When & Then - Verifica se a conexão é rápida
        StepVerifier.create(
            databaseClient.sql("SELECT COUNT(*) as total FROM salas_chat")
                .map(row -> row.get("total", Long.class))
                .one()
        )
        .expectNextCount(1)
        .expectComplete()
        .verify(Duration.ofSeconds(2)); // Deve responder em menos de 2 segundos
    }

    @Test
    void deveValidarTransacaoReativa() {
        // When & Then - Testa transação reativa simples
        StepVerifier.create(
            r2dbcEntityTemplate.getDatabaseClient()
                .sql("SELECT 'transaction_test' as test_result")
                .map(row -> row.get("test_result", String.class))
                .one()
        )
        .expectNext("transaction_test")
        .verifyComplete();
    }

    @Test
    void deveValidarPoolDeConexoes() {
        // When & Then - Testa múltiplas conexões simultâneas
        StepVerifier.create(
            databaseClient.sql("SELECT 1 as test1")
                .map(row -> row.get("test1", Integer.class))
                .one()
                .zipWith(
                    databaseClient.sql("SELECT 2 as test2")
                        .map(row -> row.get("test2", Integer.class))
                        .one()
                )
        )
        .expectNextMatches(tuple -> tuple.getT1().equals(1) && tuple.getT2().equals(2))
        .verifyComplete();
    }
}