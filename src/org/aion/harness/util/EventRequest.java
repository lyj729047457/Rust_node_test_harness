package org.aion.harness.util;

import org.aion.harness.result.Result;

/**
 * A basic request that can be given a result.
 *
 * The ID and request itself are both immutable.
 *
 * The result can only be set once.
 */
public final class EventRequest {
    private final NodeEvent requestedEvent;

    private volatile Result eventResult;

    public EventRequest(NodeEvent eventToRequest) {
        this.requestedEvent = eventToRequest;
        this.eventResult = null;
    }

    /**
     * Adds the result only if no result already exists.
     *
     * @param result The result to add.
     */
    public void addResult(Result result) {
        if (this.eventResult == null) {
            this.eventResult = result;
        }
    }

    public NodeEvent getRequest() {
        return this.requestedEvent;
    }

    public Result getResult() {
        return this.eventResult;
    }

    public boolean hasResult() {
        return this.eventResult != null;
    }

    @Override
    public String toString() {
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
