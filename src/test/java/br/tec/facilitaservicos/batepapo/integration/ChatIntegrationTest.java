package br.tec.facilitaservicos.batepapo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import br.tec.facilitaservicos.batepapo.apresentacao.dto.MensagemDto;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

/**
 * ============================================================================
 * 🧪 TESTES DE INTEGRAÇÃO - BATE-PAPO
 * ============================================================================
 * 
 * Testes de integração para validar a funcionalidade completa do microserviço:
 * - Autenticação JWT
 * - Endpoints REST
 * - Server-Sent Events
 * - Integração com bancos
 * - Chat em tempo real
 * - Controle de presença
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class ChatIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void devePermitirAccessoComTokenJWTValido() {
        webTestClient
            .mutateWith(mockJwt()
                .authorities("ROLE_USER")
                .jwt(jwt -> jwt
                    .subject("user123")
                    .claim("roles", "USER")
                ))
            .get()
            .uri("/api/chat/salas")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void deveNegarAccessoSemToken() {
        webTestClient
            .get()
            .uri("/api/chat/salas")
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    void devePermitirAccessoSalasPublicas() {
        // Salas públicas devem ser acessíveis sem autenticação
        webTestClient
            .get()
            .uri("/api/chat/salas/publicas")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void devePermitirEnvioMensagemComAutenticacao() {
        MensagemDto mensagem = MensagemDto.builder()
                .sala("geral")
                .conteudo("Olá, pessoal!")
                .build();

        webTestClient
            .mutateWith(mockJwt()
                .authorities("ROLE_USER")
                .jwt(jwt -> jwt
                    .subject("user123")
                    .claim("roles", "USER")
                ))
            .post()
            .uri("/api/chat/mensagem")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mensagem)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.conteudo").isEqualTo("Olá, pessoal!")
            .jsonPath("$.sala").isEqualTo("geral")
            .jsonPath("$.autorId").isEqualTo("user123");
    }

    @Test
    void deveListarMensagensComPaginacao() {
        webTestClient
            .mutateWith(mockJwt()
                .authorities("ROLE_USER")
                .jwt(jwt -> jwt
                    .subject("user123")
                    .claim("roles", "USER")
                ))
            .get()
            .uri("/api/chat/mensagens/geral?page=0&size=10")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON);
    }

    @Test
    void devePermitirEntrarSair() {
        // Entrar na sala
        webTestClient
            .mutateWith(mockJwt()
                .authorities("ROLE_USER")
                .jwt(jwt -> jwt
                    .subject("user123")
                    .claim("roles", "USER")
                ))
            .post()
            .uri("/api/chat/entrar/geral")
            .exchange()
            .expectStatus().isOk();

        // Sair da sala
        webTestClient
            .mutateWith(mockJwt()
                .authorities("ROLE_USER")
                .jwt(jwt -> jwt
                    .subject("user123")
                    .claim("roles", "USER")
                ))
            .delete()
            .uri("/api/chat/sair/geral")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void deveListarUsuariosOnline() {
        webTestClient
            .mutateWith(mockJwt()
                .authorities("ROLE_USER")
                .jwt(jwt -> jwt
                    .subject("user123")
                    .claim("roles", "USER")
                ))
            .get()
            .uri("/api/chat/online/geral")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON);
    }

    @Test
    void deveAtualizarHeartbeat() {
        webTestClient
            .mutateWith(mockJwt()
                .authorities("ROLE_USER")
                .jwt(jwt -> jwt
                    .subject("user123")
                    .claim("roles", "USER")
                ))
            .put()
            .uri("/api/chat/heartbeat")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void devePermitirHealthCheckPublico() {
        webTestClient
            .get()
            .uri("/api/chat/health")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("UP")
            .jsonPath("$.service").isEqualTo("chat-microservice")
            .jsonPath("$.features.jwt_auth").isEqualTo(true)
            .jsonPath("$.features.sse").isEqualTo(true);
    }

    @Test
    void deveValidarRolesDiferentes() {
        // Usuário normal
        webTestClient
            .mutateWith(mockJwt()
                .authorities("ROLE_USER")
                .jwt(jwt -> jwt
                    .subject("user123")
                    .claim("roles", "USER")
                ))
            .get()
            .uri("/api/chat/salas")
            .exchange()
            .expectStatus().isOk();

        // Moderador deve ter acesso a estatísticas
        webTestClient
            .mutateWith(mockJwt()
                .authorities("ROLE_CHAT_MODERATOR")
                .jwt(jwt -> jwt
                    .subject("moderator123")
                    .claim("roles", "MODERATOR")
                ))
            .get()
            .uri("/api/chat/salas/geral/estatisticas")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void deveValidarConexaoSSE() {
        // Teste básico de conexão SSE
        webTestClient
            .mutateWith(mockJwt()
                .authorities("ROLE_USER")
                .jwt(jwt -> jwt
                    .subject("user123")
                    .claim("roles", "USER")
                ))
            .get()
            .uri("/api/chat/stream/geral")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.TEXT_EVENT_STREAM);
    }

    @Test
    void deveValidarStreamPresenca() {
        webTestClient
            .mutateWith(mockJwt()
                .authorities("ROLE_USER")
                .jwt(jwt -> jwt
                    .subject("user123")
                    .claim("roles", "USER")
                ))
            .get()
            .uri("/api/chat/stream/presenca")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.TEXT_EVENT_STREAM);
    }
}