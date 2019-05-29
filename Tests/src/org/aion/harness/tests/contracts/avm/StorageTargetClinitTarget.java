package org.aion.harness.tests.contracts.avm;

import avm.Blockchain;

public class StorageTargetClinitTarget {
    static {

        byte[] key = new byte[32];
        key[0] = 0x1;
        for(int i =0; i< 3; i++) {
            Blockchain.putStorage(key, new byte[0]);
            Blockchain.require(Blockchain.getStorage(key).length == 0);
            Blockchain.putStorage(key, new byte[]{0});
            Blockchain.require(Blockchain.getStorage(key).length == 1);
            Blockchain.putStorage(key, null);
            Blockchain.require(Blockchain.getStorage(key) == null);
        }
    }

    public static byte[] main(){
        return new byte[0];
    }
}
