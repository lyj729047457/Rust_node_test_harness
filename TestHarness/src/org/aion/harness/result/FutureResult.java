package org.aion.harness.result;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * A non-cancelling implementation of {@link Future}.
 *
 * This class provides two {@code get()} methods that will block until the result is available to
 * consume.
 */
public class FutureResult<V> implements Future {
    private CountDownLatch resultLatch = new CountDownLatch(1);
    private V result = null;

    /**
     * Finishes the future by supplying it with a result and releasing any thread blocked on this
     * result.
     *
     * Once a result exists this method does not modify it. A result can only be set once.
     *
     * This method should <b>never</b> be called by client code. This is for internal use only.
     *
     * @param result The result.
     */
    public void finish(V result) {
        if (resultLatch.getCount() > 0) {
            this.result = result;
            this.resultLatch.countDown();
        }
    }

    /**
     * This method does nothing. It always returns {@code false}.
     *
     * This future cannot be cancelled.
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    /**
     * This future cannot be cancelled. This method always returns {@code false}.
     */
    @Override
    public boolean isCancelled() {
        return false;
    }

    /**
     * Returns {@code true} only if the result is ready to be consumed.
     *
     * @return true if the future is finished.
     */
    @Override
    public boolean isDone() {
        return this.resultLatch.getCount() == 0;
    }

    /**
     * Blocks until the result is ready for consumption.
     *
     * This method itself imposes no timeout restrictions on the blocking. However, as is often the
     * case, the result being waited on does have a timeout and thus this method implicitly times
     * out in such a case.
     *
     * To strictly enforce a timeout, the other {@code get()} method should be used.
     *
     * @return the result.
     */
    @Override
    public V get() throws InterruptedException {
        this.resultLatch.await();
        return this.result;
    }

    /**
     * Blocks until the result is ready for consumption for until the timeout duration elapses,
     * whichever happens first.
     *
     * @param timeout The timeout duration.
     * @param unit The unit of measurement of the timeout quantity.
     * @return the result.
     */
    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException {
        this.resultLatch.await(timeout, unit);
        return this.result;
    }

    @Override
    public String toString() {
        if (isDone()) {
            return "FutureResult { task completed, result = " + this.result + " }";
        } else {
            return "FutureResult { task waiting to complete }";
        }
    }
}
