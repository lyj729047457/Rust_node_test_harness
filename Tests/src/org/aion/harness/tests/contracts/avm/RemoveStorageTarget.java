package org.aion.harness.tests.contracts.avm;

import avm.Blockchain;
import java.math.BigInteger;
import org.aion.avm.userlib.abi.ABIDecoder;

public class RemoveStorageTarget {
    private static byte[] K;
    private static byte[] V;

    static {
        K = new byte[32];
        K[0] = 0xa;
        V = new byte[32];
        V[0] = 0x7;
    }

    public static byte[] main() {
        ABIDecoder decoder = new ABIDecoder(Blockchain.getData());
        String method = decoder.decodeMethodName();

        if (method.equals("putStorageLengthZero")) {
            putStorageLengthZero();
        } else if (method.equals("putStorageLengthOne")) {
            putStorageLengthOne();
        } else if (method.equals("resetStorage")) {
            resetStorage();
        } else if (method.equals("putResetPut")) {
            putResetPut();
        } else if (method.equals("verifyAllStorageRemoved")) {
            verifyAllStorageRemoved();
        } else if (method.equals("validateStoragePreviousTxLength")) {
            validateStoragePreviousTxLength(decoder.decodeOneInteger());
        } else if (method.equals("putStorageAddress")) {
            putStorageAddress();
        } else if (method.equals("setStorageSameKey")) {
            setStorageSameKey(decoder.decodeOneByteArray());
        } else if (method.equals("getStorageOneKey")) {
            getStorageOneKey(decoder.decodeOneInteger());
        } else if (method.equals("reentrantCallAfterPut")) {
            reentrantCallAfterPut(decoder.decodeOneByteArray());
        } else if (method.equals("putZeroResetVerify")) {
            putZeroResetVerify();
        } else if (method.equals("putOneResetVerify")) {
            putOneResetVerify();
        } else if (method.equals("putAddressResetVerify")) {
            putAddressResetVerify();
        } else if (method.equals("ResetResetVerify")) {
            ResetResetVerify();
        } else if (method.equals("putStatic")) {
            putStatic();
        } else if (method.equals("resetStatic")) {
            resetStatic();
        } else if (method.equals("verifyStatic")) {
            verifyStatic();
        }
        return null;
    }

    public static void putZeroResetVerify() {
        putStorageLengthZero();
        resetStorage();
        verifyAllStorageRemoved();
    }

    public static void putOneResetVerify() {
        putStorageLengthOne();
        resetStorage();
        verifyAllStorageRemoved();
    }

    public static void putAddressResetVerify() {
        putStorageAddress();
        resetStorage();
        verifyAllStorageRemoved();
    }

    public static void ResetResetVerify() {
        resetStorage();
        resetStorage();
        verifyAllStorageRemoved();
    }

    public static void putStorageLengthZero() {
        byte[] key = new byte[32];
        key[0] = 0x1;
        Blockchain.putStorage(key, new byte[0]);
        key[0] = 0x2;
        Blockchain.putStorage(key, new byte[0]);
        key[0] = 0x3;
        Blockchain.putStorage(key, new byte[0]);
        key[0] = 0x4;
        Blockchain.putStorage(key, new byte[0]);
        key[0] = 0x5;
        Blockchain.putStorage(key, new byte[0]);
    }

    public static void putStorageLengthOne() {
        byte[] key = new byte[32];
        key[0] = 0x1;
        Blockchain.putStorage(key, new byte[]{(byte)0});
        key[0] = 0x2;
        Blockchain.putStorage(key, new byte[]{(byte)0});
        key[0] = 0x3;
        Blockchain.putStorage(key, new byte[]{(byte)0});
        key[0] = 0x4;
        Blockchain.putStorage(key, new byte[]{(byte)0});
        key[0] = 0x5;
        Blockchain.putStorage(key, new byte[]{(byte)0});
    }

    public static void validateStoragePreviousTxLength(int length) {
        byte[] key = new byte[32];
        key[0] = 0x1;
        Blockchain.require(Blockchain.getStorage(key).length == length);
        key[0] = 0x2;
        Blockchain.require(Blockchain.getStorage(key).length == length);
        key[0] = 0x3;
        Blockchain.require(Blockchain.getStorage(key).length == length);
        key[0] = 0x4;
        Blockchain.require(Blockchain.getStorage(key).length == length);
        key[0] = 0x5;
        Blockchain.require(Blockchain.getStorage(key).length == length);
    }

    public static void resetStorage() {
        byte[] key = new byte[32];
        key[0] = 0x1;
        Blockchain.putStorage(key, null);
        key[0] = 0x2;
        Blockchain.putStorage(key, null);
        key[0] = 0x3;
        Blockchain.putStorage(key, null);
        key[0] = 0x4;
        Blockchain.putStorage(key, null);
        key[0] = 0x5;
        Blockchain.putStorage(key, null);
    }

    public static void verifyAllStorageRemoved() {
        byte[] key = new byte[32];
        key[0] = 0x1;
        Blockchain.require(Blockchain.getStorage(key) == null);
        key[0] = 0x2;
        Blockchain.require(Blockchain.getStorage(key) == null);
        key[0] = 0x3;
        Blockchain.require(Blockchain.getStorage(key) == null);
        key[0] = 0x4;
        Blockchain.require(Blockchain.getStorage(key) == null);
        key[0] = 0x5;
        Blockchain.require(Blockchain.getStorage(key) == null);
    }

    public static void putResetPut() {
        byte[] key = new byte[32];
        key[0] = 0x1;
        Blockchain.putStorage(key, new byte[0]);
        Blockchain.require(Blockchain.getStorage(key).length == 0);
        Blockchain.putStorage(key, null);
        Blockchain.require(Blockchain.getStorage(key) == null);
        Blockchain.putStorage(key, new byte[0]);
        Blockchain.require(Blockchain.getStorage(key).length == 0);
    }

    public static void putStorageAddress() {
        byte[] key = new byte[32];
        key[0] = 0x1;
        Blockchain.putStorage(key, Blockchain.getCaller().toByteArray());
        key[0] = 0x2;
        Blockchain.putStorage(key, Blockchain.getOrigin().toByteArray());
        key[0] = 0x3;
        Blockchain.putStorage(key, Blockchain.getCaller().toByteArray());
        key[0] = 0x4;
        Blockchain.putStorage(key, Blockchain.getOrigin().toByteArray());
        key[0] = 0x5;
        Blockchain.putStorage(key, Blockchain.getCaller().toByteArray());
    }

    public static void setStorageSameKey(byte[] value) {
        for (int i = 0; i < 5; i++) {
            byte[] key = new byte[32];
            key[0] = 0x1;
            Blockchain.putStorage(key, value);
        }
    }

    public static void getStorageOneKey(int length) {
        byte[] key = new byte[32];
        key[0] = 0x1;
        if(length < 0){
            Blockchain.require(Blockchain.getStorage(key) == null);
        } else {
            Blockchain.require(Blockchain.getStorage(key).length == length);
        }
    }

    public static void reentrantCallAfterPut(byte[] data) {
        putStorageLengthZero();
        Blockchain.require(Blockchain.call(Blockchain.getAddress(), BigInteger.ZERO, data, Blockchain.getRemainingEnergy()).isSuccess());
        verifyAllStorageRemoved();
    }

    public static void putStatic() {
        Blockchain.putStorage(K, V);
    }

    public static void resetStatic() {
        Blockchain.putStorage(K, null);
    }

    public static void verifyStatic() {
        byte[] out = Blockchain.getStorage(K);
        if (out == null) {
            Blockchain.println("CORRECT: found null");
        } else {
            Blockchain.println("INCORRECT: found non-null");
        }
    }
}
