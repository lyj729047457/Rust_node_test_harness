package org.aion.harness.kernel;

import java.util.Arrays;
import main.SignedTransactionBuilder;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import org.apache.commons.codec.binary.Hex;

/**
 * A signed Aion transaction.
 *
 * This transaction is "raw", indicating that it is a byte array representative of a transaction
 * (its encoding), rather than a typical object with fields associated with it.
 *
 * A raw transaction is used to send transactions over the RPC layer to the node.
 *
 * A transaction is immutable.
 */
public final class RawTransaction {
    private final byte[] signedTransaction;
    private byte[] hash;

    private RawTransaction(PrivateKey sender, BigInteger nonce, Address destination, byte[] data,
        long energyLimit, long energyPrice, BigInteger value, boolean isAvmCreate)
        throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {

        SignedTransactionBuilder transactionBuilder = new SignedTransactionBuilder()
                .privateKey(sender.getPrivateKeyBytes())
                .senderNonce(nonce)
                .destination((destination == null) ? null : destination.getAddressBytes())
                .data(data)
                .energyLimit(energyLimit)
                .energyPrice(energyPrice)
                .value(value);

        if (isAvmCreate) {
            transactionBuilder.useAvmTransactionType();
        }

        this.signedTransaction = transactionBuilder.buildSignedTransaction();
    }

    /**
     * Returns a new general transaction. In particular, a general transaction is any transaction
     * that is not an Avm create transaction.
     *
     * @param senderPrivateKey The private key of the sender.
     * @param nonce The nonce of the sender.
     * @param destination The destination address.
     * @param data The data.
     * @param energyLimit The energy limit.
     * @param energyPrice The price per unit energy.
     * @param value The amount to be transferred.
     * @return a new signed transaction.
     */
    public static RawTransaction newGeneralTransaction(PrivateKey senderPrivateKey, BigInteger nonce, Address destination, byte[] data, long energyLimit, long energyPrice, BigInteger value) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        return new RawTransaction(senderPrivateKey, nonce, destination, data, energyLimit, energyPrice, value, false);
    }

    /**
     * Returns a new avm create transaction. This transaction attempts to deploy a new Java smart
     * contract.
     *
     * @param senderPrivateKey The private key of the sender.
     * @param nonce The nonce of the sender.
     * @param data The bytes of the jar file to be deployed.
     * @param energyLimit The energy limit.
     * @param energyPrice The price per unit energy.
     * @param value The amount to be transferred to the contract.
     * @return a new signed transaction.
     */
    public static RawTransaction newAvmCreateTransaction(PrivateKey senderPrivateKey, BigInteger nonce, byte[] data, long energyLimit, long energyPrice, BigInteger value) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        return new RawTransaction(senderPrivateKey, nonce, null, data, energyLimit, energyPrice, value, true);
    }

    /**
     * Returns the bytes of the signed transaction.
     *
     * @return The transaction bytes.
     */
    public byte[] getSignedTransactionBytes() {
        return Arrays.copyOf(this.signedTransaction, this.signedTransaction.length);
    }

    /**
     * Returns the hash of this transaction.
     *
     * @return The transaction hash.
     */
    public byte[] getTransactionHash() {
        if (this.hash == null) {
            this.hash = SignedTransactionBuilder.getTransactionHashOfSignedTransaction(this.signedTransaction);
        }
        return this.hash;
    }

    @Override
    public String toString() {
        return "Transaction { hash = " + Hex.encodeHexString(getTransactionHash()) + " }";
    }

}
