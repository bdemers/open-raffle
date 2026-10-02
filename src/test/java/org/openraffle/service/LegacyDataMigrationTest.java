package org.openraffle.service;

import org.junit.jupiter.api.Test;
import org.openraffle.domain.Event;
import org.openraffle.domain.Participant;
import org.openraffle.domain.Prize;
import org.openraffle.repository.EventRepository;
import org.openraffle.repository.ParticipantRepository;
import org.openraffle.repository.PrizeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(LegacyDataMigration.class)
class LegacyDataMigrationTest {

    @Autowired
    LegacyDataMigration migration;

    @Autowired
    EventRepository events;

    @Autowired
    ParticipantRepository participants;

    @Autowired
    PrizeRepository prizes;

    @Autowired
    TestEntityManager em;

    @Test
    void rowsWithoutAnEventAreAttachedToADefaultEvent() {
        Participant p = new Participant();
        p.setName("Old-timer");
        p.addRange(1, 5);
        p.setPhone("555-0100");
        p.setToken("legacy");
        em.persist(p);
        Prize z = new Prize();
        z.setName("Old prize");
        em.persist(z);
        em.flush();
        em.clear();

        migration.run(new DefaultApplicationArguments());
        em.clear();

        Event def = events.findByNameIgnoreCase(LegacyDataMigration.DEFAULT_EVENT_NAME).orElseThrow();
        assertThat(participants.findByToken("legacy")).get().extracting(Participant::getEvent).isEqualTo(def);
        assertThat(prizes.findAllByEventAlphabetically(def)).extracting(Prize::getName).containsExactly("Old prize");
        assertThat(participants.countByEventIsNull()).isZero();
        assertThat(prizes.countByEventIsNull()).isZero();
    }

    @Test
    void aSingleLegacyRangeBecomesTheFirstRange() {
        Event fair = new Event();
        fair.setName("Fair");
        em.persist(fair);
        // A row written by a pre-0.4.0 build: ticket_start/ticket_end set, no ranges.
        em.getEntityManager().createNativeQuery(
                "insert into participant (event_id, name, phone, token, created_at, ticket_start, ticket_end)"
                        + " values (?1, 'Old-timer', '555-0100', 'old', current_timestamp, 40, 45)")
                .setParameter(1, fair.getId()).executeUpdate();
        em.flush();
        em.clear();

        migration.run(new DefaultApplicationArguments());
        em.flush();
        em.clear();

        Participant old = participants.findByToken("old").orElseThrow();
        assertThat(old.getTicketRangeLabel()).isEqualTo("40 – 45");
        assertThat(old.holdsTicket(42)).isTrue();
    }

    @Test
    void doesNothingWhenEverythingHasAnEvent() {
        migration.run(new DefaultApplicationArguments());

        assertThat(events.findByNameIgnoreCase(LegacyDataMigration.DEFAULT_EVENT_NAME)).isEmpty();
    }
}
