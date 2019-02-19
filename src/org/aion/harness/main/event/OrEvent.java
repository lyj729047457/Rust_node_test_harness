package org.aion.harness.main.event;

import java.util.ArrayList;
import java.util.List;

/**
 * An OrEvent is the logical-or of two underlying events, each of which may
 * themselves be "leaf" events with event strings declared or may be conditional
 * events as well.
 *
 * An OrEvent is only satisfied if at least one of its underlying events are
 * satisfied.
 *
 * This class satisfies the immutability requirements of the IEvent interface.
 */
public final class OrEvent implements IEvent {
    private final IEvent event1;
    private final IEvent event2;

    private boolean event1isSatisfied = false;
    private boolean event2isSatisfied = false;

    public OrEvent(IEvent event1, IEvent event2) {
        if ((event1 == null) || (event2 == null)) {
            throw new NullPointerException("cannot construct OrEvent from a null event.");
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
    public boolean isSatisfiedBy(String line) {
        // Prevent this event from being 're-satisfied' if it is already.
        if (this.event1isSatisfied || this.event2isSatisfied) {
            return true;
        }

        if (!this.event1isSatisfied) {
            this.event1isSatisfied = this.event1.isSatisfiedBy(line);
        }
        if (!this.event2isSatisfied) {
            this.event2isSatisfied = this.event2.isSatisfiedBy(line);
        }

        return this.event1isSatisfied || this.event2isSatisfied;
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

    @Override
    public String eventStatement() {
        return "(" + this.event1.eventStatement() + " OR " + this.event2.eventStatement() + ")";
    }

    /**
     * A String representation of this object, based off of the event statement (see the
     * {@code eventStatement()} method).
     *
     * @return this object as a string.
     */
    @Override
    public String toString() {
        return "OrEvent { " + this.eventStatement() + " }";
    }

}
