package org.openraffle.service;

import org.openraffle.domain.Participant;
import org.openraffle.domain.Prize;
import org.openraffle.repository.PrizeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
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

    /** New prizes go to the bottom of the list; existing ones keep their position. */
    public Prize save(Prize prize) {
        if (prize.getId() == null) {
            prize.setSortOrder(prizes.findAll().stream().mapToInt(Prize::getSortOrder).max().orElse(-1) + 1);
        }
        return prizes.save(prize);
    }

    public void delete(Prize prize) {
        prizes.delete(prize);
    }

    /**
     * Moves a prize one step up ({@code delta = -1}) or down ({@code delta = +1}) in the
     * organizer's list and renumbers everything so positions stay contiguous.
     */
    public void move(Prize prize, int delta) {
        List<Prize> ordered = prizes.findAllByOrderBySortOrderAscNameAsc();
        int from = ordered.indexOf(prize);
        int to = from + delta;
        if (from < 0 || to < 0 || to >= ordered.size()) {
            return;
        }
        Collections.swap(ordered, from, to);
        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).setSortOrder(i);
        }
        prizes.saveAll(ordered);
    }

    /** Records that {@code winner} took the prize. Fails if someone else already has it. */
    public Prize claim(Prize prize, Participant winner) {
        Prize current = prizes.findById(prize.getId()).orElseThrow();
        if (current.isClaimed() && !current.isClaimedBy(winner)) {
            throw new IllegalStateException(current.getName() + " was already claimed by " + current.getClaimedBy().getName());
        }
        current.setClaimedBy(winner);
        current.setClaimedAt(Instant.now());
        return prizes.save(current);
    }

    public Prize unclaim(Prize prize) {
        Prize current = prizes.findById(prize.getId()).orElseThrow();
        current.setClaimedBy(null);
        current.setClaimedAt(null);
        return prizes.save(current);
    }

    /** Called before a participant is deleted so their claims don't dangle. */
    public void releaseClaimsOf(Participant participant) {
        List<Prize> claimed = prizes.findAllByClaimedBy(participant);
        claimed.forEach(p -> {
            p.setClaimedBy(null);
            p.setClaimedAt(null);
        });
        prizes.saveAll(claimed);
    }
}
