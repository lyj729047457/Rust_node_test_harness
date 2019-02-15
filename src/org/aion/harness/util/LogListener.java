package org.aion.harness.util;

import java.util.concurrent.atomic.AtomicBoolean;
import org.aion.harness.misc.Assumptions;
import org.aion.harness.result.EventRequestResult;
import org.aion.harness.result.Result;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;

import java.util.*;

public final class LogListener implements TailerListener {
    private static final int CAPACITY = 10;

    private AtomicBoolean isListening = new AtomicBoolean(false);

    private List<EventRequest> requestPool = new ArrayList<>(CAPACITY);

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
    public EventRequestResult submitEventRequest(EventRequest eventRequest, long timeoutInMillis) {
        if (eventRequest == null) {
            throw new NullPointerException("Cannot submit a null event request.");
        }

        if (!this.isListening.get()) {
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
     * Attempts to turn the listener on so that it listens to the output log.
     *
     * If the listening is already on then this method does nothing.
     *
     * Returns a successful result only if the listener was off at the time of this class and was
     * turned on, otherwise an unsuccessful result is returned instead.
     *
     * <b>This method should only be called by the {@link LogReader} class. And in particular,
     * only by the instance of {@link LogReader} that created this listener object.</b>
     *
     * @return Whether or not the listener went from off to on.
     */
    Result startListening() {
        boolean success = !this.isListening.compareAndExchange(false, true);

        return (success)
            ? Result.successful()
            : Result.unsuccessful(Assumptions.PRODUCTION_ERROR_STATUS, "Listener is already listening.");
    }

    /**
     * Turns the listener off so that it no longer listens to the output log.
     *
     * After calling this method the listening will be turned off.
     *
     * <b>This method should only be called by the {@link LogReader} class. And in particular,
     * only by the instance of {@link LogReader} that created this listener object.</b>
     */
    void stopListening() {
        this.isListening.set(false);
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

    @Override
    public void init(Tailer tailer) {
        //TODO
    }

    @Override
    public void fileNotFound() {
        System.err.println("FILE NOT FOUND");
    }

    @Override
    public void fileRotated() {
        System.err.println("FILE ROTATED!");
    }

    @Override
    public void handle(String nextLine) {
        // If we are not listening to the log then ignore this line.
        if (!this.isListening.get()) {
            return;
        }

        // Check the pool and determine if an event has been observed and if so, grab that event.
        NodeEvent observedEvent = getObservedEvent(nextLine);
        long timeOfObservation = System.nanoTime();

        // Marks all applicable events as "observed", clear the pool of them and notify the waiting threads.
        markRequestAsObservedAndCleanPool(observedEvent, timeOfObservation);
    }

    @Override
    public void handle(Exception e) {
        e.printStackTrace();
    }

}
