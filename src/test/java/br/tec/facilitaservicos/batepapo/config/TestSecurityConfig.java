package br.tec.facilitaservicos.batepapo.config;

import br.tec.facilitaservicos.batepapo.configuracao.SecurityConfigSimples;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * ============================================================================
 * 🧪 CONFIGURAÇÃO DE SEGURANÇA PARA TESTES - BATEPAPO
 * ============================================================================
 * 
 * Configuração de segurança simplificada para testes:
 * - JWT desabilitado
 * - CORS permissivo
 * - Todos os endpoints permitidos
 * - Override da configuração principal
 * 
 * @author Sistema de Testes
 * @version 1.0
 * @since 2024
 */
@TestConfiguration
public class TestSecurityConfig {

    /**
     * Configuração de segurança permissiva para testes que substitui a principal
     */
    @Bean(name = "securityWebFilterChain")
    @Primary
    public SecurityWebFilterChain testSecurityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .cors(cors -> cors.configurationSource(testCorsConfigurationSource()))
            
            // Desabilitar OAuth2 Resource Server para testes
            .authorizeExchange(exchanges -> exchanges
                .anyExchange().permitAll()
            )
            .build();
    }

    /**
     * Configuração CORS permissiva para testes
     */
    @Bean(name = "corsConfigurationSource")
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
}