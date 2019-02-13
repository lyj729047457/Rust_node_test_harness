package org.aion.harness.result;

/**
 * A simple result specifying successful and error messaging.
 *
 * If {@code success == true} then the values of {@code status} and {@code error} are meaningless.
 *
 * It is guaranteed that {@code error != null}.
 *
 * A result is immutable.
 */
public final class Result {
    public final boolean success;
    public final int status;
    public final String error;

    private Result(boolean success, int status, String error) {
        this.success = success;
        this.status = status;
        this.error = (error == null) ? "" : error;
    }

    public static Result successful() {
        return new Result(true, 0, "");
    }

    public static Result unsuccessful(int status, String error) {
        return new Result(false, status, error);
    }

    @Override
    public String toString() {
        return "Result { success = " + this.success + ", status = " + this.status + ", error = " + this.error + " }";
    }

}
