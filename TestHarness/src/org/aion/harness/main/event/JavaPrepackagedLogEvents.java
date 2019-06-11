package org.aion.harness.main.event;

import org.aion.harness.kernel.RawTransaction;
import org.apache.commons.codec.binary.Hex;

public final class JavaPrepackagedLogEvents implements PrepackagedLogEvents {
    @Override
    public IEvent getStartedMiningEvent() {
        return new Event("sealer starting");
    }

    @Override
    public IEvent getTransactionSealedEvent(RawTransaction transaction) {
        if (transaction == null) {
            throw new NullPointerException("Cannot get event for null transaction hash.");
        }
        return new Event("Transaction: " + Hex.encodeHexString(transaction.getTransactionHash()) + " was sealed into block");
    }

    @Override
    public IEvent getTransactionRejectedEvent(RawTransaction transaction) {
        if (transaction == null) {
            throw new NullPointerException("Cannot get event for null transaction hash.");
        }
        return new Event("tx " + Hex.encodeHexString(transaction.getTransactionHash()) + " is rejected");
    }

    @Override
    public IEvent getHeartbeatEvent() {
        return new Event("p2p-status");
    }

}
