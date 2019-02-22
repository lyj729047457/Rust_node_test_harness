package org.aion.harness.result;

import org.aion.harness.main.tools.RpcOutputParser;

/**
 * A result from an RPC call.
 *
 * An rpc result is either successful or not.
 *
 * If the rpc result is unsuccessful then {@code output} and {@code timeOfCallInMillis} are both
 * meaningless, but {@code error} will be meaningful.
 *
 * If the rpc result is successful then {@code timeOfCallInMillis} represents the time at which the
 * RPC call was made, in milliseconds.
 *
 * The preferred way of extracing meaningful information from the {@code output} field is using the
 * {@link RpcOutputParser} class.
 *
 * There is not concept of equality defined for an rpc result.
 *
 * An rpc result is immutable.
 */
public final class RpcResult {
    public final boolean success;
    public final String output;
    public final String error;
    public final long timeOfCallInMillis;

    private RpcResult(boolean success, String output, String error, long time) {
        if (output == null) {
            throw new NullPointerException("Cannot construct rpc result with null output.");
        }
        if (error == null) {
            throw new NullPointerException("Cannot construct rpc result with null error.");
        }

        this.success = success;
        this.output = output;
        this.error = error;
        this.timeOfCallInMillis = time;
    }

    /**
     * Constructs a successful rpc result, where output is the output returned by the rpc call and
     * timeOfCallInMillis is the time at which the call was made, in milliseconds.
     *
     * @param output The output of the RPC call.
     * @param timeOfCallInMillis The time of the RPC call.
     * @return a successful rpc result.
     */
    public static RpcResult successful(String output, long timeOfCallInMillis) {
        if (timeOfCallInMillis < 0) {
            throw new IllegalArgumentException("cannot construct successful rpc result with negative-valued timestamp.");
        }
        return new RpcResult(true, output, "", timeOfCallInMillis);
    }

    /**
     * Constrcuts an unsuccessful rpc result, where error is a string descriptive of the error that
     * occurred.
     *
     * @param error The cause of the unsuccessful call.
     * @return an unsuccessful rpc result.
     */
    public static RpcResult unsuccessful(String error) {
        return new RpcResult(false, "", error, -1);
    }

    @Override
    public String toString() {
        if (this.success) {
            return "RpcResult { successful | timestamp: " + this.timeOfCallInMillis + " (millis) | output: " + this.output + " }";
        } else {
            return "RpcResult { unsuccessful due to: " + this.error + " }";
        }
    }
}
