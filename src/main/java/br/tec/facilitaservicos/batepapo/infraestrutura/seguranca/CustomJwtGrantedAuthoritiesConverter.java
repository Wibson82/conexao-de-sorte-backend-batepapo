package br.tec.facilitaservicos.batepapo.infraestrutura.seguranca;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * ============================================================================
 * 🔐 CONVERSOR CUSTOMIZADO DE AUTHORITIES JWT - BATE-PAPO
 * ============================================================================
 * 
 * Converte claims JWT em authorities do Spring Security:
 * - Extrai roles do claim 'roles' ou 'authorities'
 * - Adiciona prefixo 'ROLE_' conforme padrão Spring Security
 * - Suporta tanto arrays quanto strings separadas por vírgula
 * - Fallback para ROLE_USER se nenhum role encontrado
 * - Roles específicos do chat: ROLE_MODERATOR, ROLE_CHAT_ADMIN
 * 
 * Claims JWT esperados:
 * - "roles": ["USER", "ADMIN", "MODERATOR"] ou "USER,ADMIN,MODERATOR"
 * - "authorities": ["USER", "ADMIN", "MODERATOR"] ou "USER,ADMIN,MODERATOR"
 * - "scope": "user admin moderator" (OAuth2 standard)
 * - "chat_roles": ["MODERATOR", "VIP"] (específico do chat)
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
public class CustomJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String ROLE_PREFIX = "ROLE_";
    private static final String DEFAULT_ROLE = "USER";
    
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        // Tenta extrair authorities de diferentes claims
        Collection<GrantedAuthority> authorities = extractFromClaim(jwt, "roles");
        
        if (authorities.isEmpty()) {
            authorities = extractFromClaim(jwt, "authorities");
        }
        
        if (authorities.isEmpty()) {
            authorities = extractFromScope(jwt);
        }

        // Adicionar roles específicos do chat se existirem
        Collection<GrantedAuthority> chatRoles = extractFromClaim(jwt, "chat_roles");
        if (!chatRoles.isEmpty()) {
            authorities.addAll(chatRoles);
        }
        
        // Se nenhum role foi encontrado, adiciona role padrão
        if (authorities.isEmpty()) {
            authorities = Collections.singleton(
                new SimpleGrantedAuthority(ROLE_PREFIX + DEFAULT_ROLE)
            );
        }
        
        return authorities;
    }

    /**
     * Extrai authorities de um claim específico
     */
    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractFromClaim(Jwt jwt, String claimName) {
        Object claimValue = jwt.getClaim(claimName);
        
        if (claimValue == null) {
            return Collections.emptyList();
        }
        
        List<String> roleNames;
        
        if (claimValue instanceof List<?>) {
            // Se é uma lista
            roleNames = ((List<?>) claimValue).stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .collect(Collectors.toList());
        } else if (claimValue instanceof String) {
            // Se é uma string separada por vírgulas
            roleNames = List.of(((String) claimValue).split(","));
        } else {
            return Collections.emptyList();
        }
        
        return roleNames.stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .map(this::addRolePrefix)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    /**
     * Extrai authorities do claim 'scope' (padrão OAuth2)
     */
    private Collection<GrantedAuthority> extractFromScope(Jwt jwt) {
        String scope = jwt.getClaimAsString("scope");
        
        if (scope == null || scope.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        return List.of(scope.split("\\s+")).stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .map(this::addRolePrefix)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    /**
     * Adiciona prefixo ROLE_ se não existir
     */
    private String addRolePrefix(String role) {
        if (role.startsWith(ROLE_PREFIX)) {
            return role;
        }
        
        // Roles específicos do chat mantêm nomenclatura especial
        if (role.equals("MODERATOR")) {
            return ROLE_PREFIX + "CHAT_MODERATOR";
        }
        if (role.equals("VIP")) {
            return ROLE_PREFIX + "CHAT_VIP";
        }
        if (role.equals("BANNED")) {
            return ROLE_PREFIX + "CHAT_BANNED";
        }
        
        return ROLE_PREFIX + role;
    }
}