package org.openraffle.security;

import java.util.Optional;

/** Who is using the app right now, as far as the services need to know. */
public interface CurrentUser {

    /** Lower-cased email of the logged-in user, or empty when anonymous or no email claim. */
    Optional<String> email();

    /** Display name for the UI. */
    String displayName();

    boolean isAdmin();

    boolean isOrganizer();
}
