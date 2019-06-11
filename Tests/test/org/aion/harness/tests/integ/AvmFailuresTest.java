package org.aion.harness.tests.integ;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
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
import org.aion.harness.main.ProhibitConcurrentHarness;
import org.aion.harness.main.RPC;
import org.aion.harness.main.event.IEvent;
import org.aion.harness.main.event.PrepackagedLogEvents;
import org.aion.harness.main.types.ReceiptHash;
import org.aion.harness.main.types.TransactionReceipt;
import org.aion.harness.result.FutureResult;
import org.aion.harness.result.LogEventResult;
import org.aion.harness.result.Result;
import org.aion.harness.result.RpcResult;
import org.aion.harness.result.TransactionResult;
import org.aion.harness.tests.contracts.Assertions;
import org.aion.harness.tests.contracts.avm.AvmFailureModes;
import org.aion.harness.tests.contracts.avm.ByteArrayHolder;
import org.aion.harness.tests.integ.runner.ExcludeNodeType;
import org.aion.harness.tests.integ.runner.SequentialRunner;
import org.aion.harness.tests.integ.runner.internal.LocalNodeListener;
import org.aion.harness.tests.integ.runner.internal.PreminedAccount;
import org.aion.harness.tests.integ.runner.internal.PrepackagedLogEventsFactory;
import org.aion.harness.util.SimpleLog;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests the behaviour of various AVM success/failure modes, checking that the consumed energy is consistent with expectations.
 */
@RunWith(SequentialRunner.class)
@ExcludeNodeType(NodeType.RUST_NODE) // exclude Rust for now due to bugs that prevent tests from passing
public class AvmFailuresTest {
    private static final long ENERGY_LIMIT = 2_000_000L;
    private static final long ENERGY_PRICE = 10_010_020_345L;

    private final SimpleLog log = new SimpleLog("org.aion.harness.tests.integ.AvmFailuresTest");

    private static RPC rpc = new RPC("127.0.0.1", "8545");

    @Rule
    private PreminedAccount preminedAccount = new PreminedAccount(BigInteger.valueOf(1_000_000_000_000_000_000L));

    @Rule
    private LocalNodeListener listener = new LocalNodeListener();

    @Rule
    private PrepackagedLogEventsFactory prepackagedLogEventsFactory = new PrepackagedLogEventsFactory();

    @Test
    public void testPass() throws Exception {
        Address contract = deployContract();
        
        // Send the "pass" (0).
        TransactionReceipt receipt = sendTransactionToContract(contract, (byte)0);
        boolean wasSuccess = receipt.transactionWasSuccessful();
        long energyConsumed = receipt.getTransactionEnergyConsumed();
        Assert.assertEquals(true, wasSuccess);
        Assert.assertEquals(21378, energyConsumed);
    }

    @Test
    public void testFailException() throws Exception {
        Address contract = deployContract();
        
        // Send the "fail - exception" (1).
        TransactionReceipt receipt = sendTransactionToContract(contract, (byte)1);
        boolean wasSuccess = receipt.transactionWasSuccessful();
        long energyConsumed = receipt.getTransactionEnergyConsumed();
        Assert.assertEquals(false, wasSuccess);
        Assert.assertEquals(ENERGY_LIMIT, energyConsumed);
    }

    @Test
    public void testFailRevert() throws Exception {
        Address contract = deployContract();
        
        // Send the "fail - revert" (2).
        TransactionReceipt receipt = sendTransactionToContract(contract, (byte)2);
        boolean wasSuccess = receipt.transactionWasSuccessful();
        long energyConsumed = receipt.getTransactionEnergyConsumed();
        Assert.assertEquals(false, wasSuccess);
        Assert.assertEquals(21378, energyConsumed);
    }

    @Test
    public void testFailEnergy() throws Exception {
        Address contract = deployContract();
        
        // Send the "fail - out of energy" (3).
        TransactionReceipt receipt = sendTransactionToContract(contract, (byte)3);
        boolean wasSuccess = receipt.transactionWasSuccessful();
        long energyConsumed = receipt.getTransactionEnergyConsumed();
        Assert.assertEquals(false, wasSuccess);
        Assert.assertEquals(ENERGY_LIMIT, energyConsumed);
    }


    private Address deployContract() throws InterruptedException {
        // build contract deployment Tx
        TransactionResult deploy = RawTransaction.buildAndSignAvmCreateTransaction(
            this.preminedAccount.getPrivateKey(),
            this.preminedAccount.getAndIncrementNonce(),
            new CodeAndArguments(
                JarBuilder.buildJarForMainAndClasses(AvmFailureModes.class), new byte[0])
                .encodeToBytes(),
            ENERGY_LIMIT,
            ENERGY_PRICE,
            BigInteger.ZERO /* amount */);
        Assert.assertTrue(deploy.isSuccess());
        
        // send contract deployment Tx
        TransactionReceipt deployReceipt = sendRawTransactionSynchronously(deploy.getTransaction());
        Assert.assertTrue(deployReceipt.getAddressOfDeployedContract().isPresent());
        return deployReceipt.getAddressOfDeployedContract().get();
    }

    private TransactionReceipt sendTransactionToContract(Address contract, byte argument) throws InterruptedException {
        // Create the transaction.
        TransactionResult transaction = RawTransaction.buildAndSignGeneralTransaction(
                this.preminedAccount.getPrivateKey(),
                this.preminedAccount.getAndIncrementNonce(),
                contract,
                new byte[] {argument},
                ENERGY_LIMIT,
                ENERGY_PRICE,
                BigInteger.ZERO
        );
        Assert.assertTrue(transaction.isSuccess());
        
        return sendRawTransactionSynchronously(transaction.getTransaction());
    }

    private TransactionReceipt sendRawTransactionSynchronously(RawTransaction rawTransaction) throws InterruptedException {
        // Capture the event for asynchronous waiting.
        IEvent transactionIsSealed = prepackagedLogEventsFactory.build().getTransactionSealedEvent(rawTransaction);
        FutureResult<LogEventResult> future = this.listener.listenForEvent(transactionIsSealed, 5, TimeUnit.MINUTES);
        
        // Send the transaction.
        RpcResult<ReceiptHash> sendResult = AvmFailuresTest.rpc.sendTransaction(rawTransaction);
        Assertions.assertRpcSuccess(sendResult);
        
        // Wait for this to be mined.
        waitForEvent(future);
        
        // Get the receipt (depends on it being mined).
        ReceiptHash hash = sendResult.getResult();
        RpcResult<TransactionReceipt> receiptResult = AvmFailuresTest.rpc.getTransactionReceipt(hash);
        Assertions.assertRpcSuccess(receiptResult);
        return receiptResult.getResult();
    }

    private void waitForEvent(FutureResult<LogEventResult> future) throws InterruptedException {
        log.log("Waiting for the transaction to process...");
        LogEventResult listenResult = future.get();
        Assert.assertTrue(listenResult.eventWasObserved());
        log.log("Transaction was sealed into a block.");
    }
}
