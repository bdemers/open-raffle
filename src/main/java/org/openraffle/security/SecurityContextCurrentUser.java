package org.openraffle.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityContextCurrentUser implements CurrentUser {

    @Override
    public Optional<String> email() {
        return oidcUser().map(u -> u.getEmail() != null ? u.getEmail() : u.getPreferredUsername())
                .map(String::toLowerCase);
    }

    @Override
    public String displayName() {
        return oidcUser().map(u -> u.getFullName() != null ? u.getFullName() : u.getPreferredUsername()).orElse("");
    }

    @Override
    public boolean isAdmin() {
        return hasRole(SecurityConfig.ROLE_ADMIN);
    }

    @Override
    public boolean isOrganizer() {
        return hasRole(SecurityConfig.ROLE_ORGANIZER);
    }

    private static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    private static Optional<OidcUser> oidcUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof OidcUser user ? Optional.of(user) : Optional.empty();
    }
}
