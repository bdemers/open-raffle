package org.openraffle.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A raffle participant holding a contiguous range of physical ticket numbers.
 * The {@code token} is the secret embedded in the participant's QR code.
 */
@Entity
@Table(name = "participant")
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Required; the column is nullable only so the schema update succeeds on databases that
     * predate events, whose rows {@code LegacyDataMigration} attaches to a default event.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "event_id")
    private Event event;

    @NotBlank
    @Column(nullable = false)
    private String name;

    /**
     * Lets the organizer reach a winner who has stepped away. Required when organizers
     * create or edit a participant (enforced by {@code ParticipantService.save}, not here:
     * rows from before 0.2.2 have none, and their wishlist updates must still succeed).
     */
    @Column(length = 32)
    private String phone;

    @Column(nullable = false)
    private long ticketStart;

    @Column(nullable = false)
    private long ticketEnd;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant wishlistUpdatedAt;

    /** No longer collected (the wishlist page dropped its notes field in 0.2.2); kept for the schema. */
    @Column(length = 2000)
    private String notes;

    /** Ordered by preference: index 0 is the participant's top choice. */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "participant_wishlist",
            joinColumns = @JoinColumn(name = "participant_id"),
            inverseJoinColumns = @JoinColumn(name = "prize_id"))
    @OrderColumn(name = "rank")
    private List<Prize> wishlist = new ArrayList<>();

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

    /** Digits with the usual separators, optionally led by a country code: "+44 20 7946 0958". */
    private static final Pattern PHONE_CHARACTERS = Pattern.compile("\\+?[0-9 ().-]+");

    public static final String PHONE_RULE =
            "Digits, spaces, dashes or parentheses; start with + and the country code outside the US";

    /**
     * Whether this looks like a dialable phone number anywhere in the world: only phone
     * characters, 7 to 15 digits (the ITU maximum), optional leading +.
     */
    public static boolean isPlausiblePhone(String phone) {
        if (phone == null) {
            return false;
        }
        String trimmed = phone.trim();
        long digits = trimmed.chars().filter(Character::isDigit).count();
        return PHONE_CHARACTERS.matcher(trimmed).matches() && digits >= 7 && digits <= 15;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone == null || phone.isBlank() ? null : phone.trim();
    }

    public long getTicketStart() {
        return ticketStart;
    }

    public void setTicketStart(long ticketStart) {
        this.ticketStart = ticketStart;
    }

    public long getTicketEnd() {
        return ticketEnd;
    }

    public void setTicketEnd(long ticketEnd) {
        this.ticketEnd = ticketEnd;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getWishlistUpdatedAt() {
        return wishlistUpdatedAt;
    }

    public void setWishlistUpdatedAt(Instant wishlistUpdatedAt) {
        this.wishlistUpdatedAt = wishlistUpdatedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<Prize> getWishlist() {
        return wishlist;
    }

    public void setWishlist(List<Prize> wishlist) {
        this.wishlist = wishlist;
    }

    public long getTicketCount() {
        return ticketEnd - ticketStart + 1;
    }

    public boolean holdsTicket(long ticket) {
        return ticket >= ticketStart && ticket <= ticketEnd;
    }

    public String getTicketRangeLabel() {
        return ticketStart == ticketEnd ? String.valueOf(ticketStart) : ticketStart + " – " + ticketEnd;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Participant other && id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
