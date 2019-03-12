package org.aion.harness.main.types;

import java.util.Arrays;
import org.aion.harness.main.RPC;
import org.apache.commons.codec.binary.Hex;

/**
 * A receipt hash is a hashed value, represented as a byte array, that identifies a transaction
 * receipt uniquely.
 *
 * A receipt hash can be used to query a specific receipt using the {@link RPC} class, and is
 * generally the return type of an RPC call that sends a transaction (and therefore produces a
 * transaction receipt).
 *
 * A receipt hash is immutable.
 */
public final class ReceiptHash {
    private final byte[] hash;

    /**
     * Constructs a new receipt hash given the provided hash.
     *
     * @param hash The receipt hash.
     */
    public ReceiptHash(byte[] hash) {
        if (hash == null) {
            throw new NullPointerException("Cannot construct receipt hash with null hash.");
        }

        this.hash = Arrays.copyOf(hash, hash.length);
    }

    /**
     * Returns the receipt hash.
     *
     * @return the receipt hash.
     */
    public byte[] getHash() {
        return Arrays.copyOf(this.hash, this.hash.length);
    }

    @Override
    public String toString() {
        return "ReceiptHash { hash = 0x" + Hex.encodeHexString(this.hash) + " }";
    }

    /**
     * Returns {@code true} if, and only if, other is a receipt hash and it has the same hash as
     * this receipt hash.
     *
     * @param other The other object whose equality is to be tested.
     * @return true if other is a receipt hash with the same hash as this receipt hash.
     */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ReceiptHash)) {
            return false;
        }

        ReceiptHash otherHash = (ReceiptHash) other;
        return Arrays.equals(this.hash, otherHash.hash);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.hash);
    }

}
