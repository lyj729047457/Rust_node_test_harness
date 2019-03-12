package org.aion.harness.result;

import org.aion.harness.kernel.RawTransaction;

/**
 * A result from some process that generates a {@link RawTransaction} upon success.
 *
 * If {@code success == true} then the transaction will be non-null.
 *
 * If {@code success == false} then the transaction field is meaningless, and error will be non-null.
 *
 * There is not concept of equality defined for a log event result.
 *
 * A transaction result is immutable.
 */
public final class TransactionResult {
    private final boolean success;
    private final String error;
    private final RawTransaction transaction;

    private TransactionResult(boolean success, String error, RawTransaction transaction) {
        if (error == null) {
            throw new NullPointerException("Cannot construct transaction result with null error.");
        }

        this.success = success;
        this.error = error;
        this.transaction = transaction;
    }

    public static TransactionResult successful(RawTransaction transaction) {
        if (transaction == null) {
            throw new NullPointerException("Cannot construct successful transaction result with null transaction.");
        }

        return new TransactionResult(true, "", transaction);
    }

    public static TransactionResult unsuccessful(String error) {
        return new TransactionResult(false, error, null);
    }

    /**
     * Returns the transaction if the action responsible for producing it was successful.
     *
     * @return the transaction.
     */
    public RawTransaction getTransaction() {
        return this.transaction;
    }

    /**
     * Returns {@code true} only if the action that this result represents was successful.
     *
     * @return whether or not the action was successful.
     */
    public boolean isSuccess() {
        return this.success;
    }

    /**
     * Returns the error if one exists.
     *
     * @return the error.
     */
    public String getError() {
        return this.error;
    }

    @Override
    public String toString() {
        if (this.success) {
            return "TransactionResult { successful | transaction: " + this.transaction + " }";
        } else {
            return "TransactionResult { unsuccessful due to: " + this.error + " }";
        }
    }

}
