package org.aion.harness.main.types;

import java.math.BigInteger;

/**
 * A class that holds the status of a syncing node.
 *
 * If the node is not syncing then {@code isSyncing == false} and all other fields are meaningless
 * and possibly null.
 *
 * Otherwise, if {@code isSyncing == true} then {@code waitingToConnect} is meaningful. This field
 * is used to determine whether or not the node is currently waiting to connect to its peers or
 * not. This is the first step in syncing.
 *
 * This class is immutable.
 */
public final class SyncStatus {
    private final boolean isSyncing;
    private final boolean waitingToConnect;
    private final BigInteger startingBlockNumber;
    private final BigInteger currentBlockNumber;
    private final BigInteger highestBlockNumber;

    private SyncStatus(boolean isSyncing, boolean waitingToConnect, BigInteger startingBlock, BigInteger currentBlock, BigInteger bestBlock) {
        this.isSyncing = isSyncing;
        this.waitingToConnect = waitingToConnect;
        this.startingBlockNumber = startingBlock;
        this.currentBlockNumber = currentBlock;
        this.highestBlockNumber = bestBlock;
    }

    /**
     * Returns a new sync status object that is not currently syncing.
     *
     * @return a non-syncing status.
     */
    public static SyncStatus notSyncing() {
        return new SyncStatus(false, false, null, null, null);
    }

    /**
     * Returns a new sync status object that is syncing and has the specified status values.
     *
     * @param startingBlockNumber The block number at which the syncing started.
     * @param currentBlockNumber The current block number in the sync.
     * @param bestBlockNumber The block number of the highest block.
     * @return a syncing status.
     */
    public static SyncStatus syncing(boolean waitingToConnect, BigInteger startingBlockNumber, BigInteger currentBlockNumber, BigInteger bestBlockNumber) {
        if (startingBlockNumber == null) {
            throw new NullPointerException("Cannot construct a sync status with a null starting block.");
        }
        if (currentBlockNumber == null) {
            throw new NullPointerException("Cannot construct a sync status with a null current block.");
        }
        if (bestBlockNumber == null) {
            throw new NullPointerException("Cannot construct a sync status with a null best block.");
        }

        return new SyncStatus(true, waitingToConnect, startingBlockNumber, currentBlockNumber, bestBlockNumber);
    }

    /**
     * Returns {@code true} if, and only if, the node is still syncing.
     *
     * @return whether or not a node is syncing.
     */
    public boolean isSyncing() {
        return this.isSyncing;
    }

    /**
     * Returns {@code true} if, and only if, the node is currently waiting to connect to its peers.
     *
     * @return whether or not the node is waiting to connect.
     */
    public boolean isWaitingToConnect() {
        return this.waitingToConnect;
    }

    /**
     * Returns the block number of the block at which syncing began.
     *
     * @return the starting block number.
     */
    public BigInteger getSyncStartingBlockNumber() {
        return this.startingBlockNumber;
    }

    /**
     * Returns the block number of the block that the sync is currently at.
     *
     * @return the current block number.
     */
    public BigInteger getSyncCurrentBlockNumber() {
        return this.currentBlockNumber;
    }

    /**
     * Returns the block number of the highest block in the blockchain.
     *
     * @return the highest block number.
     */
    public BigInteger getHighestBlockNumber() {
        return this.highestBlockNumber;
    }

    @Override
    public String toString() {
        if (this.isSyncing) {
            if (this.waitingToConnect) {
                return "SyncStatus { waiting to connect to the network }";
            } else {
                return "SyncStatus { starting block number = " + this.startingBlockNumber
                    + ", current block number = " + this.currentBlockNumber
                    + ", highest block number = " + this.highestBlockNumber + " }";
            }
        } else {
            return "SyncStatus { not currently syncing }";
        }
    }

}
