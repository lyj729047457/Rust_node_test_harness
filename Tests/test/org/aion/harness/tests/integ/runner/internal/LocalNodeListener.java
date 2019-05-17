package org.aion.harness.tests.integ.runner.internal;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.aion.harness.main.NodeListener;
import org.aion.harness.main.event.IEvent;
import org.aion.harness.result.FutureResult;
import org.aion.harness.result.LogEventResult;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A node listener that can be used by tests to listen for log events on the node they are talking
 * to.
 */
public final class LocalNodeListener implements TestRule {
    private NodeListener listener;

    /**
     * This method is invoked via reflection by the custom runner classes to initialize the listener.
     */
    private void setListener(NodeListener listener) {
        this.listener = listener;
    }

    /**
     * Do not invoke.
     */
    @Override
    public Statement apply(Statement statement, Description description) {
        throw new UnsupportedOperationException("Didn't you read my doc?");
    }

    /**
     * Returns a future result of waiting for the given event to be witnessed.
     *
     * This method is non-blocking.
     *
     * @param event The event to listen for.
     * @param timeout The timeout amount.
     * @param unit The time units of the timeout amount.
     * @return A future result.
     */
    public FutureResult<LogEventResult> listenForEvent(IEvent event, long timeout, TimeUnit unit) {
        return this.listener.listenForEvent(event, timeout, unit);
    }

    /**
     * Returns a list of future results of waiting for the given events to be witnessed.
     *
     * This method is non-blocking.
     *
     * @param events The events to listen for.
     * @param timeout The timeout amount.
     * @param unit The time units of the timeout amount.
     * @return A list of future results.
     */
    public List<FutureResult<LogEventResult>> listenForEvents(List<IEvent> events, long timeout, TimeUnit unit) {
        return this.listener.listenForEvents(events, timeout, unit);
    }
}
