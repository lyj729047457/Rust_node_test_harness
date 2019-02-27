package org.aion.harness.util;

import org.aion.harness.main.event.IEvent;
import org.aion.harness.misc.Assumptions;
import org.aion.harness.result.StatusResult;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;

import java.util.*;

/**
 * A listener that "tails" the output log of a node and processes every line in that log one by one
 * to determine if any threads have submitted a request for an event string to be observed and
 * whether this current line satisfies any of those requested events.
 *
 * A log listener maintains a pool of pending requests. Each request is an {@link IEvent} object,
 * and is therefore a conditional request for certain substrings to be witnessed in the log file.
 *
 * Each time this listener receives a new line in the log, it checks each of the requests and
 * attempts to satisfy their logic.
 *
 * Requests can be in 1 of 5 states: pending, satisfied, unobserved, expired, rejected.
 *
 * All requests enter the pool in the pending state. Once they move out of the pending state this
 * listener has the right to remove them from the pool.
 *
 * If a request is observed then it is marked satisfied.
 *
 * If the node shuts down then all pending requests in the pool will be marked unobserved.
 *
 * If a request times out it is marked expired.
 *
 * If the listener gets into a fatal state, if it is not currently listening to a log file, if it
 * stops listening to a log file, or if the requester receives an interrupt signal while the request
 * is in the pool, then it will be marked as rejected.
 *
 * This class is thread-safe.
 */
public final class LogListener implements TailerListener {
    private static final int CAPACITY = 10;

    // The tailer is responsible for reading each line and updating us. We are its "observer".
    private Tailer tailer;

    private enum ListenerState { ALIVE_AND_LISTENING, ALIVE_AND_NOT_LISTENING, DEAD }

    // We begin as alive but not listening to any log file.
    private ListenerState currentState = ListenerState.ALIVE_AND_NOT_LISTENING;

    private List<IEventRequest> requestPool = new ArrayList<>(CAPACITY);

    /**
     * Returns true only if the listener is not dead.
     */
    synchronized boolean isAlive() {
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
    public void submitEventRequest(IEventRequest eventRequest) {
        if (eventRequest == null) {
            throw new NullPointerException("Cannot submit a null event request.");
        }

        synchronized (this) {

            if (this.currentState != ListenerState.ALIVE_AND_LISTENING) {
                eventRequest.markAsRejected("Listener is not currently listening to a log file.");
            }

            // Attempt to add the request to the pool.
            addRequest(eventRequest);
        }

        // If the request is pending then it was added to the pool and hasn't been satisfied yet,
        // so we wait for the outcome.
        if (eventRequest.isPending()) {
            eventRequest.waitForOutcome();
        }
    }

    /**
     * Returns success only if the listener is currently not dead and not listening and has now
     * started listening.
     *
     * If the listener is currently dead or is already listening then an appropriate unsuccessful
     * result is returned.
     */
    synchronized StatusResult startListening() {
        if (this.currentState == ListenerState.DEAD) {
            return StatusResult.unsuccessful(Assumptions.PRODUCTION_ERROR_STATUS, "Listener is dead!");
        } else if (this.currentState == ListenerState.ALIVE_AND_LISTENING) {
            // awkward for this to be "unsuccessful"
            return StatusResult.unsuccessful(Assumptions.PRODUCTION_ERROR_STATUS, "Listener is already listening!");
        } else {
            this.currentState = ListenerState.ALIVE_AND_LISTENING;
            return StatusResult.successful();
        }
    }

    /**
     * If the listener is listening when this method is invoked, then it will be moved into the
     * not listening state, all requests currently in the pool will be marked unobserved and all
     * waiting threads will be notified, and the request pool will be cleared.
     *
     * Otherwise, if the listener is not listening when this method is invoked, nothing happens.
     */
    synchronized void stopListening() {
        if (this.currentState == ListenerState.ALIVE_AND_LISTENING) {
            this.currentState = ListenerState.ALIVE_AND_NOT_LISTENING;

            for (IEventRequest request : this.requestPool) {
                request.markAsUnobserved();
                request.notifyRequestIsResolved();
            }
            this.requestPool.clear();

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
    private synchronized void addRequest(IEventRequest request) {
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
     * Receives the incoming next line in the log file and processed it.
     *
     * If any events are satisfied by this line (or were previously satisfied) then they are removed
     * from the request pool and their owners are notified.
     *
     * The incoming string is only handled if this listener is alive and is listening.
     *
     * @param nextLine The next line in the log file.
     */
    @Override
    public synchronized void handle(String nextLine) {
        if (this.currentState == ListenerState.ALIVE_AND_LISTENING) {

            long currentTimeInMillis = System.currentTimeMillis();

            // Iterate over each of the requests in the pool.
            Iterator<IEventRequest> requestIterator = this.requestPool.iterator();

            while (requestIterator.hasNext()) {
                IEventRequest request = requestIterator.next();

                if (!request.isPending()) {
                    requestIterator.remove();
                    request.notifyRequestIsResolved();
                } else if (request.isSatisfiedBy(nextLine, currentTimeInMillis)) {
                    request.notifyRequestIsResolved();
                    requestIterator.remove();
                }
            }
        }
    }

    /**
     * Called by the {@link Tailer} when it is first initialized with this listener. This is here
     * so that we can grab hold of this reference and shut it down if we panic.
     *
     * The {@link Tailer} is the class responsible for reading the log file and for invoking our
     * {@code handle()} method (or any other exceptional method) with the next line it reads in the
     * file.
     *
     * @param tailer The class that is currently "tailing" the log file and alerting us.
     */
    @Override
    public void init(Tailer tailer) {
        if (tailer == null) {
            throw new NullPointerException("Cannot initialize with a null tailer.");
        }

        this.tailer = tailer;
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

    /**
     * Moves this listener to the dead state, rejects all events in the request pool, notifies all
     * requesting threads that their events are now satisfied, clears the pool, and stops the tailer
     * from "tailing" the log file.
     *
     * @param cause The reason for the fatal panic.
     */
    private void panic(String cause) {
        killRequestPool(cause);
        this.tailer.stop();
    }

    /**
     * Rejects every request currently in the pool with the specified cause of the panic, then clears
     * the pool and notifies all waiting threads.
     *
     * @param causeOfPanic The reason for why the request pool is being killed.
     */
    private synchronized void killRequestPool(String causeOfPanic) {
        this.currentState = ListenerState.DEAD;

        for (IEventRequest request : this.requestPool) {
            request.markAsRejected(causeOfPanic);
            request.notifyRequestIsResolved();
        }
        this.requestPool.clear();
    }

}