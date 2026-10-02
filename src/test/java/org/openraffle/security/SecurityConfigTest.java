package org.openraffle.security;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    private final GrantedAuthoritiesMapper mapper = new SecurityConfig().keycloakAuthoritiesMapper();

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
    void adminImpliesOrganizer() {
        OidcIdToken token = OidcIdToken.withTokenValue("t")
                .subject("boss")
                .claim("realm_access", Map.of("roles", List.of("ADMIN")))
                .build();

        Collection<? extends GrantedAuthority> mapped = mapper.mapAuthorities(List.of(new OidcUserAuthority(token, null)));

        assertThat(mapped).extracting(GrantedAuthority::getAuthority).contains("ROLE_ADMIN", "ROLE_ORGANIZER");
    }

    @Test
    void organizerDoesNotImplyAdmin() {
        OidcIdToken token = OidcIdToken.withTokenValue("t")
                .subject("helper")
                .claim("realm_access", Map.of("roles", List.of("ORGANIZER")))
                .build();

        Collection<? extends GrantedAuthority> mapped = mapper.mapAuthorities(List.of(new OidcUserAuthority(token, null)));

        assertThat(mapped).extracting(GrantedAuthority::getAuthority).contains("ROLE_ORGANIZER").doesNotContain("ROLE_ADMIN");
    }

    @Test
    void realmRolesAreReadFromAnAccessTokenJwt() {
        // Keycloak's default realm-roles mapper puts roles in the access token only.
        String accessToken = new PlainJWT(new JWTClaimsSet.Builder()
                .subject("organizer")
                .claim("realm_access", Map.of("roles", List.of("ADMIN", "offline_access")))
                .build()).serialize();

        assertThat(SecurityConfig.realmRoles(SecurityConfig.jwtClaims(accessToken)))
                .containsExactlyInAnyOrder("ADMIN", "offline_access");
    }

    @Test
    void opaqueOrMalformedAccessTokensYieldNoRoles() {
        assertThat(SecurityConfig.realmRoles(SecurityConfig.jwtClaims("not-a-jwt"))).isEmpty();
        assertThat(SecurityConfig.realmRoles(SecurityConfig.jwtClaims(""))).isEmpty();
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
