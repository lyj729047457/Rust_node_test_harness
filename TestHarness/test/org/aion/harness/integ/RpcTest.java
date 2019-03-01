package org.aion.harness.integ;

import java.security.spec.InvalidKeySpecException;
import java.util.Optional;
import org.aion.harness.kernel.Address;
import org.aion.harness.kernel.PrivateKey;
import org.aion.harness.kernel.Transaction;
import org.aion.harness.main.Node;
import org.aion.harness.main.NodeFactory;
import org.aion.harness.main.NodeListener;
import org.aion.harness.main.RPC;
import org.aion.harness.main.types.FutureResult;
import org.aion.harness.main.types.Network;
import org.aion.harness.main.types.NodeConfigurationBuilder;
import org.aion.harness.main.types.NodeConfigurations;
import org.aion.harness.main.types.ReceiptHash;
import org.aion.harness.main.types.TransactionReceipt;
import org.aion.harness.misc.Assumptions;
import org.aion.harness.result.LogEventResult;
import org.aion.harness.result.RpcResult;
import org.aion.harness.result.Result;
import org.aion.harness.util.NodeFileManager;
import org.aion.harness.result.TransactionResult;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class RpcTest {
    private static boolean doFullInitialization = false;
    private static File nodeDirectory = NodeFileManager.getNodeDirectory();
    private static File kernelDirectory = NodeFileManager.getKernelDirectory();
    private static Address destination;
    private static Address preminedAddress;
    private static PrivateKey preminedPrivateKey;
    private static long energyLimit = 2_000_000;
    private static long energyPrice = 10_000_000_000L;

    private RPC rpc;
    private Node node;

    @Before
    public void setup() throws IOException, DecoderException, InvalidKeySpecException {
        destination = new Address(Hex.decodeHex("a0e9f9832d581246a9665f64599f405e8927993c6bef4be2776d91a66b466d30"));
        preminedPrivateKey = PrivateKey.fromBytes(Hex.decodeHex(Assumptions.PREMINED_PRIVATE_KEY));
        preminedAddress = preminedPrivateKey.getAddress();
        deleteInitializationDirectories();
        this.node = NodeFactory.getNewNodeInstance(NodeFactory.NodeType.JAVA_NODE);
        this.node.configure(NodeConfigurationBuilder.defaultConfigurations());
        this.rpc = new RPC("127.0.0.1", "8545");
    }

    @After
    public void tearDown() throws IOException {
        shutdownNodeIfRunning();
        deleteInitializationDirectories();
        deleteLogs();
        this.node = null;
        this.rpc = null;
    }

    @Test
    public void testSendValueViaRPC() throws InterruptedException {
        initializeNodeWithChecks();
        Result result = this.node.start();
        System.out.println("Start result = " + result);

        assertTrue(result.success);
        assertTrue(this.node.isAlive());

        TransactionResult transactionResult = constructTransaction(
            preminedPrivateKey,
            destination,
            BigInteger.ONE,
            BigInteger.ZERO);

        assertTrue(transactionResult.success);

        RpcResult<ReceiptHash> rpcResult = this.rpc.sendTransaction(transactionResult.getTransaction());
        System.out.println("Rpc result = " + rpcResult);
        assertTrue(rpcResult.success);

        result = this.node.stop();
        System.out.println("Stop result = " + result);

        assertTrue(result.success);
        assertFalse(this.node.isAlive());
    }

    @Test
    public void testGetBalanceViaRPC() throws Exception {
        initializeNodeWithChecks();
        Result result = this.node.start();
        System.out.println("Start result = " + result);

        assertTrue(result.success);
        assertTrue(this.node.isAlive());

        RpcResult<BigInteger> rpcResult = this.rpc.getBalance(destination.getAddressBytes());
        System.out.println("Rpc result = " + rpcResult);
        assertTrue(rpcResult.success);

        result = this.node.stop();
        System.out.println("Stop result = " + result);

        assertTrue(result.success);
        assertFalse(this.node.isAlive());
    }

    @Test
    public void testSendValueWithIncorrectNonceViaRPC() throws InterruptedException {
        initializeNodeWithChecks();
        Result result = this.node.start();
        System.out.println("Start result = " + result);

        assertTrue(result.success);
        assertTrue(this.node.isAlive());

        TransactionResult transactionResult = constructTransaction(
            preminedPrivateKey,
            destination,
            BigInteger.ONE,
            BigInteger.valueOf(100));

        assertTrue(transactionResult.success);

        RpcResult<ReceiptHash> rpcResult = this.rpc.sendTransaction(transactionResult.getTransaction());
        System.out.println("Rpc result = " + rpcResult);
        assertTrue(rpcResult.success);

        result = this.node.stop();
        System.out.println("Stop result = " + result);

        assertTrue(result.success);
        assertFalse(this.node.isAlive());
    }

    @Test
    public void testSendNegativeValueViaRPC() throws InterruptedException {
        initializeNodeWithChecks();
        Result result = this.node.start();
        System.out.println("Start result = " + result);

        assertTrue(result.success);
        assertTrue(this.node.isAlive());

        TransactionResult transactionResult = constructTransaction(
            preminedPrivateKey,
            destination,
            BigInteger.valueOf(-1),
            BigInteger.ZERO);

        assertTrue(transactionResult.success);

        RpcResult<ReceiptHash> rpcResult = this.rpc.sendTransaction(transactionResult.getTransaction());
        System.out.println("Rpc result = " + rpcResult);
        assertTrue(rpcResult.success);

        result = this.node.stop();
        System.out.println("Stop result = " + result);

        assertTrue(result.success);
        assertFalse(this.node.isAlive());
    }

    @Test
    public void testSendZeroValueViaRPC() throws InterruptedException {
        initializeNodeWithChecks();
        Result result = this.node.start();
        System.out.println("Start result = " + result);

        assertTrue(result.success);
        assertTrue(this.node.isAlive());

        TransactionResult transactionResult = constructTransaction(
            preminedPrivateKey,
            destination,
            BigInteger.ZERO,
            BigInteger.ZERO);

        assertTrue(transactionResult.success);

        RpcResult<ReceiptHash> rpcResult = this.rpc.sendTransaction(transactionResult.getTransaction());
        System.out.println("Rpc result = " + rpcResult);
        assertTrue(rpcResult.success);

        result = this.node.stop();
        System.out.println("Stop result = " + result);

        assertTrue(result.success);
        assertFalse(this.node.isAlive());
    }

    @Test
    public void testSendMultipleValueTransfersViaRPC() throws InterruptedException {
        initializeNodeWithChecks();
        Result result = this.node.start();
        System.out.println("Start result = " + result);

        assertTrue(result.success);
        assertTrue(this.node.isAlive());

        TransactionResult transactionResult = constructTransaction(
            preminedPrivateKey,
            destination,
            BigInteger.ONE,
            BigInteger.ZERO);

        assertTrue(transactionResult.success);

        RpcResult<ReceiptHash> rpcResult = this.rpc.sendTransaction(transactionResult.getTransaction());
        assertTrue(rpcResult.success);

        transactionResult = constructTransaction(
            preminedPrivateKey,
            destination,
            BigInteger.ONE,
            BigInteger.ONE);

        assertTrue(transactionResult.success);
        rpcResult = this.rpc.sendTransaction(transactionResult.getTransaction());
        System.out.println("Rpc result = " + rpcResult);
        assertTrue(rpcResult.success);

        result = this.node.stop();
        System.out.println("Stop result = " + result);

        assertTrue(result.success);
        assertFalse(this.node.isAlive());
    }

    @Test
    public void testSendTransactionWhenNoNodeIsAlive() throws InterruptedException {
        assertFalse(this.node.isAlive());
        TransactionResult transactionResult = constructTransaction(
            preminedPrivateKey,
            destination,
            BigInteger.ZERO,
            BigInteger.ZERO);

        assertTrue(transactionResult.success);

        RpcResult<ReceiptHash> rpcResult = this.rpc.sendTransaction(transactionResult.getTransaction());
        System.out.println("Rpc result = " + rpcResult);
        assertFalse(rpcResult.success);
    }

    @Test
    public void testSendValueToInvalidAddress() throws InterruptedException {
        initializeNodeWithChecks();
        Result result = this.node.start();
        System.out.println("Start result = " + result);

        assertTrue(result.success);
        assertTrue(this.node.isAlive());

        TransactionResult transactionResult = constructTransaction(
            preminedPrivateKey,
            destination,
            BigInteger.ONE,
            BigInteger.ZERO);

        assertTrue(transactionResult.success);

        RpcResult<ReceiptHash> rpcResult = this.rpc.sendTransaction(transactionResult.getTransaction());
        System.out.println("Rpc result = " + rpcResult);
        assertTrue(rpcResult.success);

        result = this.node.stop();
        System.out.println("Stop result = " + result);

        assertTrue(result.success);
        assertFalse(this.node.isAlive());
    }

    @Test
    public void testSendValueWithInsufficientBalance() throws Exception {
        byte[] badKey = Hex.decodeHex("223f19370d95582055bd8072cf3ffd635d2712a7171e4888091a060b9f4f63d5");
        PrivateKey badPrivateKey = PrivateKey.fromBytes(badKey);

        initializeNodeWithChecks();
        Result result = this.node.start();
        System.out.println("Start result = " + result);

        assertTrue(result.success);
        assertTrue(this.node.isAlive());

        TransactionResult transactionResult = constructTransaction(
            badPrivateKey,
            destination,
            BigInteger.ONE,
            BigInteger.ZERO);

        assertTrue(transactionResult.success);

        RpcResult<ReceiptHash> rpcResult = this.rpc.sendTransaction(transactionResult.getTransaction());
        System.out.println("Rpc result = " + rpcResult);
        assertFalse(rpcResult.success);
        assertTrue(rpcResult.error.contains("Transaction dropped"));

        result = this.node.stop();
        System.out.println("Stop result = " + result);

        assertTrue(result.success);
        assertFalse(this.node.isAlive());
    }

    @Test
    public void testBalanceTransferAndCheck() throws Exception {
        initializeNodeWithChecks();
        Result result = this.node.start();
        System.out.println("Start result = " + result);

        assertTrue(result.success);
        assertTrue(this.node.isAlive());

        BigInteger transferValue = BigInteger.valueOf(50);

        // check balance before
        RpcResult<BigInteger> rpcResult = this.rpc.getBalance(destination.getAddressBytes());
        System.out.println("Rpc result = " + rpcResult);
        assertTrue(rpcResult.success);
        assertEquals(BigInteger.ZERO, rpcResult.getResult());

        // transfer and wait
        doBalanceTransfer(transferValue);

        // check balance after
        rpcResult = this.rpc.getBalance(destination.getAddressBytes());
        System.out.println("Rpc result = " + rpcResult);
        assertTrue(rpcResult.success);
        assertEquals(transferValue, rpcResult.getResult());
    }

    @Test
    public void testBalanceTransferZeroBalanceAndCheck() throws Exception {
        initializeNodeWithChecks();
        Result result = this.node.start();
        System.out.println("Start result = " + result);

        assertTrue(result.success);
        assertTrue(this.node.isAlive());

        BigInteger transferValue = BigInteger.ZERO;

        // check balance before
        RpcResult<BigInteger> rpcResult = this.rpc.getBalance(destination.getAddressBytes());
        System.out.println("Rpc result = " + rpcResult);
        assertTrue(rpcResult.success);
        assertEquals(BigInteger.ZERO, rpcResult.getResult());

        // transfer and wait
        doBalanceTransfer(transferValue);

        // check balance after
        rpcResult = this.rpc.getBalance(destination.getAddressBytes());
        System.out.println("Rpc result = " + rpcResult);
        assertTrue(rpcResult.success);
        assertEquals(BigInteger.ZERO, rpcResult.getResult());
    }

    @Test
    public void testBalanceTransferNegativeBalanceAndCheck() throws Exception {
        // When someone tries to transfer a negative value(BigInteger), the positive representation of
        // that BigInteger will be the transfer value. This should be something handled by the server.
        initializeNodeWithChecks();
        Result result = this.node.start();
        System.out.println("Start result = " + result);

        assertTrue(result.success);
        assertTrue(this.node.isAlive());

        BigInteger negativeTransferValue = BigInteger.valueOf(-10);
        BigInteger positiveRepresentation = new BigInteger(1, negativeTransferValue.toByteArray());

        // check balance before
        RpcResult<BigInteger> rpcResult = this.rpc.getBalance(destination.getAddressBytes());
        System.out.println("Rpc result = " + rpcResult);
        assertTrue(rpcResult.success);
        assertEquals(BigInteger.ZERO, rpcResult.getResult());

        // transfer and wait
        doBalanceTransfer(negativeTransferValue);

        // check balance after - for positive representation
        rpcResult = this.rpc.getBalance(destination.getAddressBytes());
        System.out.println("Rpc result = " + rpcResult);
        assertTrue(rpcResult.success);
        assertEquals(positiveRepresentation, rpcResult.getResult());
    }

    @Test
    public void testGetNonce() throws Exception {
        initializeNodeWithChecks();
        Result result = this.node.start();
        System.out.println("Start result = " + result);

        assertTrue(result.success);
        assertTrue(this.node.isAlive());

        // check nonce before
        RpcResult<BigInteger> rpcResult = this.rpc.getNonce(preminedAddress.getAddressBytes());
        System.out.println("Rpc result = " + rpcResult);

        BigInteger nonceBefore = rpcResult.getResult();
        System.out.println("nonce is: " + nonceBefore);

        // do a transfer and wait
        doBalanceTransfer(BigInteger.ONE);

        // check nonce after
        rpcResult = this.rpc.getNonce(preminedAddress.getAddressBytes());
        System.out.println("Rpc result = " + rpcResult);

        BigInteger nonceAfter = rpcResult.getResult();
        System.out.println("nonce is: " + nonceAfter);

        assertEquals(nonceBefore.add(BigInteger.ONE), nonceAfter);
    }

    @Test
    public void testGetTransactionReceipt() throws InterruptedException {
        initializeNodeWithChecks();
        Result result = this.node.start();
        System.out.println("Start result = " + result);

        assertTrue(result.success);
        assertTrue(this.node.isAlive());

        TransactionResult transactionResult = constructTransaction(
            preminedPrivateKey,
            destination,
            BigInteger.ONE,
            BigInteger.ZERO);

        assertTrue(transactionResult.success);

        NodeListener listener = new NodeListener();
        Transaction transaction = transactionResult.getTransaction();

        FutureResult<LogEventResult> futureResult = listener.listenForTransactionToBeProcessed(transaction.getTransactionHash(), 2, TimeUnit.MINUTES);

        RpcResult<ReceiptHash> rpcResult = this.rpc.sendTransaction(transaction);
        System.out.println("Rpc result = " + rpcResult);
        assertTrue(rpcResult.success);

        LogEventResult waitResult = futureResult.get();
        System.out.println("Listener result = " + waitResult);
        assertTrue(waitResult.eventWasObserved());

        RpcResult<TransactionReceipt> receiptResult = this.rpc.getTransactionReceipt(rpcResult.getResult());
        System.out.println("Receipt result = " + receiptResult);
        assertTrue(receiptResult.success);

        // Assert the fields in the receipt are as expected.
        TransactionReceipt receipt = receiptResult.getResult();

        assertEquals(energyLimit, receipt.getTransactionEnergyLimit());
        assertEquals(energyPrice, receipt.getTransactionEnergyPrice());
        assertEquals(21_000L, receipt.getTransactionEnergyConsumed());
        assertEquals(21_000L, receipt.getCumulativeEnergyConsumed());
        assertEquals(0, receipt.getTransactionIndex());
        assertArrayEquals(transaction.getTransactionHash(), receipt.getTransactionHash());
        assertEquals(BigInteger.ONE, receipt.getBlockNumber());
        assertFalse(receipt.getAddressOfDeployedContract().isPresent());
        assertEquals(preminedAddress, receipt.getTransactionSender());

        Optional<Address> transactionDestination = receipt.getTransactionDestination();
        assertTrue(transactionDestination.isPresent());
        assertEquals(destination, transactionDestination.get());

        result = this.node.stop();
        System.out.println("Stop result = " + result);

        assertTrue(result.success);
        assertFalse(this.node.isAlive());
    }

    @Test
    public void testGetTransactionReceiptFromContractDeploy() throws InterruptedException {
        initializeNodeWithChecks();
        Result result = this.node.start();
        System.out.println("Start result = " + result);

        assertTrue(result.success);
        assertTrue(this.node.isAlive());

        TransactionResult transactionResult = constructTransaction(
            preminedPrivateKey,
            null,
            BigInteger.ONE,
            BigInteger.ZERO);

        assertTrue(transactionResult.success);

        NodeListener listener = new NodeListener();
        Transaction transaction = transactionResult.getTransaction();

        FutureResult<LogEventResult> futureResult = listener.listenForTransactionToBeProcessed(transaction.getTransactionHash(), 2, TimeUnit.MINUTES);

        RpcResult<ReceiptHash> rpcResult = this.rpc.sendTransaction(transaction);
        System.out.println("Rpc result = " + rpcResult);
        assertTrue(rpcResult.success);

        LogEventResult waitResult = futureResult.get();
        System.out.println("Listener result = " + waitResult);
        assertTrue(waitResult.eventWasObserved());

        RpcResult<TransactionReceipt> receiptResult = this.rpc.getTransactionReceipt(rpcResult.getResult());
        System.out.println("Receipt result = " + receiptResult);
        assertTrue(receiptResult.success);

        // Assert the fields in the receipt are as expected.
        TransactionReceipt receipt = receiptResult.getResult();

        assertEquals(energyLimit, receipt.getTransactionEnergyLimit());
        assertEquals(energyPrice, receipt.getTransactionEnergyPrice());
        assertEquals(0, receipt.getTransactionIndex());
        assertArrayEquals(transaction.getTransactionHash(), receipt.getTransactionHash());
        assertEquals(BigInteger.ONE, receipt.getBlockNumber());
        assertTrue(receipt.getAddressOfDeployedContract().isPresent());
        assertEquals(preminedAddress, receipt.getTransactionSender());
        assertFalse(receipt.getTransactionDestination().isPresent());

        result = this.node.stop();
        System.out.println("Stop result = " + result);

        assertTrue(result.success);
        assertFalse(this.node.isAlive());
    }

    @Test
    public void testGetTransactionReceiptFromNonExistentReceiptHash() throws InterruptedException {
        initializeNodeWithChecks();
        Result result = this.node.start();
        System.out.println("Start result = " + result);

        assertTrue(result.success);
        assertTrue(this.node.isAlive());

        TransactionResult transactionResult = constructTransaction(
            preminedPrivateKey,
            destination,
            BigInteger.ONE,
            BigInteger.ZERO);

        assertTrue(transactionResult.success);

        ReceiptHash receiptHash = new ReceiptHash(new byte[32]);

        RpcResult<TransactionReceipt> receiptResult = this.rpc.getTransactionReceipt(receiptHash);
        System.out.println("Receipt result = " + receiptResult);
        assertFalse(receiptResult.success);

        result = this.node.stop();
        System.out.println("Stop result = " + result);

        assertTrue(result.success);
        assertFalse(this.node.isAlive());
    }

    /**
     * This test does not assert anything about the syncing process because there's not really
     * anything meaningful to latch on to. It is here so that we can visually monitor this output.
     */
    @Test
    public void testSyncingToNetwork() {
        NodeConfigurations configurations = new NodeConfigurationBuilder()
            .network(Network.MAINNET)
            .build();

        this.node.configure(configurations);

        initializeNodeWithChecks();
        Result result = this.node.start();
        System.out.println("Start result = " + result);

        assertTrue(result.success);
        assertTrue(this.node.isAlive());

        Result syncResult = this.rpc.waitForSyncToComplete(TimeUnit.SECONDS.toMillis(10), TimeUnit.SECONDS.toMillis(40));
        System.out.println("Sync result: " + syncResult);

        result = this.node.stop();
        System.out.println("Stop result = " + result);

        assertTrue(result.success);
        assertFalse(this.node.isAlive());
    }

    private void doBalanceTransfer(BigInteger transferValue) throws InterruptedException {
        TransactionResult transactionResult = constructTransaction(
            preminedPrivateKey,
            destination,
            transferValue,
            BigInteger.ZERO);

        Transaction transaction = transactionResult.getTransaction();

        FutureResult<LogEventResult> futureResult = new NodeListener().listenForTransactionToBeProcessed(
            transaction.getTransactionHash(),
            1,
            TimeUnit.MINUTES);

        RpcResult<ReceiptHash> result = this.rpc.sendTransaction(transaction);
        System.out.println("Rpc result = " + result);
        assertTrue(result.success);

        LogEventResult eventResult = futureResult.get();
        assertTrue(eventResult.eventWasObserved());
    }

    private TransactionResult constructTransaction(PrivateKey senderPrivateKey, Address destination, BigInteger value, BigInteger nonce) {
        return Transaction
            .buildAndSignTransaction(senderPrivateKey, nonce, destination, new byte[0], energyLimit, energyPrice, value);
    }

    private Result initializeNode() {
        if (doFullInitialization) {
            Result result = this.node.buildKernel();
            if (!result.success) {
                return result;
            }
        }

        return this.node.initializeKernel();
    }

    private void initializeNodeWithChecks() {
        Result result = initializeNode();
        assertTrue(result.success);

        // verify the node directory was created.
        assertTrue(nodeDirectory.exists());
        assertTrue(nodeDirectory.isDirectory());

        // veirfy the node directory contains the aion directory.
        File[] nodeDirectoryEntries = nodeDirectory.listFiles();
        assertNotNull(nodeDirectoryEntries);
        assertEquals(1, nodeDirectoryEntries.length);
        assertEquals(kernelDirectory, nodeDirectoryEntries[0]);
        assertTrue(nodeDirectoryEntries[0].isDirectory());
    }

    private static void deleteInitializationDirectories() throws IOException {
        if (nodeDirectory.exists()) {
            FileUtils.deleteDirectory(nodeDirectory);
        }
        if (kernelDirectory.exists()) {
            FileUtils.deleteDirectory(kernelDirectory);
        }
    }

    private static void deleteLogs() throws IOException {
        FileUtils.deleteDirectory(NodeFileManager.getLogsDirectory());
    }

    private void shutdownNodeIfRunning() {
        if ((this.node != null) && (this.node.isAlive())) {
            Result result = this.node.stop();
            System.out.println("shutdownNodeIfRunning Stop result = " + result);

            assertTrue(result.success);
            assertFalse(this.node.isAlive());
        }
    }
}
