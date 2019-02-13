package org.aion.harness;

import org.apache.commons.codec.binary.Hex;
import java.util.Arrays;

/**
 * An Immutable address, this provides a more intuitive way of talking about an account.
 */
public class Address {
    public static final int ADDRESS_BYTES_LENGTH = 32;
    public static final int PRIVATE_KEY_BYTES_LENGTH = 32;

    private final byte[] addressBytes;
    private final byte[] privateKeyBytes;

    public static Address createAddress(byte[] addressBytes) {
        if (addressBytes == null) {
            throw new NullPointerException("address bytes cannot be null");
        }
        if (addressBytes.length != ADDRESS_BYTES_LENGTH) {
            throw new IllegalArgumentException("bytes of an address must have a length of " + ADDRESS_BYTES_LENGTH);
        }
        return new Address(copyByteArray(addressBytes), null);
    }

    public static Address createAddressWithPrivateKey(byte[] addressBytes, byte[] privateKeyBytes) {
        if (addressBytes == null) {
            throw new NullPointerException("address bytes cannot be null");
        }
        if (privateKeyBytes == null) {
            throw new NullPointerException("private key bytes cannot be null");
        }
        if (addressBytes.length != ADDRESS_BYTES_LENGTH) {
            throw new IllegalArgumentException("bytes of an address must have a length of " + ADDRESS_BYTES_LENGTH);
        }
        if (privateKeyBytes.length != PRIVATE_KEY_BYTES_LENGTH) {
            throw new IllegalArgumentException("bytes of a private key must have a length of " + PRIVATE_KEY_BYTES_LENGTH);
        }
        return new Address(copyByteArray(addressBytes), copyByteArray(privateKeyBytes));
    }

    public byte[] getAddressBytes() {
        return copyByteArray(this.addressBytes);
    }

    public byte[] getPrivateKeyBytes() {
        return copyByteArray(this.privateKeyBytes);
    }

    private Address(byte[] addressBytes, byte[] privateKeyBytes) {
        this.addressBytes = addressBytes;
        this.privateKeyBytes = privateKeyBytes;
    }

    /**
     * This helper is used to make the bytes immutable
     */
    private static byte[] copyByteArray(byte[] byteArray) {
        return Arrays.copyOf(byteArray, byteArray.length);
    }

    public String toString() {
        return "Address { " + Hex.encodeHexString(this.addressBytes) + " }";
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Address)) {
            return false;
        }
        if (other == this) {
            return true;
        }

        Address otherAddress = (Address)other;
        return Arrays.equals(this.addressBytes, otherAddress.getAddressBytes());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.addressBytes);
    }
}
