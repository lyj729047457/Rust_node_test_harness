package org.aion.harness.result;

/**
 * A basic result that is either successful or unsuccessful.
 *
 * If unsuccessful, then this result will hold a String with an error message.
 *
 * If successful, then the error string is meaningless and possibly null.
 *
 * There is not a concept of equality defined for a result.
 *
 * A result is immutable.
 */
public final class Result {
    private final boolean success;
    private final String error;

    private Result(boolean success, String error) {
        this.success = success;
        this.error = error;
    }

    public static Result successful() {
        return new Result(true, null);
    }

    public static Result unsuccessfulDueTo(String error) {
        if (error == null) {
            throw new NullPointerException("Cannot create result with null error.");
        }

        return new Result(false, error);
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
    
    @Override
    public String toString() {
        if (this.success) {
            return "Result { successful }";
        } else {
            return "Result { unsuccessful due to: " + this.error + " }";
        }
    }

}
