package org.aion.harness.tests.integ.runner.internal;

import java.util.concurrent.CountDownLatch;
import org.junit.runner.Description;

/**
 * A future execution result of running a test.
 *
 * The test description is guaranteed to be ready for consumption immediately (no blocking required)
 * as well as the {@code ignored} field. Both of these are computed in the constructor since this is
 * information that the caller should not have to block on to receive -- nothing needs to actually
 * be executed to determine these fields!
 *
 * The actual {@code wasSuccessful()} and {@code getError()} methods are blocking and these methods
 * should never be called until {@code resultIsFinished()} returns true.
 * Note that {@code waitUntilFinished()} will block and when it returns it is guaranteed that
 * {@code resultIsFinished()} will be true.
 */
public final class FutureExecutionResult {
    public final Description testDescription;
    public final boolean ignored;

    private CountDownLatch finished = new CountDownLatch(1);
    private boolean success;
    private Throwable error;

    public FutureExecutionResult(Description testDescription, boolean ignored) {
        this.testDescription = testDescription;
        this.ignored = ignored;

        // If the test was ignored then it is finished being processed.
        if (this.ignored) {
            this.success = false;
            this.error = null;
            this.finished.countDown();
        }
    }

    /**
     * This should only be invoked by the {@link TestExecutor}! Not the consumer.
     */
    public void markAsSuccess() {
        this.success = true;
        this.error = null;
        this.finished.countDown();
    }

    /**
     * This should only be invoked by the {@link TestExecutor}! Not the consumer.
     */
    public void markAsFailed(Throwable error) {
        this.success = false;
        this.error = error;
        this.finished.countDown();
    }

    /**
     * Returns true only if this result is finished and ready to be consumed.
     */
    public boolean resultIsFinished() {
        return this.finished.getCount() == 0;
    }

    /**
     * Blocks until this result is populated with its finished state.
     */
    public void waitUntilFinished() throws InterruptedException {
        this.finished.await();
    }

    /**
     * The method {@code waitUntilFinished()} must return {@code true} before this method can be
     * called! If the result is not yet ready to consume an exception will be thrown.
     *
     * @return Whether the test was successful or not.
     */
    public boolean wasSuccessful() {
        if (this.finished.getCount() > 0) {
            throw new IllegalStateException("Attempted to read the future result before it was finished!");
        } else {
            return this.success;
        }
    }

    /**
     * The method {@code waitUntilFinished()} must return {@code true} before this method can be
     * called! If the result is not yet ready to consume an exception will be thrown.
     *
     * @return The error that occurred, if one did, during the run of the test.
     */
    public Throwable getTestError() {
        if (this.finished.getCount() > 0) {
            throw new IllegalStateException("Attempted to read the future result before it was finished!");
        } else {
            return this.error;
        }
    }
}
