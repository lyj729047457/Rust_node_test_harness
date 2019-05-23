package org.aion.harness.tests.integ.runner.internal;

import org.junit.runner.Description;

public final class TestResult {
    public final Description description;
    public final boolean ignored;
    public final boolean success;
    public final Throwable error;

    // The stdout and stderr streams, as strings, corresponding to the test whose result this is.
    public byte[] stdout;
    public byte[] stderr;

    private TestResult(Description description, boolean ignored, boolean success, Throwable error) {
        this.description = description;
        this.ignored = ignored;
        this.success = success;
        this.error = error;
    }

    public static TestResult ignored(Description description) {
        return new TestResult(description,true, true, null);
    }

    public static TestResult successful(Description description) {
        return new TestResult(description, false,true, null);
    }

    public static TestResult failed(Description description, Throwable error) {
        return new TestResult(description, false,false, error);
    }
}
