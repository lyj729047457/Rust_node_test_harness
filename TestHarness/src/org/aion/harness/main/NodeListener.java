package org.aion.harness.main;

import java.util.concurrent.TimeUnit;
import org.aion.harness.kernel.RawTransaction;
import org.aion.harness.main.event.Event;
import org.aion.harness.main.event.IEvent;
import org.aion.harness.main.event.OrEvent;
import org.aion.harness.main.global.SingletonFactory;
import org.aion.harness.result.FutureResult;
import org.aion.harness.result.LogEventResult;
import org.aion.harness.util.LogListener;
import org.apache.commons.codec.binary.Hex;

/**
 * A listener that listens to a node and waits for logging events to occur.
 *
 * If no node is currently alive then all {@code waitFor...} methods in this class return immediately
 * with rejected results.
 *
 * All methods that begin with {@code waitFor} are blocking methods that will not return until the
 * event has been moved into some final state: either it was observed, unobserved (meaning the node
 * shut down before it was seen), it expired (timed out) or it was rejected (for various reasons).
 *
 * This class is not thread-safe.
 */
public final class NodeListener {
    private final LogListener logListener;

    private NodeListener(LogListener logListener) {
        this.logListener = logListener;
    }

    public static NodeListener listenTo(Node node) {
        if (node == null) {
            throw new IllegalStateException("node cannot be null");
        }
        return new NodeListener(SingletonFactory.singleton().nodeWatcher().getReaderForNodeByID(node.getID()).getLogListener());
    }

    /**
     * Listens for the miners to start up.
     *
     * This method is non-blocking but returns a blocking {@link java.util.concurrent.Future}
     * implementation.
     *
     * @param timeout The duration after which the event expires.
     * @param unit The time unit of the duration.
     * @return the result of this event.
     */
    public FutureResult<LogEventResult> listenForMinersToStart(long timeout, TimeUnit unit) {
        if (timeout < 0) {
            throw new IllegalArgumentException("Timeout value was negative: " + timeout);
        }

        return this.logListener.submitEventToBeListenedFor(getStartedMiningEvent(), timeout, unit);
    }

    /**
     * Listens for the transaction to be processed.
     *
     * This method is non-blocking but returns a blocking {@link java.util.concurrent.Future}
     * implementation.
     *
     * @param transaction The transaction.
     * @param timeout The duration after which the event expires.
     * @param unit The time unit of the duration.
     * @return the result of this event.
     */
    public FutureResult<LogEventResult> listenForTransactionToBeProcessed(RawTransaction transaction, long timeout, TimeUnit unit) {
        if (timeout < 0) {
            throw new IllegalArgumentException("Timeout value was negative: " + timeout);
        }

        IEvent transactionSealedEvent = getTransactionSealedEvent(transaction);
        IEvent transactionRejectedEvent = getTransactionRejectedEvent(transaction);
        IEvent transactionProcessedEvent = new OrEvent(transactionSealedEvent, transactionRejectedEvent);

        return this.logListener.submitEventToBeListenedFor(transactionProcessedEvent, timeout, unit);
    }

    /**
     * Listens for the heartbeat event to occur.
     *
     * This method is non-blocking but returns a blocking {@link java.util.concurrent.Future}
     * implementation.
     *
     * @param timeout The duration after which the event expires.
     * @param unit The time unit of the duration.
     * @return the result of this event.
     */
    public FutureResult<LogEventResult> listenForHeartbeat(long timeout, TimeUnit unit) {
        if (timeout < 0) {
            throw new IllegalArgumentException("Timeout value was negative: " + timeout);
        }

        return this.logListener.submitEventToBeListenedFor(getHeartbeatEvent(), timeout, unit);
    }

    /**
     * Listens for the specified event to occur.
     *
     * This method is non-blocking but returns a blocking {@link java.util.concurrent.Future}
     * implementation.
     *
     * @param event The event to listen for.
     * @param timeout The duration after which the event expires.
     * @param unit The time unit of the duration.
     * @return the result of this event.
     */
    public FutureResult<LogEventResult> listenForEvent(IEvent event, long timeout, TimeUnit unit) {
        if (event == null) {
            throw new NullPointerException("Cannot wait for a null event.");
        }
        if (timeout < 0) {
            throw new IllegalArgumentException("Timeout value was negative: " + timeout);
        }

        return this.logListener.submitEventToBeListenedFor(event, timeout, unit);
    }

    /**
     * Returns the number of events that are currently being listened for. These events may have
     * been requested by separate {@link NodeListener} objects. But these are the total number
     * currently being processed.
     *
     * @return total number of events being listened for.
     */
    public  int numberOfEventsBeingListenedFor() {
        return this.logListener.numberOfPendingEventRequests();
    }

    // ------------ pre-packaged events that this class provides ---------------

    private IEvent getStartedMiningEvent() {
        return new Event("sealer starting");
    }

    private IEvent getTransactionSealedEvent(RawTransaction transaction) {
        if (transaction == null) {
            throw new NullPointerException("Cannot get event for null transaction.");
        }
        return new Event("Transaction: " + Hex.encodeHexString(transaction.getTransactionHash()) + " was sealed into block");
    }

    private IEvent getTransactionRejectedEvent(RawTransaction transaction) {
        if (transaction == null) {
            throw new NullPointerException("Cannot get event for null transaction.");
        }
        return new Event("tx " + Hex.encodeHexString(transaction.getTransactionHash()) + " is rejected");
    }

    private IEvent getHeartbeatEvent() {
        return new Event("p2p-status");
    }


}
