package org.aion.harness.result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The result of a request for some log event to be observed.
 *
 * A log event result can be in 1 of 4 possible states:
 *   - The event has been observed: indicates the event occurred.
 *   - The event was not observed: indicates the node shutdown before the event was witnessed.
 *   - The event was rejected: indicates that observation at the time of the request was not possible.
 *   - The event was expired: indicates that the event timed out before being satisfied.
 *
 * In the case of an event being observed, it will also come with a timestamp (in milliseconds)
 * indicating when the listener observed the event.
 *
 * In the case of an event being rejected, it will also come with a reason for why the event request
 * was rejected.
 *
 * There is not concept of equality defined for a log event result.
 *
 * A log event result is immutable.
 */
public final class LogEventResult {
    private final RequestResultState resultState;
    private final List<String> observedEvents;
    private final List<String> observedLogs;

    private final long timeOfObservationInMillis;
    private final String causeOfRejection;

    private enum RequestResultState { OBSERVED, UNOBSERVED, REJECTED, EXPIRED }

    private LogEventResult(RequestResultState requestState, List<String> observedEvents, List<String> observedLogs, String rejectionCause, long observationTime) {
        if (requestState == null) {
            throw new NullPointerException("Cannot construct result with null state.");
        }

        this.resultState = requestState;
        this.observedEvents = (observedEvents == null) ? Collections.emptyList() : new ArrayList<>(observedEvents);
        this.observedLogs = (observedLogs == null) ? Collections.emptyList() : new ArrayList<>(observedLogs);
        this.causeOfRejection = rejectionCause;
        this.timeOfObservationInMillis = observationTime;
    }

    /**
     * Returns a new event request result such that the corresponding event is confirmed to have
     * been observed at the specified time.
     *
     * @param observedEvents The event strings that were observed.
     * @param observedLogs The log lines that satisfied the observed event strings.
     * @param timeOfObservationInMillis Time at which event was observed in milliseconds.
     * @return a new observed event request result.
     */
    public static LogEventResult observedEvent(List<String> observedEvents, List<String> observedLogs, long timeOfObservationInMillis) {
        return new LogEventResult(RequestResultState.OBSERVED, observedEvents, observedLogs, null, timeOfObservationInMillis);
    }

    /**
     * Returns a new event request result such that the corresponding event is confirmed to have not
     * been observed at all. This can only happen if the event request was filed while the node was
     * running and the node was shutdown before the event was witnessed.
     *
     * @param observedEvents The event strings that were observed.
     * @param observedLogs The log lines that satisfied the observed event strings.
     * @return a new unobserved event request result.
     */
    public static LogEventResult unobservedEvent(List<String> observedEvents, List<String> observedLogs) {
        return new LogEventResult(RequestResultState.UNOBSERVED, observedEvents, observedLogs, null, -1);
    }

    /**
     * Returns a new rejected request event result, indicating that the event request could not
     * possibly have been satisfied in any meaningful way. Since there are a variety of reasons for
     * this situation, a cause is also associated with this result.
     *
     * @param causeOfRejection The reason the event request was rejected.
     * @param observedEvents The event strings that were observed.
     * @param observedLogs The log lines that satisfied the observed event strings.
     * @return a new rejected event request result
     */
    public static LogEventResult rejectedEvent(String causeOfRejection, List<String> observedEvents, List<String> observedLogs) {
        return new LogEventResult(RequestResultState.REJECTED, observedEvents, observedLogs, causeOfRejection, -1);
    }

    /**
     * Returns a new expired request event result, indicating that the event request had timed out
     * before it had been satisfied.
     *
     * @param observedEvents The event strings that were observed.
     * @param observedLogs The log lines that satisfied the observed event strings.
     * @return a new expired event request result.
     */
    public static LogEventResult expiredEvent(List<String> observedEvents, List<String> observedLogs) {
        return new LogEventResult(RequestResultState.EXPIRED, observedEvents, observedLogs,null, -1);
    }

    /**
     * Returns {@code true} only if the event was observed.
     *
     * @return whether or not the event was observed.
     */
    public boolean eventWasObserved() {
        return this.resultState == RequestResultState.OBSERVED;
    }

    /**
     * Returns {@code true} only if the event was rejected.
     *
     * @return whether or not the event was rejected.
     */
    public boolean eventWasRejected() {
        return this.resultState == RequestResultState.REJECTED;
    }

    /**
     * Returns {@code true} only if the event was unobserved - that is, if the node shut down before
     * the event was witnessed.
     *
     * @return whether or not the event was unobserved.
     */
    public boolean eventWasUnobserved() {
        return this.resultState == RequestResultState.UNOBSERVED;
    }

    /**
     * Returns {@code true} only if the event had expired (timed out).
     *
     * @return whether or not the event expired.
     */
    public boolean eventExpired() {
        return this.resultState == RequestResultState.EXPIRED;
    }

    /**
     * Returns the cause for this result being rejected only if it was rejected. Otherwise, returns
     * null.
     *
     * @return A cause if rejected or else null.
     */
    public String causeOfRejection() {
        return this.causeOfRejection;
    }

    /**
     * Returns the time that the event was observed (in milliseconds) only if it was observed.
     * Otherwise, returns a negative number.
     *
     * @return The observation time or a negative number.
     */
    public long timeOfObservationInMilliseconds() {
        return this.timeOfObservationInMillis;
    }

    /**
     * Returns a list of all of the underlying event strings that were actually observed by the
     * listener.
     *
     * @return All event strings that were observed by the listener.
     */
    public List<String> getAllObservedEvents() {
        return new ArrayList<>(this.observedEvents);
    }

    /**
     * Returns a list of all the log lines that contributed towards satisfying an underlying event
     * string.
     *
     * @return All log lines that observed an underlying event string.
     */
    public List<String> getObservedLogs() {
        return new ArrayList<>(this.observedLogs);
    }

    @Override
    public String toString() {
        if (this.resultState == RequestResultState.OBSERVED) {
            return "LogEventResult { Observed at time: " + this.timeOfObservationInMillis + " (millis) }";
        } else if (this.resultState == RequestResultState.UNOBSERVED) {
            return "LogEventResult { Unobserved }";
        } else if (this.resultState == RequestResultState.REJECTED) {
            return "LogEventResult { Rejected due to: " + this.causeOfRejection + " }";
        } else {
            return "LogEventResult { Expired }";
        }
    }

}
