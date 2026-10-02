package org.openraffle.security;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
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

    private final ClientRegistrationRepository clientRegistrations;

    public SecurityConfig(ClientRegistrationRepository clientRegistrations) {
        this.clientRegistrations = clientRegistrations;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // The hosting platform's health check must not be bounced to the login page.
        http.authorizeHttpRequests(auth -> auth.requestMatchers(EndpointRequest.to(HealthEndpoint.class)).permitAll());
        super.configure(http);
        // Redirect unauthenticated users straight to Keycloak; after logout, send them
        // through Keycloak's end-session endpoint and back to the app root.
        setOAuth2LoginPage(http, "/oauth2/authorization/keycloak", "{baseUrl}");
        // The Keycloak client requires PKCE (S256), which Spring only adds by default
        // for public clients.
        DefaultOAuth2AuthorizationRequestResolver pkce =
                new DefaultOAuth2AuthorizationRequestResolver(clientRegistrations, "/oauth2/authorization");
        pkce.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        http.oauth2Login(login -> login.authorizationEndpoint(ep -> ep.authorizationRequestResolver(pkce)));
    }

    /**
     * Keycloak puts realm roles under {@code realm_access.roles}. Map them to
     * {@code ROLE_*} authorities so Vaadin's {@code @RolesAllowed} works. The claim is
     * looked for in the ID token and in the userinfo response, since which one carries
     * it depends on how the client's realm-roles mapper is configured in Keycloak.
     */
    @Bean
    GrantedAuthoritiesMapper keycloakAuthoritiesMapper() {
        return authorities -> {
            Set<GrantedAuthority> mapped = new HashSet<>(authorities);
            for (GrantedAuthority authority : authorities) {
                List<Map<String, Object>> claimSources = switch (authority) {
                    case OidcUserAuthority oidc -> oidc.getUserInfo() == null
                            ? List.of(oidc.getIdToken().getClaims())
                            : List.of(oidc.getIdToken().getClaims(), oidc.getUserInfo().getClaims());
                    case OAuth2UserAuthority oauth -> List.of(oauth.getAttributes());
                    default -> List.of();
                };
                for (Map<String, Object> claims : claimSources) {
                    for (String role : realmRoles(claims)) {
                        mapped.add(new SimpleGrantedAuthority("ROLE_" + role));
                    }
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
