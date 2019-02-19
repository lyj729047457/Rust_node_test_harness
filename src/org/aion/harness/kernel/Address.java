package org.aion.harness.kernel;

import org.apache.commons.codec.binary.Hex;
import java.util.Arrays;

/**
 * An Immutable address, this provides a more intuitive way of talking about an account.
 */
public class Address {
    private final byte[] addressBytes;
    public static final int SIZE = 32;

    private Address(byte[] addressBytes, byte[] privateKeyBytes) {
        if (addressBytes == null) {
            throw new NullPointerException("address bytes cannot be null");
        }
        if (addressBytes.length != SIZE) {
            throw new IllegalArgumentException("bytes of an address must have a length of " + SIZE);
        }
        this.addressBytes = addressBytes;
    }

    public static Address createAddress(byte[] addressBytes) {
        return new Address(copyByteArray(addressBytes), null);
    }

    public byte[] getAddressBytes() {
        return copyByteArray(this.addressBytes);
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
