package org.aion.harness.kernel;

import org.apache.commons.codec.binary.Hex;
import java.util.Arrays;

/**
 * An Aion address.
 *
 * An address consists of {@value SIZE} bytes.
 *
 * This class is immutable.
 */
public final class Address {
    public static final int SIZE = 32;

    private final byte[] addressBytes;

    /**
     * Constructs a new address using the provided bytes.
     *
     * @param addressBytes The bytes of the address.
     */
    public Address(byte[] addressBytes) {
        if (addressBytes == null) {
            throw new NullPointerException("address bytes cannot be null");
        }
        if (addressBytes.length != SIZE) {
            throw new IllegalArgumentException("bytes of an address must have a length of " + SIZE);
        }
        this.addressBytes = copyByteArray(addressBytes);
    }

    /**
     * Returns the bytes of this address.
     *
     * @return the bytes of this address.
     */
    public byte[] getAddressBytes() {
        return copyByteArray(this.addressBytes);
    }

    /**
     * This helper is used to make the bytes immutable
     */
    private static byte[] copyByteArray(byte[] byteArray) {
        return Arrays.copyOf(byteArray, byteArray.length);
    }

    @Override
    public String toString() {
        return "Address { 0x" + Hex.encodeHexString(this.addressBytes) + " }";
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
