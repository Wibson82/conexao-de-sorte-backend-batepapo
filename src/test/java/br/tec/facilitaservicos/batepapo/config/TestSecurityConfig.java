package br.tec.facilitaservicos.batepapo.config;

import java.util.List;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import reactor.core.publisher.Mono;

/**
 * ============================================================================
 * 🧪 CONFIGURAÇÃO DE SEGURANÇA PARA TESTES - BATEPAPO
 * ============================================================================
 * 
 * Configuração de segurança simplificada para testes:
 * - Todos os endpoints permitidos
 * - CORS permissivo
 * - Sem autenticação JWT
 * 
 * @author Sistema de Testes
 * @version 1.0
 * @since 2024
 */
@TestConfiguration
@EnableWebFluxSecurity
public class TestSecurityConfig {

    /**
     * Configuração de segurança permissiva para testes
     */
    @Bean
    @Primary
    public SecurityWebFilterChain testSecurityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .cors(cors -> cors.configurationSource(testCorsConfigurationSource()))
            .authorizeExchange(exchanges -> exchanges
                .anyExchange().permitAll()
            )
            .build();
    }

    /**
     * Configuração CORS permissiva para testes
     */
    @Bean
    @Primary
    public CorsConfigurationSource testCorsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Mock ReactiveJwtDecoder para testes
     */
    @Bean
    @Primary
    public ReactiveJwtDecoder testReactiveJwtDecoder() {
        ReactiveJwtDecoder mockDecoder = Mockito.mock(ReactiveJwtDecoder.class);
        
        // Mock JWT para testes
        Jwt mockJwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .claim("sub", "test-user")
                .claim("authorities", List.of("USER"))
                .build();
        
        Mockito.when(mockDecoder.decode(Mockito.any()))
                .thenReturn(Mono.just(mockJwt));
                
        return mockDecoder;
    }
}