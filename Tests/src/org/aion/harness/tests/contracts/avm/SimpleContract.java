package org.aion.harness.tests.contracts.avm;

import org.aion.avm.api.BlockchainRuntime;

public class SimpleContract {

    public static byte[] main() {
        BlockchainRuntime.println("I'm a pretty dull contract.");
        return null;
    }

}
