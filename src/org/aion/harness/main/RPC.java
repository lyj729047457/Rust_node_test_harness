package org.aion.harness.main;

import java.util.Optional;
import org.aion.harness.kernel.Transaction;
import org.aion.harness.main.tools.RpcOutputParser;
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
            return RpcResult.unsuccessful(((e.getMessage() == null) ? e.toString() : e.getMessage()));
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

        RpcResult errorResult;
        if (status == 0) {

            // There could still be a kernel-side error. If there is, this method will produce it.
            // Otherwise, this method returns null.
            errorResult = extractKernelError(response);
            return (errorResult == null) ? RpcResult.successful(response, timestamp) : errorResult;

        } else {

            errorResult = extractKernelError(response);
            return (errorResult == null) ? RpcResult.unsuccessful("RPC exit code: " + status) : errorResult;
        }

    }

    /**
     * Extracts any error returned by the kernel in the provided rpc output and returns that error
     * as an unsuccessful rpc result object.
     *
     * Otherwise, if no kernel error was reported, this method returns null.
     *
     * @param output The rpc output.
     * @return null if no error, otherwise an unsuccessful rpc result.
     */
    private RpcResult extractKernelError(String output) {
        RpcOutputParser outputParser = (output.isEmpty()) ? null : new RpcOutputParser(output);

        // If output was empty or there is no 'error' attribute we can return now.
        if ((outputParser == null) || (!outputParser.hasErrorAttribute())) {
            return null;
        }

        Optional<String> kernelError = outputParser.errorAsString();

        if (kernelError.isPresent()) {
            String kernelErrorString = kernelError.get();
            RpcOutputParser errorParser = new RpcOutputParser(kernelErrorString);
            Optional<String> kernelErrorData = errorParser.dataAsString();

            if (kernelErrorData.isPresent()) {
                return RpcResult.unsuccessful("RPC call failed due to: " + kernelErrorData.get());
            } else {
                // There may still be a 'message' attribute with error information.
                Optional<String> kernelMessageString = errorParser.messageAsString();
                if (kernelMessageString.isPresent()) {
                    return RpcResult.unsuccessful("RPC call failed due to: " + kernelMessageString.get());
                } else {
                    return RpcResult.unsuccessful("RPC call failued due to: unknown cause.");
                }
            }

        }

        return null;
    }
}
