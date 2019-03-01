package org.aion.harness.main;

import java.math.BigInteger;
import java.text.NumberFormat;
import org.aion.harness.kernel.Transaction;
import org.aion.harness.main.tools.InternalRpcResult;
import org.aion.harness.main.tools.RpcCaller;
import org.aion.harness.main.tools.RpcMethod;
import org.aion.harness.main.tools.JsonStringParser;
import org.aion.harness.main.tools.RpcPayload;
import org.aion.harness.main.tools.RpcPayloadBuilder;
import org.aion.harness.main.types.ReceiptHash;
import org.aion.harness.main.types.SyncStatus;
import org.aion.harness.main.types.TransactionReceipt;
import org.aion.harness.main.types.TransactionReceiptBuilder;
import org.aion.harness.misc.Assumptions;
import org.aion.harness.result.Result;
import org.aion.harness.result.RpcResult;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

/**
 * A class that facilitates communication with the node via the kernel's RPC server.
 *
 * This class interacts directly with the RPC endpoints in the kernel and does not go through the
 * Java or Web3 APIs, for example.
 *
 * This class is not thread-safe.
 */
public final class RPC {
    private final RpcCaller rpc;

    public RPC(String ip, String port) {
        this.rpc = new RpcCaller(ip, port);
    }

    /**
     * Sends the specified transaction to the node.
     *
     * This call is asynchronous, and as such, the returned receipt hash will not correspond to a
     * receipt until the transaction has been fully processed.
     *
     * @param transaction The transaction to send.
     * @return the result of this attempt to send the transaction.
     */
    public RpcResult<ReceiptHash> sendTransaction(Transaction transaction) throws InterruptedException {
        return callSendTransaction(transaction, false);
    }

    /**
     * Sends the specified transaction to the node.
     *
     * Displays the I/O of the attempt to hit the RPC endpoint.
     *
     * This call is asynchronous, and as such, the returned receipt hash will not correspond to a
     * receipt until the transaction has been fully processed.
     *
     * @param transaction The transaction to send.
     * @return the result of this attempt to send the transaction.
     */
    public RpcResult<ReceiptHash> sendTransactionVerbose(Transaction transaction) throws InterruptedException {
        return callSendTransaction(transaction, true);
    }

    /**
     * Returns the transaction receipt whose hash is the specified receipt hash.
     *
     * Displays the I/O of the attempt to hit the RPC endpoint.
     *
     * Note that a receipt will not become available until a transaction has been sealed into a
     * block.
     *
     * @param receiptHash The receipt hash of the receipt to get.
     * @return the result of this attempt to get the transaction receipt.
     */
    public RpcResult<TransactionReceipt> getTransactionReceiptVerbose(ReceiptHash receiptHash) throws InterruptedException {
        return callGetTransactionReceipt(receiptHash, true);
    }

    /**
     * Returns the transaction receipt whose hash is the specified receipt hash.
     *
     * Note that a receipt will not become available until a transaction has been sealed into a
     * block.
     *
     * @param receiptHash The receipt hash of the receipt to get.
     * @return the result of this attempt to get the transaction receipt.
     */
    public RpcResult<TransactionReceipt> getTransactionReceipt(ReceiptHash receiptHash) throws InterruptedException {
        return callGetTransactionReceipt(receiptHash, false);
    }

    /**
     * Returns the balance of the specified address.
     *
     * @param address The address whose balance is to be queried.
     * @return the result of the call.
     */
    public RpcResult<BigInteger> getBalance(byte[] address) throws InterruptedException {
        return callGetBalance(address, false);
    }

    /**
     * Returns the balance of the specified address.
     *
     * Displays the I/O of the attempt to hit the RPC endpoint.
     *
     * @param address The address whose balance is to be queried.
     * @return the result of the call.
     */
    public RpcResult<BigInteger> getBalanceVerbose(byte[] address) throws InterruptedException {
        return callGetBalance(address, true);
    }

    /**
     * Returns the nonce of the specified address.
     *
     * @param address The address whose nonce is to be queried.
     * @return the result of the call.
     */
    public RpcResult<BigInteger> getNonce(byte[] address) throws InterruptedException {
        return callGetNonce(address, false);
    }

    /**
     * Returns the nonce of the specified address.
     *
     * Displays the I/O of the attempt to hit the RPC endpoint.
     *
     * @param address The address whose nonce is to be queried.
     * @return the result of the call.
     */
    public RpcResult<BigInteger> getNonceVerbose(byte[] address) throws InterruptedException {
        return callGetNonce(address, true);
    }

    /**
     * Returns the syncing status of the node.
     *
     * Note that the node will be considered as attempting to connect to the network if it has zero
     * peers. In the case of a private network, this means that this method will always time out.
     * Therefore, this method should only be called on nodes that are connected to / attempting to
     * connect to public networks.
     *
     * Displays the I/O of the attempt to hit the RPC endpoint.
     *
     * @return the result of the call.
     */
    public RpcResult<SyncStatus> getSyncingStatusVerbose() throws InterruptedException {
        return callSyncing(true);
    }

    /**
     * Returns the syncing status of the node.
     *
     * Note that the node will be considered as attempting to connect to the network if it has zero
     * peers. In the case of a private network, this means that this method will always time out.
     * Therefore, this method should only be called on nodes that are connected to / attempting to
     * connect to public networks.
     *
     * @return the result of the call.
     */
    public RpcResult<SyncStatus> getSyncingStatus() throws InterruptedException {
        return callSyncing(false);
    }

    /**
     * Blocks until the node has finished syncing with the rest of the network, or until the request
     * times out.
     *
     * Technically speaking, a node never finishes syncing with the network unless it is always
     * at the top block and always the first to mine the next block. However, once at the top of
     * the chain, a node is in a relatively stable position with regards to the network. Therefore
     * this method considers a node in sync with the network if it is within 5 blocks of the top
     * of the chain.
     *
     * @param delayInMillis The amount of time to wait between checking the current sync status.
     * @param timeoutInMillis The total amount of time to wait for syncing.
     * @return  the result of this event.
     */
    public Result waitForSyncToComplete(long delayInMillis, long timeoutInMillis) {
        if (timeoutInMillis < 0) {
            throw new IllegalArgumentException("Timeout value was negative: " + timeoutInMillis);
        }

        try {
            long currentTime = System.currentTimeMillis();
            long deadline = currentTime + timeoutInMillis;

            RpcResult<SyncStatus> syncStatus = getSyncingStatus();
            if (!syncStatus.success) {
                return Result.unsuccessfulDueTo(syncStatus.error);
            }

            while ((currentTime < deadline) && (syncStatus.getResult().isSyncing())) {
                // Log the current status.
                SyncStatus status = syncStatus.getResult();
                broadcastSyncUpdate(status.isWaitingToConnect(), status.getSyncCurrentBlockNumber(), status.getHighestBlockNumber());

                // Sleep for the specified delay, unless there is less time remaining until the
                // deadline, then sleep only until the deadline.
                Thread.sleep(Math.min(deadline - currentTime, delayInMillis));

                // Update the status.
                syncStatus = getSyncingStatus();
                if (!syncStatus.success) {
                    return Result.unsuccessfulDueTo(syncStatus.error);
                }

                currentTime = System.currentTimeMillis();
            }

            // We either timed out or finished syncing.
            if (currentTime >= deadline) {
                return Result.unsuccessfulDueTo("Timed out waiting for sync to finish.");
            } else {
                System.out.println(syncStatus);
                return Result.successful();
            }

        } catch (InterruptedException e) {
            return Result.unsuccessfulDueToException(e);
        }
    }

    private void broadcastSyncUpdate(boolean waitingToConnect, BigInteger currentBlock, BigInteger highestBlock) {
        if (waitingToConnect) {
            System.out.println(Assumptions.LOGGER_BANNER + "Sync Progress = { waiting to connect to peers }");
        } else {
            System.out.println(Assumptions.LOGGER_BANNER + "Sync Progress = { At block: "
                + NumberFormat.getIntegerInstance().format(currentBlock)
                + " of " + NumberFormat.getIntegerInstance().format(highestBlock) + " }");
        }
    }

    private RpcResult<ReceiptHash> callSendTransaction(Transaction transaction, boolean verbose) throws InterruptedException {
        if (transaction == null) {
            throw new IllegalArgumentException("Cannot send a null transaction.");
        }

        RpcPayload payload = new RpcPayloadBuilder()
            .method(RpcMethod.SEND_RAW_TRANSACTION)
            .params(Hex.encodeHexString(transaction.getSignedTransactionBytes()))
            .useLatestBlock()
            .build();

        InternalRpcResult internalResult = this.rpc.call(payload, verbose);

        if (internalResult.success) {
            JsonStringParser outputParser = new JsonStringParser(internalResult.output);
            String result = outputParser.attributeToString("result");

            // This should never happen.
            if (result == null) {
                throw new IllegalStateException("No 'result' content to parse from: " + internalResult.output);
            }
            
            try {
                return RpcResult.successful(new ReceiptHash(Hex.decodeHex(result)), internalResult.timeOfCall);
            } catch (DecoderException e) {
                return RpcResult.unsuccessful(e.toString());
            }
        } else {
            return RpcResult.unsuccessful(internalResult.error);
        }
    }

    private RpcResult<BigInteger> callGetBalance(byte[] address, boolean verbose) throws InterruptedException {
        if (address == null) {
            throw new IllegalArgumentException("Cannot get balance of a null address.");
        }

        RpcPayload payload = new RpcPayloadBuilder()
            .method(RpcMethod.GET_BALANCE)
            .params(Hex.encodeHexString(address))
            .useLatestBlock()
            .build();

        InternalRpcResult internalResult = this.rpc.call(payload, verbose);

        if (internalResult.success) {
            JsonStringParser outputParser = new JsonStringParser(internalResult.output);
            String result = outputParser.attributeToString("result");

            // This should never happen.
            if (result == null) {
                throw new IllegalStateException("No 'result' content to parse from: " + internalResult.output);
            }

            return RpcResult.successful(new BigInteger(result, 16), internalResult.timeOfCall);
        } else {
            return RpcResult.unsuccessful(internalResult.error);
        }
    }

    private RpcResult<BigInteger> callGetNonce(byte[] address, boolean verbose) throws InterruptedException {
        if (address == null) {
            throw new IllegalArgumentException("Cannot get nonce of a null address.");
        }

        RpcPayload payload = new RpcPayloadBuilder()
            .method(RpcMethod.GET_NONCE)
            .params(Hex.encodeHexString(address))
            .useLatestBlock()
            .build();

        InternalRpcResult internalResult = this.rpc.call(payload, verbose);

        if (internalResult.success) {
            JsonStringParser outputParser = new JsonStringParser(internalResult.output);
            String result = outputParser.attributeToString("result");

            // This should never happen.
            if (result == null) {
                throw new IllegalStateException("No 'result' content to parse from: " + internalResult.output);
            }

            return RpcResult.successful(new BigInteger(result, 16), internalResult.timeOfCall);
        } else {
            return RpcResult.unsuccessful(internalResult.error);
        }
    }

    private RpcResult<TransactionReceipt> callGetTransactionReceipt(ReceiptHash receiptHash, boolean verbose) throws InterruptedException {
        if (receiptHash == null) {
            throw new NullPointerException("Cannot get a receipt from a null receipt hash.");
        }

        RpcPayload payload = new RpcPayloadBuilder()
            .method(RpcMethod.GET_TRANSACTION_RECEIPT)
            .params(Hex.encodeHexString(receiptHash.getHash()))
            .useLatestBlock()
            .build();

        InternalRpcResult internalResult = this.rpc.call(payload, verbose);

        if (internalResult.success) {
            JsonStringParser outputParser = new JsonStringParser(internalResult.output);
            String result = outputParser.attributeToString("result");

            // This should never happen.
            if (result == null) {
                throw new IllegalStateException("No 'result' content to parse from: " + internalResult.output);
            }

            try {
                TransactionReceipt receipt = new TransactionReceiptBuilder().buildFromJsonString(result);
                return RpcResult.successful(receipt, internalResult.timeOfCall);
            } catch (DecoderException e) {
                return RpcResult.unsuccessful(e.toString());
            }

        } else {
            return RpcResult.unsuccessful(internalResult.error);
        }
    }

    private RpcResult<SyncStatus> callSyncing(boolean verbose) throws InterruptedException {
        RpcPayload payload = new RpcPayloadBuilder()
            .method(RpcMethod.IS_SYNCED)
            .build();

        InternalRpcResult internalResult = this.rpc.call(payload, verbose);

        if (internalResult.success) {
            JsonStringParser outputParser = new JsonStringParser(internalResult.output);
            String result = outputParser.attributeToString("result");

            // This should never happen.
            if (result == null) {
                throw new IllegalStateException("No 'result' content to parse from: " + internalResult.output);
            }

            if (result.equals("false")) {
                return RpcResult.successful(SyncStatus.notSyncing(), internalResult.timeOfCall);
            } else {
                JsonStringParser statusParser = new JsonStringParser(result);
                String startBlock = statusParser.attributeToString("startingBlock");
                String currBlock = statusParser.attributeToString("currentBlock");
                String highBlock = statusParser.attributeToString("highestBlock");

                BigInteger startingBlock = (startBlock == null) ? null : new BigInteger(startBlock, 16);
                BigInteger currentBlock = (currBlock == null) ? null : new BigInteger(currBlock, 16);
                BigInteger highestBlock = (highBlock == null) ? null : new BigInteger(highBlock, 16);

                // We can tell that we haven't connected to the network yet if its highest block number is zero.
                boolean waitingToConnect = (highestBlock != null) && (highestBlock.equals(BigInteger.ZERO));

                return RpcResult.successful(SyncStatus.syncing(waitingToConnect, startingBlock, currentBlock, highestBlock), internalResult.timeOfCall);
            }

        } else {
            return RpcResult.unsuccessful(internalResult.error);
        }
    }

}
