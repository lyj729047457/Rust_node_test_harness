package org.aion.harness.tests.contracts.avm;

import avm.Blockchain;

public class ByteArrayHolder {
    private static byte[] data;

    static {
        Blockchain.println("HELLO IN CLINIT: " + Blockchain.getAddress());
        data = new byte[] { 0x1, 0x2 };
    }

    public static byte[] main() {
        Blockchain.println("HELLO IN MAIN: " + Blockchain.getAddress());
        byte[] result = data;
        data = Blockchain.getData();
        return result;
    }
}

