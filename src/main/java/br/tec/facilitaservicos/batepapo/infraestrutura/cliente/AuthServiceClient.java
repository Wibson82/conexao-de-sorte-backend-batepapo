package br.tec.facilitaservicos.batepapo.infraestrutura.cliente;

import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import reactor.core.publisher.Mono;

/**
 * Cliente para integração com o Auth Service
 * Responsável por validar tokens e verificar status de usuários
 */
@Component
public class AuthServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceClient.class);

    private final WebClient webClient;
    private final String authBaseUrl;
    private final String validateEndpoint;
    private final String usersEndpoint;

    public AuthServiceClient(WebClient.Builder webClientBuilder,
                           @Value("${services.auth.url}") String authBaseUrl,
                           @Value("${services.auth.endpoints.validate}") String validateEndpoint,
                           @Value("${services.auth.endpoints.users}") String usersEndpoint) {
        this.authBaseUrl = authBaseUrl;
        this.validateEndpoint = validateEndpoint;
        this.usersEndpoint = usersEndpoint;
        this.webClient = webClientBuilder
            .baseUrl(this.authBaseUrl)
            .build();
        
        logger.info("🔐 AuthServiceClient configurado: baseUrl={} com Circuit Breaker", this.authBaseUrl);
    }

    /**
     * Verifica se um usuário está online
     */
    @CircuitBreaker(name = "auth-service", fallbackMethod = "fallbackIsUserOnline")
    @Retry(name = "auth-service")
    @TimeLimiter(name = "auth-service")
    public Mono<Boolean> isUserOnline(Long userId) {
        logger.debug("🔍 Verificando se usuário está online: userId={}", userId);

        return webClient
            .get()
            .uri(usersEndpoint + "/{userId}/status", userId)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .map(response -> {
                Object onlineVal = response.get("online");
                boolean online = onlineVal instanceof Boolean ? (Boolean) onlineVal : Boolean.parseBoolean(String.valueOf(onlineVal));
                logger.debug("✅ Status do usuário {}: {}", userId, online ? "online" : "offline");
                return online;
            })
            .onErrorResume(WebClientResponseException.class, ex -> {
                if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                    logger.warn("⚠️ Usuário não encontrado: userId={}", userId);
                    return Mono.just(false);
                } else if (ex.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
                    logger.warn("⚠️ Auth Service indisponível, assumindo usuário online: userId={}", userId);
                    return Mono.just(true); // Graceful degradation
                }
                logger.error("❌ Erro ao verificar status do usuário {}: {}", userId, ex.getMessage());
                return Mono.just(false);
            })
            .onErrorResume(Exception.class, ex -> {
                logger.error("❌ Erro inesperado ao verificar usuário {}: {}", userId, ex.getMessage());
                return Mono.just(false);
            })
            .doOnSuccess(online -> {
                if (online) {
                    logger.debug("🟢 Usuário {} está online", userId);
                } else {
                    logger.debug("🔴 Usuário {} está offline", userId);
                }
            });
    }

    /**
     * Valida um token JWT
     */
    @CircuitBreaker(name = "auth-service", fallbackMethod = "fallbackValidateToken")
    @Retry(name = "auth-service")
    @TimeLimiter(name = "auth-service")
    public Mono<Boolean> validateToken(String token) {
        logger.debug("🔍 Validando token JWT");

        return webClient
            .post()
            .uri(validateEndpoint)
            .header("Authorization", "Bearer " + token)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .map(response -> {
                Object validVal = response.get("valid");
                boolean valid = validVal instanceof Boolean ? (Boolean) validVal : Boolean.parseBoolean(String.valueOf(validVal));
                logger.debug("✅ Token válido: {}", valid);
                return valid;
            })
            .onErrorResume(WebClientResponseException.class, ex -> {
                if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    logger.debug("🔴 Token inválido ou expirado");
                    return Mono.just(false);
                } else if (ex.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
                    logger.warn("⚠️ Auth Service indisponível para validação de token");
                    return Mono.just(false);
                }
                logger.error("❌ Erro ao validar token: {}", ex.getMessage());
                return Mono.just(false);
            })
            .onErrorReturn(false);
    }

    /**
     * Obtém informações básicas do usuário
     */
    @CircuitBreaker(name = "auth-service", fallbackMethod = "fallbackGetUserInfo")
    @Retry(name = "auth-service")
    @TimeLimiter(name = "auth-service")
    public Mono<Map<String, Object>> getUserInfo(Long userId) {
        logger.debug("👤 Obtendo informações do usuário: userId={}", userId);

        return webClient
            .get()
            .uri(usersEndpoint + "/{userId}", userId)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .timeout(Duration.ofSeconds(5))
            .onErrorResume(WebClientResponseException.class, ex -> {
                if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                    logger.warn("⚠️ Usuário não encontrado: userId={}", userId);
                    return Mono.just(Map.of(
                        "id", userId,
                        "username", "usuario_" + userId,
                        "fullName", "Usuário Desconhecido",
                        "active", false
                    ));
                }
                logger.error("❌ Erro ao obter info do usuário {}: {}", userId, ex.getMessage());
                return Mono.just(Map.of("id", userId, "active", false));
            })
            .onErrorReturn(Map.of("id", userId, "active", false))
            .doOnSuccess(v -> logger.debug("✅ Info do usuário obtida: userId={}", userId));
    }

    /**
     * Verifica se o Auth Service está disponível
     */
    public Mono<Boolean> isAuthServiceHealthy() {
        return webClient
            .get()
            .uri("/actuator/health")
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .map(response -> "UP".equals(String.valueOf(response.get("status"))))
            .timeout(Duration.ofSeconds(2))
            .onErrorReturn(false)
            .doOnSuccess(healthy -> {
                if (healthy) {
                    logger.debug("✅ Auth Service está saudável");
                } else {
                    logger.warn("⚠️ Auth Service não está saudável");
                }
            });
    }
    
    // Métodos de fallback
    
    public Mono<Boolean> fallbackIsUserOnline(Long userId, Exception ex) {
        logger.warn("🔴 Fallback isUserOnline para usuário {}: {}", userId, ex.getMessage());
        return Mono.just(false); // Assume offline em caso de falha
    }
    
    public Mono<Boolean> fallbackValidateToken(String token, Exception ex) {
        logger.warn("🔴 Fallback validateToken: {}", ex.getMessage());
        return Mono.just(false); // Token inválido em caso de falha
    }
    
    public Mono<Map<String, Object>> fallbackGetUserInfo(Long userId, Exception ex) {
        logger.warn("🔴 Fallback getUserInfo para usuário {}: {}", userId, ex.getMessage());
        return Mono.just(Map.of(
            "id", userId,
            "username", "usuario_" + userId,
            "active", false,
            "fallback", true
        ));
    }
}