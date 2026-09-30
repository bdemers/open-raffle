package org.openraffle.repository;

import org.openraffle.domain.Prize;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrizeRepository extends JpaRepository<Prize, Long> {

    List<Prize> findAllByOrderBySortOrderAscNameAsc();
}
