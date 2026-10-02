package org.openraffle.repository;

import org.openraffle.domain.Event;
import org.openraffle.domain.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    Optional<Participant> findByToken(String token);

    List<Participant> findAllByEventOrderByTicketStartAsc(Event event);

    Optional<Participant> findFirstByEventAndTicketStartLessThanEqualAndTicketEndGreaterThanEqual(
            Event event, long ticket, long sameTicket);

    /** Any participant of the event (other than {@code excludeId}) whose range overlaps [start, end]. */
    @Query("""
            select p from Participant p
            where p.event = :event
              and p.ticketStart <= :end and p.ticketEnd >= :start
              and (:excludeId is null or p.id <> :excludeId)
            """)
    List<Participant> findOverlapping(@Param("event") Event event,
                                      @Param("start") long start,
                                      @Param("end") long end,
                                      @Param("excludeId") Long excludeId);

    long countByEventIsNull();

    @Modifying
    @Query("update Participant p set p.event = :event where p.event is null")
    int attachOrphansTo(@Param("event") Event event);
}
