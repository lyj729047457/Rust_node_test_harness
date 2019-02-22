package org.aion.harness.result;

import java.util.Optional;
import org.aion.harness.kernel.Transaction;

/**
 * A result from some process that generates a {@link Transaction} upon success.
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
    public final boolean success;
    public final String error;
    private final Transaction transaction;

    private TransactionResult(boolean success, String error, Transaction transaction) {
        if (error == null) {
            throw new NullPointerException("Cannot construct transaction result with null error.");
        }

        this.success = success;
        this.error = error;
        this.transaction = transaction;
    }

    public static TransactionResult successful(Transaction transaction) {
        if (transaction == null) {
            throw new NullPointerException("Cannot construct successful transaction result with null transaction.");
        }

        return new TransactionResult(true, "", transaction);
    }

    public static TransactionResult unsuccessful(String error) {
        return new TransactionResult(false, error, null);
    }

    public Optional<Transaction> getTransaction() {
        return (this.transaction == null) ? Optional.empty() : Optional.of(this.transaction);
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
