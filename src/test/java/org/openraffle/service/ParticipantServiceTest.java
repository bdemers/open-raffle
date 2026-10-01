package org.openraffle.service;

import org.junit.jupiter.api.Test;
import org.openraffle.domain.Participant;
import org.openraffle.domain.Prize;
import org.openraffle.repository.ParticipantRepository;
import org.openraffle.repository.PrizeRepository;
import org.openraffle.service.ParticipantService.TicketRangeConflictException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({ParticipantService.class, PrizeService.class})
class ParticipantServiceTest {

    @Autowired
    ParticipantService participantService;

    @Autowired
    PrizeService prizeService;

    @Autowired
    ParticipantRepository participants;

    @Autowired
    PrizeRepository prizes;

    @Autowired
    TestEntityManager em;

    @Test
    void saveIssuesAnUnguessableTokenThatLooksTheParticipantUp() {
        Participant saved = participantService.save(participant("Ann", 1, 10));

        assertThat(saved.getToken()).hasSizeGreaterThanOrEqualTo(32).doesNotContain("=", "+", "/");
        assertThat(participantService.findByToken(saved.getToken())).contains(saved);
        assertThat(participantService.findByToken("nope")).isEmpty();
    }

    @Test
    void overlappingTicketRangesAreRejected() {
        participantService.save(participant("Ann", 1, 10));

        assertThatThrownBy(() -> participantService.save(participant("Bob", 10, 20)))
                .isInstanceOf(TicketRangeConflictException.class)
                .hasMessageContaining("Ann (1 – 10)");
        assertThatThrownBy(() -> participantService.save(participant("Cat", 0, 100)))
                .isInstanceOf(TicketRangeConflictException.class);
        assertThat(participantService.save(participant("Dan", 11, 20)).getId()).isNotNull();
    }

    @Test
    void editingAParticipantDoesNotConflictWithItself() {
        Participant ann = participantService.save(participant("Ann", 1, 10));

        ann.setTicketEnd(12);

        assertThat(participantService.save(ann).getTicketEnd()).isEqualTo(12);
    }

    @Test
    void reversedRangeIsRejected() {
        assertThatThrownBy(() -> participantService.save(participant("Ann", 10, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findByTicketCoversTheWholeRangeInclusive() {
        Participant ann = participantService.save(participant("Ann", 100, 104));

        assertThat(participantService.findByTicket(100)).contains(ann);
        assertThat(participantService.findByTicket(104)).contains(ann);
        assertThat(participantService.findByTicket(99)).isEmpty();
        assertThat(participantService.findByTicket(105)).isEmpty();
    }

    @Test
    void updateWishlistKeepsOrderAndNormalisesNotes() {
        Participant ann = participantService.save(participant("Ann", 1, 10));
        Prize bike = prizeService.save(prize("Bike"));
        Prize book = prizeService.save(prize("Book"));
        Prize mug = prizeService.save(prize("Mug"));

        participantService.updateWishlist(ann.getToken(), List.of(mug, bike, book), "  size L  ");
        em.flush();
        em.clear();

        Participant reloaded = participants.findByToken(ann.getToken()).orElseThrow();
        assertThat(reloaded.getWishlist()).extracting(Prize::getName).containsExactly("Mug", "Bike", "Book");
        assertThat(reloaded.getNotes()).isEqualTo("size L");
        assertThat(reloaded.getWishlistUpdatedAt()).isNotNull();

        participantService.updateWishlist(ann.getToken(), List.of(book), "   ");
        em.flush();
        em.clear();
        reloaded = participants.findByToken(ann.getToken()).orElseThrow();
        assertThat(reloaded.getWishlist()).extracting(Prize::getName).containsExactly("Book");
        assertThat(reloaded.getNotes()).isNull();
    }

    @Test
    void addToWishlistAppendsOnceAtTheBottom() {
        Participant ann = participantService.save(participant("Ann", 1, 10));
        Prize bike = prizeService.save(prize("Bike"));
        Prize mug = prizeService.save(prize("Mug"));
        participantService.updateWishlist(ann.getToken(), List.of(bike), null);

        participantService.addToWishlist(ann, mug);
        participantService.addToWishlist(ann, mug);
        participantService.addToWishlist(ann, bike);
        em.flush();
        em.clear();

        Participant reloaded = participants.findByToken(ann.getToken()).orElseThrow();
        assertThat(reloaded.getWishlist()).extracting(Prize::getName).containsExactly("Bike", "Mug");
    }

    @Test
    void deletingAParticipantReleasesTheirClaims() {
        Participant ann = participantService.save(participant("Ann", 1, 10));
        Prize bike = prizeService.save(prize("Bike"));
        prizeService.claim(bike, ann);

        participantService.delete(ann);
        em.flush();
        em.clear();

        assertThat(participants.findByToken(ann.getToken())).isEmpty();
        Prize reloaded = prizes.findById(bike.getId()).orElseThrow();
        assertThat(reloaded.isClaimed()).isFalse();
    }

    private static Participant participant(String name, long start, long end) {
        Participant p = new Participant();
        p.setName(name);
        p.setTicketStart(start);
        p.setTicketEnd(end);
        return p;
    }

    private static Prize prize(String name) {
        Prize prize = new Prize();
        prize.setName(name);
        return prize;
    }
}
