package org.aion.harness.main.event;

import org.aion.harness.kernel.RawTransaction;
import org.apache.commons.codec.binary.Hex;

public class RustPrepackagedLogEvents implements PrepackagedLogEvents {

    @Override
    public IEvent getStartedMiningEvent() {
        // Rust doesn't emit a message for started mining.  We will
        // rely on the heartbeat event message instead; from past
        // observation, by the time this messsage gets printed, the external
        // miner is attached and doing work.
        return getHeartbeatEvent();
    }

    @Override
    public IEvent getTransactionSealedEvent(RawTransaction transaction) {
        if (transaction == null) {
            throw new NullPointerException("Cannot get event for null transaction hash.");
        }
        return new Event(
            "Transaction mined (hash " + Hex.encodeHexString(transaction.getTransactionHash()) + ")");
    }

    @Override
    public IEvent getTransactionRejectedEvent(RawTransaction transaction) {
        if (transaction == null) {
            throw new NullPointerException("Cannot get event for null transaction hash.");
        }
        return new Event(
            "Transaction rejected (hash " + Hex.encodeHexString(transaction.getTransactionHash()) + ")");

    }

    @Override
    public IEvent getHeartbeatEvent() {
        return new Event("= Sync Statics =");
    }
}
