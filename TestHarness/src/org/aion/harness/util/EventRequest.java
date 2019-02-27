package org.aion.harness.util;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.aion.harness.main.event.IEvent;

/**
 * A request for some listener to listen for an event.
 *
 * This class satisfies the immutability guarantee of the {@link IEventRequest} interface.
 *
 * This class is thread-safe where documented to be so.
 */
public final class EventRequest implements IEventRequest {
    private final IEvent requestedEvent;
    private final long deadline;

    private enum RequestState { PENDING, SATISFIED, UNOBSERVED, REJECTED, EXPIRED }

    private RequestState currentState = RequestState.PENDING;

    private String causeOfRejection;
    private long timeOfObservation = -1;

    private CountDownLatch pendingLatch = new CountDownLatch(1);

    /**
     * Constructs a new event request for the specified event.
     *
     * @param eventToRequest The event to request to be listened for.
     */
    public EventRequest(IEvent eventToRequest, long deadline) {
        this.requestedEvent = eventToRequest;
        this.deadline = deadline;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean isSatisfiedBy(String line, long currentTimeInMillis) {
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

    /**
     * {@inheritDoc}
     *
     * Thread safe.
     */
    @Override
    public boolean isExpiredAtTime(long currentTimeInMillis) {
        return currentTimeInMillis > this.deadline;
    }

    /**
     * {@inheritDoc}
     *
     * Not thread safe.
     */
    @Override
    public void waitForOutcome() {
        long timeout = this.deadline - System.currentTimeMillis();

        try {

            if (!this.pendingLatch.await(timeout, TimeUnit.MILLISECONDS)) {
                markAsExpired();
            }

        } catch (InterruptedException e) {
            markAsRejected("Interrupted while waiting for request outcome!");
        }

    }

    /**
     * {@inheritDoc}
     *
     * Thread safe.
     */
    @Override
    public void notifyRequestIsResolved() {
        this.pendingLatch.countDown();
    }

    /**
     * {@inheritDoc}
     *
     * Thread safe.
     */
    @Override
    public synchronized List<String> getAllObservedEvents() {
        return this.requestedEvent.getAllObservedEvents();
    }

    /**
     * {@inheritDoc}
     *
     * Thread safe.
     */
    @Override
    public synchronized List<String> getAllObservedLogs() {
        return this.requestedEvent.getAllObservedLogs();
    }

    /**
     * {@inheritDoc}
     *
     * Thread safe.
     */
    @Override
    public long timeOfObservation() {
        return (this.currentState == RequestState.SATISFIED) ? this.timeOfObservation : -1;
    }

    /**
     * {@inheritDoc}
     *
     * Thread safe.
     */
    @Override
    public long deadline() {
        return this.deadline;
    }

    /**
     * {@inheritDoc}
     *
     * Thread safe.
     */
    @Override
    public synchronized void markAsRejected(String cause) {
        if (this.currentState == RequestState.PENDING) {
            this.causeOfRejection = cause;
            this.currentState = RequestState.REJECTED;
        }
    }

    /**
     * {@inheritDoc}
     *
     * Thread safe.
     */
    @Override
    public synchronized void markAsUnobserved() {
        if (this.currentState == RequestState.PENDING) {
            this.currentState = RequestState.UNOBSERVED;
        }
    }

    /**
     * {@inheritDoc}
     *
     * Thread safe.
     */
    @Override
    public synchronized void markAsExpired() {
        if (this.currentState == RequestState.PENDING) {
            this.currentState = RequestState.EXPIRED;
        }
    }

    /**
     * {@inheritDoc}
     *
     * Thread safe.
     */
    @Override
    public synchronized String getCauseOfRejection() {
        return this.causeOfRejection;
    }

    /**
     * {@inheritDoc}
     *
     * Thread safe.
     */
    @Override
    public synchronized boolean isPending() {
        return this.currentState == RequestState.PENDING;
    }

    /**
     * {@inheritDoc}
     *
     * Thread safe.
     */
    @Override
    public synchronized boolean isRejected() {
        return this.currentState == RequestState.REJECTED;
    }

    /**
     * {@inheritDoc}
     *
     * Thread safe.
     */
    @Override
    public synchronized boolean isUnobserved() {
        return this.currentState == RequestState.UNOBSERVED;
    }

    /**
     * {@inheritDoc}
     *
     * Thread safe.
     */
    @Override
    public synchronized boolean isSatisfied() {
        return this.currentState == RequestState.SATISFIED;
    }

    /**
     * {@inheritDoc}
     *
     * Thread safe.
     */
    @Override
    public synchronized boolean isExpired() {
        return this.currentState == RequestState.EXPIRED;
    }

    private synchronized void markAsExpiredIfPastDeadline(long currentTimeInMillis) {
        if (isExpiredAtTime(currentTimeInMillis)) {
            markAsExpired();
        }
    }

    /**
     * {@inheritDoc}
     *
     * Thread safe.
     */
    @Override
    public synchronized String toString() {
        return "EventRequest { event request = " + this.requestedEvent + ", event state = " + this.currentState + " }";
    }

    /**
     * {@inheritDoc}
     *
     * Thread safe.
     */
    @Override
    public synchronized boolean equals(Object other) {
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
