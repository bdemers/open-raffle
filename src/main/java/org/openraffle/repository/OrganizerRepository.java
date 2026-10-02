package org.openraffle.repository;

import org.openraffle.domain.Organizer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizerRepository extends JpaRepository<Organizer, Long> {

    Optional<Organizer> findByEmail(String email);

    List<Organizer> findAllByOrderByNameAscEmailAsc();
}
