package org.aion.harness.main.event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * An AndEvent is the logical-and of two underlying events, each of which may
 * themselves be "leaf" events with event strings declared or may be conditional
 * events as well.
 *
 * An AndEvent is only satisfied if both of its underlying events are satisfied.
 *
 * This class satisfies the immutability requirements of the IEvent interface.
 */
public final class AndEvent implements IEvent {
    private final IEvent event1;
    private final IEvent event2;

    private boolean event1isSatisfied = false;
    private boolean event2isSatisfied = false;
    private long timeOfObservationInNanos = -1;

    public AndEvent(IEvent event1, IEvent event2) {
        if ((event1 == null) || (event2 == null)) {
            throw new NullPointerException("cannot construct AndEvent from a null event.");
        }
        this.event1 = event1;
        this.event2 = event2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AndEvent and(IEvent event) {
        return new AndEvent(this, event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OrEvent or(IEvent event) {
        return new OrEvent(this, event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean isSatisfiedBy(String line, long observedAt, TimeUnit unit) {
        if (line == null) {
            throw new NullPointerException("Cannot check satisfaction on null line.");
        }
        if (observedAt < 0) {
            throw new IllegalArgumentException("Cannot check satisfaction given negative timestamp.");
        }
        if (unit == null) {
            throw new NullPointerException("Cannot check satisfaction given null time unit.");
        }

        // Once satisfied this boolean never changes.
        if (!this.event1isSatisfied) {
            this.event1isSatisfied = this.event1.isSatisfiedBy(line, observedAt, unit);
        }
        if (!this.event2isSatisfied) {
            this.event2isSatisfied = this.event2.isSatisfiedBy(line, observedAt, unit);
        }

        boolean isSatisfied = this.event1isSatisfied && this.event2isSatisfied;
        if ((isSatisfied) && (this.timeOfObservationInNanos < 0)) {
            this.timeOfObservationInNanos = unit.toNanos(observedAt);
        }

        return isSatisfied;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean hasBeenObserved() {
        return this.event1.hasBeenObserved() && this.event2.hasBeenObserved();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getAllObservedEvents() {
        List<String> events = new ArrayList<>();
        events.addAll(this.event1.getAllObservedEvents());
        events.addAll(this.event2.getAllObservedEvents());
        return events;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getAllObservedLogs() {
        List<String> allEventLogs = new ArrayList<>();
        allEventLogs.addAll(this.event1.getAllObservedLogs());
        allEventLogs.addAll(this.event2.getAllObservedLogs());
        return allEventLogs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized long observedAt(TimeUnit unit) {
        return (this.timeOfObservationInNanos < 0)
            ? this.timeOfObservationInNanos
            : unit.convert(this.timeOfObservationInNanos, TimeUnit.NANOSECONDS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String eventStatement() {
        return "(" + this.event1.eventStatement() + " AND " + this.event2.eventStatement() + ")";
    }

    /**
     * A String representation of this object, based off of the event statement (see the
     * {@code eventStatement()} method).
     *
     * @return this object as a string.
     */
    @Override
    public String toString() {
        return "AndEvent { " + this.eventStatement() + " }";
    }

}
