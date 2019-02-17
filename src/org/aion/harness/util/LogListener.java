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
     * Adds the specified event to the list of events that this listener is currently listening for.
     *
     * If the listener is already listening to the maximum number of events then the caller will
     * wait until space becomes available.
     *
     * This method is blocking and will return only once the event request has been observed or
     * if the timeout has occurred or if the thread is interrupted, in which case it will return
     * immediately.
     *
     * @param eventRequest The event to request the listener listen for.
     * @param timeoutInMillis The amount of milliseconds to timeout after.
     * @return the result of this request.
     */
    public synchronized EventRequestResult submitEventRequest(EventRequest eventRequest, long timeoutInMillis) {
        if (eventRequest == null) {
            throw new NullPointerException("Cannot submit a null event request.");
        }

        if (this.currentState != ListenerState.ALIVE_AND_LISTENING) {
            return EventRequestResult.createRejectedEvent("Listener is not currently listening to a log file.");
        }

        long endTimeInMillis = System.currentTimeMillis() + timeoutInMillis;

        try {

            // Add the request to the pool, if we time out then return.
            if (!addRequest(eventRequest, endTimeInMillis)) {
                eventRequest.cancel(); // shouldn't be necessary, here for sanity.
                return EventRequestResult.createRejectedEvent("Timed out waiting for availability in the request pool.");
            }

            // Request was added and we have not yet necessarily timed out, so wait for response.
            long currentTimeInMillis = System.currentTimeMillis();

            synchronized (this) {
                while ((currentTimeInMillis < endTimeInMillis) && (!eventRequest.hasResult())) {
                    System.err.println("Waiting for response..");
                    wait(endTimeInMillis - currentTimeInMillis);
                    currentTimeInMillis = System.currentTimeMillis();
                }
            }

            System.err.println("Got response or timed out!");
            if (eventRequest.hasResult()) {
                return eventRequest.getResult();
            } else {
                eventRequest.cancel();
                return EventRequestResult.createRejectedEvent("Timed out waiting for event to occur.");
            }

        } catch (InterruptedException e) {
            eventRequest.cancel();
            return EventRequestResult.createRejectedEvent("Interrupted while waiting for event.");
        }

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
     * Attempts to add the specified request to the list of requests that this listener is listening
     * for.
     *
     * If the current time (in milliseconds) ever becomes equal to or greater than endTimeInMillis
     * then this action will be considered as having timed out and the request will not be added.
     *
     * @param request The request to add to the list of monitored events.
     * @param endTimeInMillis The latest current time to continue attempting to add the event at.
     * @return true if request was added, false if timed out waiting to add request.
     */
    private synchronized boolean addRequest(EventRequest request, long endTimeInMillis) throws InterruptedException {
        long currentTimeInMillis = System.currentTimeMillis();

        while ((currentTimeInMillis < endTimeInMillis) && (this.requestPool.size() == CAPACITY)) {
            System.err.println("Waiting for capacity to free up...");
            wait(endTimeInMillis - currentTimeInMillis);
            currentTimeInMillis = System.currentTimeMillis();
        }

        if (currentTimeInMillis >= endTimeInMillis) {
            System.err.println("Timed out waiting for capacity...");
            return false;
        }

        this.requestPool.add(request);
        System.err.println("Added request, pool size is now = " + this.requestPool.size());
        return true;
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

            if (request.isCancelled()) {
                requestIterator.remove();
            } else if (request.getRequest().equals(event)) {
                request.addResult(EventRequestResult.createObservedEvent(timeOfObservation));
                requestIterator.remove();
            }
        }

        System.err.println("Finished iterating. Current pool size is now = " + this.requestPool.size());

        notifyAll();
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
        }
        this.requestPool.clear();

        notifyAll();
    }

    @Override
    public void init(Tailer tailer) {
        if (tailer == null) {
            throw new NullPointerException("Cannot initialize with a null tailer.");
        }

        this.tailer = tailer;
    }

    @Override
    public void fileNotFound() {
        // Reject all requests in the pool and clear the pool.
        killRequestPool("Log file not found!");

        // Shut down the tailer.
        this.tailer.stop();
    }

    @Override
    public void fileRotated() {
        // Reject all requests in the pool and clear the pool.
        killRequestPool("Log file not found!");

        // Shut down the tailer.
        this.tailer.stop();
    }

    @Override
    public synchronized void handle(String nextLine) {
        if (this.currentState == ListenerState.ALIVE_AND_LISTENING) {

            // Check the pool and determine if an event has been observed and if so, grab that event.
            NodeEvent observedEvent = getObservedEvent(nextLine);
            long timeOfObservation = System.nanoTime();

            // Marks all applicable events as "observed", clear the pool of them and notify the waiting threads.
            markRequestAsObservedAndCleanPool(observedEvent, timeOfObservation);
        }
    }

    @Override
    public void handle(Exception e) {
        // Reject all requests in the pool and clear the pool.
        killRequestPool(e.toString());

        // Shut down the tailer.
        this.tailer.stop();
    }

}
