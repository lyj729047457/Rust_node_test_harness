package org.aion.harness.util;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.aion.harness.main.event.IEvent;
import org.aion.harness.main.types.FutureResult;
import org.aion.harness.result.LogEventResult;

public final class EventRequest {
    private final IEvent requestedEvent;
    private final long deadlineInMilliseconds;

    private enum RequestState { PENDING, SATISFIED, UNOBSERVED, REJECTED, EXPIRED }

    private RequestState currentState = RequestState.PENDING;

    private String causeOfRejection;
    private long timeOfObservationInMilliseconds = -1;

    private CountDownLatch pendingLatch = new CountDownLatch(1);

    public final FutureResult<LogEventResult> future = new FutureResult<>();

    /**
     * Constructs a new event request for the specified event.
     *
     * @param eventToRequest The event to request to be listened for.
     * @param deadline The time at which this request expires.
     * @param unit The unit of time of the deadline.
     */
    public EventRequest(IEvent eventToRequest, long deadline, TimeUnit unit) {
        this.requestedEvent = eventToRequest;
        this.deadlineInMilliseconds = unit.toMillis(deadline);
    }

    /**
     * This must be called by the {@link LogListener} thread <b>after</b> moving this request into
     * a finalized state.
     *
     * Once this method is called, the future (which is handed off to the event submitting thread)
     * will be consumable.
     *
     * Not thread-safe.
     */
    public void finishFuture() {
        this.future.finish(extractResultFromRequest());
    }

    public synchronized boolean isSatisfiedBy(String line, long currentTime, TimeUnit unit) {
        markAsExpiredIfPastDeadline(currentTime, unit);

        if (this.currentState != RequestState.PENDING) {
            return true;
        }

        boolean isSatisfied = this.requestedEvent.isSatisfiedBy(line);

        if (isSatisfied) {
            this.currentState = RequestState.SATISFIED;
            this.timeOfObservationInMilliseconds = unit.toMillis(currentTime);
        }

        return isSatisfied;
    }

    public boolean isExpiredAtTime(long time, TimeUnit unit) {
        return unit.toMillis(time) > this.deadlineInMilliseconds;
    }

    public void waitForOutcome() {
        long timeout = this.deadlineInMilliseconds - System.currentTimeMillis();

        try {

            if (!this.pendingLatch.await(timeout, TimeUnit.MILLISECONDS)) {
                markAsExpired();
            }

        } catch (InterruptedException e) {
            markAsRejected("Interrupted while waiting for request outcome!");
        }

    }

    public void notifyRequestIsResolved() {
        this.pendingLatch.countDown();
    }

    public synchronized List<String> getAllObservedEvents() {
        return this.requestedEvent.getAllObservedEvents();
    }

    public synchronized List<String> getAllObservedLogs() {
        return this.requestedEvent.getAllObservedLogs();
    }

    /**
     * Returns the time at which the event was observed. The resulting number will be given in terms
     * of the specified time units.
     *
     * Returns a negative number if it has not yet been observed or was not observed.
     *
     * @param unit The unit of time to return the observation time in.
     * @return the time the event was observed or a negative number if it was not.
     */
    public long timeOfObservation(TimeUnit unit) {
        return (this.currentState == RequestState.SATISFIED)
            ? unit.convert(this.timeOfObservationInMilliseconds, TimeUnit.MILLISECONDS)
            : -1;
    }

    /**
     * Returns the time at which this request expires in the specified time units.
     *
     * @param unit The unit of time to return the deadline in.
     * @return the deadline for this request.
     */
    public long deadline(TimeUnit unit) {
        return unit.convert(this.deadlineInMilliseconds, TimeUnit.MILLISECONDS);
    }

    public synchronized void markAsRejected(String cause) {
        if (this.currentState == RequestState.PENDING) {
            this.causeOfRejection = cause;
            this.currentState = RequestState.REJECTED;
        }
    }

    public synchronized void markAsUnobserved() {
        if (this.currentState == RequestState.PENDING) {
            this.currentState = RequestState.UNOBSERVED;
        }
    }

    public synchronized void markAsExpired() {
        if (this.currentState == RequestState.PENDING) {
            this.currentState = RequestState.EXPIRED;
        }
    }

    public synchronized String getCauseOfRejection() {
        return this.causeOfRejection;
    }

    public synchronized boolean isPending() {
        return this.currentState == RequestState.PENDING;
    }

    public synchronized boolean isRejected() {
        return this.currentState == RequestState.REJECTED;
    }

    public synchronized boolean isUnobserved() {
        return this.currentState == RequestState.UNOBSERVED;
    }

    public synchronized boolean isSatisfied() {
        return this.currentState == RequestState.SATISFIED;
    }

    public synchronized boolean isExpired() {
        return this.currentState == RequestState.EXPIRED;
    }

    private synchronized void markAsExpiredIfPastDeadline(long time, TimeUnit unit) {
        if (isExpiredAtTime(time, unit)) {
            markAsExpired();
        }
    }

    private LogEventResult extractResultFromRequest() {
        if (isPending()) {
            throw new IllegalStateException("Cannot extract result from a still-pending request.");
        }

        if (this.currentState == RequestState.SATISFIED) {
            return LogEventResult.observedEvent(this.requestedEvent.getAllObservedEvents(), this.requestedEvent.getAllObservedEvents(), this.timeOfObservationInMilliseconds);
        } else if (this.currentState == RequestState.REJECTED) {
            return LogEventResult.rejectedEvent(this.causeOfRejection, this.requestedEvent.getAllObservedEvents(), this.requestedEvent.getAllObservedEvents());
        } else if (this.currentState == RequestState.EXPIRED) {
            return LogEventResult.expiredEvent(this.requestedEvent.getAllObservedEvents(), this.requestedEvent.getAllObservedEvents());
        } else {
            return LogEventResult.unobservedEvent(this.requestedEvent.getAllObservedEvents(), this.requestedEvent.getAllObservedEvents());
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
