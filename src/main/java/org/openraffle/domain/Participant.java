package org.openraffle.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

/**
 * A raffle participant holding one or more ranges of physical ticket numbers (people come
 * back to buy more). The {@code token} is the secret embedded in the participant's QR code.
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

    /** Every run of tickets this participant bought, in the order they were entered. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "participant_ticket_range", joinColumns = @JoinColumn(name = "participant_id"))
    @OrderColumn(name = "position")
    private List<TicketRange> ranges = new ArrayList<>();

    /**
     * Unused since 0.4.0, when {@link #ranges} replaced the single range. Kept because the
     * columns are NOT NULL in databases created before then; the service mirrors the first
     * range into them and {@code LegacyDataMigration} turns old values into a range.
     */
    @Column(name = "ticket_start")
    private Long legacyTicketStart;

    @Column(name = "ticket_end")
    private Long legacyTicketEnd;

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

    public List<TicketRange> getRanges() {
        return ranges;
    }

    public void setRanges(List<TicketRange> ranges) {
        this.ranges = ranges;
    }

    public void addRange(long start, long end) {
        ranges.add(new TicketRange(start, end));
    }

    /** Ranges sorted by first ticket, for display. */
    public List<TicketRange> getRangesInOrder() {
        return ranges.stream().sorted(Comparator.comparingLong(TicketRange::getStart)).toList();
    }

    /** First ticket of the lowest range; what the participant list is sorted by. */
    public long getFirstTicket() {
        return ranges.stream().mapToLong(TicketRange::getStart).min().orElse(Long.MAX_VALUE);
    }

    Long getLegacyTicketStart() {
        return legacyTicketStart;
    }

    Long getLegacyTicketEnd() {
        return legacyTicketEnd;
    }

    /** Keeps the pre-0.4.0 NOT NULL columns satisfied; see the field comment. */
    public void mirrorFirstRangeIntoLegacyColumns() {
        legacyTicketStart = ranges.isEmpty() ? null : getFirstTicket();
        legacyTicketEnd = ranges.isEmpty() ? null : ranges.stream().mapToLong(TicketRange::getEnd).max().orElse(0);
    }

    /** For {@code LegacyDataMigration}: the old single range, if this row predates ranges. */
    public boolean adoptLegacyRange() {
        if (!ranges.isEmpty() || legacyTicketStart == null || legacyTicketEnd == null) {
            return false;
        }
        ranges.add(new TicketRange(legacyTicketStart, legacyTicketEnd));
        return true;
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
        return ranges.stream().mapToLong(TicketRange::getCount).sum();
    }

    public boolean holdsTicket(long ticket) {
        return ranges.stream().anyMatch(r -> r.contains(ticket));
    }

    /** "1 – 10" or, with several ranges, "1 – 10, 25 – 30". */
    public String getTicketRangeLabel() {
        return getRangesInOrder().stream().map(TicketRange::getLabel).collect(Collectors.joining(", "));
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
