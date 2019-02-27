package org.aion.harness;

import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.aion.avm.api.ABIEncoder;
import org.aion.avm.core.dappreading.JarBuilder;
import org.aion.avm.core.util.CodeAndArguments;
import org.aion.harness.contracts.HelloWorld;
import org.aion.harness.kernel.Address;
import org.aion.harness.kernel.PrivateKey;
import org.aion.harness.kernel.Transaction;
import org.aion.harness.main.Node;
import org.aion.harness.main.NodeFactory;
import org.aion.harness.main.NodeFactory.NodeType;
import org.aion.harness.main.NodeListener;
import org.aion.harness.main.RPC;
import org.aion.harness.main.types.Network;
import org.aion.harness.main.types.NodeConfigurationBuilder;
import org.aion.harness.main.types.NodeConfigurations;
import org.aion.harness.main.types.ReceiptHash;
import org.aion.harness.main.types.TransactionReceipt;
import org.aion.harness.result.LogEventResult;
import org.aion.harness.result.Result;
import org.aion.harness.result.RpcResult;
import org.aion.harness.result.TransactionResult;
import org.apache.commons.codec.binary.Hex;

public class Example1 {
    private static final long CREATE_ENERGY = 5_000_000;
    private static final long CALL_ENERGY = 2_000_000;
    private static final long ENERGY_PRICE = 10_000_000_000L;

    private static Node node;

    // Stops the running node if it is alive.
    private static void cleanup() {
        if ((node != null) && (node.isAlive())) {
            node.stop();
        }
    }

    // Grabs the bytes of the HelloWorld as a jar file.
    private static byte[] getContractBytes() {
        return new CodeAndArguments(JarBuilder.buildJarForMainAndClasses(HelloWorld.class), new byte[0]).encodeToBytes();
    }

    // Makes a call into the 'hello' method of the contract.
    private static byte[] getCallBytes() {
        return ABIEncoder.encodeMethodArguments("hello");
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("USAGE: < account address, account private key, path to kernel tar.bz2 file >");
            System.exit(1);
        }

        Address preminedAddress = new Address(Hex.decodeHex(args[0]));
        PrivateKey preminedPrivateKey = new PrivateKey(Hex.decodeHex(args[1]));

        // Configure the node to connect to the avm testnet and point to the built kernel tar file.
        NodeConfigurations configurations = new NodeConfigurationBuilder()
            .network(Network.AVMTESTNET)
            .directoryOfBuiltKernel(args[2])
            .build();

        // Grab the Java node and start it up, preserving the database if this has been called before.
        node = NodeFactory.getNewNodeInstance(NodeType.JAVA_NODE);
        node.configure(configurations);
        node.initializeKernelAndPreserveDatabase();

        if (!node.start().success) {
            System.out.println("Failed to start the node!");
            System.exit(1);
        }

        // Wait for the node to sync up with the avmtestnet network.
        NodeListener listener = new NodeListener();

        long updateDelay = TimeUnit.SECONDS.toMillis(30);
        long syncTimeout = TimeUnit.HOURS.toMillis(2);

        Result result = listener.waitForSyncToComplete(updateDelay, syncTimeout);
        if (!result.success) {
            System.out.println("Failed to sync to network: " + result);
            cleanup();
            System.exit(1);
        }

        // Grab the current nonce of the pre-mined account so we can send a transaction.
        RPC rpc = new RPC();

        RpcResult<BigInteger> nonceResult = rpc.getNonce(preminedAddress.getAddressBytes());
        if (!nonceResult.success) {
            System.out.println("Failed to get pre-mined account nonce!");
            cleanup();
            System.exit(1);
        }

        BigInteger nonce = nonceResult.getResult();

        // Construct the contract creation transaction.
        TransactionResult buildResult = Transaction.buildAndSignAvmTransaction(
            preminedPrivateKey,
            nonce,
            null,
            getContractBytes(),
            CREATE_ENERGY,
            ENERGY_PRICE,
            BigInteger.ZERO);

        if (!buildResult.success) {
            System.out.println("Failed to build the contract!");
            cleanup();
            System.exit(1);
        }

        // Deploy the contract.
        Transaction transaction = buildResult.getTransaction();
        RpcResult<ReceiptHash> sendResult = rpc.sendTransaction(transaction);
        if (!sendResult.success) {
            System.out.println("Failed to send the deploy transaction!");
            cleanup();
            System.exit(1);
        }

        // Wait for the transaction to be processed.
        long sendTimeout = TimeUnit.MINUTES.toMillis(1);
        LogEventResult waitResult = listener.waitForTransactionToBeProcessed(transaction.getTransactionHash(), sendTimeout);
        if (!waitResult.eventWasObserved()) {
            System.out.println("Send transaction was never processed!");
            cleanup();
            System.exit(1);
        }

        // Get the transaction receipt.
        RpcResult<TransactionReceipt> receiptResult = rpc.getTransactionReceipt(sendResult.getResult());
        if (!receiptResult.success) {
            System.out.println("Failed to get the transaction receipt!");
            cleanup();
            System.exit(1);
        }

        // Get the address of the newly deployed contract from the receipt.
        Optional<Address> deployedAddress = receiptResult.getResult().getAddressOfDeployedContract();
        if (!deployedAddress.isPresent()) {
            System.out.println("No newly deployed contract address was returned!");
            cleanup();
            System.exit(1);
        }

        Address contract = deployedAddress.get();

        // Verify that the pre-mined account's nonce increased.
        BigInteger expectedNonce = nonce.add(BigInteger.ONE);
        BigInteger actualNonce = rpc.getNonce(preminedAddress.getAddressBytes()).getResult();

        if (!expectedNonce.equals(actualNonce)) {
            System.out.println("The sender's nonce did not increment!");
            cleanup();
            System.exit(1);
        }

        // Print off how much time it took for the transaction to be processed.
        long sendTime = sendResult.timeOfCallInMillis;
        long processTime = waitResult.timeOfObservationInMilliseconds();
        System.out.println("It took apx. " + TimeUnit.MILLISECONDS.toSeconds(processTime - sendTime) + " second(s) to process the deploy transaction!");

        // Prepare a transaction that will call into the deployed contract.
        buildResult = Transaction.buildAndSignAvmTransaction(
            preminedPrivateKey,
            actualNonce,
            contract,
            getCallBytes(),
            CALL_ENERGY,
            ENERGY_PRICE,
            BigInteger.ZERO);

        if (!buildResult.success) {
            System.out.println("Failed to construct the contract call transaction!");
            cleanup();
            System.exit(1);
        }

        // Make the contract call
        transaction = buildResult.getTransaction();
        RpcResult<ReceiptHash> callResult = rpc.sendTransaction(transaction);
        if (!callResult.success) {
            System.out.println("Failed to send the call transaction!");
            cleanup();
            System.exit(1);
        }

        // Wait for the call event to be processed.
        waitResult = listener.waitForTransactionToBeProcessed(transaction.getTransactionHash(), sendTimeout);
        if (!waitResult.eventWasObserved()) {
            System.out.println("Call transaction was never processed!");
            cleanup();
            System.exit(1);
        }

        // Once again, check the pre-mined account's nonce.
        expectedNonce = expectedNonce.add(BigInteger.ONE);
        actualNonce = rpc.getNonce(preminedAddress.getAddressBytes()).getResult();

        if (!expectedNonce.equals(actualNonce)) {
            System.out.println("The sender's nonce did not increment!");
            cleanup();
            System.exit(1);
        }

        // Print off how much time it took for the transaction to be processed.
        long callTime = callResult.timeOfCallInMillis;
        processTime = waitResult.timeOfObservationInMilliseconds();
        System.out.println("It took apx. " + TimeUnit.MILLISECONDS.toSeconds(processTime - callTime) + " second(s) to process the call transaction!");

        cleanup();
    }

}
