package org.aion.harness.kernel;

import org.apache.commons.codec.binary.Hex;

import java.util.Arrays;

public class PrivateKey {
    public static final int SIZE = 32;
    private final byte[] privateKeyBytes;

    private PrivateKey(byte[] privateKeyBytes) {
        if (privateKeyBytes == null) {
            throw new NullPointerException("private key bytes cannot be null");
        }
        if (privateKeyBytes.length != SIZE) {
            throw new IllegalArgumentException("bytes of a private key must have a length of " + SIZE);
        }
        this.privateKeyBytes = privateKeyBytes;
    }

    public static PrivateKey createPrivateKey(byte[] privateKeyBytes) {
        return new PrivateKey(copyByteArray(privateKeyBytes));
    }

    public String toString() {
        return "PrivateKey { " + Hex.encodeHexString(this.privateKeyBytes) + " }";
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PrivateKey)) {
            return false;
        }
        if (other == this) {
            return true;
        }

        PrivateKey otherPrivateKey = (PrivateKey)other;
        return Arrays.equals(this.privateKeyBytes, otherPrivateKey.getPrivateKeyBytes());
    }

    public byte[] getPrivateKeyBytes() {
        return copyByteArray(this.privateKeyBytes);
    }

    private static byte[] copyByteArray(byte[] byteArray) {
        return Arrays.copyOf(byteArray, byteArray.length);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.privateKeyBytes);
    }
}
