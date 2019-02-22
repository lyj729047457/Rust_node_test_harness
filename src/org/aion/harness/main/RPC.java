package org.aion.harness.main;

import org.aion.harness.kernel.Transaction;
import org.aion.harness.misc.Assumptions;
import org.aion.harness.result.RpcResult;
import org.apache.commons.codec.binary.Hex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * A class that facilitates communication with the node via the kernel's RPC server.
 *
 * This class interacts directly with the RPC endpoints in the kernel and does not go through the
 * Java or Web3 APIs, for example.
 *
 * This class is not thread-safe.
 */
public final class RPC {

    /**
     * Sends the specified transaction to the node.
     *
     * @param transaction The transaction to send.
     * @return the result of this attempt to send the transaction.
     */
    public RpcResult sendTransaction(Transaction transaction) {
        return sendTransactionInternal(transaction, false);
    }

    /**
     * Sends the specified transaction to the node.
     *
     * Displays the I/O of the attempt to hit the RPC endpoint.
     *
     * @param transaction The transaction to send.
     * @return the result of this attempt to send the transaction.
     */
    public RpcResult sendTransactionVerbose(Transaction transaction) {
        return sendTransactionInternal(transaction, true);
    }

    /**
     * Returns the balance of the specified address.
     *
     * @param address The address whose balance is to be queried.
     * @return the result of the call.
     */
    public RpcResult getBalance(byte[] address) throws IOException, InterruptedException {
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
    public RpcResult getBalanceVerbose(byte[] address) throws IOException, InterruptedException {
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
    public RpcResult getNonce(byte[] address) throws IOException, InterruptedException {
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
    public RpcResult getNonceVerbose(byte[] address) throws IOException, InterruptedException {
        if (address == null) {
            throw new IllegalArgumentException("address cannot be null.");
        }

        return getNonceOverRPC(address, true);
    }

    private RpcResult sendTransactionInternal(Transaction transaction, boolean verbose) {
        if (transaction == null) {
            throw new IllegalArgumentException("transaction cannot be null.");
        }

        try {
            System.out.println(Assumptions.LOGGER_BANNER + "Sending transaction to the node...");
            return sendTransactionOverRPC(transaction.getSignedTransactionBytes(), verbose);
        } catch (Exception e) {
            return RpcResult
                .unsuccessful(Assumptions.PRODUCTION_ERROR_STATUS, "Error: " + ((e.getMessage() == null) ? e.toString() : e.getMessage()));
        }
    }

    private RpcResult sendTransactionOverRPC(byte[] signedTransaction, boolean verbose) throws IOException, InterruptedException {
        String data = "{\"jsonrpc\":\"2.0\",\"method\":\"eth_sendRawTransaction\",\"params\":[\"0x" + Hex.encodeHexString(signedTransaction) + "\"],\"id\":1}";

        ProcessBuilder processBuilder = new ProcessBuilder()
                .command("curl", "-X", "POST", "--data", data, Assumptions.IP + ":" + Assumptions.PORT);

        if (verbose) {
            processBuilder.inheritIO();
        }

        return callRPC(processBuilder.start(), System.currentTimeMillis());
    }

    private RpcResult getBalanceOverRPC(byte[] address, boolean verbose) throws  IOException, InterruptedException {
        String data = "{\"jsonrpc\":\"2.0\",\"method\":\"eth_getBalance\",\"params\":[\"0x" + Hex.encodeHexString(address) + "\", \"latest\"" + "],\"id\":1}";

        ProcessBuilder processBuilder = new ProcessBuilder()
                .command("curl", "-X", "POST", "--data", data, Assumptions.IP + ":" + Assumptions.PORT);

        if (verbose) {
            processBuilder.inheritIO();
        }

        return callRPC(processBuilder.start(), System.currentTimeMillis());
    }

    private RpcResult getNonceOverRPC(byte[] address, boolean verbose) throws IOException, InterruptedException {
        String data = "{\"jsonrpc\":\"2.0\",\"method\":\"eth_getTransactionCount\",\"params\":[\"0x" + Hex.encodeHexString(address) + "\", \"latest\"" + "],\"id\":1}";

        ProcessBuilder processBuilder = new ProcessBuilder()
                .command("curl", "-X", "POST", "--data", data, Assumptions.IP + ":" + Assumptions.PORT);

        if (verbose) {
            processBuilder.inheritIO();
        }

        return callRPC(processBuilder.start(), System.currentTimeMillis());
    }

    private RpcResult callRPC(Process process, long timestamp) throws IOException, InterruptedException {
        int status = process.waitFor();

        String line;
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }

        String response = stringBuilder.toString();

        if (status != 0) {
            return RpcResult.unsuccessful(status, "RPC call failed!");
        } else if (response.contains("error")) {
            return RpcResult
                .unsuccessful(status, response.substring(response.indexOf("error") + 7, response.length() - 1));
        } else {
            return RpcResult.successful(response, timestamp);
        }
    }
}
