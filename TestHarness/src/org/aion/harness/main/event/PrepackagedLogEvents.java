package org.aion.harness.main.event;

import org.aion.harness.kernel.RawTransaction;
import org.apache.commons.codec.binary.Hex;

/**
 * A class that holds a number of prepackaged events that may be of convenience.
 *
 * All events in this class are log events: events that are witnessed in the log file of a node.
 *
 * These events should be submitted to a {@link org.aion.harness.main.NodeListener}, who can then
 * determine whether or not the events occurred.
 *
 * This class always returns a {@code new} event when any of its methods are called.
 */
public final class PrepackagedLogEvents {

    /**
     * Returns an event that captures the miners being started up.
     *
     * @return the event.
     */
    public static IEvent getStartedMiningEvent() {
        return new Event("sealer starting");
    }

    /**
     * Returns an event that captures a transaction being processed.
     *
     * A transaction is considered as processed if either it is sealed into a block or it is
     * rejected.
     *
     * @param transaction The transaction.
     * @return the event.
     */
    public static IEvent getTransactionProcessedEvent(RawTransaction transaction) {
        return Event.or(getTransactionSealedEvent(transaction), getTransactionRejectedEvent(transaction));
    }

    /**
     * Returns an event that captures a transaction being sealed into a block.
     *
     * @param transaction The transaction.
     * @return the event.
     */
    public static IEvent getTransactionSealedEvent(RawTransaction transaction) {
        if (transaction == null) {
            throw new NullPointerException("Cannot get event for null transaction hash.");
        }
        return new Event("Transaction: " + Hex.encodeHexString(transaction.getTransactionHash()) + " was sealed into block");
    }

    /**
     * Returns an event that captures the node rejecting a transaction.
     *
     * @param transaction The transaction.
     * @return the event.
     */
    public static IEvent getTransactionRejectedEvent(RawTransaction transaction) {
        if (transaction == null) {
            throw new NullPointerException("Cannot get event for null transaction hash.");
        }
        return new Event("tx " + Hex.encodeHexString(transaction.getTransactionHash()) + " is rejected");
    }

    /**
     * Returns an event that captures a log line that is expected to occur consistently over the
     * lifetime of a node.
     *
     * @return the event.
     */
    public static IEvent getHeartbeatEvent() {
        return new Event("p2p-status");
    }

}
