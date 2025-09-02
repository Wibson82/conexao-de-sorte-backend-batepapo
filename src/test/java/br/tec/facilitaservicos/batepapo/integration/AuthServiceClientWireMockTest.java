package br.tec.facilitaservicos.batepapo.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * ============================================================================
 * 🧪 TESTE DE INTEGRAÇÃO COM WIREMOCK - COMPATÍVEL JAVA 24
 * ============================================================================
 * 
 * Testa integração com serviços externos usando WireMock:
 * - Mock de APIs REST externas
 * - Simulação de respostas HTTP
 * - Teste de timeout e erro handling
 * - Sem dependência do Mockito
 * 
 * @author Sistema de Testes Java 24
 * @version 1.0
 * @since 2024
 */
class AuthServiceClientWireMockTest {

    private WireMockServer wireMockServer;
    private WebClient webClient;

    @BeforeEach
    void setUp() {
        // Configurar WireMock Server
        wireMockServer = new WireMockServer(8089); // Porta diferente para evitar conflitos
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);

        // Configurar WebClient para apontar para o WireMock
        webClient = WebClient.builder()
            .baseUrl("http://localhost:8089")
            .build();
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    void testValidateToken_Success() {
        // Configurar mock response
        stubFor(post(urlEqualTo("/auth/validate"))
            .withHeader("Content-Type", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"valid\": true, \"user\": \"testuser\"}")));

        // Executar chamada
        Mono<String> response = webClient.post()
            .uri("/auth/validate")
            .header("Content-Type", "application/json")
            .bodyValue("{\"token\": \"valid-token\"}")
            .retrieve()
            .bodyToMono(String.class);

        // Verificar resultado
        StepVerifier.create(response)
            .assertNext(body -> {
                assertThat(body).contains("\"valid\": true");
                assertThat(body).contains("\"user\": \"testuser\"");
            })
            .verifyComplete();

        // Verificar que a chamada foi feita
        verify(postRequestedFor(urlEqualTo("/auth/validate"))
            .withHeader("Content-Type", equalTo("application/json")));
    }

    @Test
    void testValidateToken_InvalidToken() {
        // Configurar mock response para token inválido
        stubFor(post(urlEqualTo("/auth/validate"))
            .willReturn(aResponse()
                .withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"valid\": false, \"error\": \"Invalid token\"}")));

        // Executar chamada
        Mono<String> response = webClient.post()
            .uri("/auth/validate")
            .bodyValue("{\"token\": \"invalid-token\"}")
            .retrieve()
            .onStatus(status -> status.is4xxClientError(), 
                clientResponse -> Mono.just(new RuntimeException("Unauthorized")))
            .bodyToMono(String.class)
            .onErrorReturn("Unauthorized");

        // Verificar resultado
        StepVerifier.create(response)
            .assertNext(body -> {
                assertThat(body).isEqualTo("Unauthorized");
            })
            .verifyComplete();
    }

    @Test
    void testGetUserInfo_Success() {
        // Configurar mock response
        stubFor(get(urlPathMatching("/users/.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\": 123, \"username\": \"testuser\", \"active\": true}")));

        // Executar chamada
        Mono<String> response = webClient.get()
            .uri("/users/123")
            .retrieve()
            .bodyToMono(String.class);

        // Verificar resultado
        StepVerifier.create(response)
            .assertNext(body -> {
                assertThat(body).contains("\"id\": 123");
                assertThat(body).contains("\"username\": \"testuser\"");
                assertThat(body).contains("\"active\": true");
            })
            .verifyComplete();
    }

    @Test
    void testServiceTimeout() {
        // Configurar mock com delay para testar timeout
        stubFor(get(urlEqualTo("/slow-endpoint"))
            .willReturn(aResponse()
                .withStatus(200)
                .withFixedDelay(5000) // 5 segundos de delay
                .withBody("Too slow")));

        // Configurar WebClient com timeout curto
        WebClient timeoutClient = WebClient.builder()
            .baseUrl("http://localhost:8089")
            .build();

        // Executar chamada com timeout
        Mono<String> response = timeoutClient.get()
            .uri("/slow-endpoint")
            .retrieve()
            .bodyToMono(String.class)
            .timeout(java.time.Duration.ofSeconds(1)) // Timeout de 1 segundo
            .onErrorReturn("Timeout occurred");

        // Verificar que timeout ocorreu
        StepVerifier.create(response)
            .assertNext(body -> {
                assertThat(body).isEqualTo("Timeout occurred");
            })
            .verifyComplete();
    }

    @Test
    void testHealthCheck() {
        // Configurar mock para health check
        stubFor(get(urlEqualTo("/actuator/health"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"status\": \"UP\", \"components\": {\"db\": {\"status\": \"UP\"}}}")));

        // Executar health check
        Mono<String> response = webClient.get()
            .uri("/actuator/health")
            .retrieve()
            .bodyToMono(String.class);

        // Verificar resultado
        StepVerifier.create(response)
            .assertNext(body -> {
                assertThat(body).contains("\"status\": \"UP\"");
                assertThat(body).contains("\"components\"");
            })
            .verifyComplete();
    }
}