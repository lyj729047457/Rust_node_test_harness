package org.aion.harness.tests.integ;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.math.BigInteger;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.aion.avm.core.dappreading.JarBuilder;
import org.aion.avm.core.util.CodeAndArguments;
import org.aion.harness.kernel.Address;
import org.aion.harness.kernel.PrivateKey;
import org.aion.harness.kernel.RawTransaction;
import org.aion.harness.kernel.Transaction;
import org.aion.harness.main.LocalNode;
import org.aion.harness.main.Network;
import org.aion.harness.main.NodeConfigurations;
import org.aion.harness.main.NodeConfigurations.DatabaseOption;
import org.aion.harness.main.NodeFactory;
import org.aion.harness.main.NodeFactory.NodeType;
import org.aion.harness.main.NodeListener;
import org.aion.harness.main.RPC;
import org.aion.harness.main.event.IEvent;
import org.aion.harness.main.event.PrepackagedLogEvents;
import org.aion.harness.main.types.ReceiptHash;
import org.aion.harness.main.types.TransactionReceipt;
import org.aion.harness.result.FutureResult;
import org.aion.harness.result.LogEventResult;
import org.aion.harness.result.RpcResult;
import org.aion.harness.result.TransactionResult;
import org.aion.harness.tests.contracts.avm.ByteArrayHolder;
import org.aion.harness.tests.contracts.avm.SimpleContract;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AvmTxSmokeTest {
    private static final String BUILT_KERNEL = System.getProperty("user.dir") + "/aion";
    private static final String PREMINED_KEY = "4c3c8a7c0292bc55d97c50b4bdabfd47547757d9e5c194e89f66f25855baacd0";
    private static final long ENERGY_LIMIT = 1_234_567L;
    private static final long ENERGY_PRICE = 10_010_020_345L;
    private static final int NUMBER_OF_TRANSACTIONS = 25;

    private LocalNode node;
    private RPC rpc;
    private NodeListener listener;
    private PrivateKey preminedPrivateKey;

    @Before
    public void setup() throws IOException, InterruptedException, DecoderException, InvalidKeySpecException {
        this.preminedPrivateKey = PrivateKey.fromBytes(Hex.decodeHex(PREMINED_KEY));

        NodeConfigurations configurations = NodeConfigurations.alwaysUseBuiltKernel(Network.CUSTOM, BUILT_KERNEL, DatabaseOption.PRESERVE_DATABASE);

        this.node = NodeFactory.getNewLocalNodeInstance(NodeType.JAVA_NODE);
        this.node.configure(configurations);

        assertTrue(this.node.initialize().isSuccess());
        assertTrue(this.node.start().isSuccess());
        assertTrue(this.node.isAlive());

        this.rpc = new RPC("127.0.0.1", "8545");
        this.listener = NodeListener.listenTo(this.node);
    }

    @After
    public void tearDown() throws IOException, InterruptedException {
        assertTrue(this.node.stop().isSuccess());
        assertFalse(this.node.isAlive());
        this.node = null;
        this.rpc = null;
        this.listener = null;

        // If we close and reopen the DB too quickly we get an error... this sleep tries to avoid
        // this issue so that the DB lock is released in time.
        Thread.sleep(TimeUnit.SECONDS.toMillis(10));
    }

    @Test
    public void test() throws Exception {
        // build contract deployment Tx
        TransactionResult deploy = RawTransaction.buildAndSignAvmTransaction(
            this.preminedPrivateKey,
            getNonce(),
            null,
            new CodeAndArguments(
                JarBuilder.buildJarForMainAndClasses(ByteArrayHolder.class), new byte[0])
                .encodeToBytes(),
            ENERGY_LIMIT,
            ENERGY_PRICE,
            BigInteger.ZERO /* amount */);
        if(! deploy.isSuccess()) {
            throw new IllegalStateException("failed to construct the deployment tx");
        }

        // send contract deployment Tx
        TransactionReceipt deployReceipt = sendTransaction(deploy.getTransaction());
        assertThat("expected address of deployed contract to be present in receipt of deployment tx",
            deployReceipt.getAddressOfDeployedContract().isPresent(), is(true));
        Address contract = deployReceipt.getAddressOfDeployedContract().get();

        // check initial state of deployed contract
        byte[] result = this.rpc.call(new Transaction(contract));
        assertThat("initial state of deployed contract",
            Arrays.equals(result, new byte[] { 0x1, 0x2 }),
            is(true));

        // set "data" field of the contract
        byte[] newData1 = new byte[] { 0x21, 0x00, 0x01, 0x61 };
        TransactionResult tx1 =
            RawTransaction.buildAndSignAvmTransaction(this.preminedPrivateKey, getNonce(), contract,
                newData1, ENERGY_LIMIT, ENERGY_PRICE, BigInteger.ZERO /* amount */);
        if(! tx1.isSuccess()) {
            throw new IllegalStateException("failed to construct transaction 1 for setting data");
        }
        TransactionReceipt tx1Receipt = sendTransaction(tx1.getTransaction());
        assertThat(tx1Receipt, is(not(nullValue()))); // can get more rigourous with this

        // check state of deployed contract after tx1
        byte[] resultAfterTx1 = this.rpc.call(new Transaction(contract));
        assertThat("data field of contract should have changed",
            Arrays.equals(resultAfterTx1, newData1),
            is(true));

        // set "data" field of the contract
        byte[] newData2 = new byte[] { 0x21, 0x00, 0x04, 0x41, 0x53, 0x44, 0x46 };
        TransactionResult tx2 =
            RawTransaction.buildAndSignAvmTransaction(this.preminedPrivateKey, getNonce(), contract,
                newData2, ENERGY_LIMIT, ENERGY_PRICE, BigInteger.ZERO /* amount */);
        if(! tx2.isSuccess()) {
            throw new IllegalStateException("failed to construct transaction 2 for setting data");
        }
        TransactionReceipt tx2Receipt = sendTransaction(tx2.getTransaction());
        assertThat(tx2Receipt, is(not(nullValue()))); // can get more rigourous with this

        // check state of deployed contract after tx1
        byte[] resultAfterTx2 = this.rpc.call(new Transaction(contract));
        assertThat("data field of contract should have changed",
            Arrays.equals(resultAfterTx2, newData2),
            is(true));

    }

    private BigInteger getNonce() throws InterruptedException {
        RpcResult<BigInteger> nonceResult = this.rpc.getNonce(this.preminedPrivateKey.getAddress());
        assertTrue(nonceResult.isSuccess());
        return nonceResult.getResult();
    }

    private TransactionReceipt sendTransaction(RawTransaction transaction) throws InterruptedException {
        // we want to ensure that the transaction gets sealed into a block.
        IEvent transactionIsSealed = PrepackagedLogEvents.getTransactionSealedEvent(transaction);
        FutureResult<LogEventResult> future = this.listener.listenForEvent(transactionIsSealed, 5, TimeUnit.MINUTES);

        // Send the transaction off.
        System.out.println("Sending the transaction...");
        RpcResult<ReceiptHash> sendResult = this.rpc.sendTransaction(transaction);
        assertTrue(sendResult.isSuccess());

        // Wait on the future to complete and ensure we saw the transaction get sealed.
        System.out.println("Waiting for the transaction to process...");
        LogEventResult listenResult = future.get();
        assertTrue(listenResult.eventWasObserved());
        System.out.println("Transaction was sealed into a block.");

        ReceiptHash hash = sendResult.getResult();

        RpcResult<TransactionReceipt> receiptResult = this.rpc.getTransactionReceipt(hash);
        assertTrue(receiptResult.isSuccess());
        return receiptResult.getResult();
    }


}
