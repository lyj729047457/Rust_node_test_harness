package org.aion.harness.main;

import java.math.BigInteger;
import java.text.NumberFormat;
import org.aion.harness.main.event.Event;
import org.aion.harness.main.event.IEvent;
import org.aion.harness.main.event.OrEvent;
import org.aion.harness.main.global.SingletonFactory;
import org.aion.harness.main.types.SyncStatus;
import org.aion.harness.misc.Assumptions;
import org.aion.harness.result.LogEventResult;
import org.aion.harness.result.Result;
import org.aion.harness.result.RpcResult;
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
    public LogEventResult waitForMinersToStart(long timeoutInMillis) {
        if (timeoutInMillis < 0) {
            throw new IllegalArgumentException("Timeout value was negative: " + timeoutInMillis);
        }

        long deadline = System.currentTimeMillis() + timeoutInMillis;
        IEventRequest request = new EventRequest(getStartedMiningEvent(), deadline);
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
    public LogEventResult waitForTransactionToBeProcessed(byte[] transactionHash, long timeoutInMillis) {
        if (transactionHash == null) {
            throw new NullPointerException("Cannot wait for a null transaction hash.");
        }
        if (timeoutInMillis < 0) {
            throw new IllegalArgumentException("Timeout value was negative: " + timeoutInMillis);
        }

        long deadline = System.currentTimeMillis() + timeoutInMillis;

        IEvent transactionSealedEvent = getTransactionSealedEvent(transactionHash);
        IEvent transactionRejectedEvent = getTransactionRejectedEvent(transactionHash);
        IEvent transactionProcessedEvent = new OrEvent(transactionSealedEvent, transactionRejectedEvent);

        IEventRequest request = new EventRequest(transactionProcessedEvent, deadline);

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
    public LogEventResult waitForHeartbeat(long timeoutInMillis) {
        if (timeoutInMillis < 0) {
            throw new IllegalArgumentException("Timeout value was negative: " + timeoutInMillis);
        }

        long deadline = System.currentTimeMillis() + timeoutInMillis;
        IEventRequest request = new EventRequest(getHeartbeatEvent(), deadline);
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
    public LogEventResult waitForEvent(IEvent event, long timeoutInMillis) {
        if (event == null) {
            throw new NullPointerException("Cannot wait for a null event.");
        }
        if (timeoutInMillis < 0) {
            throw new IllegalArgumentException("Timeout value was negative: " + timeoutInMillis);
        }

        long deadline = System.currentTimeMillis() + timeoutInMillis;
        IEventRequest request = new EventRequest(event, deadline);
        this.logListener.submitEventRequest(request);
        return extractResult(request);
    }

    /**
     * Blocks until the node has finished syncing with the rest of the network, or until the request
     * times out.
     *
     * Technically speaking, a node never finishes syncing with the network unless it is always
     * at the top block and always the first to mine the next block. However, once at the top of
     * the chain, a node is in a relatively stable position with regards to the network. Therefore
     * this method considers a node in sync with the network if it is within 5 blocks of the top
     * of the chain.
     *
     * @param delayInMillis The amount of time to wait between checking the current sync status.
     * @param timeoutInMillis The total amount of time to wait for syncing.
     * @return  the result of this event.
     */
    public Result waitForSyncToComplete(long delayInMillis, long timeoutInMillis) {
        if (timeoutInMillis < 0) {
            throw new IllegalArgumentException("Timeout value was negative: " + timeoutInMillis);
        }

        RPC rpc = new RPC();

        try {
            long currentTime = System.currentTimeMillis();
            long deadline = currentTime + timeoutInMillis;

            RpcResult<SyncStatus> syncStatus = rpc.getSyncingStatus();
            if (!syncStatus.success) {
                return Result.unsuccessfulDueTo(syncStatus.error);
            }

            while ((currentTime < deadline) && (syncStatus.getResult().isSyncing())) {
                // Log the current status.
                SyncStatus status = syncStatus.getResult();
                broadcastSyncUpdate(status.isWaitingToConnect(), status.getSyncCurrentBlockNumber(), status.getHighestBlockNumber());

                // Sleep for the specified delay, unless there is less time remaining until the
                // deadline, then sleep only until the deadline.
                Thread.sleep(Math.min(deadline - currentTime, delayInMillis));

                // Update the status.
                syncStatus = rpc.getSyncingStatus();
                if (!syncStatus.success) {
                    return Result.unsuccessfulDueTo(syncStatus.error);
                }

                currentTime = System.currentTimeMillis();
            }

            // We either timed out or finished syncing.
            if (currentTime >= deadline) {
                return Result.unsuccessfulDueTo("Timed out waiting for sync to finish.");
            } else {
                System.out.println(syncStatus);
                return Result.successful();
            }

        } catch (InterruptedException e) {
            return Result.unsuccessfulDueToException(e);
        }
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

    private void broadcastSyncUpdate(boolean waitingToConnect, BigInteger currentBlock, BigInteger highestBlock) {
        if (waitingToConnect) {
            System.out.println(Assumptions.LOGGER_BANNER + "Sync Process = { waiting to connect to peers }");
        } else {
            System.out.println(Assumptions.LOGGER_BANNER + "Sync Progress = { At block: "
                + NumberFormat.getIntegerInstance().format(currentBlock)
                + " of " + NumberFormat.getIntegerInstance().format(highestBlock) + " }");
        }
    }

    private LogEventResult extractResult(IEventRequest request) {
        if (request.isUnobserved()) {
            return LogEventResult.unobservedEvent(request.getAllObservedEvents(), request.getAllObservedEvents());
        } else if (request.isSatisfied()) {
            return LogEventResult.observedEvent(request.getAllObservedEvents(), request.getAllObservedEvents(), request.timeOfObservation());
        } else if (request.isExpired()) {
            return LogEventResult.expiredEvent(request.getAllObservedEvents(), request.getAllObservedEvents());
        } else if (request.isRejected()) {
            return LogEventResult.rejectedEvent(request.getCauseOfRejection(), request.getAllObservedEvents(), request.getAllObservedEvents());
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

    private IEvent getTransactionRejectedEvent(byte[] transactionHash) {
        if (transactionHash == null) {
            throw new NullPointerException("Cannot get event for null transaction hash.");
        }
        return new Event("tx " + Hex.encodeHexString(transactionHash) + " is rejected");
    }

    private IEvent getHeartbeatEvent() {
        return new Event("p2p-status");
    }


}
