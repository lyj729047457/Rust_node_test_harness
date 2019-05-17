package org.aion.harness.tests.integ.runner.exception;

/**
 * Thrown when something unexpected occurs as the custom test runner is running tests.
 */
public final class UnexpectedTestRunnerException extends RuntimeException {

    public UnexpectedTestRunnerException(String message) {
        super(message);
    }

    public UnexpectedTestRunnerException(String message, Throwable cause) {
        super(message, cause);
    }
}
