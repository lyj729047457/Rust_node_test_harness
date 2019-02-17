package org.aion.harness.util;

import org.aion.harness.misc.Assumptions;
import org.aion.harness.result.EventRequestResult;
import org.aion.harness.result.Result;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;

import java.util.*;

public final class LogListener implements TailerListener {
    private static final int CAPACITY = 10;

    // The tailer is responsible for reading each line and updating us. We are its "observer".
    private Tailer tailer;

    private enum ListenerState { ALIVE_AND_LISTENING, ALIVE_AND_NOT_LISTENING, DEAD }

    // We begin as alive but not listening to any log file.
    private ListenerState currentState = ListenerState.ALIVE_AND_NOT_LISTENING;

    private List<EventRequest> requestPool = new ArrayList<>(CAPACITY);

    /**
     * Returns true only if the listener is not dead.
     */
    public synchronized boolean isAlive() {
        return this.currentState != ListenerState.DEAD;
    }

    /**
     * Returns the number of events that are currently being listened for. These events may have
     * been requested by separate {@link org.aion.harness.main.NodeListener} objects.
     * But these are the total number currently being processed.
     *
     * @return total number of events being listened for.
     */
    public synchronized int numberOfPendingEventRequests() {
        return this.requestPool.size();
    }

    /**
     * Attempts to submit the specified event request into the request pool.
     *
     * This method will block until the request is resolved. The request may
     * be resolved in any one of the following ways:
     *
     * 1. The listener was not listening when the request came in or it stopped
     *    listening after the request came in.
     *    -> request is marked rejected.
     * 2. The node is shut down after the request has come in.
     *    -> request is marked unobserved.
     * 3. The request expires.
     *    -> request is marked expired.
     * 4. The requester is interrupted while waiting for the outcome.
     *    -> request is marked rejected.
     * 5. The request is observed.
     *    -> request is marked satisfied.
     */
    public EventRequestResult submitEventRequest(EventRequest eventRequest) {
        if (eventRequest == null) {
            throw new NullPointerException("Cannot submit a null event request.");
        }

        synchronized (this) {

            if (this.currentState != ListenerState.ALIVE_AND_LISTENING) {
                return EventRequestResult.createRejectedEvent("Listener is not currently listening to a log file.");
            }

            // Attempt to add the request to the pool.
            addRequest(eventRequest);
        }

        // If the request is pending then it was added to the pool and hasn't been satisfied yet,
        // so we wait for the outcome.
        if (eventRequest.isPending()) {
            eventRequest.waitForOutcome();
        }

        return eventRequest.getResult();
    }

    /**
     * Returns success only if the listener is currently not dead and not listening and has now
     * started listening.
     *
     * If the listener is currently dead or is already listening then an appropriate unsuccessful
     * result is returned.
     */
    Result startListening() {
        if (this.currentState == ListenerState.DEAD) {
            return Result.unsuccessful(Assumptions.PRODUCTION_ERROR_STATUS, "Listener is dead!");
        } else if (this.currentState == ListenerState.ALIVE_AND_LISTENING) {
            // awkward for this to be "unsuccessful"
            return Result.unsuccessful(Assumptions.PRODUCTION_ERROR_STATUS, "Listener is already listening!");
        } else {
            this.currentState = ListenerState.ALIVE_AND_LISTENING;
            return Result.successful();
        }
    }

    /**
     * Sets the current state of the listener to be not listening only if the listener is currently
     * listening.
     */
    void stopListening() {
        if (this.currentState == ListenerState.ALIVE_AND_LISTENING) {
            this.currentState = ListenerState.ALIVE_AND_NOT_LISTENING;
        }
    }

    /**
     * Attempts to add the specified request to the request pool.
     *
     * If the pool is currently full then this method will wait until space frees up and will
     * add the request then.
     *
     * This attempt to add the request can fail for the following reasons. In each case the
     * request will no longer be in a 'pending' state so that the caller can verify whether
     * or not the request was added:
     *
     * 1. The listener stops listening before the request is added to the pool.
     *    -> request is marked rejected.
     * 2. The request expires before it is added to the pool.
     *    -> request is marked expired.
     * 3. An interrupt exception occurs before adding the request to the pool.
     *    -> request is marked rejected.
     */
    private synchronized void addRequest(EventRequest request) {
        long currentTimeInMillis = System.currentTimeMillis();

        // If the pool is full and we are not expired and listener is listening then wait until space frees up.
        ListenerState state = this.currentState;
        boolean expired = request.isExpiredAtTime(currentTimeInMillis);

        while ((state == ListenerState.ALIVE_AND_LISTENING) && (!expired) && (this.requestPool.size() == CAPACITY)) {

            try {
                Thread.sleep(request.deadline() - currentTimeInMillis);
            } catch (InterruptedException e) {
                request.markAsRejected("Interrupted while waiting on request!");
            }

            currentTimeInMillis = System.currentTimeMillis();
            expired = request.isExpiredAtTime(currentTimeInMillis);
            state = this.currentState;
        }

        // Check which of the above conditions changed and handle it.
        if (this.currentState != ListenerState.ALIVE_AND_LISTENING) {
            request.markAsRejected("Listener is no longer listening to a log file.");
        } else if (request.isExpiredAtTime(currentTimeInMillis)) {
            request.markAsExpired();
        } else {
            this.requestPool.add(request);
        }
    }

    /**
     * Runs through the entire request pool and marks every request that is waiting for the
     * specified event as "observed" and removes it from the request pool.
     *
     * In addition to marking the events as observed, any cancelled events are removed from the
     * request pool.
     *
     * If an event is both cancelled and observed then the cancellation trumps the latter and the
     * event is dropped.
     *
     * If {@code event == null} then no events will be marked as "observed".
     *
     * After walking through the entire pool, all threads waiting on this object's monitor are
     * awoken.
     *
     * @param event The event that has been observed.
     * @param timeOfObservation The time at which event was observed, in nanoseconds.
     */
    private synchronized void markRequestAsObservedAndCleanPool(NodeEvent event, long timeOfObservation) {
        System.err.println("Iterating over pool. Current pool size = " + this.requestPool.size());
        Iterator<EventRequest> requestIterator = this.requestPool.iterator();

        while (requestIterator.hasNext()) {
            EventRequest request = requestIterator.next();

            if (request.isExpired()) {
                requestIterator.remove();
                request.notifyRequestIsResolved();
            } else if (request.isCancelled()) {
                requestIterator.remove();
            } else if (request.getRequest().equals(event)) {
                request.addResult(EventRequestResult.createObservedEvent(Collections.singletonList(event.getEventString()), timeOfObservation));
                requestIterator.remove();

                request.markAsSatisfied();
                request.notifyRequestIsResolved();
            }
        }

        System.err.println("Finished iterating. Current pool size is now = " + this.requestPool.size());
    }

    /**
     * Returns the {@link NodeEvent} that has been observed in the specified event string.
     *
     * If no event in the pool corresponds to the provided event string then no event has been
     * observed and null is returned.
     *
     * @param eventString Some string that may or may not contain an event being listened for.
     * @return An observed event or null if no event is observed.
     */
    private synchronized NodeEvent getObservedEvent(String eventString) {
        if ((eventString == null) || (this.requestPool.isEmpty())) {
            return null;
        }

        for (EventRequest request : this.requestPool) {
            if (eventString.contains(request.getRequest().getEventString())) {
                return request.getRequest();
            }
        }

        return null;
    }

    /**
     * Rejects every request currently in the pool with the specified cause of the panic, then clears
     * the pool and notifies all waiting threads.
     *
     * @param causeOfPanic The reason for why the request pool is being killed.
     */
    private synchronized void killRequestPool(String causeOfPanic) {
        this.currentState = ListenerState.DEAD;

        for (EventRequest request : this.requestPool) {
            request.addResult(EventRequestResult.createRejectedEvent(causeOfPanic));
            request.markAsRejected(causeOfPanic);
            request.notifyRequestIsResolved();
        }
        this.requestPool.clear();
    }

    @Override
    public void init(Tailer tailer) {
        if (tailer == null) {
            throw new NullPointerException("Cannot initialize with a null tailer.");
        }

        this.tailer = tailer;
    }

    @Override
    public synchronized void handle(String nextLine) {
        if (this.currentState == ListenerState.ALIVE_AND_LISTENING) {

            // Check the pool and determine if an event has been observed and if so, grab that event.
            NodeEvent observedEvent = getObservedEvent(nextLine);
            long timeOfObservation = System.currentTimeMillis();

            // Marks all applicable events as "observed", clear the pool of them and notify the waiting threads.
            markRequestAsObservedAndCleanPool(observedEvent, timeOfObservation);
        }
    }

    @Override
    public void fileNotFound() {
        panic("Log file not found!");
    }

    @Override
    public void fileRotated() {
        // File not found because we die immediately there is no time to tell it was rotated.
        panic("Log file not found!");
    }

    @Override
    public void handle(Exception e) {
        panic(e.toString());
    }

    private void panic(String cause) {
        killRequestPool(cause);
        this.tailer.stop();
    }

}
