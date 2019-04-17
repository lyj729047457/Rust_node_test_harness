package org.aion.harness.tests.contracts.avm;

import avm.Blockchain;

public class SimpleContract {

    public static byte[] main() {
        Blockchain.println("I'm a pretty dull contract.");
        return null;
    }

}
