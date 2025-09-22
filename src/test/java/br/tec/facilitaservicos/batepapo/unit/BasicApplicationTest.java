package br.tec.facilitaservicos.batepapo.unit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ============================================================================
 * 🧪 TESTE BÁSICO DE APLICAÇÃO - COMPATÍVEL JAVA 25
 * ============================================================================
 * 
 * Teste mínimo para verificar se a aplicação inicializa:
 * - Sem Mockito
 * - Sem dependências externas
 * - Configuração mínima
 * - Apenas teste de contexto
 * 
 * @author Sistema de Testes Java 25
 * @version 1.0
 * @since 2024
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = {br.tec.facilitaservicos.batepapo.BatepapoSimplesApplication.class}
)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.r2dbc.url=r2dbc:h2:mem:///testdb",
    "spring.r2dbc.username=sa",
    "spring.r2dbc.password=",
    "spring.flyway.enabled=false",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9999/.well-known/jwks.json",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379",
    "feature.batepapo-ms=true",
    "feature.websocket=false",
    "feature.sse=false",
    "feature.redis-streams=false",
    "management.endpoints.web.exposure.include=health"
})
class BasicApplicationTest {

    @Test
    void testContextLoads() {
        // Teste básico - se chegou até aqui, o contexto Spring carregou com sucesso
        assertTrue(true, "Application context loads successfully without Mockito");
    }

    @Test
    void testJavaVersion() {
        // Verificar se estamos usando Java 25
        String javaVersion = System.getProperty("java.version");
        assertTrue(javaVersion.startsWith("25"), "Should be running on Java 25");
    }

    @Test
    void testClassLoading() {
        // Verificar se as principais classes estão disponíveis
        try {
            Class.forName("br.tec.facilitaservicos.batepapo.BatepapoSimplesApplication");
            assertTrue(true, "Main application class loads successfully");
        } catch (ClassNotFoundException e) {
            assertTrue(false, "Main application class not found: " + e.getMessage());
        }
    }

    @Test
    void testSpringBootAvailability() {
        // Verificar se Spring Boot está disponível
        try {
            Class.forName("org.springframework.boot.SpringApplication");
            assertTrue(true, "Spring Boot classes are available");
        } catch (ClassNotFoundException e) {
            assertTrue(false, "Spring Boot not available: " + e.getMessage());
        }
    }

    @Test
    void testR2dbcAvailability() {
        // Verificar se R2DBC está disponível
        try {
            Class.forName("org.springframework.r2dbc.core.DatabaseClient");
            assertTrue(true, "R2DBC classes are available");
        } catch (ClassNotFoundException e) {
            assertTrue(false, "R2DBC not available: " + e.getMessage());
        }
    }
}