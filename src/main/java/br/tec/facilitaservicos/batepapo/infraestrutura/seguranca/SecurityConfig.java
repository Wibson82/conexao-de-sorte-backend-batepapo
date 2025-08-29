package br.tec.facilitaservicos.batepapo.infraestrutura.seguranca;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * ============================================================================
 * 🔐 CONFIGURAÇÃO DE SEGURANÇA JWT - BATE-PAPO MICROSERVICE
 * ============================================================================
 * 
 * Configuração de segurança Spring WebFlux com validação JWT:
 * - Validação JWT via JWKS endpoint do microserviço de autenticação
 * - Conversão de claims JWT para roles Spring Security
 * - Configuração CORS para APIs e WebSocket
 * - Endpoints públicos e protegidos
 * - SSE (Server-Sent Events) com autenticação
 * - WebSocket com autenticação JWT
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    /**
     * Configuração principal da cadeia de filtros de segurança
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                // Desabilitar CSRF para APIs REST e WebSocket
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                
                // Configuração CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                
                // Autorização de requisições
                .authorizeExchange(exchanges -> exchanges
                        // Endpoints públicos - não requerem autenticação
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .pathMatchers("/ws/**").permitAll() // WebSocket auth handled separately
                        .pathMatchers("/api/v1/chat/health").permitAll()
                        .pathMatchers("/api/v1/chat/salas/publicas").permitAll() // Salas públicas visíveis para todos
                        
                        // Todos os outros endpoints requerem autenticação
                        .anyExchange().authenticated()
                )
                
                // Configuração JWT
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                )
                
                // Headers de segurança
                .headers(headers -> headers
                        .frameOptions(ServerHttpSecurity.HeaderSpec.FrameOptionsSpec::deny)
                        .contentTypeOptions(ServerHttpSecurity.HeaderSpec.ContentTypeOptionsSpec::and)
                )
                
                .build();
    }

    /**
     * Conversor de JWT para Authentication
     * Converte claims JWT em roles do Spring Security
     */
    @Bean
    public ReactiveJwtAuthenticationConverterAdapter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new CustomJwtGrantedAuthoritiesConverter());
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }

    /**
     * Configuração CORS para permitir requests do frontend e WebSocket
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Origens permitidas - configurável via application.yml
        configuration.addAllowedOriginPattern("http://localhost:*");
        configuration.addAllowedOriginPattern("https://*.conexaodesorte.com");
        configuration.addAllowedOriginPattern("https://conexaodesorte.com");
        
        // Métodos permitidos
        configuration.addAllowedMethod("GET");
        configuration.addAllowedMethod("POST");
        configuration.addAllowedMethod("PUT");
        configuration.addAllowedMethod("DELETE");
        configuration.addAllowedMethod("OPTIONS");
        configuration.addAllowedMethod("HEAD");
        
        // Headers permitidos (importante para SSE e WebSocket)
        configuration.addAllowedHeader("*");
        configuration.addExposedHeader("X-Chat-Stream-ID");
        configuration.addExposedHeader("X-User-Online-Count");
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}