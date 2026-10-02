package org.openraffle.service;

import org.openraffle.security.CurrentUser;

import java.util.Optional;

/** Mutable stand-in for the logged-in user in service tests. */
class StubCurrentUser implements CurrentUser {

    String email;
    boolean admin;
    boolean organizer;

    StubCurrentUser admin() {
        email = "admin@example.com";
        admin = true;
        organizer = true;
        return this;
    }

    StubCurrentUser organizer(String email) {
        this.email = email;
        admin = false;
        organizer = true;
        return this;
    }

    @Override
    public Optional<String> email() {
        return Optional.ofNullable(email).map(String::toLowerCase);
    }

    @Override
    public String displayName() {
        return email == null ? "" : email;
    }

    @Override
    public boolean isAdmin() {
        return admin;
    }

    @Override
    public boolean isOrganizer() {
        return organizer;
    }
}
