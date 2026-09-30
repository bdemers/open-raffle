package org.openraffle.security;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Admin views require an OIDC login against Keycloak. Participant wishlist pages
 * (reached via QR code) are explicitly {@code @AnonymousAllowed}.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends VaadinWebSecurity {

    public static final String ROLE_ADMIN = "ADMIN";

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);
        // Redirect unauthenticated users straight to Keycloak; after logout, send them
        // through Keycloak's end-session endpoint and back to the app root.
        setOAuth2LoginPage(http, "/oauth2/authorization/keycloak", "{baseUrl}");
    }

    /**
     * Keycloak puts realm roles under {@code realm_access.roles}. Map them to
     * {@code ROLE_*} authorities so Vaadin's {@code @RolesAllowed} works.
     */
    @Bean
    GrantedAuthoritiesMapper keycloakAuthoritiesMapper() {
        return authorities -> {
            Set<GrantedAuthority> mapped = new HashSet<>(authorities);
            for (GrantedAuthority authority : authorities) {
                Map<String, Object> claims = switch (authority) {
                    case OidcUserAuthority oidc -> oidc.getIdToken().getClaims();
                    case OAuth2UserAuthority oauth -> oauth.getAttributes();
                    default -> Map.of();
                };
                for (String role : realmRoles(claims)) {
                    mapped.add(new SimpleGrantedAuthority("ROLE_" + role));
                }
            }
            return mapped;
        };
    }

    @SuppressWarnings("unchecked")
    private static Collection<String> realmRoles(Map<String, Object> claims) {
        Object realmAccess = claims.get("realm_access");
        if (realmAccess instanceof Map<?, ?> map && map.get("roles") instanceof Collection<?> roles) {
            return (Collection<String>) roles;
        }
        return List.of();
    }
}
