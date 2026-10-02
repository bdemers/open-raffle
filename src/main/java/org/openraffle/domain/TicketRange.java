package org.openraffle.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** A contiguous run of physical ticket numbers, both ends inclusive. */
@Embeddable
public class TicketRange {

    @Column(name = "ticket_start", nullable = false)
    private long start;

    @Column(name = "ticket_end", nullable = false)
    private long end;

    protected TicketRange() {
        // JPA
    }

    public TicketRange(long start, long end) {
        this.start = start;
        this.end = end;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public boolean isValid() {
        return start <= end;
    }

    public long getCount() {
        return end - start + 1;
    }

    public boolean contains(long ticket) {
        return ticket >= start && ticket <= end;
    }

    public boolean overlaps(TicketRange other) {
        return start <= other.end && other.start <= end;
    }

    public String getLabel() {
        return start == end ? String.valueOf(start) : start + " – " + end;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TicketRange other && start == other.start && end == other.end;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(start) * 31 + Long.hashCode(end);
    }

    @Override
    public String toString() {
        return getLabel();
    }
}
