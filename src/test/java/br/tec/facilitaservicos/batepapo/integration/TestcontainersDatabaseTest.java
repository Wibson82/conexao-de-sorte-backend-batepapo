package br.tec.facilitaservicos.batepapo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ============================================================================
 * 🧪 TESTE DE INTEGRAÇÃO COM TESTCONTAINERS - COMPATÍVEL JAVA 24
 * ============================================================================
 * 
 * Testa integração com banco de dados real usando Testcontainers:
 * - MySQL container real para testes
 * - Testes de operações R2DBC
 * - Verificação de schema e dados
 * - Sem dependência do Mockito
 * 
 * @author Sistema de Testes Java 24
 * @version 1.0
 * @since 2024
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test-containers")
class TestcontainersDatabaseTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("batepapo_test")
            .withUsername("testuser")
            .withPassword("testpass")
            .withExposedPorts(3306);

    @Autowired
    private DatabaseClient databaseClient;

    @Test
    void testDatabaseConnection() {
        // Verificar se o container está rodando
        assertThat(mysql.isRunning()).isTrue();
        assertThat(mysql.getJdbcUrl()).contains("batepapo_test");
    }

    @Test
    void testCreateAndQueryTable() {
        // Criar tabela de teste
        StepVerifier.create(
                databaseClient.sql("CREATE TABLE IF NOT EXISTS test_messages (" +
                        "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                        "content VARCHAR(500) NOT NULL, " +
                        "sender VARCHAR(100) NOT NULL, " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ")")
                        .fetch()
                        .rowsUpdated()
        )
        .expectNext(0L) // CREATE TABLE retorna 0 rows updated
        .verifyComplete();

        // Inserir dados de teste
        StepVerifier.create(
                databaseClient.sql("INSERT INTO test_messages (content, sender) VALUES (?, ?)")
                        .bind(0, "Mensagem de teste")
                        .bind(1, "usuario_teste")
                        .fetch()
                        .rowsUpdated()
        )
        .expectNext(1L)
        .verifyComplete();

        // Consultar dados inseridos
        StepVerifier.create(
                databaseClient.sql("SELECT id, content, sender FROM test_messages WHERE sender = ?")
                        .bind(0, "usuario_teste")
                        .map(row -> {
                            return String.format("ID: %s, Content: %s, Sender: %s",
                                    row.get("id"),
                                    row.get("content"),
                                    row.get("sender"));
                        })
                        .all()
        )
        .assertNext(result -> {
            assertThat(result).contains("Content: Mensagem de teste");
            assertThat(result).contains("Sender: usuario_teste");
            assertThat(result).contains("ID: ");
        })
        .verifyComplete();
    }

    @Test
    void testMultipleInserts() {
        // Criar tabela de teste para múltiplos inserts
        StepVerifier.create(
                databaseClient.sql("CREATE TABLE IF NOT EXISTS test_chat_rooms (" +
                        "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                        "name VARCHAR(100) NOT NULL, " +
                        "description TEXT, " +
                        "active BOOLEAN DEFAULT TRUE" +
                        ")")
                        .fetch()
                        .rowsUpdated()
        )
        .expectNext(0L)
        .verifyComplete();

        // Inserir múltiplas salas
        String[] roomNames = {"Geral", "Tecnologia", "Jogos", "Música"};
        String[] descriptions = {
            "Sala para conversas gerais",
            "Discussões sobre tecnologia",
            "Falar sobre jogos",
            "Compartilhar músicas"
        };

        for (int i = 0; i < roomNames.length; i++) {
            final int index = i;
            StepVerifier.create(
                    databaseClient.sql("INSERT INTO test_chat_rooms (name, description) VALUES (?, ?)")
                            .bind(0, roomNames[index])
                            .bind(1, descriptions[index])
                            .fetch()
                            .rowsUpdated()
            )
            .expectNext(1L)
            .verifyComplete();
        }

        // Verificar que todas as salas foram inseridas
        StepVerifier.create(
                databaseClient.sql("SELECT COUNT(*) as total FROM test_chat_rooms")
                        .map(row -> (Long) row.get("total"))
                        .one()
        )
        .assertNext(total -> assertThat(total).isEqualTo(4L))
        .verifyComplete();

        // Verificar salas ativas
        StepVerifier.create(
                databaseClient.sql("SELECT name FROM test_chat_rooms WHERE active = TRUE ORDER BY name")
                        .map(row -> (String) row.get("name"))
                        .all()
                        .collectList()
        )
        .assertNext(rooms -> {
            assertThat(rooms).hasSize(4);
            assertThat(rooms).contains("Geral", "Tecnologia", "Jogos", "Música");
        })
        .verifyComplete();
    }

    @Test
    void testTransactionRollback() {
        // Criar tabela para teste de transação
        StepVerifier.create(
                databaseClient.sql("CREATE TABLE IF NOT EXISTS test_users (" +
                        "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                        "username VARCHAR(50) UNIQUE NOT NULL, " +
                        "email VARCHAR(100) NOT NULL" +
                        ")")
                        .fetch()
                        .rowsUpdated()
        )
        .expectNext(0L)
        .verifyComplete();

        // Inserir usuário válido
        StepVerifier.create(
                databaseClient.sql("INSERT INTO test_users (username, email) VALUES (?, ?)")
                        .bind(0, "user1")
                        .bind(1, "user1@test.com")
                        .fetch()
                        .rowsUpdated()
        )
        .expectNext(1L)
        .verifyComplete();

        // Tentar inserir usuário com username duplicado (deve falhar)
        StepVerifier.create(
                databaseClient.sql("INSERT INTO test_users (username, email) VALUES (?, ?)")
                        .bind(0, "user1") // Username duplicado
                        .bind(1, "user1duplicate@test.com")
                        .fetch()
                        .rowsUpdated()
        )
        .expectError() // Deve falhar por causa da constraint UNIQUE
        .verify();

        // Verificar que apenas o primeiro usuário existe
        StepVerifier.create(
                databaseClient.sql("SELECT COUNT(*) as total FROM test_users WHERE username = ?")
                        .bind(0, "user1")
                        .map(row -> (Long) row.get("total"))
                        .one()
        )
        .assertNext(total -> assertThat(total).isEqualTo(1L))
        .verifyComplete();
    }

    @Test
    void testComplexQuery() {
        // Criar estrutura mais complexa para teste
        StepVerifier.create(
                databaseClient.sql("CREATE TABLE IF NOT EXISTS test_message_stats (" +
                        "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                        "room_name VARCHAR(100) NOT NULL, " +
                        "message_count INT DEFAULT 0, " +
                        "last_message_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ")")
                        .fetch()
                        .rowsUpdated()
        )
        .expectNext(0L)
        .verifyComplete();

        // Inserir dados de teste
        String[] rooms = {"geral", "tech", "games"};
        int[] messageCounts = {150, 89, 203};

        for (int i = 0; i < rooms.length; i++) {
            final int index = i;
            StepVerifier.create(
                    databaseClient.sql("INSERT INTO test_message_stats (room_name, message_count) VALUES (?, ?)")
                            .bind(0, rooms[index])
                            .bind(1, messageCounts[index])
                            .fetch()
                            .rowsUpdated()
            )
            .expectNext(1L)
            .verifyComplete();
        }

        // Query complexa: encontrar salas com mais de 100 mensagens
        StepVerifier.create(
                databaseClient.sql("SELECT room_name, message_count FROM test_message_stats " +
                        "WHERE message_count > 100 ORDER BY message_count DESC")
                        .map(row -> String.format("%s:%d", row.get("room_name"), row.get("message_count")))
                        .all()
                        .collectList()
        )
        .assertNext(results -> {
            assertThat(results).hasSize(2);
            assertThat(results.get(0)).isEqualTo("games:203");
            assertThat(results.get(1)).isEqualTo("geral:150");
        })
        .verifyComplete();

        // Agregação: soma total de mensagens
        StepVerifier.create(
                databaseClient.sql("SELECT SUM(message_count) as total_messages FROM test_message_stats")
                        .map(row -> (Long) row.get("total_messages"))
                        .one()
        )
        .assertNext(total -> assertThat(total).isEqualTo(442L))
        .verifyComplete();
    }
}