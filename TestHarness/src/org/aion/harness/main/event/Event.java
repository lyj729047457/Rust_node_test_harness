package org.aion.harness.main.event;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A "base" or "leaf" event in the sense that this event is represented solely as a single String
 * with no conditional logic. Thus, this event counts as being "satisfied" or "observed" if this
 * String has been witnessed.
 *
 * This class meets the immutability guarantees of the {@link IEvent} interface.
 */
public final class Event implements IEvent {
    private final String eventString;
    private boolean isSatisfied = false;
    private long timeOfObservationInNanos = -1;
    private String log = null;

    /**
     * Constrcuts a new event that is considered to be observed once the provided event string has
     * been witnessed.
     *
     * @param eventString The string to witness.
     * @throws NullPointerException if eventString is null.
     */
    public Event(String eventString) {
        if (eventString == null) {
            throw new NullPointerException("Cannot construct node event with null event string.");
        }
        this.eventString = eventString;
    }

    /**
     * Returns the logical-and of the two event strings, each of which will be
     * wrapped up into single Event objects.
     */
    public static AndEvent and(String eventString1, String eventString2) {
        return new AndEvent(new Event(eventString1), new Event(eventString2));
    }

    /**
     * Returns the logical-and of the two events.
     */
    public static AndEvent and(IEvent event1, IEvent event2) {
        return new AndEvent(event1, event2);
    }

    /**
     * Returns the logical-or of the two event strings, each of which will be
     * wrapped up into single Event objects.
     */
    public static OrEvent or(String eventString1, String eventString2) {
        return new OrEvent(new Event(eventString1), new Event(eventString2));
    }

    /**
     * Returns the logical-or of the two events.
     */
    public static OrEvent or(IEvent event1, IEvent event2) {
        return new OrEvent(event1, event2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String eventStatement() {
        return "(" + this.eventString + ")";
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

        // Once satisfied, this value can never change.
        if (!this.isSatisfied) {
            if (line.contains(this.eventString)) {
                this.log = line;
                this.isSatisfied = true;
                this.timeOfObservationInNanos = unit.toNanos(observedAt);
            }
        }
        return this.isSatisfied;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean hasBeenObserved() {
        return this.isSatisfied;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized List<String> getAllObservedEvents() {
        return (this.isSatisfied) ? Collections.singletonList(this.eventString) : Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized List<String> getAllObservedLogs() {
        return (this.isSatisfied) ? Collections.singletonList(this.log) : Collections.emptyList();
    }

    @Override
    public synchronized long observedAt(TimeUnit unit) {
        return (this.isSatisfied)
            ? unit.convert(this.timeOfObservationInNanos, TimeUnit.NANOSECONDS)
            : -1;
    }

    /**
     * A String representation of this object, based off of the event statement (see the
     * {@code eventStatement()} method).
     *
     * @return this object as a string.
     */
    @Override
    public String toString() {
        return "Event { " + this.eventStatement() + " }";
    }

}
