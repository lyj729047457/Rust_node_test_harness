package org.aion.harness.contracts;

import org.aion.avm.api.ABIDecoder;
import org.aion.avm.api.BlockchainRuntime;

public class HelloWorld {

    public static void hello() {
        BlockchainRuntime.println("Hello World!");
    }

    public static byte[] main() {
        return ABIDecoder.decodeAndRunWithClass(HelloWorld.class, BlockchainRuntime.getData());
    }

}
