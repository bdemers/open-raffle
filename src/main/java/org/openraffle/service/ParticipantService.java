package org.openraffle.service;

import org.openraffle.domain.Event;
import org.openraffle.domain.Participant;
import org.openraffle.domain.Prize;
import org.openraffle.repository.ParticipantRepository;
import org.openraffle.repository.PrizeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ParticipantService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ParticipantRepository participants;
    private final PrizeRepository prizes;

    public ParticipantService(ParticipantRepository participants, PrizeRepository prizes) {
        this.participants = participants;
        this.prizes = prizes;
    }

    @Transactional(readOnly = true)
    public List<Participant> findAll(Event event) {
        return participants.findAllByEventOrderByTicketStartAsc(event);
    }

    @Transactional(readOnly = true)
    public Optional<Participant> findByToken(String token) {
        return participants.findByToken(token);
    }

    @Transactional(readOnly = true)
    public Optional<Participant> findByTicket(Event event, long ticket) {
        return participants.findFirstByEventAndTicketStartLessThanEqualAndTicketEndGreaterThanEqual(event, ticket, ticket);
    }

    /**
     * Saves a participant after validating the ticket range does not overlap any other
     * participant of the same event.
     *
     * @throws TicketRangeConflictException if the range overlaps another participant's tickets
     */
    public Participant save(Participant participant) {
        if (participant.getEvent() == null) {
            throw new IllegalArgumentException("Participant must belong to an event");
        }
        if (participant.getTicketStart() > participant.getTicketEnd()) {
            throw new IllegalArgumentException("Ticket start must be less than or equal to ticket end");
        }
        List<Participant> overlaps = participants.findOverlapping(
                participant.getEvent(), participant.getTicketStart(), participant.getTicketEnd(), participant.getId());
        if (!overlaps.isEmpty()) {
            throw new TicketRangeConflictException(overlaps);
        }
        if (participant.getToken() == null) {
            participant.setToken(newToken());
        }
        return participants.save(participant);
    }

    public void delete(Participant participant) {
        // Release any prizes they claimed during the draw so the FK doesn't block the delete.
        List<Prize> claimed = prizes.findAllByClaimedBy(participant);
        claimed.forEach(p -> {
            p.setClaimedBy(null);
            p.setClaimedAt(null);
        });
        prizes.saveAll(claimed);
        participants.delete(participant);
    }

    public Participant updateWishlist(String token, List<Prize> orderedPrizes, String notes) {
        Participant participant = participants.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Unknown participant token"));
        participant.getWishlist().clear();
        participant.getWishlist().addAll(orderedPrizes);
        participant.setNotes(notes == null || notes.isBlank() ? null : notes.trim());
        participant.setWishlistUpdatedAt(Instant.now());
        return participants.save(participant);
    }

    /**
     * Appends a prize the organizer handed out during the draw to the participant's list
     * (at the bottom) if they had not picked it themselves, so the draw page shows it
     * among their preferences.
     */
    public Participant addToWishlist(Participant participant, Prize prize) {
        Participant p = participants.findById(participant.getId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown participant"));
        if (!p.getWishlist().contains(prize)) {
            p.getWishlist().add(prize);
            p = participants.save(p);
        }
        return p;
    }

    private static String newToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static class TicketRangeConflictException extends RuntimeException {
        private final List<Participant> conflicts;

        public TicketRangeConflictException(List<Participant> conflicts) {
            super("Ticket range overlaps: " + conflicts.stream()
                    .map(p -> p.getName() + " (" + p.getTicketRangeLabel() + ")")
                    .reduce((a, b) -> a + ", " + b).orElse(""));
            this.conflicts = conflicts;
        }

        public List<Participant> getConflicts() {
            return conflicts;
        }
    }
}
