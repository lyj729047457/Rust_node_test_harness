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

}
