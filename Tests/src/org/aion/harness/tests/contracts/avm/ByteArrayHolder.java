package org.aion.harness.tests.contracts.avm;

import org.aion.avm.api.BlockchainRuntime;

public class ByteArrayHolder {
    private static byte[] data;

    static {
        BlockchainRuntime.println("HELLO IN CLINIT: " + BlockchainRuntime.getAddress());
        data = new byte[] { 0x1, 0x2 };
    }

    public static byte[] main() {
        BlockchainRuntime.println("HELLO IN MAIN: " + BlockchainRuntime.getAddress());
        byte[] result = data;
        data = BlockchainRuntime.getData();
        return result;
    }
}