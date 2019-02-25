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
     * See {@link NodeListener#waitForTransactionToBeProcessed(byte[], long)}.
     *
     * @param transaction The transaction to send.
     * @return the result of this attempt to send the transaction.
     */
    public RpcResult<ReceiptHash> sendTransactionVerbose(Transaction transaction) throws InterruptedException {
        return callSendTransaction(transaction, true);
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

    private RpcResult<ReceiptHash> callSendTransaction(Transaction transaction, boolean verbose) throws InterruptedException {
        if (transaction == null) {
            throw new IllegalArgumentException("Cannot send a null transaction.");
        }

        RpcPayload payload = new RpcPayloadBuilder()
            .method(RpcMethod.SEND_RAW_TRANSACTION)
            .params(Hex.encodeHexString(transaction.getSignedTransactionBytes()))
            .build();

        InternalRpcResult internalResult = this.rpc.call(payload, verbose);

        if (internalResult.success) {
            RpcOutputParser outputParser = new RpcOutputParser(internalResult.output);
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
            .build();

        InternalRpcResult internalResult = this.rpc.call(payload, verbose);

        if (internalResult.success) {
            RpcOutputParser outputParser = new RpcOutputParser(internalResult.output);
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
            .build();

        InternalRpcResult internalResult = this.rpc.call(payload, verbose);

        if (internalResult.success) {
            RpcOutputParser outputParser = new RpcOutputParser(internalResult.output);
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

}
