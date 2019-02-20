package org.aion.harness.main;

import org.aion.harness.kernel.Transaction;
import org.aion.harness.misc.Assumptions;
import org.aion.harness.result.RPCResult;
import org.apache.commons.codec.binary.Hex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * A class that facilitates communication with the node.
 *
 * This class is not thread-safe.
 */
public final class RPC {

    public RPCResult sendTransaction(Transaction transaction) {
        return sendTransactionInternal(transaction, false);
    }

    public RPCResult sendTransactionVerbose(Transaction transaction) {
        return sendTransactionInternal(transaction, true);
    }

    public RPCResult getBalance(byte[] address) throws IOException, InterruptedException {
        if (address == null) {
            throw new IllegalArgumentException("address cannot be null.");
        }

        return getBalanceOverRPC(address, false);
    }

    public RPCResult getBalanceVerbose(byte[] address) throws IOException, InterruptedException {
        if (address == null) {
            throw new IllegalArgumentException("address cannot be null.");
        }

        return getBalanceOverRPC(address, true);
    }

    public RPCResult getNonce(byte[] address) throws IOException, InterruptedException {
        if (address == null) {
            throw new IllegalArgumentException("address cannot be null.");
        }

        return getNonceOverRPC(address, false);
    }

    public RPCResult getNonceVerbose(byte[] address) throws IOException, InterruptedException {
        if (address == null) {
            throw new IllegalArgumentException("address cannot be null.");
        }

        return getNonceOverRPC(address, true);
    }

    private RPCResult sendTransactionInternal(Transaction transaction, boolean verbose) {
        if (transaction == null) {
            throw new IllegalArgumentException("transaction cannot be null.");
        }

        try {
            System.out.println(Assumptions.LOGGER_BANNER + "Sending transaction to the node...");
            return sendTransactionOverRPC(transaction.getBytes(), verbose);
        } catch (Exception e) {
            return RPCResult.unsuccessful(Assumptions.PRODUCTION_ERROR_STATUS, "Error: " + ((e.getMessage() == null) ? e.toString() : e.getMessage()));
        }
    }

    private RPCResult sendTransactionOverRPC(byte[] signedTransaction, boolean verbose) throws IOException, InterruptedException {
        String data = "{\"jsonrpc\":\"2.0\",\"method\":\"eth_sendRawTransaction\",\"params\":[\"0x" + Hex.encodeHexString(signedTransaction) + "\"],\"id\":1}";

        ProcessBuilder processBuilder = new ProcessBuilder()
                .command("curl", "-X", "POST", "--data", data, Assumptions.IP + ":" + Assumptions.PORT);

        if (verbose) {
            processBuilder.inheritIO();
        }

        return callRPC(processBuilder.start(), System.currentTimeMillis());
    }

    private RPCResult getBalanceOverRPC(byte[] address, boolean verbose) throws  IOException, InterruptedException {
        String data = "{\"jsonrpc\":\"2.0\",\"method\":\"eth_getBalance\",\"params\":[\"0x" + Hex.encodeHexString(address) + "\", \"latest\"" + "],\"id\":1}";

        ProcessBuilder processBuilder = new ProcessBuilder()
                .command("curl", "-X", "POST", "--data", data, Assumptions.IP + ":" + Assumptions.PORT);

        if (verbose) {
            processBuilder.inheritIO();
        }

        return callRPC(processBuilder.start(), System.currentTimeMillis());
    }

    private RPCResult getNonceOverRPC(byte[] address, boolean verbose) throws IOException, InterruptedException {
        String data = "{\"jsonrpc\":\"2.0\",\"method\":\"eth_getTransactionCount\",\"params\":[\"0x" + Hex.encodeHexString(address) + "\", \"latest\"" + "],\"id\":1}";

        ProcessBuilder processBuilder = new ProcessBuilder()
                .command("curl", "-X", "POST", "--data", data, Assumptions.IP + ":" + Assumptions.PORT);

        if (verbose) {
            processBuilder.inheritIO();
        }

        return callRPC(processBuilder.start(), System.currentTimeMillis());
    }

    private RPCResult callRPC(Process process, long timestamp) throws IOException, InterruptedException {
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
            return RPCResult.unsuccessful(status, "RPC call failed!");
        } else if (response.contains("error")) {
            return RPCResult.unsuccessful(status, response.substring(response.indexOf("error") + 7, response.length() - 1));
        } else {
            return RPCResult.successful(response, timestamp);
        }
    }
}
