package org.aion.harness.util;

import org.aion.harness.result.EventRequestResult;

/**
 * A request for some listener to listen for an event.
 *
 * If an event is cancelled it cannot be uncancelled and any possible listener has no obligation to
 * listen for a cancelled event.
 *
 * This class is thread-safe.
 */
public final class EventRequest {
    private final NodeEvent requestedEvent;
    private EventRequestResult eventResult;

    /**
     * Constructs a new event request for the specified event.
     *
     * @param eventToRequest The event to request to be listened for.
     */
    public EventRequest(NodeEvent eventToRequest) {
        this.requestedEvent = eventToRequest;
        this.eventResult = null;
    }

    /**
     * Sets the result of this event request to the specified result.
     *
     * A non-null result can only be set once. The first time this method is called and supplied
     * with a non-null result, no other results can be set. After this point this method does
     * effectively nothing.
     *
     * @param result The result to set.
     */
    public synchronized void addResult(EventRequestResult result) {
        if (this.eventResult == null) {
            this.eventResult = result;
        }
    }

    /**
     * Returns the event that has been requested to be listened for.
     *
     * @return The requested event.
     */
    public NodeEvent getRequest() {
        return this.requestedEvent;
    }

    /**
     * Returns the result of the requested event or {@code null} if no result has been set yet.
     *
     * @return The result or {@code null} if no result has been set.
     */
    public synchronized EventRequestResult getResult() {
        return this.eventResult;
    }

    /**
     * Returns {@code true} if, and only if, a non-null result has been set for this request.
     *
     * @return Whether or not a result has been set.
     */
    public synchronized boolean hasResult() {
        return this.eventResult != null;
    }

    @Override
    public synchronized String toString() {
        if (this.eventResult == null) {
            return "EventRequest { event requested = " + this.requestedEvent + ", event result = pending }";
        } else {
            return "EventRequest { event requested = " + this.requestedEvent + ", event result = " + this.eventResult + " }";
        }
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof EventRequest)) {
            return false;
        }

        if (this == other) {
            return true;
        }

        EventRequest otherEventRequest = (EventRequest) other;
        return this.requestedEvent.equals(otherEventRequest.requestedEvent);
    }

    @Override
    public int hashCode() {
        return this.requestedEvent.getEventString().hashCode();
    }

}
