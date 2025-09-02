package br.tec.facilitaservicos.batepapo.configuracao;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * 🔐 CONFIGURAÇÃO DE SEGURANÇA BATEPAPO - OIDC HABILITADO
 * ============================================================================
 * 
 * Configuração de segurança para microserviço de chat:
 * - Validação JWT via JWKS do microserviço de autenticação
 * - Controle de acesso para chat endpoints
 * - CORS seguro baseado no padrão do autenticacao
 * - WebSocket security (a implementar)
 * 
 * CORRIGIDO: Removido .anyExchange().permitAll() - VULNERABILIDADE CRÍTICA
 * 
 * @author Sistema de Padronização OIDC
 * @version 2.0
 * @since 2024
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfigSimples {

    /**
     * Configuração de segurança com OIDC - CORRIGIDO
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Configuração OAuth2 Resource Server para validação JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            
            .authorizeExchange(exchanges -> exchanges
                // Endpoints públicos - health checks para load balancer
                .pathMatchers(
                    "/actuator/health**",
                    "/actuator/health/liveness**",
                    "/actuator/health/readiness**",
                    "/actuator/info",
                    "/actuator/prometheus"
                ).permitAll()
                
                // Endpoints do Actuator sensíveis (requer autenticação)
                .pathMatchers("/actuator/metrics/**", "/actuator/env**", "/actuator/configprops**").authenticated()
                
                // Outros endpoints do actuator são públicos para monitoramento
                .pathMatchers("/actuator/**").permitAll()
                
                // ✅ CORRIGIDO: Todos os endpoints de chat requerem autenticação
                .anyExchange().authenticated()
            )
            .build();
    }

    /**
     * Conversor de JWT para authorities do Spring Security
     */
    @Bean
    public ReactiveJwtAuthenticationConverterAdapter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new CustomJwtGrantedAuthoritiesConverter());
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }

    /**
     * Configuração de CORS baseada no padrão do microserviço de autenticação - CORRIGIDO
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // ✅ CORRIGIDO: Origens permitidas baseadas em variáveis de ambiente (não mais "*")
        String allowedOrigins = System.getenv("conexao-de-sorte-cors-allowed-origins");
        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            configuration.setAllowedOriginPatterns(List.of(allowedOrigins.split(",")));
        } else {
            // Fallback para desenvolvimento
            configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "https://*.conexaodesorte.com",
                "https://*.facilitaservicos.com.br"
            ));
        }
        
        // Métodos HTTP permitidos
        configuration.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"
        ));
        
        // Headers permitidos
        configuration.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "X-Request-ID",
            "X-Trace-ID"
        ));
        
        // Headers expostos
        configuration.setExposedHeaders(List.of(
            "X-Request-ID",
            "X-Trace-ID",
            "X-Rate-Limit-Remaining",
            "X-Rate-Limit-Reset"
        ));
        
        // Allow credentials baseado em variável de ambiente
        String allowCredentials = System.getenv("conexao-de-sorte-cors-allow-credentials");
        configuration.setAllowCredentials(Boolean.parseBoolean(allowCredentials != null ? allowCredentials : "true"));
        configuration.setMaxAge(3600L); // 1 hora
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Conversor customizado para extrair authorities do JWT
     */
    public static class CustomJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        
        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            // Extrair roles do claim 'authorities' ou 'roles'
            Collection<String> authorities = jwt.getClaimAsStringList("authorities");
            if (authorities == null || authorities.isEmpty()) {
                authorities = jwt.getClaimAsStringList("roles");
            }
            if (authorities == null) {
                authorities = List.of("USER"); // Role padrão
            }
            
            return authorities.stream()
                .map(authority -> new SimpleGrantedAuthority("ROLE_" + authority.toUpperCase()))
                .collect(Collectors.toList());
        }
    }
}
