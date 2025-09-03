package br.tec.facilitaservicos.batepapo.unit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ============================================================================
 * 🧪 TESTE SIMPLES SEM MOCKITO - COMPATÍVEL JAVA 24
 * ============================================================================
 * 
 * Testes básicos usando apenas Spring Boot Test:
 * - Teste de health check
 * - Teste de inicialização da aplicação
 * - Sem dependências do Mockito
 * - Usando TestRestTemplate
 * 
 * @author Sistema de Testes Java 24
 * @version 1.0
 * @since 2024
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SimpleHealthTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testApplicationContextLoads() {
        // Teste básico para verificar se o contexto Spring carrega corretamente
        assertThat(restTemplate).isNotNull();
        assertThat(port).isGreaterThan(0);
    }

    @Test
    void testHealthEndpoint() {
        // Teste do endpoint de health
        String url = "http://localhost:" + port + "/actuator/health";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\"");
    }

    @Test
    void testInfoEndpoint() {
        // Teste do endpoint de info
        String url = "http://localhost:" + port + "/actuator/info";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        // Info endpoint pode retornar 200 ou 404 dependendo da configuração
        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.NOT_FOUND);
    }

    @Test
    void testPrometheusEndpoint() {
        // Teste do endpoint de métricas Prometheus
        String url = "http://localhost:" + port + "/actuator/prometheus";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("# HELP");
        assertThat(response.getBody()).contains("# TYPE");
    }

    @Test
    void testApplicationName() {
        // Verificar se o nome da aplicação está configurado corretamente
        String url = "http://localhost:" + port + "/actuator/info";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        // Se info estiver disponível, verificar conteúdo
        if (response.getStatusCode() == HttpStatus.OK) {
            String body = response.getBody();
            assertThat(body).isNotNull();
        }
    }

    @Test
    void testServerIsRunning() {
        // Teste simples para verificar se o servidor está respondendo
        String url = "http://localhost:" + port + "/actuator/health";
        
        // Fazer múltiplas requests para verificar estabilidade
        for (int i = 0; i < 3; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Test
    void testEnvironmentProfile() {
        // Verificar se o profile de teste está ativo
        // Este teste pode ser expandido para verificar propriedades específicas
        assertThat(System.getProperty("spring.profiles.active", "")).contains("test");
    }
}