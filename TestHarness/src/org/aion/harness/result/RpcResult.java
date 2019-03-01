package org.aion.harness.result;

import java.util.concurrent.TimeUnit;

/**
 * A result from an RPC call.
 *
 * An rpc result is either successful or not.
 *
 * If the rpc result is unsuccessful then {@code getResult()} and {@code timeOfCall} are
 * meaningless, but {@code error} will be meaningful.
 *
 * If the rpc result is successful then {@code timeOfCall} represents the time at which the
 * RPC call was made, and {@code getResult()} will return the particular result of the call.
 *
 * There is not concept of equality defined for an rpc result.
 *
 * An rpc result is immutable, except for the generic object returned by {@code getResult()}, whose
 * immutability is of course dependent upon whatever type this ends up being.
 */
public final class RpcResult<T> {
    private final boolean success;
    private final String error;
    private final long timeOfCallInNanos;

    private final T result;

    private RpcResult(boolean success, T result, String error, long time, TimeUnit unit) {
        if (error == null) {
            throw new NullPointerException("Cannot construct rpc result with null error.");
        }

        this.success = success;
        this.error = error;
        this.timeOfCallInNanos = (time < 0) ? time : unit.toNanos(time);
        this.result = result;
    }

    /**
     * Constructs a successful rpc result, where output is the output returned by the rpc call and
     * timeOfCall is the time at which the call was made.
     *
     * @param result The result of the RPC call.
     * @param timeOfCall The time of the RPC call.
     * @param unit The unit of time of the timeOfCall quantity.
     * @return a successful rpc result.
     */
    public static <T> RpcResult<T> successful(T result, long timeOfCall, TimeUnit unit) {
        if (result == null) {
            throw new NullPointerException("Cannot construct rpc result with null result.");
        }
        if (timeOfCall < 0) {
            throw new IllegalArgumentException("cannot construct successful rpc result with negative-valued timestamp.");
        }
        if (unit == null) {
            throw new NullPointerException("Cannot construct rpc result with null time unit.");
        }
        return new RpcResult<>(true, result, "", timeOfCall, unit);
    }

    /**
     * Constrcuts an unsuccessful rpc result, where error is a string descriptive of the error that
     * occurred.
     *
     * @param error The cause of the unsuccessful call.
     * @return an unsuccessful rpc result.
     */
    public static <T> RpcResult<T> unsuccessful(String error) {
        return new RpcResult<>(false, null, error, -1, null);
    }

    /**
     * Returns the result of the RPC call.
     *
     * @return the result of the call.
     */
    public T getResult() {
        return this.result;
    }

    /**
     * Returns the time at which the RPC call was issued in the desired time units.
     *
     * @param unit The time units of the returned result.
     * @return the time of the RPC call.
     */
    public long getTimeOfCall(TimeUnit unit) {
        return (this.timeOfCallInNanos < 0)
            ? this.timeOfCallInNanos
            : unit.convert(this.timeOfCallInNanos, TimeUnit.NANOSECONDS);
    }

    /**
     * Returns {@code true} only if the RPC call this result represents was successful.
     *
     * @return whether or not the call was a success.
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
            return "RpcResult { successful | timestamp: " + this.timeOfCallInNanos + " (nanos) | output: " + this.result + " }";
        } else {
            return "RpcResult { unsuccessful due to: " + this.error + " }";
        }
    }
}
