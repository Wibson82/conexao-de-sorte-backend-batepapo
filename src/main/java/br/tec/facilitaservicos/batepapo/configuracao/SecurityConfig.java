package br.tec.facilitaservicos.batepapo.configuracao;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * ============================================================================
 * 🔐 CONFIGURAÇÃO DE SEGURANÇA REATIVA - MICROSERVIÇO BATE-PAPO
 * ============================================================================
 * 
 * Configuração de segurança 100% reativa para WebFlux:
 * - Validação JWT via JWKS do microserviço de autenticação
 * - Proteção para todos os endpoints do chat
 * - CORS configurado para WebSockets e SSE
 * - Headers de segurança otimizados para tempo real
 * - Rate limiting configurado via properties
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("#{'${cors.allowed-origins}'.split(',')}")
    private List<String> allowedOrigins;

    @Value("#{'${cors.allowed-methods}'.split(',')}")
    private List<String> allowedMethods;

    @Value("${cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${cors.max-age:3600}")
    private long maxAge;

    /**
     * Configuração principal da cadeia de filtros de segurança
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            // Desabilitar proteções desnecessárias para API reativa
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())

            // Configurar CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Configurar autorização
            .authorizeExchange(exchanges -> exchanges
                // Endpoints públicos (sem autenticação)
                .pathMatchers(
                    // Actuator/Health checks
                    "/actuator/health/**",
                    "/actuator/info",
                    "/actuator/metrics",
                    "/actuator/prometheus",
                    
                    // Documentação OpenAPI
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/webjars/**",
                    
                    // Favicon
                    "/favicon.ico"
                ).permitAll()
                
                // Todos os endpoints de chat requerem autenticação
                .pathMatchers("/api/chat/**", "/ws/**").authenticated()
                
                // Endpoints administrativos (requerem scope admin)
                .pathMatchers("/actuator/**").hasAuthority("SCOPE_admin")
                
                // Qualquer outra requisição requer autenticação
                .anyExchange().authenticated()
            )

            // Configurar JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtDecoder(reactiveJwtDecoder())
                )
            )

            // Headers de segurança específicos para chat
            .headers(headers -> headers
                // Content Security Policy adaptada para WebSockets e SSE
                .contentSecurityPolicy("default-src 'self'; " +
                                     "script-src 'self' 'unsafe-inline'; " +
                                     "style-src 'self' 'unsafe-inline'; " +
                                     "img-src 'self' data: https:; " +
                                     "connect-src 'self' ws: wss:; " +
                                     "worker-src 'self'")
                
                // Outros headers de segurança
                .frameOptions(frame -> frame.deny())
                .referrerPolicy(ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)
                    .includeSubdomains(true)
                )
            )

            // Configurar tratamento de exceções de segurança
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((exchange, ex) -> {
                    var response = exchange.getResponse();
                    response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                    response.getHeaders().add("Content-Type", "application/json");
                    
                    String body = """
                        {
                            "status": 401,
                            "erro": "Não autorizado",
                            "mensagem": "Token JWT inválido ou ausente para acessar o chat",
                            "timestamp": "%s"
                        }
                        """.formatted(java.time.LocalDateTime.now());
                    
                    var buffer = response.bufferFactory().wrap(body.getBytes());
                    return response.writeWith(reactor.core.publisher.Mono.just(buffer));
                })
                .accessDeniedHandler((exchange, denied) -> {
                    var response = exchange.getResponse();
                    response.setStatusCode(org.springframework.http.HttpStatus.FORBIDDEN);
                    response.getHeaders().add("Content-Type", "application/json");
                    
                    String body = """
                        {
                            "status": 403,
                            "erro": "Acesso negado",
                            "mensagem": "Permissões insuficientes para acessar este recurso do chat",
                            "timestamp": "%s"
                        }
                        """.formatted(java.time.LocalDateTime.now());
                    
                    var buffer = response.bufferFactory().wrap(body.getBytes());
                    return response.writeWith(reactor.core.publisher.Mono.just(buffer));
                })
            )

            .build();
    }

    /**
     * Decodificador JWT reativo via JWKS
     */
    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    /**
     * Configuração CORS específica para chat (WebSockets + SSE)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Origins permitidas
        configuration.setAllowedOrigins(allowedOrigins);
        
        // Métodos HTTP permitidos (incluindo OPTIONS para WebSocket)
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        
        // Headers permitidos (incluindo WebSocket)
        configuration.setAllowedHeaders(List.of("*"));
        
        // Permitir cookies/credenciais
        configuration.setAllowCredentials(allowCredentials);
        
        // Cache preflight
        configuration.setMaxAge(maxAge);
        
        // Headers expostos (incluindo SSE)
        configuration.setExposedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "Cache-Control",
            "Connection",
            "Keep-Alive",
            "Transfer-Encoding",
            "X-Total-Count",
            "X-Chat-Room",
            "X-User-Count"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}