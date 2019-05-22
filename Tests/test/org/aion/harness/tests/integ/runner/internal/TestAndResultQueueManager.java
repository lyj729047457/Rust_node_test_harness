package org.aion.harness.tests.integ.runner.internal;

import java.util.concurrent.atomic.AtomicInteger;

public final class TestAndResultQueueManager {
    private final SimpleBlockingQueue<TestContext> testsQueue = new SimpleBlockingQueue<>();
    private final SimpleBlockingQueue<TestResult> resultsQueue = new SimpleBlockingQueue<>();
    private AtomicInteger numThreadsOutstanding;

    public TestAndResultQueueManager(int numThreadsOutstanding) {
        this.numThreadsOutstanding = new AtomicInteger(numThreadsOutstanding);
    }

    /**
     * Puts a new test for the test executor threads to run.
     *
     * @param testContext A new test to run.
     */
    public void putTest(TestContext testContext) {
        this.testsQueue.put(testContext);
    }

    /**
     * Takes the next test to run, blocking if no next test is present.
     *
     * Returns null if there are no tests left to run and it is safe to shut down.
     *
     * @return The next test or null.
     */
    public TestContext takeTest() {
        return this.testsQueue.take();
    }

    /**
     * Signals that no more tests will be added. The {@code takeTest()} method will return null once
     * any remaining tests have been consumed.
     */
    public void reportAllTestsSubmitted() {
        this.testsQueue.close();
    }

    /**
     * Puts a new test result for the runner to consume and notfify JUnit accordingly.
     *
     * @param result A new result.
     */
    public void putResult(TestResult result) {
        this.resultsQueue.put(result);
    }

    /**
     * Takes the next result, blocking if no next result is present.
     *
     * Returns null if there are no more results left to take and it is safe to shut down.
     *
     * @return The next result or null.
     */
    public TestResult takeResult() {
        return this.resultsQueue.take();
    }

    /**
     * Signals that another thread has finished executing all of its tests. This method should be
     * called once per thread.
     *
     * When the last thread calls this method, then the {@code takeResult()} method will return null
     * once any remaining results have been consumed.
     */
    public void reportThreadIsFinished() {
        int currentValue = this.numThreadsOutstanding.decrementAndGet();
        if (currentValue == 0) {
            this.resultsQueue.close();
        }
    }
}
