package org.aion.harness.integ;

import java.util.concurrent.TimeUnit;
import org.aion.harness.kernel.Address;
import org.aion.harness.kernel.PrivateKey;
import org.aion.harness.kernel.Transaction;
import org.aion.harness.kernel.utils.CryptoUtils;
import org.aion.harness.main.Node;
import org.aion.harness.main.NodeFactory;
import org.aion.harness.main.NodeListener;
import org.aion.harness.main.RPC;
import org.aion.harness.main.types.FutureResult;
import org.aion.harness.main.types.NodeConfigurationBuilder;
import org.aion.harness.main.types.ReceiptHash;
import org.aion.harness.main.types.TransactionReceipt;
import org.aion.harness.misc.Assumptions;
import org.aion.harness.result.LogEventResult;
import org.aion.harness.result.Result;
import org.aion.harness.result.RpcResult;
import org.aion.harness.result.TransactionResult;
import org.aion.harness.util.NodeFileManager;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.spec.InvalidKeySpecException;

import static org.junit.Assert.*;

public class KernelAddressTest {
    private static boolean doFullInitialization = false;
    private static File nodeDirectory = NodeFileManager.getNodeDirectory();
    private static File kernelDirectory = NodeFileManager.getKernelDirectory();
    private static PrivateKey preminedPrivateKey;
    private static Address destination;
    private static long energyLimit = 2_000_000;
    private static long energyPrice = 10_000_000_000L;

    private RPC rpc;
    private Node node;
    @Before
    public void setup() throws IOException, DecoderException, InvalidKeySpecException {
        preminedPrivateKey = PrivateKey.fromBytes(Hex.decodeHex(Assumptions.PREMINED_PRIVATE_KEY));
        destination = new Address(Hex.decodeHex("a0e9f9832d581246a9665f64599f405e8927993c6bef4be2776d91a66b466d30"));
        deleteInitializationDirectories();
        this.node = NodeFactory.getNewNodeInstance(NodeFactory.NodeType.JAVA_NODE);
        this.node.configure(NodeConfigurationBuilder.defaultConfigurations());
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
    public void testDeriveAddress() throws DecoderException, InvalidKeySpecException {
        String preminedPrivateKey = "223f19377d95582055bd8972cf3ffd635d2712a7171e4888091a066b9f4f63d5";
        String preminedAddress = "a0d6dec327f522f9c8d342921148a6c42f40a3ce45c1f56baa7bfa752200d9e5";

        byte[] computedAddress = CryptoUtils.deriveAddress(Hex.decodeHex(preminedPrivateKey));
        System.out.println(Hex.encodeHexString(computedAddress));

        assertArrayEquals(Hex.decodeHex(preminedAddress), computedAddress);
    }


    @Test
    public void testCorrectness() throws InterruptedException, InvalidKeySpecException {
        NodeListener nodeListener = new NodeListener();
        PrivateKey senderPrivateKey = PrivateKey.random();
        System.out.println("private key = " + senderPrivateKey);

        initializeNodeWithChecks();
        Result result = this.node.start();
        System.out.println("Start result = " + result);

        assertTrue(result.success);
        assertTrue(this.node.isAlive());

        // Transfer funds to the new private key so it can pay for transaction.
        transferFunds(nodeListener, senderPrivateKey);

        // send transaction with new address private key
        TransactionResult transactionResult = constructTransaction(
            senderPrivateKey,
            destination,
            BigInteger.ZERO,
            BigInteger.ZERO);
        assertTrue(transactionResult.success);
        Transaction transaction = transactionResult.getTransaction();

        FutureResult<LogEventResult> futureResult = nodeListener.listenForTransactionToBeProcessed(
            transaction.getTransactionHash(),
            2,
            TimeUnit.MINUTES);

        RpcResult<ReceiptHash> rpcResult = this.rpc.sendTransaction(transaction);
        System.out.println("Rpc result = " + rpcResult);
        assertTrue(rpcResult.success);

        LogEventResult logEventResult = futureResult.get();
        assertTrue(logEventResult.eventWasObserved());

        // use the receipt to determine the real address
        RpcResult<TransactionReceipt> rpcResult2 = this.rpc.getTransactionReceipt(rpcResult.getResult());
        assertTrue(rpcResult2.success);
        TransactionReceipt transactionReceipt = rpcResult2.getResult();

        assertEquals(senderPrivateKey.getAddress(), transactionReceipt.getTransactionSender());

        result = this.node.stop();
        System.out.println("Stop result = " + result);

        assertTrue(result.success);
        assertFalse(this.node.isAlive());
    }

    private void transferFunds(NodeListener nodeListener, PrivateKey senderPrivateKey) throws InterruptedException {
        TransactionResult transactionResult = constructTransaction(
            preminedPrivateKey,
            senderPrivateKey.getAddress(),
            BigInteger.TEN.pow(20),
            BigInteger.ZERO);
        assertTrue(transactionResult.success);

        Transaction transaction = transactionResult.getTransaction();

        FutureResult<LogEventResult> futureResult = nodeListener.listenForTransactionToBeProcessed(
            transaction.getTransactionHash(),
            2,
            TimeUnit.MINUTES);

        RpcResult<ReceiptHash> rpcResult = this.rpc.sendTransaction(transaction);
        System.out.println("Rpc result = " + rpcResult);
        assertTrue(rpcResult.success);

        LogEventResult logEventResult = futureResult.get();
        assertTrue(logEventResult.eventWasObserved());
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
