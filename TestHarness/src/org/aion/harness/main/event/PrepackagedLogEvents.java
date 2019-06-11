package org.aion.harness.main.event;

import org.aion.harness.kernel.RawTransaction;

/**
 * An interface that provides a number of prepackaged events for kernel events.
 *
 * All events in this class are log events: events that are witnessed in the log file of a node.
 *
 * These events should be submitted to a {@link org.aion.harness.main.NodeListener}, who can then
 * determine whether or not the events occurred.
 *
 * This class always returns a {@code new} event when any of its methods are called.
 */
public interface PrepackagedLogEvents {
    /**
     * Returns an event that captures the miners being started up.
     *
     * @return the event.
     */
    IEvent getStartedMiningEvent();

    /**
     * Returns an event that captures a transaction being processed.
     *
     * A transaction is considered as processed if either it is sealed into a block or it is
     * rejected.
     *
     * @param transaction The transaction.
     * @return the event.
     */
    default IEvent getTransactionProcessedEvent(RawTransaction transaction) {
        return Event.or(getTransactionSealedEvent(transaction), getTransactionRejectedEvent(transaction));
    }

    /**
     * Returns an event that captures a transaction being sealed into a block.
     *
     * @param transaction The transaction.
     * @return the event.
     */
    IEvent getTransactionSealedEvent(RawTransaction transaction);

    /**
     * Returns an event that captures the node rejecting a transaction.
     *
     * @param transaction The transaction.
     * @return the event.
     */
    IEvent getTransactionRejectedEvent(RawTransaction transaction);

    /**
     * Returns an event that captures a log line that is expected to occur consistently over the
     * lifetime of a node.
     *
     * @return the event.
     */
    IEvent getHeartbeatEvent();
}
