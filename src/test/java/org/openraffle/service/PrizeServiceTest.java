package org.openraffle.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openraffle.domain.Event;
import org.openraffle.domain.Participant;
import org.openraffle.domain.Prize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(PrizeService.class)
class PrizeServiceTest {

    @Autowired
    PrizeService prizeService;

    @Autowired
    TestEntityManager em;

    private Event event;

    @BeforeEach
    void event() {
        event = event("Fair");
    }

    @Test
    void prizesAreListedAlphabeticallyPerEvent() {
        Event other = event("Other fair");
        prizeService.save(prize("mug"));
        prizeService.save(prize("Bike"));
        prizeService.save(prize("book"));
        Prize x = new Prize();
        x.setEvent(other);
        x.setName("Apple");
        prizeService.save(x);

        assertThat(prizeService.findAll(event)).extracting(Prize::getName).containsExactly("Bike", "book", "mug");
        assertThat(prizeService.findAll(other)).extracting(Prize::getName).containsExactly("Apple");
    }

    @Test
    void claimRecordsTheWinnerAndUnclaimClearsIt() {
        Participant winner = participant("Ann", 1, 5);
        Prize bike = prizeService.save(prize("Bike"));

        Prize claimed = prizeService.claim(bike, winner);
        assertThat(claimed.isClaimed()).isTrue();
        assertThat(claimed.isClaimedBy(winner)).isTrue();
        assertThat(claimed.getClaimedAt()).isNotNull();

        Prize released = prizeService.unclaim(bike);
        assertThat(released.isClaimed()).isFalse();
        assertThat(released.getClaimedAt()).isNull();
    }

    @Test
    void claimingSomeoneElsesPrizeFails() {
        Participant ann = participant("Ann", 1, 5);
        Participant bob = participant("Bob", 6, 10);
        Prize bike = prizeService.save(prize("Bike"));
        prizeService.claim(bike, ann);

        assertThatThrownBy(() -> prizeService.claim(bike, bob))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ann");
        // Re-claiming by the same winner is harmless.
        assertThat(prizeService.claim(bike, ann).isClaimedBy(ann)).isTrue();
    }

    private Prize prize(String name) {
        Prize prize = new Prize();
        prize.setEvent(event);
        prize.setName(name);
        return prize;
    }

    private Event event(String name) {
        Event e = new Event();
        e.setName(name);
        return em.persistAndFlush(e);
    }

    private Participant participant(String name, long start, long end) {
        Participant p = new Participant();
        p.setEvent(event);
        p.setName(name);
        p.addRange(start, end);
        p.setPhone("555-0100");
        p.setToken("token-" + name);
        return em.persistAndFlush(p);
    }
}
