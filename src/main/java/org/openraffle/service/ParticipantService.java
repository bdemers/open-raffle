package org.openraffle.service;

import org.openraffle.domain.Participant;
import org.openraffle.domain.Prize;
import org.openraffle.repository.ParticipantRepository;
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

    public ParticipantService(ParticipantRepository participants) {
        this.participants = participants;
    }

    @Transactional(readOnly = true)
    public List<Participant> findAll() {
        return participants.findAllByOrderByTicketStartAsc();
    }

    @Transactional(readOnly = true)
    public Optional<Participant> findByToken(String token) {
        return participants.findByToken(token);
    }

    @Transactional(readOnly = true)
    public Optional<Participant> findByTicket(long ticket) {
        return participants.findFirstByTicketStartLessThanEqualAndTicketEndGreaterThanEqual(ticket, ticket);
    }

    /**
     * Saves a participant after validating the ticket range does not overlap any other participant.
     *
     * @throws TicketRangeConflictException if the range overlaps another participant's tickets
     */
    public Participant save(Participant participant) {
        if (participant.getTicketStart() > participant.getTicketEnd()) {
            throw new IllegalArgumentException("Ticket start must be less than or equal to ticket end");
        }
        List<Participant> overlaps = participants.findOverlapping(
                participant.getTicketStart(), participant.getTicketEnd(), participant.getId());
        if (!overlaps.isEmpty()) {
            throw new TicketRangeConflictException(overlaps);
        }
        if (participant.getToken() == null) {
            participant.setToken(newToken());
        }
        return participants.save(participant);
    }

    public void delete(Participant participant) {
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
