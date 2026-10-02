package org.openraffle.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {

    private final GrantedAuthoritiesMapper mapper =
            new SecurityConfig(mock(ClientRegistrationRepository.class)).keycloakAuthoritiesMapper();

    @Test
    void keycloakRealmRolesBecomeRoleAuthorities() {
        OidcIdToken token = OidcIdToken.withTokenValue("t")
                .subject("organizer")
                .claim("realm_access", Map.of("roles", List.of("ADMIN", "offline_access")))
                .build();

        Collection<? extends GrantedAuthority> mapped = mapper.mapAuthorities(List.of(new OidcUserAuthority(token, null)));

        assertThat(mapped).extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ADMIN", "ROLE_offline_access", "OIDC_USER");
    }

    @Test
    void realmRolesOnlyInUserInfoAreMappedToo() {
        // A centrally managed realm may not add roles to the ID token; userinfo must do.
        OidcIdToken token = OidcIdToken.withTokenValue("t").subject("organizer").build();
        OidcUserInfo userInfo = OidcUserInfo.builder()
                .subject("organizer")
                .claim("realm_access", Map.of("roles", List.of("ADMIN")))
                .build();

        Collection<? extends GrantedAuthority> mapped =
                mapper.mapAuthorities(List.of(new OidcUserAuthority(token, userInfo)));

        assertThat(mapped).extracting(GrantedAuthority::getAuthority).contains("ROLE_ADMIN");
    }

    @Test
    void tokensWithoutRealmRolesKeepOnlyTheOriginalAuthorities() {
        OidcIdToken token = OidcIdToken.withTokenValue("t").subject("someone").build();

        Collection<? extends GrantedAuthority> mapped = mapper.mapAuthorities(
                List.of(new OidcUserAuthority(token, null), new SimpleGrantedAuthority("SCOPE_openid")));

        assertThat(mapped).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("OIDC_USER", "SCOPE_openid");
    }
}
