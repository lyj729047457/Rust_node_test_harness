package org.aion.harness.main.event;

import java.util.Collections;
import java.util.List;
import org.aion.harness.main.event.IEvent;

public final class NodeEvent implements IEvent {
    private final String eventString;

    private boolean isSatisfied = false;

    public NodeEvent(String eventString) {
        if (eventString == null) {
            throw new NullPointerException("Cannot construct node event with null event string.");
        }
        this.eventString = eventString;
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
    public IEvent and(IEvent event) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IEvent or(IEvent event) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSatisfiedBy(String line) {
        // Once satisfied, this value can never change.
        if (!this.isSatisfied) {
            this.isSatisfied = line.contains(this.eventString);
        }
        return this.isSatisfied;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getAllObservedEvents() {
        return (this.isSatisfied) ? Collections.singletonList(this.eventString) : Collections.emptyList();
    }

    @Override
    public String toString() {
        return "NodeEvent { " + this.eventStatement() + " }";
    }

}
