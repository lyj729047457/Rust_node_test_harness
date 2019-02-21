package org.aion.harness.kernel;

import org.apache.commons.codec.binary.Hex;

import java.util.Arrays;

/**
 * An Aion private key corresponding to some Aion address.
 *
 * A private key is immutable.
 */
public final class PrivateKey {
    public static final int SIZE = 32;

    private final byte[] privateKeyBytes;

    /**
     * Constructs a new private key consisting of the provided bytes.
     *
     * @param privateKeyBytes The bytes of the private key.
     */
    public PrivateKey(byte[] privateKeyBytes) {
        if (privateKeyBytes == null) {
            throw new NullPointerException("private key bytes cannot be null");
        }
        if (privateKeyBytes.length != SIZE) {
            throw new IllegalArgumentException("bytes of a private key must have a length of " + SIZE);
        }
        this.privateKeyBytes = copyByteArray(privateKeyBytes);
    }

    /**
     * Returns the bytes of this private key.
     *
     * @return The bytes of the private key.
     */
    public byte[] getPrivateKeyBytes() {
        return copyByteArray(this.privateKeyBytes);
    }

    private static byte[] copyByteArray(byte[] byteArray) {
        return Arrays.copyOf(byteArray, byteArray.length);
    }

    @Override
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

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.privateKeyBytes);
    }
}
