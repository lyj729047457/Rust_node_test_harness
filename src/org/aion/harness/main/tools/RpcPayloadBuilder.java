package org.aion.harness.main.tools;

/**
 * A class for constructing new instances of {@link RpcPayload}.
 *
 * Subsequent calls to {@code build()} will reuse the details that were originally set.
 *
 * If a method is invoked multiple times before a call to {@code build()} then its latest
 * invocation takes precedence.
 *
 * An rpc payload builder is not thread-safe.
 */
public final class RpcPayloadBuilder {
    private RpcMethod method;
    private String params;

    public RpcPayloadBuilder method(RpcMethod method) {
        this.method = method;
        return this;
    }

    public RpcPayloadBuilder params(String params) {
        this.params = params;
        return this;
    }

    public RpcPayload build() {
        return new RpcPayload(this.method, this.params);
    }
}
