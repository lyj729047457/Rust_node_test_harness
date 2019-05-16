package org.aion.harness.tests.contracts.avm;

import avm.Blockchain;


public class AvmFailureModes {
    public static byte[] main() {
        // Check the input byte to determine what we want to test.
        byte input = Blockchain.getData()[0];
        switch (input) {
        case 0:
            // Success.
            break;
        case 1:
            // Fail - exception.
            ((Object)null).hashCode();
            break;
        case 2:
            // Fail - revert.
            Blockchain.revert();
            break;
        case 3:
            // Fail - out-of-energy.
            while (true) {
            }
        }
        return new byte[0];
    }
}
