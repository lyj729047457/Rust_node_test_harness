package org.aion.harness.main;

import java.math.BigInteger;
import org.aion.harness.kernel.Transaction;
import org.aion.harness.main.tools.InternalRpcResult;
import org.aion.harness.main.tools.RpcCaller;
import org.aion.harness.main.tools.RpcMethod;
import org.aion.harness.main.tools.RpcOutputParser;
import org.aion.harness.main.tools.RpcPayload;
import org.aion.harness.main.tools.RpcPayloadBuilder;
import org.aion.harness.main.types.ReceiptHash;
import org.aion.harness.result.RpcResult;
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
    private final RpcCaller rpc = new RpcCaller();

    /**
     * Sends the specified transaction to the node.
     *
     * This call is asynchronous, and as such, the returned receipt hash will not correspond to a
     * receipt until the transaction has been fully processed.
     *
     * See {@link NodeListener#waitForTransactionToBeProcessed(byte[], long)}.
     *
     * @param transaction The transaction to send.
     * @return the result of this attempt to send the transaction.
     */
    public RpcResult<ReceiptHash> sendTransaction(Transaction transaction) throws InterruptedException {
        return sendTransactionOverRPC(transaction, false);
    }

    /**
     * Sends the specified transaction to the node.
     *
     * Displays the I/O of the attempt to hit the RPC endpoint.
     *
     * This call is asynchronous, and as such, the returned receipt hash will not correspond to a
     * receipt until the transaction has been fully processed.
     *
     * See {@link NodeListener#waitForTransactionToBeProcessed(byte[], long)}.
     *
     * @param transaction The transaction to send.
     * @return the result of this attempt to send the transaction.
     */
    public RpcResult<ReceiptHash> sendTransactionVerbose(Transaction transaction) throws InterruptedException {
        return sendTransactionOverRPC(transaction, true);
    }

    /**
     * Returns the balance of the specified address.
     *
     * @param address The address whose balance is to be queried.
     * @return the result of the call.
     */
    public RpcResult<BigInteger> getBalance(byte[] address) throws InterruptedException {
        if (address == null) {
            throw new IllegalArgumentException("address cannot be null.");
        }

        return getBalanceOverRPC(address, false);
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
        if (address == null) {
            throw new IllegalArgumentException("address cannot be null.");
        }

        return getBalanceOverRPC(address, true);
    }

    /**
     * Returns the nonce of the specified address.
     *
     * @param address The address whose nonce is to be queried.
     * @return the result of the call.
     */
    public RpcResult<BigInteger> getNonce(byte[] address) throws InterruptedException {
        if (address == null) {
            throw new IllegalArgumentException("address cannot be null.");
        }

        return getNonceOverRPC(address, false);
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
        if (address == null) {
            throw new IllegalArgumentException("address cannot be null.");
        }

        return getNonceOverRPC(address, true);
    }

    private RpcResult<ReceiptHash> sendTransactionOverRPC(Transaction transaction, boolean verbose) throws InterruptedException {
        RpcPayload payload = new RpcPayloadBuilder()
            .method(RpcMethod.SEND_RAW_TRANSACTION)
            .params(Hex.encodeHexString(transaction.getSignedTransactionBytes()))
            .build();

        InternalRpcResult internalResult = this.rpc.call(payload, verbose);

        if (internalResult.success) {
            RpcOutputParser outputParser = new RpcOutputParser(internalResult.output);
            return RpcResult.successful(internalResult.output, new ReceiptHash(outputParser.resultAsByteArray().get()), internalResult.timeOfCall);
        } else {
            return RpcResult.unsuccessful(internalResult.error);
        }
    }

    private RpcResult<BigInteger> getBalanceOverRPC(byte[] address, boolean verbose) throws InterruptedException {
        RpcPayload payload = new RpcPayloadBuilder()
            .method(RpcMethod.GET_BALANCE)
            .params(Hex.encodeHexString(address))
            .build();

        InternalRpcResult internalResult = this.rpc.call(payload, verbose);

        if (internalResult.success) {
            RpcOutputParser outputParser = new RpcOutputParser(internalResult.output);
            return RpcResult.successful(internalResult.output, outputParser.resultAsBigInteger().get(), internalResult.timeOfCall);
        } else {
            return RpcResult.unsuccessful(internalResult.error);
        }
    }

    private RpcResult<BigInteger> getNonceOverRPC(byte[] address, boolean verbose) throws InterruptedException {
        RpcPayload payload = new RpcPayloadBuilder()
            .method(RpcMethod.GET_NONCE)
            .params(Hex.encodeHexString(address))
            .build();

        InternalRpcResult internalResult = this.rpc.call(payload, verbose);

        if (internalResult.success) {
            RpcOutputParser outputParser = new RpcOutputParser(internalResult.output);
            return RpcResult.successful(internalResult.output, outputParser.resultAsBigInteger().get(), internalResult.timeOfCall);
        } else {
            return RpcResult.unsuccessful(internalResult.error);
        }
    }

}
