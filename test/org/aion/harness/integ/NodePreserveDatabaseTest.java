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
import org.aion.harness.result.Result;
import org.aion.harness.result.RpcResult;
import org.aion.harness.result.TransactionResult;
import org.aion.harness.util.NodeFileManager;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.junit.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NodePreserveDatabaseTest {
    private static PrivateKey preminedPrivateKey;
    private static Address destination;

    private RPC rpc;
    private Node node;

    @Before
    public void setup() throws DecoderException {
        destination = new Address(Hex.decodeHex("a0e9f9832d581246a9665f64599f405e8927993c6bef4be2776d91a66b466d30"));
        preminedPrivateKey = new PrivateKey(Hex.decodeHex(Assumptions.PREMINED_PRIVATE_KEY));
        this.node = NodeFactory.getNewNodeInstance(NodeFactory.NodeType.JAVA_NODE);
        this.rpc = new RPC();
    }

    @After
    public void tearDown() throws IOException {
        shutdownNodeIfRunning();
        deleteLogs();
        this.node = null;
        deleteInitializationDirectories();
    }

    private static void deleteInitializationDirectories() throws IOException {
        if (NodeFileManager.getNodeDirectory().exists()) {
            FileUtils.deleteDirectory(NodeFileManager.getNodeDirectory());
        }
        if (NodeFileManager.getKernelDirectory().exists()) {
            FileUtils.deleteDirectory(NodeFileManager.getKernelDirectory());
        }
    }

    @Test
    public void testPreserveDatabaseCheckTransaction() throws IOException, InterruptedException {
        // initialize and run node to generate a database, then shutdown the node
        Result result = this.node.initializeKernelAndPreserveDatabase();
        assertTrue(result.success);

        // start the node
        result = this.node.start();
        assertTrue(result.success);

        // transfer and wait
        BigInteger transferValue = BigInteger.valueOf(50);
        doBalanceTransfer(transferValue);

        // check balance of destination address
        RpcResult<BigInteger> rpcResult = this.rpc.getBalance(destination.getAddressBytes());
        BigInteger balanceAfter = rpcResult.getResult();
        Assert.assertEquals(transferValue, balanceAfter);

        // now stop the node
        result = this.node.stop();
        System.out.println("Stop result = " + result);

        assertTrue(result.success);
        assertFalse(this.node.isAlive());

        // re-initialize node, but with the preserved database
        result = this.node.initializeKernelAndPreserveDatabase();
        assertTrue(result.success);

        // start the node
        result = this.node.start();
        assertTrue(result.success);

        // check that the transfer is still present
        rpcResult = this.rpc.getBalance(destination.getAddressBytes());
        BigInteger balanceAfter2 = rpcResult.getResult();
        Assert.assertEquals(transferValue, balanceAfter2);

        // check that the balance has not changed
        Assert.assertEquals(balanceAfter, balanceAfter2);

        // now stop the node
        result = this.node.stop();
        System.out.println("Stop result = " + result);

        assertTrue(result.success);
        assertFalse(this.node.isAlive());
    }

    @Test
    public void testInitializeWHenNewNetworkDirectoryNotCreated() throws IOException, InterruptedException {
        // initialize and run node to create the node folder
        Result result = this.node.initializeKernelAndPreserveDatabase();
        assertTrue(result.success);

        // try to preserve a database that does not yet exist
        result = this.node.initializeKernelAndPreserveDatabase();
        assertTrue(result.success);
    }

    private static void deleteLogs() throws IOException {
        FileUtils.deleteDirectory(NodeFileManager.getLogsDirectory());
    }

    private TransactionResult constructTransaction(PrivateKey senderPrivateKey, Address destination, BigInteger value, BigInteger nonce) {
        return Transaction
                .buildAndSignTransaction(senderPrivateKey, nonce, destination, new byte[0], 2_000_000, 10_000_000_000L, value);
    }

    private void shutdownNodeIfRunning() {
        if ((this.node != null) && (this.node.isAlive())) {
            Result result = this.node.stop();
            System.out.println("Stop result = " + result);

            assertTrue(result.success);
            assertFalse(this.node.isAlive());
        }
    }

    private void doBalanceTransfer(BigInteger transferValue) throws InterruptedException {
        TransactionResult transactionResult = constructTransaction(
                preminedPrivateKey,
                destination,
                transferValue,
                BigInteger.ZERO);

        Transaction transaction = transactionResult.getTransaction();

        RpcResult<ReceiptHash> result = this.rpc.sendTransaction(transaction);
        System.out.println("Rpc result = " + result);
        assertTrue(result.success);

        byte[] transactionHash = transaction.getTransactionHash();
        long timeout = TimeUnit.MINUTES.toMillis(1);

        LogEventResult eventResult = new NodeListener().waitForTransactionToBeProcessed(transactionHash, timeout);
        assertTrue(eventResult.eventWasObserved());
    }
}