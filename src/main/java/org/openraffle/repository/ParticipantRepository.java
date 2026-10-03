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

    List<Participant> findAllByEvent(Event event);

    /** The participant of the event holding the ticket (prefix + sequence) in any of their ranges. */
    @Query("""
            select distinct p from Participant p join p.ranges r
            where p.event = :event and coalesce(r.prefix, '') = :prefix
              and r.start <= :ticket and r.end >= :ticket
            """)
    List<Participant> findHolding(@Param("event") Event event,
                                  @Param("prefix") String prefix,
                                  @Param("ticket") long ticket);

    /**
     * Participants of the event (other than {@code excludeId}) with a range of the same
     * prefix overlapping [start, end].
     */
    @Query("""
            select distinct p from Participant p join p.ranges r
            where p.event = :event and coalesce(r.prefix, '') = :prefix
              and r.start <= :end and r.end >= :start
              and (:excludeId is null or p.id <> :excludeId)
            """)
    List<Participant> findOverlapping(@Param("event") Event event,
                                      @Param("prefix") String prefix,
                                      @Param("start") long start,
                                      @Param("end") long end,
                                      @Param("excludeId") Long excludeId);

    List<Participant> findAllByRangesIsEmpty();

    long countByEventIsNull();

    @Modifying
    @Query("update Participant p set p.event = :event where p.event is null")
    int attachOrphansTo(@Param("event") Event event);
}
