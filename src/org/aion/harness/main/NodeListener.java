package org.aion.harness.main;

import org.aion.harness.main.global.SingletonFactory;
import org.aion.harness.result.EventRequestResult;
import org.aion.harness.util.EventRequest;
import org.aion.harness.util.IEventRequest;
import org.aion.harness.util.LogListener;
import org.aion.harness.util.NodeEvent;

/**
 * A class that listens to a node and waits for events to occur.
 */
public final class NodeListener {
    private final LogListener logListener = SingletonFactory.singleton().logReader().getLogListener();

    public EventRequestResult waitForMinersToStart(long timeoutInMillis) {
        long deadline = System.currentTimeMillis() + timeoutInMillis;
        EventRequest request = new EventRequest(NodeEvent.getStartedMiningEvent(), deadline);
        this.logListener.submitEventRequest(request);
        return extractResult(request);
    }

    public EventRequestResult waitForTransactionToBeSealed(byte[] transactionHash, long timeoutInMillis) {
        long deadline = System.currentTimeMillis() + timeoutInMillis;
        EventRequest request = new EventRequest(NodeEvent.getTransactionSealedEvent(transactionHash), deadline);
        this.logListener.submitEventRequest(request);
        return extractResult(request);
    }

    public EventRequestResult waitForHeartbeat(long timeoutInMillis) {
        long deadline = System.currentTimeMillis() + timeoutInMillis;
        EventRequest request = new EventRequest(NodeEvent.getHeartbeatEvent(), deadline);
        this.logListener.submitEventRequest(request);
        return extractResult(request);
    }

    public EventRequestResult waitForLine(String line, long timeoutInMillis) {
        long deadline = System.currentTimeMillis() + timeoutInMillis;
        EventRequest request = new EventRequest(NodeEvent.getCustomStringEvent(line), deadline);
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


}
