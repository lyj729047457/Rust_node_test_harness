package org.aion.harness.kernel;

import java.math.BigInteger;
import java.util.Arrays;
import org.apache.commons.codec.binary.Hex;

/**
 * An unsigned transaction.
 *
 * This class is immutable.
 */
public final class UnsignedTransaction {
    public final Address sender;
    public final Address destination;
    public final BigInteger senderNonce;
    public final BigInteger valueToTransfer;
    public final long energyLimit;
    public final long energyPrice;
    private final byte[] data;

    private UnsignedTransaction(Address sender, Address destination, BigInteger senderNonce, BigInteger valueToTransfer, byte[] data, long energyLimit, long energyPrice) {
        if (sender == null) {
            throw new NullPointerException("Cannot create unsigned transaction with null sender!");
        }
        if (senderNonce == null) {
            throw new NullPointerException("Cannot create unsigned transaction with null nonce!");
        }
        if (valueToTransfer == null) {
            throw new NullPointerException("Cannot create unsigned transaction with null value!");
        }
        if (data == null) {
            throw new NullPointerException("Cannot create unsigned transaction with null data!");
        }
        this.sender = sender;
        this.destination = destination;
        this.senderNonce = senderNonce;
        this.valueToTransfer = valueToTransfer;
        this.energyLimit = energyLimit;
        this.energyPrice = energyPrice;
        this.data = Arrays.copyOf(data, data.length);
    }

    /**
     * Returns a new unsigned create transaction.
     */
    public static UnsignedTransaction newCreateTransaction(Address sender, BigInteger senderNonce, BigInteger valueToTransfer, byte[] data, long energyLimit, long energyPrice) {
        return new UnsignedTransaction(sender, null, senderNonce, valueToTransfer, data, energyLimit, energyPrice);
    }

    /**
     * Returns a new unsigned non-create transaction.
     */
    public static UnsignedTransaction newNonCreateTransaction(Address sender, Address destination, BigInteger senderNonce, BigInteger valueToTransfer, byte[] data, long energyLimit, long energyPrice) {
        if (destination == null) {
            throw new NullPointerException("Cannot create unsigned transaction with null destination!");
        }
        return new UnsignedTransaction(sender, destination, senderNonce, valueToTransfer, data, energyLimit, energyPrice);
    }

    public byte[] copyOfData() {
        return Arrays.copyOf(this.data, this.data.length);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof UnsignedTransaction)) {
            return false;
        } else if (other == this) {
            return true;
        }

        UnsignedTransaction otherTransaction = (UnsignedTransaction) other;
        if (!this.sender.equals(otherTransaction.sender)) {
            return false;
        }
        if (!this.senderNonce.equals(otherTransaction.senderNonce)) {
            return false;
        }
        if (!this.valueToTransfer.equals(otherTransaction.valueToTransfer)) {
            return false;
        }
        if (this.energyLimit != otherTransaction.energyLimit) {
            return false;
        }
        if (this.energyPrice != otherTransaction.energyPrice) {
            return false;
        }
        if (!Arrays.equals(this.data, otherTransaction.data)) {
            return false;
        }
        return (this.destination == null) ? (otherTransaction.destination == null) : this.destination.equals(otherTransaction.destination);
    }

    @Override
    public int hashCode() {
        int hash = 16;
        hash += this.sender.hashCode() + this.senderNonce.hashCode() + this.valueToTransfer.hashCode() + Arrays.hashCode(this.data) + this.energyLimit + this.energyPrice;
        return (this.destination == null) ? hash : hash + this.destination.hashCode();
    }

    @Override
    public String toString() {
        if (this.destination == null) {
            return "UnsignedTransaction { [CREATE] sender = " + this.sender
                + ", nonce = " + this.senderNonce
                + ", value = " + this.valueToTransfer
                + ", data = " + Hex.encodeHexString(this.data)
                + ", energy limit = " + this.energyLimit
                + ", energy price = " + this.energyPrice + " }";
        } else {
            return "UnsignedTransaction { [NON-CREATE] sender = " + this.sender
                + ", destination = " + this.destination
                + ", nonce = " + this.senderNonce
                + ", value = " + this.valueToTransfer
                + ", data = " + Hex.encodeHexString(this.data)
                + ", energy limit = " + this.energyLimit
                + ", energy price = " + this.energyPrice + " }";
        }
    }
}
