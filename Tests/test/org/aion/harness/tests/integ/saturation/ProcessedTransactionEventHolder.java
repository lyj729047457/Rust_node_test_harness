package org.aion.harness.tests.integ.saturation;

import org.aion.harness.main.event.IEvent;

public final class ProcessedTransactionEventHolder {
    public final IEvent transactionIsSealed;
    public final IEvent transactionIsRejected;
    public final IEvent transactionIsSealedOrRejected;

    public ProcessedTransactionEventHolder(IEvent transactionIsSealed, IEvent transactionIsRejected, IEvent transactionIsSealedOrRejected) {
        this.transactionIsSealed = transactionIsSealed;
        this.transactionIsRejected = transactionIsRejected;
        this.transactionIsSealedOrRejected = transactionIsSealedOrRejected;
    }
}
