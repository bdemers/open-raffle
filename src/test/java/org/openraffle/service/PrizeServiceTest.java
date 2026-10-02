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
    void prizesAreOrderedPerEvent() {
        Event other = event("Other fair");
        Prize a = prizeService.save(prize("A"));
        Prize b = prizeService.save(prize("B"));
        Prize x = new Prize();
        x.setEvent(other);
        x.setName("X");
        x = prizeService.save(x);

        assertThat(x.getSortOrder()).isEqualTo(0);
        assertThat(prizeService.findAll(event)).containsExactly(a, b);
        assertThat(prizeService.findAll(other)).containsExactly(x);
    }

    @Test
    void newPrizesAppendToTheBottom() {
        Prize first = prizeService.save(prize("Bike"));
        Prize second = prizeService.save(prize("Book"));

        assertThat(first.getSortOrder()).isEqualTo(0);
        assertThat(second.getSortOrder()).isEqualTo(1);
        assertThat(prizeService.findAll(event)).extracting(Prize::getName).containsExactly("Bike", "Book");
    }

    @Test
    void savingAnExistingPrizeKeepsItsPosition() {
        Prize bike = prizeService.save(prize("Bike"));
        prizeService.save(prize("Book"));

        bike.setName("Mountain bike");
        prizeService.save(bike);

        assertThat(prizeService.findAll(event)).extracting(Prize::getName).containsExactly("Mountain bike", "Book");
    }

    @Test
    void moveSwapsNeighboursAndRenumbersContiguously() {
        Prize a = prizeService.save(prize("A"));
        Prize b = prizeService.save(prize("B"));
        Prize c = prizeService.save(prize("C"));
        // Simulate gaps left by deletions: positions 0, 5, 9.
        b.setSortOrder(5);
        c.setSortOrder(9);
        em.flush();

        prizeService.move(c, -1);

        List<Prize> ordered = prizeService.findAll(event);
        assertThat(ordered).extracting(Prize::getName).containsExactly("A", "C", "B");
        assertThat(ordered).extracting(Prize::getSortOrder).containsExactly(0, 1, 2);

        prizeService.move(a, 1);
        assertThat(prizeService.findAll(event)).extracting(Prize::getName).containsExactly("C", "A", "B");
    }

    @Test
    void moveAtTheEdgesIsANoOp() {
        Prize a = prizeService.save(prize("A"));
        Prize b = prizeService.save(prize("B"));

        prizeService.move(a, -1);
        prizeService.move(b, 1);

        assertThat(prizeService.findAll(event)).extracting(Prize::getName).containsExactly("A", "B");
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
        p.setTicketStart(start);
        p.setTicketEnd(end);
        p.setToken("token-" + name);
        return em.persistAndFlush(p);
    }
}
