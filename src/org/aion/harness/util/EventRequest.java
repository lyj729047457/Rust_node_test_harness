package org.aion.harness.util;

import java.util.List;
import org.aion.harness.main.IEvent;

/**
 * A request for some listener to listen for an event.
 *
 * If an event is cancelled it cannot be uncancelled and any possible listener has no obligation to
 * listen for a cancelled event.
 *
 * This class is thread-safe.
 */
public final class EventRequest implements IEventRequest {
    private final IEvent requestedEvent;
    private final long deadline;

    private enum RequestState { PENDING, SATISFIED, UNOBSERVED, REJECTED, EXPIRED }

    private RequestState currentState = RequestState.PENDING;

    private String causeOfRejection;
    private long timeOfObservation = -1;

    /**
     * Constructs a new event request for the specified event.
     *
     * @param eventToRequest The event to request to be listened for.
     */
    public EventRequest(IEvent eventToRequest, long deadline) {
        this.requestedEvent = eventToRequest;
        this.deadline = deadline;
    }

    @Override
    public boolean isSatisfiedBy(String line, long currentTimeInMillis) {
        markAsExpiredIfPastDeadline(currentTimeInMillis);

        if (this.currentState != RequestState.PENDING) {
            return true;
        }

        boolean isSatisfied = this.requestedEvent.isSatisfiedBy(line);

        if (isSatisfied) {
            this.currentState = RequestState.SATISFIED;
            this.timeOfObservation = currentTimeInMillis;
        }

        return isSatisfied;
    }

    @Override
    public boolean isExpiredAtTime(long currentTimeInMillis) {
        return currentTimeInMillis > this.deadline;
    }

    @Override
    public synchronized void waitForOutcome() {
        long currentTime = System.currentTimeMillis();

        while ((this.currentState == RequestState.PENDING) && (!isExpiredAtTime(currentTime))) {
            try {
                this.wait(this.deadline - currentTime);
            } catch (InterruptedException e) {
                markAsRejected("Interrupted while waiting for request outcome!");
            }

            currentTime = System.currentTimeMillis();
        }

        if (isExpiredAtTime(currentTime)) {
            markAsExpired();
        }
    }

    @Override
    public synchronized void notifyRequestIsResolved() {
        this.notifyAll();
    }

    @Override
    public List<String> getAllObservedEvents() {
        return this.requestedEvent.getAllObservedEvents();
    }

    @Override
    public long timeOfObservation() {
        return (this.currentState == RequestState.SATISFIED) ? this.timeOfObservation : -1;
    }

    @Override
    public long deadline() {
        return this.deadline;
    }

    @Override
    public synchronized void markAsRejected(String cause) {
        if (this.currentState == RequestState.PENDING) {
            this.causeOfRejection = cause;
            this.currentState = RequestState.REJECTED;
        }
    }

    @Override
    public void markAsUnobserved() {
        if (this.currentState == RequestState.PENDING) {
            this.currentState = RequestState.UNOBSERVED;
        }
    }

    @Override
    public synchronized void markAsExpired() {
        if (this.currentState == RequestState.PENDING) {
            this.currentState = RequestState.EXPIRED;
        }
    }

    @Override
    public synchronized String getCauseOfRejection() {
        return this.causeOfRejection;
    }

    @Override
    public synchronized boolean isPending() {
        return this.currentState == RequestState.PENDING;
    }

    @Override
    public synchronized boolean isRejected() {
        return this.currentState == RequestState.REJECTED;
    }

    @Override
    public synchronized boolean isUnobserved() {
        return this.currentState == RequestState.UNOBSERVED;
    }

    @Override
    public synchronized boolean isSatisfied() {
        return this.currentState == RequestState.SATISFIED;
    }

    @Override
    public synchronized boolean isExpired() {
        return this.currentState == RequestState.EXPIRED;
    }

    private synchronized void markAsExpiredIfPastDeadline(long currentTimeInMillis) {
        if (isExpiredAtTime(currentTimeInMillis)) {
            markAsExpired();
        }
    }

    @Override
    public synchronized String toString() {
        return "EventRequest { event request = " + this.requestedEvent + ", event state = " + this.currentState + " }";
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
        return this.requestedEvent.eventStatement().hashCode();
    }

}
