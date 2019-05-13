package org.aion.harness.tests.contracts.avm;

import avm.Blockchain;
import java.math.BigInteger;
import org.aion.avm.userlib.abi.ABIDecoder;

public class AvmCrossCallDispatcher {

    public static byte[] main() {
        ABIDecoder decoder = new ABIDecoder(Blockchain.getData());

        try {
            Blockchain.call(decoder.decodeOneAddress(), BigInteger.ZERO, decoder.decodeOneByteArray(), Blockchain.getBlockEnergyLimit());
        } catch (Exception e) {
            // We should catch this exception and return!
            Blockchain.println("Caught exception: " + e.toString());
            return null;
        }

        // We should never get here!
        Blockchain.require(false);
        return null;
    }

}
