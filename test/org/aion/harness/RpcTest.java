package org.aion.harness;

import org.aion.harness.misc.Assumptions;
import org.aion.harness.util.NodeFileManager;
import org.aion.harness.result.Result;
import org.aion.harness.result.TransactionResult;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class RpcTest {
    private static boolean doFullInitialization = false;
    private static File nodeDirectory = NodeFileManager.getNodeDirectory();
    private static File kernelDirectory = NodeFileManager.getKernelDirectory();
    private static Address destination;
    private static Address preminedAddress;

    private RPC rpc;
    private Node node;

    @Before
    public void setup() throws IOException, DecoderException {
        preminedAddress = Address.createAddressWithPrivateKey(Hex.decodeHex(Assumptions.PREMINED_ADDRESS), Hex.decodeHex(Assumptions.PREMINED_PRIVATE_KEY));
        destination = Address.createAddress(Hex.decodeHex("a0e9f9832d581246a9665f64599f405e8927993c6bef4be2776d91a66b466d30"));
        deleteInitializationDirectories();
        this.node = NodeFactory.getNewNodeInstance(NodeFactory.NodeType.JAVA_NODE);
        this.rpc = new RPC(this.node);
    }

    @After
    public void tearDown() throws IOException, InterruptedException {
        shutdownNodeIfRunning();
        deleteInitializationDirectories();
        deleteLogs();
        this.node = null;
        this.rpc = null;
    }

    @Test
    public void testSendValueViaRPC() throws Exception {
        initializeNodeWithChecks();
        assertTrue(this.node.start().success);

        TransactionResult transactionResult = constructTransaction(
            preminedAddress,
            destination,
            BigInteger.ONE,
            BigInteger.ZERO);

        assertTrue(transactionResult.getResultOnly().success);

        Result result = this.rpc.sendTransaction(transactionResult.getTransaction());
        assertTrue(result.success);

        this.node.stop();
    }

    @Test
    public void testSendValueWithIncorrectNonceViaRPC() throws Exception {
        initializeNodeWithChecks();
        assertTrue(this.node.start().success);

        TransactionResult transactionResult = constructTransaction(
            preminedAddress,
            destination,
            BigInteger.ONE,
            BigInteger.valueOf(100));

        assertTrue(transactionResult.getResultOnly().success);

        Result result = this.rpc.sendTransaction(transactionResult.getTransaction());
        assertTrue(result.success);

        this.node.stop();
    }

    @Test
    public void testSendNegativeValueViaRPC() throws Exception {
        initializeNodeWithChecks();
        assertTrue(this.node.start().success);

        TransactionResult transactionResult = constructTransaction(
            preminedAddress,
            destination,
            BigInteger.valueOf(-1),
            BigInteger.ZERO);

        assertTrue(transactionResult.getResultOnly().success);

        Result result = this.rpc.sendTransaction(transactionResult.getTransaction());
        assertTrue(result.success);

        this.node.stop();
    }

    @Test
    public void testSendZeroValueViaRPC() throws Exception {
        initializeNodeWithChecks();
        assertTrue(this.node.start().success);

        TransactionResult transactionResult = constructTransaction(
            preminedAddress,
            destination,
            BigInteger.ZERO,
            BigInteger.ZERO);

        assertTrue(transactionResult.getResultOnly().success);

        Result result = this.rpc.sendTransaction(transactionResult.getTransaction());
        assertTrue(result.success);

        this.node.stop();
    }

    @Test
    public void testSendMultipleValueTransfersViaRPC() throws Exception {
        initializeNodeWithChecks();
        assertTrue(this.node.start().success);

        TransactionResult transactionResult = constructTransaction(
            preminedAddress,
            destination,
            BigInteger.ONE,
            BigInteger.ZERO);

        assertTrue(transactionResult.getResultOnly().success);

        Result result = this.rpc.sendTransaction(transactionResult.getTransaction());
        assertTrue(result.success);

        transactionResult = constructTransaction(
            preminedAddress,
            destination,
            BigInteger.ONE,
            BigInteger.ONE);

        assertTrue(transactionResult.getResultOnly().success);
        result = this.rpc.sendTransaction(transactionResult.getTransaction());
        assertTrue(result.success);

        this.node.stop();
    }

    @Test(expected = IllegalStateException.class)
    public void testSendTransactionWhenNoNodeIsAlive() throws Exception {
        assertFalse(this.node.isAlive());
        TransactionResult transactionResult = constructTransaction(
            preminedAddress,
            destination,
            BigInteger.ZERO,
            BigInteger.ZERO);

        assertTrue(transactionResult.getResultOnly().success);

        this.rpc.sendTransaction(transactionResult.getTransaction());
    }

    @Test
    public void testSendValueToInvalidAddress() throws Exception {
        initializeNodeWithChecks();
        assertTrue(this.node.start().success);

        TransactionResult transactionResult = constructTransaction(
            preminedAddress,
            destination,
            BigInteger.ONE,
            BigInteger.ZERO);

        assertTrue(transactionResult.getResultOnly().success);

        Result result = this.rpc.sendTransaction(transactionResult.getTransaction());
        assertTrue(result.success);

        this.node.stop();
    }

    @Test
    public void testSendValueWithInsufficientBalance() throws Exception {
        byte[] badKey = Hex.decodeHex("223f19370d95582055bd8072cf3ffd635d2712a7171e4888091a060b9f4f63d5");
        Address badKeyAddress = Address.createAddressWithPrivateKey(Hex.decodeHex("a03f19370d95582055bd8072cf3ffd635d2712a7171e4888091a060b9f4f63d5"), badKey);

        initializeNodeWithChecks();
        assertTrue(this.node.start().success);

        TransactionResult transactionResult = constructTransaction(
            badKeyAddress,
            destination,
            BigInteger.ONE,
            BigInteger.ZERO);

        assertTrue(transactionResult.getResultOnly().success);

        Result result = this.rpc.sendTransaction(transactionResult.getTransaction());
        assertFalse(result.success);

        this.node.stop();
    }

    private TransactionResult constructTransaction(Address sender, Address destination, BigInteger value, BigInteger nonce) {
        return Transaction.buildAndSignTransaction(sender, nonce, destination, new byte[0], 2_000_000, 10_000_000_000L, value);
    }

    private Result initializeNode() throws IOException, InterruptedException {
        if (doFullInitialization) {
            return this.node.initialize();
        } else {
            boolean status = ((JavaNode) this.node).initializeButSkipKernelBuild(false);
            return (status) ? Result.successful() : Result.unsuccessful(Assumptions.TESTING_ERROR_STATUS, "Failed partial initialization in test");
        }
    }

    private void initializeNodeWithChecks() throws IOException, InterruptedException {
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

    private void shutdownNodeIfRunning() throws InterruptedException {
        if ((this.node != null) && (this.node.isAlive())) {
            this.node.stop();
        }
    }
}
