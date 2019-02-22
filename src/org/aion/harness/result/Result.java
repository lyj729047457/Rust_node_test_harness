package org.aion.harness.result;

/**
 * A basic result that is either successful or unsuccessful.
 *
 * If unsuccessful, then this result will hold either a String with an error message or else it
 * will hold an exception.
 *
 * If successful, then both the error string and the exception are meaningless and possibly null.
 *
 * There is not concept of equality defined for a result.
 *
 * A result is partially immutable; the exception field's immutability is subject to the immutability
 * guarantees of that particular class.
 */
public final class Result {
    public final boolean success;
    public final String error;
    public final Exception exception;

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
    
    @Override
    public String toString() {
        if (this.success) {
            return "Result { successful }";
        } else if (this.exception != null) {
            return "Result { unsuccessful due to exception: " + this.exception.toString() + " }";
        } else {
            return "Result { unsuccessful due to: " + this.error + " }";
        }
    }

}
