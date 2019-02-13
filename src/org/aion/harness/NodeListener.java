package org.aion.harness;

import org.aion.harness.misc.Assumptions;
import org.aion.harness.result.Result;
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

    public Result listenForMiningStarted() throws InterruptedException {
        EventRequest request = new EventRequest(NodeEvent.getStartedMiningEvent());

        Result result = this.logListener.submit(request);
        if (!result.success) {
            return result;
        }

        return waitForEvent(request);
    }

    public Result listenForTransaction(byte[] transactionHash) throws InterruptedException {
        EventRequest request = new EventRequest(NodeEvent.getTransactionSealedEvent(transactionHash));

        Result result = this.logListener.submit(request);
        if (!result.success) {
            return result;
        }

        return waitForEvent(request);
    }

    private Result waitForEvent(EventRequest eventRequest) throws InterruptedException {
        long startTimeInMillis = System.currentTimeMillis();

        while (!eventRequest.hasResult()) {
            if (startTimeInMillis > System.currentTimeMillis() + Assumptions.REQUEST_MANAGER_TIMEOUT_MILLIS) {
                return Result.unsuccessful(Assumptions.PRODUCTION_ERROR_STATUS, "timed out waiting for request");
            }

            this.logListener.waitForRequest();
        }

        return eventRequest.getResult();


//        Result result = null;
//
//        long startTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
//        while (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) <= startTime + Assumptions.LISTENER_TIMEOUT_SECS) {
//            System.out.println("loopin");
//            result = this.logListener.getEventResult();
//
//            if (result != null) {
//                break;
//            }
//
//            Thread.sleep(TimeUnit.SECONDS.toMillis(2));
//        }
//
//        return (result == null)
//                ? Result.unsuccessful(Assumptions.PRODUCTION_ERROR_STATUS, "timed out listening to event")
//                : Result.successful();
    }

}
