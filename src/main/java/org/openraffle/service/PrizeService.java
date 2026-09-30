package org.openraffle.service;

import org.openraffle.domain.Prize;
import org.openraffle.repository.PrizeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PrizeService {

    private final PrizeRepository prizes;

    public PrizeService(PrizeRepository prizes) {
        this.prizes = prizes;
    }

    @Transactional(readOnly = true)
    public List<Prize> findAll() {
        return prizes.findAllByOrderBySortOrderAscNameAsc();
    }

    public Prize save(Prize prize) {
        return prizes.save(prize);
    }

    public void delete(Prize prize) {
        prizes.delete(prize);
    }
}
