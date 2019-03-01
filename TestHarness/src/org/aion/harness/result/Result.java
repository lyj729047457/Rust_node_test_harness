package org.aion.harness.result;

/**
 * A basic result that is either successful or unsuccessful.
 *
 * If unsuccessful, then this result will hold either a String with an error message or else it
 * will hold an exception.
 *
 * It is highly recommended that the result always be printed to the console, not only because it
 * will give valuable debug information but also because it will force a stack trace to be printed
 * if an exception was caught.
 *
 * If successful, then both the error string and the exception are meaningless and possibly null.
 *
 * It is highly recommended that the result always be printed to the console, not only because it
 * will give valuable debug information but also because it will force a stack trace to be printed
 * if an exception was caught.
 *
 * There is not concept of equality defined for a result.
 *
 * A result is partially immutable; the exception field's immutability is subject to the immutability
 * guarantees of that particular class.
 */
public final class Result {
    private final boolean success;
    private final String error;
    private final Exception exception;

    private Result(boolean success, String error, Exception exception) {
        this.success = success;
        this.error = error;
        this.exception = exception;
    }

    public static Result successful() {
        return new Result(true, null, null);
    }

    public static Result unsuccessfulDueTo(String error) {
        if (error == null) {
            throw new NullPointerException("Cannot create result with null error.");
        }

        return new Result(false, error, null);
    }

    public static Result unsuccessfulDueToException(Exception e) {
        if (e == null) {
            throw new NullPointerException("Cannot create result with null exception.");
        }

        return new Result(false, e.toString(), e);
    }

    /**
     * Returns {@code true} only if the event represented by this result was successful.
     *
     * @return whether or not the event was successful or not.
     */
    public boolean isSuccess() {
        return this.success;
    }

    /**
     * Returns the error if one exists.
     *
     * @return The error.
     */
    public String getError() {
        return this.error;
    }

    /**
     * Returns the exception that occurred while executing the event this result represents, if an
     * exception did occur.
     *
     * @return The exception.
     */
    public Exception getException() {
        return this.exception;
    }
    
    @Override
    public String toString() {
        if (this.success) {
            return "Result { successful }";
        } else if (this.exception != null) {
            this.exception.printStackTrace();
            return "Result { unsuccessful due to exception: " + this.exception.toString() + " }";
        } else {
            return "Result { unsuccessful due to: " + this.error + " }";
        }
    }

}
