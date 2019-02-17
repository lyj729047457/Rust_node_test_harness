package org.aion.harness.integ;

import org.aion.harness.kernel.Address;
import org.aion.harness.kernel.Transaction;
import org.aion.harness.main.Node;
import org.aion.harness.main.NodeFactory;
import org.aion.harness.main.NodeListener;
import org.aion.harness.main.RPC;
import org.aion.harness.main.impl.JavaNode;
import org.aion.harness.misc.Assumptions;
import org.aion.harness.result.EventRequestResult;
import org.aion.harness.result.RPCResult;
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
import java.util.concurrent.TimeUnit;

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
        this.rpc = new RPC();
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

        RPCResult result = this.rpc.sendTransaction(transactionResult.getTransaction());
        assertTrue(result.getResultOnly().success);

        this.node.stop();
    }

    @Test
    public void testGetBalanceViaRPC() throws Exception {
        initializeNodeWithChecks();
        assertTrue(this.node.start().success);

        RPCResult result = this.rpc.getBalance(destination.getAddressBytes());
        assertTrue(result.getResultOnly().success);

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

        RPCResult result = this.rpc.sendTransaction(transactionResult.getTransaction());
        assertTrue(result.getResultOnly().success);

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

        RPCResult result = this.rpc.sendTransaction(transactionResult.getTransaction());
        assertTrue(result.getResultOnly().success);

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

        RPCResult result = this.rpc.sendTransaction(transactionResult.getTransaction());
        assertTrue(result.getResultOnly().success);

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

        RPCResult result = this.rpc.sendTransaction(transactionResult.getTransaction());
        assertTrue(result.getResultOnly().success);

        transactionResult = constructTransaction(
            preminedAddress,
            destination,
            BigInteger.ONE,
            BigInteger.ONE);

        assertTrue(transactionResult.getResultOnly().success);
        result = this.rpc.sendTransaction(transactionResult.getTransaction());
        assertTrue(result.getResultOnly().success);

        this.node.stop();
    }

    @Test
    public void testSendTransactionWhenNoNodeIsAlive() {
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

        RPCResult result = this.rpc.sendTransaction(transactionResult.getTransaction());
        assertTrue(result.getResultOnly().success);

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

        RPCResult result = this.rpc.sendTransaction(transactionResult.getTransaction());
        assertFalse(result.getResultOnly().success);

        this.node.stop();
    }

    @Test
    public void testBalanceTransferAndCheck() throws Exception {
        initializeNodeWithChecks();
        assertTrue(this.node.start().success);
        BigInteger transferValue = BigInteger.valueOf(50);

        // check balance before
        RPCResult result = this.rpc.getBalance(destination.getAddressBytes());
        assertTrue(result.getResultOnly().success);
        Assert.assertEquals(BigInteger.ZERO, new BigInteger(result.getOutputResult(), 16));

        // transfer and wait
        doBalanceTransfer(transferValue);

        // check balance after
        result = this.rpc.getBalance(destination.getAddressBytes());
        assertTrue(result.getResultOnly().success);
        BigInteger balanceAfter = new BigInteger(result.getOutputResult(), 16);
        Assert.assertEquals(transferValue, balanceAfter);
    }

    @Test
    public void testBalanceTransferZeroBalanceAndCheck() throws Exception {
        initializeNodeWithChecks();
        assertTrue(this.node.start().success);
        BigInteger transferValue = BigInteger.ZERO;

        // check balance before
        RPCResult result = this.rpc.getBalance(destination.getAddressBytes());
        assertTrue(result.getResultOnly().success);
        Assert.assertEquals(BigInteger.ZERO, new BigInteger(result.getOutputResult(), 16));

        // transfer and wait
        doBalanceTransfer(transferValue);

        // check balance after
        result = this.rpc.getBalance(destination.getAddressBytes());
        assertTrue(result.getResultOnly().success);
        BigInteger balanceAfter = new BigInteger(result.getOutputResult(), 16);
        Assert.assertEquals(BigInteger.ZERO, balanceAfter);
    }

    @Test
    public void testBalanceTransferNegativeBalanceAndCheck() throws Exception {
        // When someone tries to transfer a negative value(BigInteger), the positive representation of
        // that BigInteger will be the transfer value. This should be something handled by the server.
        initializeNodeWithChecks();
        assertTrue(this.node.start().success);
        BigInteger negativeTransferValue = BigInteger.valueOf(-10);
        BigInteger positiveRepresentation = new BigInteger(1, negativeTransferValue.toByteArray());

        // check balance before
        RPCResult result = this.rpc.getBalance(destination.getAddressBytes());
        assertTrue(result.getResultOnly().success);
        Assert.assertEquals(BigInteger.ZERO, new BigInteger(result.getOutputResult(), 16));

        // transfer and wait
        doBalanceTransfer(negativeTransferValue);

        // check balance after - for positive representation
        result = this.rpc.getBalance(destination.getAddressBytes());
        assertTrue(result.getResultOnly().success);
        BigInteger balanceAfter = new BigInteger(result.getOutputResult(), 16);
        Assert.assertEquals(positiveRepresentation, balanceAfter);
    }

    @Test
    public void testGetNonce() throws Exception {
        initializeNodeWithChecks();
        assertTrue(this.node.start().success);

        // check nonce before
        RPCResult result = this.rpc.getNonce(preminedAddress.getAddressBytes());
        BigInteger nonceBefore = new BigInteger(result.getOutputResult(), 16);
        System.out.println("nonce is: " + nonceBefore);

        // do a transfer and wait
        doBalanceTransfer(BigInteger.ONE);

        // check nonce after
        result = this.rpc.getNonce(preminedAddress.getAddressBytes());
        BigInteger nonceAfter = new BigInteger(result.getOutputResult(), 16);
        System.out.println("nonce is: " + nonceAfter);

        Assert.assertEquals(nonceBefore.add(BigInteger.ONE), nonceAfter);
    }

    private void doBalanceTransfer(BigInteger transferValue) {
        TransactionResult transactionResult = constructTransaction(
                preminedAddress,
                destination,
                transferValue,
                BigInteger.ZERO);
        RPCResult result = this.rpc.sendTransaction(transactionResult.getTransaction());
        assertTrue(result.getResultOnly().success);

        byte[] transactionHash = transactionResult.getTransaction().getTransactionHash();
        long timeout = TimeUnit.MINUTES.toMillis(1);

        EventRequestResult eventResult = new NodeListener().waitForTransactionToBeSealed(transactionHash, timeout);
        assertTrue(eventResult.eventWasObserved());
    }

    private TransactionResult constructTransaction(Address sender, Address destination, BigInteger value, BigInteger nonce) {
        return Transaction
            .buildAndSignTransaction(sender, nonce, destination, new byte[0], 2_000_000, 10_000_000_000L, value);
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
