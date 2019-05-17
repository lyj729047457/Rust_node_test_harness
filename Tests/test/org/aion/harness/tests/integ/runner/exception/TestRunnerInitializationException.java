package org.aion.harness.tests.integ.runner.exception;

/**
 * Thrown to indicate that some essential part of the setup process that our custom Runner classes
 * are performing went wrong.
 */
public final class TestRunnerInitializationException extends RuntimeException {

    public TestRunnerInitializationException(String message) {
        super(message);
    }

    public TestRunnerInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
