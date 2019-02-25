package org.aion.harness.integ;

import org.aion.harness.kernel.Address;
import org.aion.harness.kernel.PrivateKey;
import org.aion.harness.kernel.Transaction;
import org.aion.harness.main.Node;
import org.aion.harness.main.NodeFactory;
import org.aion.harness.main.NodeListener;
import org.aion.harness.main.RPC;
import org.aion.harness.main.types.ReceiptHash;
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

    private RPC rpc;
    private Node node;

    @Before
    public void setup() throws IOException, DecoderException {
        preminedAddress = new Address(Hex.decodeHex(Assumptions.PREMINED_ADDRESS));
        destination = new Address(Hex.decodeHex("a0e9f9832d581246a9665f64599f405e8927993c6bef4be2776d91a66b466d30"));
        preminedPrivateKey = new PrivateKey(Hex.decodeHex(Assumptions.PREMINED_PRIVATE_KEY));
        deleteInitializationDirectories();
        this.node = NodeFactory.getNewNodeInstance(NodeFactory.NodeType.JAVA_NODE);
        this.rpc = new RPC();
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
    public void testSendValueViaRPC() {
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

        RpcResult<ReceiptHash> rpcResult = this.rpc.sendTransaction(transactionResult.getTransaction().get());
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
    public void testSendValueWithIncorrectNonceViaRPC() {
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

        RpcResult<ReceiptHash> rpcResult = this.rpc.sendTransaction(transactionResult.getTransaction().get());
        System.out.println("Rpc result = " + rpcResult);
        assertTrue(rpcResult.success);

        result = this.node.stop();
        System.out.println("Stop result = " + result);

        assertTrue(result.success);
        assertFalse(this.node.isAlive());
    }

    @Test
    public void testSendNegativeValueViaRPC() {
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

        RpcResult<ReceiptHash> rpcResult = this.rpc.sendTransaction(transactionResult.getTransaction().get());
        System.out.println("Rpc result = " + rpcResult);
        assertTrue(rpcResult.success);

        result = this.node.stop();
        System.out.println("Stop result = " + result);

        assertTrue(result.success);
        assertFalse(this.node.isAlive());
    }

    @Test
    public void testSendZeroValueViaRPC() {
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

        RpcResult<ReceiptHash> rpcResult = this.rpc.sendTransaction(transactionResult.getTransaction().get());
        System.out.println("Rpc result = " + rpcResult);
        assertTrue(rpcResult.success);

        result = this.node.stop();
        System.out.println("Stop result = " + result);

        assertTrue(result.success);
        assertFalse(this.node.isAlive());
    }

    @Test
    public void testSendMultipleValueTransfersViaRPC() {
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

        RpcResult<ReceiptHash> rpcResult = this.rpc.sendTransaction(transactionResult.getTransaction().get());
        assertTrue(rpcResult.success);

        transactionResult = constructTransaction(
            preminedPrivateKey,
            destination,
            BigInteger.ONE,
            BigInteger.ONE);

        assertTrue(transactionResult.success);
        rpcResult = this.rpc.sendTransaction(transactionResult.getTransaction().get());
        System.out.println("Rpc result = " + rpcResult);
        assertTrue(rpcResult.success);

        result = this.node.stop();
        System.out.println("Stop result = " + result);

        assertTrue(result.success);
        assertFalse(this.node.isAlive());
    }

    @Test
    public void testSendTransactionWhenNoNodeIsAlive() {
        assertFalse(this.node.isAlive());
        TransactionResult transactionResult = constructTransaction(
            preminedPrivateKey,
            destination,
            BigInteger.ZERO,
            BigInteger.ZERO);

        assertTrue(transactionResult.success);

        RpcResult<ReceiptHash> rpcResult = this.rpc.sendTransaction(transactionResult.getTransaction().get());
        System.out.println("Rpc result = " + rpcResult);
        assertFalse(rpcResult.success);
    }

    @Test
    public void testSendValueToInvalidAddress() {
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

        RpcResult<ReceiptHash> rpcResult = this.rpc.sendTransaction(transactionResult.getTransaction().get());
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
        PrivateKey badPrivateKey = new PrivateKey(badKey);

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

        RpcResult<ReceiptHash> rpcResult = this.rpc.sendTransaction(transactionResult.getTransaction().get());
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

    private void doBalanceTransfer(BigInteger transferValue) {
        TransactionResult transactionResult = constructTransaction(
                preminedPrivateKey,
                destination,
                transferValue,
                BigInteger.ZERO);

        Transaction transaction = transactionResult.getTransaction().get();

        RpcResult<ReceiptHash> result = this.rpc.sendTransaction(transaction);
        System.out.println("Rpc result = " + result);
        assertTrue(result.success);

        byte[] transactionHash = transaction.getTransactionHash();
        long timeout = TimeUnit.MINUTES.toMillis(1);

        LogEventResult eventResult = new NodeListener().waitForTransactionToBeProcessed(transactionHash, timeout);
        assertTrue(eventResult.eventWasObserved());
    }

    private TransactionResult constructTransaction(PrivateKey senderPrivateKey, Address destination, BigInteger value, BigInteger nonce) {
        return Transaction
            .buildAndSignTransaction(senderPrivateKey, nonce, destination, new byte[0], 2_000_000, 10_000_000_000L, value);
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
