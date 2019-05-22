package org.aion.harness.tests.integ.runner.internal;

import java.util.LinkedList;
import java.util.List;
import org.aion.harness.tests.integ.runner.exception.UnexpectedTestRunnerException;

public final class SimpleBlockingQueue<T> {
    private final List<T> queue = new LinkedList<>();
    private boolean closed = false;

    /**
     * Puts the specified object into the queue.
     */
    public synchronized void put(T object) {
        if (this.closed) {
            throw new IllegalStateException("Attempted to put an object into a closed queue!");
        }
        this.queue.add(object);
        this.notify();
    }

    /**
     * Returns the next object from the queue, blocking if the queue is currently empty.
     *
     * Returns null if this queue is closed and no more items will be placed in it. (ie. the producer
     * is done).
     */
    public synchronized T take() {
        try {

            while (!this.closed && this.queue.isEmpty()) {
                this.wait();
            }
            return (this.queue.isEmpty()) ? null : this.queue.remove(0);

        } catch (InterruptedException e) {
            throw new UnexpectedTestRunnerException("Thread was interrupted!");
        }
    }

    /**
     * Signifies that the queue is closed. This will take effect once all remaining objects in the
     * queue have been cleared out.
     */
    public synchronized void close() {
        this.closed = true;
        this.notifyAll();
    }
}
