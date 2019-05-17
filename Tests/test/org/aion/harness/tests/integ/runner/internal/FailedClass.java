package org.aion.harness.tests.integ.runner.internal;

/**
 * A simple data structure used by the {@link TestExecutor} that allows it to capture a class-wide
 * failure.
 *
 * A class-wide failure typically only occurs in a @BeforeClass method.
 */
public final class FailedClass {
    public final Class<?> failedClass;
    public final Throwable failure;

    public FailedClass(Class<?> failedClass, Throwable failure) {
        this.failedClass = failedClass;
        this.failure = failure;
    }
}
