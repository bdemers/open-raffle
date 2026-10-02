package org.openraffle.service;

import org.openraffle.domain.Organizer;
import org.openraffle.repository.OrganizerRepository;
import org.openraffle.security.SecurityConfig;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The organizers the app has seen log in. Fed by Spring Security's success event, so it
 * needs nothing from Keycloak beyond the login itself; read by the event editor's picker.
 */
@Service
@Transactional
public class OrganizerDirectory {

    private final OrganizerRepository organizers;

    public OrganizerDirectory(OrganizerRepository organizers) {
        this.organizers = organizers;
    }

    @EventListener
    public void onLogin(AuthenticationSuccessEvent event) {
        remember(event.getAuthentication());
    }

    /** Records the login if it carries the organizer role (admins have it implicitly). */
    public Optional<Organizer> remember(Authentication authentication) {
        boolean organizer = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + SecurityConfig.ROLE_ORGANIZER));
        if (!organizer || !(authentication.getPrincipal() instanceof OidcUser user)) {
            return Optional.empty();
        }
        String email = user.getEmail() != null ? user.getEmail() : user.getPreferredUsername();
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        String name = user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName()
                : user.getPreferredUsername() != null ? user.getPreferredUsername() : email;
        Organizer record = organizers.findByEmail(email.toLowerCase()).orElseGet(() -> new Organizer(email, name));
        record.setName(name);
        record.touch();
        return Optional.of(organizers.save(record));
    }

    @Transactional(readOnly = true)
    public List<Organizer> findAll() {
        return organizers.findAllByOrderByNameAscEmailAsc();
    }

    /** Known organizers by email, for showing names next to the emails an event stores. */
    @Transactional(readOnly = true)
    public Map<String, Organizer> byEmail() {
        return organizers.findAll().stream().collect(Collectors.toMap(Organizer::getEmail, Function.identity()));
    }
}
