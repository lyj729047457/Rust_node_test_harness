package org.aion.harness.main.event;

import java.util.List;

/**
 * An event is fundamentally a String (called the "event string", and
 * an event is "satisfied" (or rather, the event has occurred) when the
 * event string has been discovered in some line. The typical use case is
 * determining whether the event string has been discovered in a file,
 * like a log file.
 *
 * However, events can be strung together into complex logic using the
 * basic AND and OR operators so that the event has now effectively
 * become a conditional string. This allows for arbitrarily complex logic
 * to be embedded into a single event, which an outside observer interacts
 * with only by asking the event whether it has been satisfied (whether its
 * conditional logic evaluates to {@code true}) by some incoming String.
 * The outside observer needs know nothing about how complex the underlying
 * event is, but only whether or not the event has been observed (satisfied).
 *
 * At any point, the "satisfied" strings of the event can be queried. This
 * will produce all underlying event strings that exist in the conditional
 * logic such that they have been satisfied (observed). Therefore, once an
 * event is satisfied, all of the underlying strings that were observed and
 * contributed to its satisfaction can be queried and examined.
 *
 * Once an event is satisfied it cannot be "re-satisfied". Consider the case
 * of a logical-or, in which only one of the two conditions need be satisfied.
 * If one condition is satisfied and the other is not then the event is
 * determined to be satisfied. If this same event is later fed a new string
 * that would count as satisfying the other still unsatisfied string, then
 * this new information is ignored and the exact same state persists.
 *
 * Therefore once an event is satisfied, the conditions which originally
 * satisfied it will be made immutable.
 *
 * It is possible for a logical-or to have both of its components satisfied,
 * since an incoming string may satisfy multiple event strings at once. There
 * is no "precedence of satisfaction".
 *
 * All classes implementing this interface should satisfy the following two
 * immutability requirements:
 *
 *   1. The event string is immutable.
 *   2. Once an event is satisfied, its entire state must become immutable.
 */
public interface IEvent {

    /**
     * Returns this event as a string of the logical conditions that must be
     * satisfied in order for this event to be "observed".
     *
     * This string may be an arbitrarily nested set of expressions, where each
     * expression is one of the following:
     *
     * 1. (expression)
     * 2. (expression and expression)
     * 3. (expression or expression)
     */
    String eventStatement();

    /**
     * Returns a new event that can only be satisfied if the current event
     * AND the specified event have both been satisfied.
     *
     * @param event The other event.
     * @return the logical-and of this event and the other event.
     */
    IEvent and(IEvent event);

    /**
     * Returns a new event that can only be satisfied if the current event
     * OR the specified event have been satisfied.
     *
     * This is a logical or, and therefore this event is also considered to
     * be satisfied if both this event and the other event are satisfied as
     * well.
     *
     * @param event The other event.
     * @return the logical-or of this event and the other event.
     */
    IEvent or(IEvent event);

    /**
     * Returns {@code true} in two cases only:
     *   1. This event has already been satisfied and therefore any
     *      additional satisfaction tests cannot "unsatisfy" the event.
     *   2. This event has not already been satisfied but the provided
     *      line now satisfies the event.
     *
     * Therefore, a satisfied event can be interpreted as an event that
     * requires no additional actions in order to determine its outcome.
     *
     * @param line The incoming String that may satisfy the event.
     * @return whether or not the event is now satisfied.
     */
    boolean isSatisfiedBy(String line);

    /**
     * Returns a list of all the event strings that have been observed so far.
     *
     * @return all observed event strings.
     */
    List<String> getAllObservedEvents();

    /**
     * Returns a list of all the logs that have been recorded so far.
     *
     * @return all logs that are recorded
     */
    List<String> getAllEventLogs();
}
