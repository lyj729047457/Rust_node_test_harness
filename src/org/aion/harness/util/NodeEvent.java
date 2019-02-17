package org.aion.harness.util;

import java.util.Collections;
import java.util.List;
import org.aion.harness.main.IEvent;

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
        return "NodeEvent { " + this.eventString + " }";
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof NodeEvent)) {
            return false;
        }

        if (this == other) {
            return true;
        }

        NodeEvent event = (NodeEvent) other;
        return event.eventString.equals(this.eventString);
    }

    @Override
    public int hashCode() {
        return this.eventString.hashCode();
    }

}
