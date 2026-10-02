package org.openraffle.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Someone who has logged in with the ORGANIZER (or ADMIN) role, remembered so admins can
 * pick organizers for an event instead of typing emails. Events still reference organizers
 * by email, so a person can be assigned before their first login.
 */
@Entity
@Table(name = "organizer")
public class Organizer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Lower-cased, as events store it. */
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Instant firstSeenAt = Instant.now();

    @Column(nullable = false)
    private Instant lastSeenAt = Instant.now();

    protected Organizer() {
        // JPA
    }

    public Organizer(String email, String name) {
        this.email = email.toLowerCase();
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getFirstSeenAt() {
        return firstSeenAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void touch() {
        lastSeenAt = Instant.now();
    }

    /** "Pat Smith <pat@example.com>", or just the email when the name is the email. */
    public String getLabel() {
        return name == null || name.isBlank() || name.equalsIgnoreCase(email) ? email : name + " <" + email + ">";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Organizer other && id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
