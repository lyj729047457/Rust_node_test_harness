package org.aion.harness;

import main.SignedTransactionBuilder;
import org.aion.harness.misc.Assumptions;
import org.aion.harness.result.TransactionResult;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import org.apache.commons.codec.binary.Hex;

/**
 * The bytes of a signed transaction.
 *
 * This class is not thread-safe.
 */
public final class Transaction {
    private final byte[] signedTransaction;

    private Transaction(byte[] privateKey, BigInteger nonce, byte[] destination, byte[] data,
        long energyLimit, long energyPrice, BigInteger value, boolean isForAvm)
        throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {

        SignedTransactionBuilder transactionBuilder = new SignedTransactionBuilder()
                .privateKey(privateKey)
                .senderNonce(nonce)
                .destination(destination)
                .data(data)
                .energyLimit(energyLimit)
                .energyPrice(energyPrice)
                .value(value);

        if (isForAvm) {
            transactionBuilder.useAvmTransactionType();
        }

        this.signedTransaction = transactionBuilder.buildSignedTransaction();
    }

    public static TransactionResult buildAndSignTransaction(byte[] privateKey, BigInteger nonce,
        byte[] destination, byte[] data, long energyLimit, long energyPrice, BigInteger value) {

        try {
            return TransactionResult.successful(new Transaction(privateKey, nonce, destination, data, energyLimit, energyPrice, value, false));
        } catch (Exception e) {
            return TransactionResult.unsuccessful(Assumptions.PRODUCTION_ERROR_STATUS, (e.getMessage() == null) ? e.toString() : e.getMessage());
        }
    }

    public static TransactionResult buildAndSignAvmTransaction(byte[] privateKey, BigInteger nonce,
        byte[] destination, byte[] data, long energyLimit, long energyPrice, BigInteger value) {

        try {
            return TransactionResult.successful(new Transaction(privateKey, nonce, destination, data, energyLimit, energyPrice, value, true));
        } catch (Exception e) {
            return TransactionResult.unsuccessful(Assumptions.PRODUCTION_ERROR_STATUS, (e.getMessage() == null) ? e.toString() : e.getMessage());
        }
    }

    public byte[] getBytes() {
        return this.signedTransaction;
    }

    public byte[] getTransactionHash() {
        return SignedTransactionBuilder.getTransactionHashOfSignedTransaction(this.signedTransaction);
    }

    @Override
    public String toString() {
        return "Transaction { " + Hex.encodeHexString(this.signedTransaction) + " }";
    }

}
