package org.aion.harness.main.tools;

/**
 * A class that generates the payload to an RPC call. The payload for an RPC call is simply the data
 * to that call (following the --data option).
 */
public final class RpcPayload {
    private static final String PAYLOAD_START = "{\"jsonrpc\":\"2.0\",\"method\":\"";
    private static final String PARAMS = "\",\"params\":[";
    private static final String PAYLOAD_END = "],\"id\":1}";

    public static String generatePayload(RpcMethod method, String params) {
        if (method == null) {
            throw new NullPointerException("Cannot generate rpc payload with null method.");
        }
        if (params == null) {
            throw new NullPointerException("Cannot generate rpc payload with null params.");
        }
        return PAYLOAD_START + method.getMethod() + PARAMS + params + PAYLOAD_END;
    }
}
