package org.openraffle.service;

import org.openraffle.domain.Event;
import org.openraffle.repository.EventRepository;
import org.openraffle.repository.ParticipantRepository;
import org.openraffle.repository.PrizeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Databases created before 0.2.0 have participants and prizes with no event. On startup,
 * attach them to a "Default event" so nothing is orphaned; runs as a no-op afterwards.
 */
@Component
public class LegacyDataMigration implements ApplicationRunner {

    static final String DEFAULT_EVENT_NAME = "Default event";

    private static final Logger log = LoggerFactory.getLogger(LegacyDataMigration.class);

    private final EventRepository events;
    private final ParticipantRepository participants;
    private final PrizeRepository prizes;

    public LegacyDataMigration(EventRepository events, ParticipantRepository participants, PrizeRepository prizes) {
        this.events = events;
        this.participants = participants;
        this.prizes = prizes;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long orphanParticipants = participants.countByEventIsNull();
        long orphanPrizes = prizes.countByEventIsNull();
        if (orphanParticipants == 0 && orphanPrizes == 0) {
            return;
        }
        Event event = events.findByNameIgnoreCase(DEFAULT_EVENT_NAME).orElseGet(() -> {
            Event created = new Event();
            created.setName(DEFAULT_EVENT_NAME);
            return events.save(created);
        });
        int p = participants.attachOrphansTo(event);
        int z = prizes.attachOrphansTo(event);
        log.info("Attached {} participant(s) and {} prize(s) that predate events to \"{}\" (id {})",
                p, z, event.getName(), event.getId());
    }
}
