package org.aion.harness.util;

import java.util.List;

/**
 * A request for some listener to listen for an event.
 *
 * All requests are in 1 of 5 states: pending, satisfied, unobserved, rejected, expired.
 *
 * All requests begin in the pending state and can move into exactly one of the other states in their
 * lifetime.
 *
 * Once a request is moved into a non-pending state it is considered finalized, and any listener
 * class that was listening for this event no longer has any obligation to continue listening for it.
 *
 * All classes implementing this interface must provide the following immutability guarantee:
 *
 *   - Once a request is moved into a finalized state it must become immutable.
 */
public interface IEventRequest {

    /**
     * Returns {@code true} only if the request is in a finalized state prior to calling this method
     * or as a consequence of calling this method.
     *
     * This method internally evaluates the underlying event to determine whether it was already
     * satisfied or not, and if so, then to determine whether or not the provided line satisfies the
     * event.
     *
     * If the request was not finalized prior to calling this method and the underlying event is
     * determined to be satisfied by the incoming line, and the current time does not cause it to
     * expire, then it will move into the "satisfied" state.
     *
     * @param line The next incoming line to check the event against.
     * @param currentTimeInMillis The current time.
     * @return whether or not this request is finalized.
     */
    public boolean isSatisfiedBy(String line, long currentTimeInMillis);

    /**
     * Returns {@code true} only if this request is timed out given the provided time in milliseconds.
     *
     * @param timeInMillis A time value to check expiry against.
     * @return true if this event expires by the provided time.
     */
    public boolean isExpiredAtTime(long timeInMillis);

    /**
     * Causes the caller to wait until the event has moved into a finalized state.
     *
     * For obvious purposes of efficiency, the listener should always notify the caller when an
     * event becomes finalized.
     */
    public void waitForOutcome();

    /**
     * Wakes up any threads currently sleeping in the {@code waitForOutcome()} method.
     */
    public void notifyRequestIsResolved();

    /**
     * Returns a list of all of the underlying event strings that were observed by the listener
     * while the request was pending, including whatever event strings caused the request to enter
     * a "satisfied" state if it did indeed enter one.
     *
     * @return All event strings witnessed before the request was finalized.
     */
    public List<String> getAllObservedEvents();

    /**
     * The time that the request was moved into a "satisfied" state if it was moved into this state.
     * Otherwise, if it is not satisfied, returns a negative number.
     *
     * @return a positive number if satisfied, representing the time of being satisfied.
     */
    public long timeOfObservation();

    /**
     * The latest time before this request expires.
     *
     * @return the deadline of this request.
     */
    public long deadline();

    /**
     * Marks this request as rejected only if it is pending at the time this method is invoked.
     *
     * @param cause The rejection cause.
     */
    public void markAsRejected(String cause);

    /**
     * Marks this request as unobserved only if it is pending at the time this method is invoked.
     */
    public void markAsUnobserved();

    /**
     * Marks this request as expired only if it is pending at the time this method is invoked.
     */
    public void markAsExpired();

    /**
     * Returns the cause for the request being rejected only if it is rejected. Otherwise returns
     * null.
     *
     * @return A rejection cause if rejected or null.
     */
    public String getCauseOfRejection();

    /**
     * Returns {@code true} only if this request is still pending.
     *
     * @return whether this request has been finalized.
     */
    public boolean isPending();

    /**
     * Returns {@code true} only if this request is rejected.
     *
     * @return whether this request has been rejected.
     */
    public boolean isRejected();

    /**
     * Returns {@code true} only if this request is marked unobserved.
     *
     * @return whether this request has not been observed.
     */
    public boolean isUnobserved();

    /**
     * Returns {@code true} only if this request is satisfied.
     *
     * @return whether this request has been satisfied.
     */
    public boolean isSatisfied();

    /**
     * Returns {@code true} only if this request is expired.
     *
     * @return whether this request is expired.
     */
    public boolean isExpired();


}
