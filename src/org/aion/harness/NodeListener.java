package org.aion.harness;

import org.aion.harness.result.EventRequestResult;
import org.aion.harness.util.EventRequest;
import org.aion.harness.util.LogListener;
import org.aion.harness.util.LogReader;
import org.aion.harness.util.NodeEvent;

/**
 * A class that listens to a node and waits for events to occur.
 */
public final class NodeListener {
    private LogListener logListener;

    public NodeListener() {
        this.logListener = LogReader.singleton().getLogListener();
    }

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
