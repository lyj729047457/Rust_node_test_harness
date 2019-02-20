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
public final class StatusResult {
    public final boolean success;
    public final int status;
    public final String error;

    private StatusResult(boolean success, int status, String error) {
        this.success = success;
        this.status = status;
        this.error = (error == null) ? "" : error;
    }

    public static StatusResult successful() {
        return new StatusResult(true, 0, "");
    }

    public static StatusResult unsuccessful(int status, String error) {
        return new StatusResult(false, status, error);
    }

    @Override
    public String toString() {
        return "StatusResult { success = " + this.success + ", status = " + this.status + ", error = " + this.error + " }";
    }

}
