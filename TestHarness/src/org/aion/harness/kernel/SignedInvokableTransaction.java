package org.aion.harness.kernel;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import main.SignedInvokableTransactionBuilder;
import main.SignedTransactionBuilder;
import org.apache.commons.codec.binary.Hex;

public class SignedInvokableTransaction {

    private final byte[] transactionBytes;
    private byte[] hash;

    public SignedInvokableTransaction(
            PrivateKey sender,
            BigInteger nonce,
            Address destination,
            byte[] data,
            BigInteger value,
            Address executor)
            throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {

        SignedInvokableTransactionBuilder transactionBuilder = new SignedInvokableTransactionBuilder()
            .privateKey(sender.getPrivateKeyBytes())
            .senderNonce(nonce)
            .destination((destination == null) ? null : destination.getAddressBytes())
            .data(data)
            .value(value)
            .executor((executor == null) ? null : executor.getAddressBytes());

        this.transactionBytes = transactionBuilder.buildSignedInvokableTransaction();
    }

    /**
     * Returns the bytes of the signed transaction.
     *
     * @return The transaction bytes.
     */
    public byte[] getSignedTransactionBytes() {
        return Arrays.copyOf(this.transactionBytes, this.transactionBytes.length);
    }

    /**
     * Returns the hash of this transaction.
     *
     * @return The transaction hash.
     */
    public byte[] getTransactionHash() {
        if (this.hash == null) {
            this.hash = SignedTransactionBuilder.getTransactionHashOfSignedTransaction(this.transactionBytes);
        }
        return this.hash;
    }

    @Override
    public String toString() {
        return "SignedTransaction { hash = " + Hex.encodeHexString(getTransactionHash()) + " }";
    }

}
