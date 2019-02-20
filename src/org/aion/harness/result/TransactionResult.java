package org.aion.harness.result;

import org.aion.harness.kernel.Transaction;

/**
 * Holds a simple result and a transaction.
 *
 * If the result is successful then transaction is meaningful.
 *
 * Otherwise, if the result is unsuccessful, then the transaction will be null.
 */
public final class TransactionResult {
    private final Transaction transaction;
    private final StatusResult result;

    private TransactionResult(StatusResult result, Transaction transaction) {
        this.result = result;
        this.transaction = transaction;
    }

    public static TransactionResult successful(Transaction transaction) {
        return new TransactionResult(StatusResult.successful(), transaction);
    }

    public static TransactionResult unsuccessful(int status, String error) {
        return new TransactionResult(StatusResult.unsuccessful(status, error), null);
    }

    public StatusResult getResultOnly() {
        return this.result;
    }

    public Transaction getTransaction() {
        return this.transaction;
    }

    @Override
    public String toString() {
        return "Result { success = " + this.result.success + ", transaction = " + this.transaction
            + ", status = " + this.result.status + ", error = " + this.result.error + " }";
    }

}
