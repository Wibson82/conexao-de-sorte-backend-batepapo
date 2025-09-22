package br.tec.facilitaservicos.batepapo.integration;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ============================================================================
 * 🧪 TESTE COM MOCKWEBSERVER (OKHTTP) - COMPATÍVEL JAVA 25
 * ============================================================================
 * 
 * Testa HTTP clients usando MockWebServer do OkHttp:
 * - Mock de respostas HTTP
 * - Verificação de requests enviados
 * - Teste de diferentes status codes
 * - Sem dependência do Mockito
 * 
 * @author Sistema de Testes Java 25
 * @version 1.0
 * @since 2024
 */
class MockWebServerHttpTest {

    private MockWebServer mockWebServer;
    private WebClient webClient;

    @BeforeEach
    void setUp() throws IOException {
        // Configurar MockWebServer
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Configurar WebClient para apontar para o MockWebServer
        String baseUrl = mockWebServer.url("/").toString();
        webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .build();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (mockWebServer != null) {
            mockWebServer.shutdown();
        }
    }

    @Test
    void testGetRequest_Success() throws InterruptedException {
        // Configurar resposta mock
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"message\": \"Hello from mock server\", \"status\": \"success\"}"));

        // Executar request
        Mono<String> response = webClient.get()
            .uri("/api/test")
            .retrieve()
            .bodyToMono(String.class);

        // Verificar resultado
        StepVerifier.create(response)
            .assertNext(body -> {
                assertThat(body).contains("\"message\": \"Hello from mock server\"");
                assertThat(body).contains("\"status\": \"success\"");
            })
            .verifyComplete();

        // Verificar request enviado
        RecordedRequest recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recordedRequest).isNotNull();
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).isEqualTo("/api/test");
    }

    @Test
    void testPostRequest_WithBody() throws InterruptedException {
        // Configurar resposta mock
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(201)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"id\": 123, \"created\": true}"));

        // Executar POST request
        Mono<String> response = webClient.post()
            .uri("/api/messages")
            .header("Content-Type", "application/json")
            .bodyValue("{\"content\": \"Test message\", \"sender\": \"testuser\"}")
            .retrieve()
            .bodyToMono(String.class);

        // Verificar resultado
        StepVerifier.create(response)
            .assertNext(body -> {
                assertThat(body).contains("\"id\": 123");
                assertThat(body).contains("\"created\": true");
            })
            .verifyComplete();

        // Verificar request enviado
        RecordedRequest recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recordedRequest).isNotNull();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getPath()).isEqualTo("/api/messages");
        assertThat(recordedRequest.getHeader("Content-Type")).isEqualTo("application/json");
        
        String requestBody = recordedRequest.getBody().readUtf8();
        assertThat(requestBody).contains("\"content\": \"Test message\"");
        assertThat(requestBody).contains("\"sender\": \"testuser\"");
    }

    @Test
    void testErrorResponse_4xx() {
        // Configurar resposta de erro
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(404)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"error\": \"Resource not found\", \"code\": 404}"));

        // Executar request
        Mono<String> response = webClient.get()
            .uri("/api/nonexistent")
            .retrieve()
            .onStatus(status -> status.is4xxClientError(),
                clientResponse -> Mono.just(new RuntimeException("Client Error")))
            .bodyToMono(String.class)
            .onErrorReturn("Error occurred");

        // Verificar que erro foi tratado
        StepVerifier.create(response)
            .assertNext(body -> {
                assertThat(body).isEqualTo("Error occurred");
            })
            .verifyComplete();
    }

    @Test
    void testErrorResponse_5xx() {
        // Configurar resposta de erro do servidor
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"error\": \"Internal server error\", \"code\": 500}"));

        // Executar request
        Mono<String> response = webClient.get()
            .uri("/api/error")
            .retrieve()
            .onStatus(status -> status.is5xxServerError(),
                serverResponse -> Mono.just(new RuntimeException("Server Error")))
            .bodyToMono(String.class)
            .onErrorReturn("Server Error occurred");

        // Verificar que erro foi tratado
        StepVerifier.create(response)
            .assertNext(body -> {
                assertThat(body).isEqualTo("Server Error occurred");
            })
            .verifyComplete();
    }

    @Test
    void testMultipleRequests() throws InterruptedException {
        // Configurar múltiplas respostas
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("{\"message\": \"First response\"}"));
        
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("{\"message\": \"Second response\"}"));

        // Executar primeira request
        Mono<String> firstResponse = webClient.get()
            .uri("/api/first")
            .retrieve()
            .bodyToMono(String.class);

        // Executar segunda request
        Mono<String> secondResponse = webClient.get()
            .uri("/api/second")
            .retrieve()
            .bodyToMono(String.class);

        // Verificar ambas as respostas
        StepVerifier.create(firstResponse)
            .assertNext(body -> assertThat(body).contains("First response"))
            .verifyComplete();

        StepVerifier.create(secondResponse)
            .assertNext(body -> assertThat(body).contains("Second response"))
            .verifyComplete();

        // Verificar que ambas as requests foram enviadas
        RecordedRequest firstRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(firstRequest.getPath()).isEqualTo("/api/first");

        RecordedRequest secondRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(secondRequest.getPath()).isEqualTo("/api/second");
    }

    @Test
    void testWithHeaders() throws InterruptedException {
        // Configurar resposta mock
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("X-Custom-Header", "test-value")
            .setBody("{\"authenticated\": true}"));

        // Executar request com headers
        Mono<String> response = webClient.get()
            .uri("/api/secure")
            .header("Authorization", "Bearer test-token")
            .header("X-Client-Id", "test-client")
            .retrieve()
            .bodyToMono(String.class);

        // Verificar resultado
        StepVerifier.create(response)
            .assertNext(body -> assertThat(body).contains("\"authenticated\": true"))
            .verifyComplete();

        // Verificar headers enviados
        RecordedRequest recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recordedRequest.getHeader("Authorization")).isEqualTo("Bearer test-token");
        assertThat(recordedRequest.getHeader("X-Client-Id")).isEqualTo("test-client");
    }
}