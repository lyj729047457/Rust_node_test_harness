package org.aion.harness.tests.contracts;

import static org.junit.Assert.assertTrue;

import org.aion.harness.result.RpcResult;

/** Holds static helpers for test assertions */
public class Assertions {

    public static void assertRpcSuccess(RpcResult result) {
        assertTrue(
            String.format("Expected RPC call to succeed, but got error: %s", result.getError()),
            result.isSuccess());
    }

}
