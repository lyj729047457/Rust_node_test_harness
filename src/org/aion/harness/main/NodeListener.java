package org.aion.harness.main;

import org.aion.harness.main.event.Event;
import org.aion.harness.main.event.IEvent;
import org.aion.harness.main.global.SingletonFactory;
import org.aion.harness.result.EventRequestResult;
import org.aion.harness.util.EventRequest;
import org.aion.harness.util.IEventRequest;
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
    private final LogListener logListener = SingletonFactory.singleton().logReader().getLogListener();

    /**
     * Blocks until the miners have been activated in the node, or until the request has timed out,
     * was rejected or was not observed before the node's shut down.
     *
     * @param timeoutInMillis Maximum time to wait in milliseconds.
     * @return the result of this event.
     * @throws IllegalArgumentException if timeoutInMillis is negative.
     */
    public EventRequestResult waitForMinersToStart(long timeoutInMillis) {
        if (timeoutInMillis < 0) {
            throw new IllegalArgumentException("Timeout value was negative: " + timeoutInMillis);
        }

        long deadline = System.currentTimeMillis() + timeoutInMillis;
        EventRequest request = new EventRequest(getStartedMiningEvent(), deadline);
        this.logListener.submitEventRequest(request);
        return extractResult(request);
    }

    /**
     * Blocks until a transaction with the specified hash has been sealed into a block, or until the
     * request has timed out, was rejected or was not observed before the node's shut down.
     *
     * @param transactionHash The hash of the transaction to watch for.
     * @param timeoutInMillis Maximum time to wait in milliseconds.
     * @return the result of this event.
     * @throws NullPointerException if transactionHash is null.
     * @throws IllegalArgumentException if timeoutInMillis is negative.
     */
    public EventRequestResult waitForTransactionToBeSealed(byte[] transactionHash, long timeoutInMillis) {
        if (transactionHash == null) {
            throw new NullPointerException("Cannot wait for a null transaction hash.");
        }
        if (timeoutInMillis < 0) {
            throw new IllegalArgumentException("Timeout value was negative: " + timeoutInMillis);
        }

        long deadline = System.currentTimeMillis() + timeoutInMillis;
        EventRequest request = new EventRequest(getTransactionSealedEvent(transactionHash), deadline);
        this.logListener.submitEventRequest(request);
        return extractResult(request);
    }

    /**
     * Blocks until some "heartbeat" event has been observed, or until the request has timed out,
     * was rejected or was not observed before the node's shut down.
     *
     * A heartbeat event is an internal detail, but is expected to be a consistently occurring event
     * in the node's output log.
     *
     * @param timeoutInMillis Maximum time to wait in milliseconds.
     * @return the result of this event.
     * @throws IllegalArgumentException if timeoutInMillis is negative.
     */
    public EventRequestResult waitForHeartbeat(long timeoutInMillis) {
        if (timeoutInMillis < 0) {
            throw new IllegalArgumentException("Timeout value was negative: " + timeoutInMillis);
        }

        long deadline = System.currentTimeMillis() + timeoutInMillis;
        EventRequest request = new EventRequest(getHeartbeatEvent(), deadline);
        this.logListener.submitEventRequest(request);
        return extractResult(request);
    }

    /**
     * Blocks until the specified event has occurred, or until the request has timed out,
     * was rejected or was not observed before the node's shut down.
     *
     * @param timeoutInMillis Maximum time to wait in milliseconds.
     * @return the result of this event.
     * @throws NullPointerException If event is null.
     * @throws IllegalArgumentException if timeoutInMillis is negative.
     */
    public EventRequestResult waitForEvent(IEvent event, long timeoutInMillis) {
        if (event == null) {
            throw new NullPointerException("Cannot wait for a null event.");
        }
        if (timeoutInMillis < 0) {
            throw new IllegalArgumentException("Timeout value was negative: " + timeoutInMillis);
        }

        long deadline = System.currentTimeMillis() + timeoutInMillis;
        EventRequest request = new EventRequest(event, deadline);
        this.logListener.submitEventRequest(request);
        return extractResult(request);
    }

    /**
     * Returns the number of events that are currently being listened for. These events may have
     * been requested by separate {@link NodeListener} objects. But these are the total number
     * currently being processed.
     *
     * @return total number of events being listened for.
     */
    public static int numberOfEventsBeingListenedFor() {
        return SingletonFactory.singleton().logReader().getLogListener().numberOfPendingEventRequests();
    }

    private EventRequestResult extractResult(IEventRequest request) {
        if (request.isUnobserved()) {
            return EventRequestResult.unobservedEvent();
        } else if (request.isSatisfied()) {
            return EventRequestResult.observedEvent(request.getAllObservedEvents(), request.timeOfObservation());
        } else if (request.isExpired()) {
            return EventRequestResult.expiredEvent();
        } else if (request.isRejected()) {
            return EventRequestResult.rejectedEvent(request.getCauseOfRejection());
        } else {
            throw new IllegalStateException("Waited for event until notified but event is still pending!");
        }
    }

    // ------------ pre-packaged events that this class provides ---------------

    private IEvent getStartedMiningEvent() {
        return new Event("sealer starting");
    }

    private IEvent getTransactionSealedEvent(byte[] transactionHash) {
        if (transactionHash == null) {
            throw new NullPointerException("Cannot get event for null transaction hash.");
        }
        return new Event("Transaction: " + Hex.encodeHexString(transactionHash) + " was sealed into block");
    }

    private IEvent getHeartbeatEvent() {
        return new Event("p2p-status");
    }


}
