package org.aion.harness.main.tools;

/**
 * A class that holds the payload for an RPC call. The payload for an RPC call is simply the data
 * to that call (following the --data option).
 *
 * The preferred way of constructing an instance of this class is to the use the
 * {@link RpcPayloadBuilder}.
 *
 * An rpc payload is immutable.
 */
public final class RpcPayload {
    public final String payload;

    public RpcPayload(RpcMethod method, String params) {
        if (method == null) {
            throw new NullPointerException("Cannot construct rpc payload with null method.");
        }
        if (params == null) {
            throw new NullPointerException("Cannot construct rpc payload with null params.");
        }

        this.payload = "{\"jsonrpc\":\"2.0\",\"method\":\"" + method.getMethod() + "\",\"params\":[\"0x" + params + "\", \"latest\"" + "],\"id\":1}";
    }
}
