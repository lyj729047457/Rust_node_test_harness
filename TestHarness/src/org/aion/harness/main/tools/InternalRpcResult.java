package org.aion.harness.main.tools;

import java.util.concurrent.TimeUnit;
import org.aion.harness.result.RpcResult;

/**
 * An internal rpc result.
 *
 * This class is never to be exposed to the client.
 *
 * An internal rpc result still contains the particular output of the RPC call, and the purpose of
 * the {@link RpcResult} class (which is public-facing) is to hide this output entirely so that the
 * user is not dependent on its implementation details.
 *
 * The class calling into this class is responsible for dealing with the raw output appropriately
 * and transforming it into a high-level concept before it hits the end user.
 */
public final class InternalRpcResult {
    public final boolean success;
    public final String output;
    public final String error;
    private final long timeOfCallInNanos;

    private InternalRpcResult(boolean success, String output, String error, long timeOfCall, TimeUnit unit) {
        this.success = success;
        this.output = output;
        this.error = error;
        this.timeOfCallInNanos = (timeOfCall < 0) ? timeOfCall : unit.toNanos(timeOfCall);
    }

    public static InternalRpcResult successful(String output, long timeOfCall, TimeUnit unit) {
        if (output == null) {
            throw new NullPointerException("Cannot construct successful internal rpc result with null output.");
        }
        if (timeOfCall < 0) {
            throw new IllegalArgumentException("Cannot construct successful internal rpc result with negative time.");
        }
        if (unit == null) {
            throw new NullPointerException("Cannot construct successful internal rpc result with null time units.");
        }

        return new InternalRpcResult(true, output, null, timeOfCall, unit);
    }

    public static InternalRpcResult unsuccessful(String error) {
        if (error == null) {
            throw new NullPointerException("Cannot construct unsuccessful internal rpc result with null error.");
        }

        return new InternalRpcResult(false, null, error, -1, null);
    }

    public long getTimeOfCall(TimeUnit unit) {
        return (this.timeOfCallInNanos < 0)
            ? this.timeOfCallInNanos
            : unit.convert(timeOfCallInNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public String toString() {
        if (this.success) {
            return "InternalRpcResult { successful | output = " + this.output
                + " | time of call = " + this.timeOfCallInNanos + " (nanos) }";
        } else {
            return "InternalRpcResult { unsuccessful due to: " + this.error + " }";
        }
    }

}
