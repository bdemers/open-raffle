package org.openraffle.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

@Entity
@Table(name = "prize")
public class Prize {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Required; nullable in the schema only for rows that predate events (see Participant). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "event_id")
    private Event event;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    /** Unused since 0.2.2 (prizes are listed alphabetically); kept because the column is NOT NULL. */
    @Column(nullable = false)
    private int sortOrder;

    /** The winner who took this prize during the draw, or null while it is still available. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "claimed_by_id")
    private Participant claimedBy;

    private Instant claimedAt;

    public Long getId() {
        return id;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Participant getClaimedBy() {
        return claimedBy;
    }

    public void setClaimedBy(Participant claimedBy) {
        this.claimedBy = claimedBy;
    }

    public Instant getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(Instant claimedAt) {
        this.claimedAt = claimedAt;
    }

    public boolean isClaimed() {
        return claimedBy != null;
    }

    public boolean isClaimedBy(Participant participant) {
        return claimedBy != null && claimedBy.equals(participant);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Prize other && id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
