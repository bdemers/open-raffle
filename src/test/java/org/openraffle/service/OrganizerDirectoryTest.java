package org.openraffle.service;

import org.junit.jupiter.api.Test;
import org.openraffle.domain.Organizer;
import org.openraffle.repository.OrganizerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(OrganizerDirectory.class)
class OrganizerDirectoryTest {

    @Autowired
    OrganizerDirectory directory;

    @Autowired
    OrganizerRepository organizers;

    private static OAuth2AuthenticationToken login(String email, String name, String... roles) {
        Set<GrantedAuthority> authorities = new java.util.LinkedHashSet<>();
        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        OidcIdToken token = OidcIdToken.withTokenValue("t").subject(email).claim("email", email).claim("name", name)
                .claim("preferred_username", email.substring(0, email.indexOf('@'))).build();
        DefaultOidcUser user = new DefaultOidcUser(authorities, token, "preferred_username");
        return new OAuth2AuthenticationToken(user, authorities, "keycloak");
    }

    @Test
    void organizerLoginsAreRememberedWithNameAndLowerCasedEmail() {
        directory.onLogin(new AuthenticationSuccessEvent(login("Pat@Example.com", "Pat Smith", "ORGANIZER")));

        List<Organizer> all = directory.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getEmail()).isEqualTo("pat@example.com");
        assertThat(all.get(0).getName()).isEqualTo("Pat Smith");
        assertThat(all.get(0).getLabel()).isEqualTo("Pat Smith <pat@example.com>");
    }

    @Test
    void repeatLoginsUpdateTheRecordInsteadOfDuplicating() {
        directory.onLogin(new AuthenticationSuccessEvent(login("pat@example.com", "Pat", "ORGANIZER")));
        Organizer first = directory.findAll().get(0);

        directory.onLogin(new AuthenticationSuccessEvent(login("PAT@example.com", "Patricia Smith", "ADMIN", "ORGANIZER")));

        List<Organizer> all = directory.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getId()).isEqualTo(first.getId());
        assertThat(all.get(0).getName()).isEqualTo("Patricia Smith");
        assertThat(all.get(0).getLastSeenAt()).isAfterOrEqualTo(first.getLastSeenAt());
    }

    @Test
    void loginsWithoutTheOrganizerRoleAreIgnored() {
        directory.onLogin(new AuthenticationSuccessEvent(login("someone@example.com", "Some One")));
        directory.onLogin(new AuthenticationSuccessEvent(login("other@example.com", "Other", "VIEWER")));

        assertThat(organizers.count()).isZero();
    }

    @Test
    void directoryIsSortedByNameAndKeyedByEmail() {
        directory.onLogin(new AuthenticationSuccessEvent(login("zoe@example.com", "Zoe", "ORGANIZER")));
        directory.onLogin(new AuthenticationSuccessEvent(login("al@example.com", "Al", "ORGANIZER")));

        assertThat(directory.findAll()).extracting(Organizer::getName).containsExactly("Al", "Zoe");
        assertThat(directory.byEmail()).containsOnlyKeys("al@example.com", "zoe@example.com");
    }
}
