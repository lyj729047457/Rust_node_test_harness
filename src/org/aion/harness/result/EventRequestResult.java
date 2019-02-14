package org.aion.harness.result;

/**
 * The result of a request for some event to be observed.
 *
 * An event request result can be in 1 of 3 possible states:
 *   - The event has been observed: indicates the event occurred.
 *   - The event was not observed: indicates the node shutdown before the event was witnessed.
 *   - The event was rejected: indicates that observation at the time of the request was not possible.
 *
 * In the case of an event being observed, it will also come with a timestamp (in nanoseconds)
 * indicating when the listener observed the event.
 *
 * In the case of an event being rejected, it will also come with a reason for why the event request
 * was rejected.
 *
 * An event request result is immutable.
 */
public final class EventRequestResult {
    private final boolean hasBeenObserved;
    private final boolean hasBeenRejected;
    private final String causeOfRejection;
    private final long timeOfObservationInNanos;

    private EventRequestResult(boolean isObserved, boolean isRejected, String rejectionCause, long observationTime) {
        if (isObserved && isRejected) {
            throw new IllegalArgumentException("An event cannot have both been observed and rejected.");
        }
        if (rejectionCause == null) {
            throw new NullPointerException("Cannot create an event request result with a null rejection cause.");
        }

        this.hasBeenObserved = isObserved;
        this.hasBeenRejected = isRejected;
        this.causeOfRejection = rejectionCause;
        this.timeOfObservationInNanos = observationTime;
    }

    /**
     * Returns a new event request result such that the corresponding event is confirmed to have
     * been observed at the specified time.
     *
     * @param timeOfObservationInNanoSeconds Time at which event was observed in nanoseconds.
     * @return a new observed event request result.
     */
    public static EventRequestResult createObservedEvent(long timeOfObservationInNanoSeconds) {
        return new EventRequestResult(true, false, "", timeOfObservationInNanoSeconds);
    }

    /**
     * Returns a new event request result such that the corresponding event is confirmed to have not
     * been observed at all. This can only happen if the event request was filed while the node was
     * running and the node was shutdown before the event was witnessed.
     *
     * @return a new unobserved event request result.
     */
    public static EventRequestResult createUnobservedEvent() {
        return new EventRequestResult(false, false, "", -1);
    }

    /**
     * Returns a new rejected request event result, indicating that the event request could not
     * possibly have been satisfied in any meaningful way. Since there are a variety of reasons for
     * this situation, a cause is also associated with this result.
     *
     * @param causeOfRejection The reason the event request was rejected.
     * @return a new rejected event request result
     */
    public static EventRequestResult createRejectedEvent(String causeOfRejection) {
        return new EventRequestResult(false, true, causeOfRejection, -1);
    }

    public boolean eventHasBeenObserved() {
        return this.hasBeenObserved;
    }

    public boolean eventHasBeenRejected() {
        return this.hasBeenRejected;
    }

    public String causeOfRejection() {
        return this.causeOfRejection;
    }

    public long timeOfObservationInNanoseconds() {
        return this.timeOfObservationInNanos;
    }

    @Override
    public String toString() {
        if (this.hasBeenObserved && !this.hasBeenRejected) {
            return "EventRequestResult { Observed at time: " + this.timeOfObservationInNanos + " (nano) }";
        } else if (!this.hasBeenObserved && this.hasBeenRejected) {
            return "EventRequestResult { Rejected due to: " + this.causeOfRejection + " }";
        } else {
            return "EventRequestResult { Unobserved }";
        }
    }

}
