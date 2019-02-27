package org.aion.harness.result;

/**
 * A simple status result specifying whether something was successful or not, and if not, displaying
 * a status code and an error message.
 *
 * Typically, a status result is returned as the result of running an external process, which
 * returns status/exit codes.
 *
 * If {@code success == true} then the values of {@code status} and {@code error} are meaningless.
 *
 * It is guaranteed that {@code error != null}.
 *
 * There is not concept of equality defined for a status result.
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
        if (this.success) {
            return "StatusResult { successful }";
        } else {
            return "StatusResult { unsuccessful | status code: " + this.success + " | error: " + this.error + " }";
        }
    }

}
