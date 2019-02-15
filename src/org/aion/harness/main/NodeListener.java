package org.aion.harness.main;

import org.aion.harness.main.global.SingletonFactory;
import org.aion.harness.result.EventRequestResult;
import org.aion.harness.util.EventRequest;
import org.aion.harness.util.LogListener;
import org.aion.harness.util.NodeEvent;

/**
 * A class that listens to a node and waits for events to occur.
 */
public final class NodeListener {
    private final LogListener logListener = SingletonFactory.singleton().logReader().getLogListener();

    public EventRequestResult waitForMinersToStart(long timeoutInMillis) {
        EventRequest request = new EventRequest(NodeEvent.getStartedMiningEvent());
        return this.logListener.submitEventRequest(request, timeoutInMillis);
    }

    public EventRequestResult waitForTransactionToBeSealed(byte[] transactionHash, long timeoutInMillis) {
        EventRequest request = new EventRequest(NodeEvent.getTransactionSealedEvent(transactionHash));
        return this.logListener.submitEventRequest(request, timeoutInMillis);
    }

    public EventRequestResult waitForHeartbeat(long timeoutInMillis) {
        EventRequest request = new EventRequest(NodeEvent.getHeartbeatEvent());
        return this.logListener.submitEventRequest(request, timeoutInMillis);
    }

}
