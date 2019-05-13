package org.aion.harness.tests.integ;

import static org.aion.harness.tests.contracts.Assertions.assertRpcSuccess;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.aion.avm.core.dappreading.JarBuilder;
import org.aion.avm.core.util.ABIUtil;
import org.aion.avm.core.util.CodeAndArguments;
import org.aion.avm.userlib.abi.ABIDecoder;
import org.aion.avm.userlib.abi.ABIException;
import org.aion.avm.userlib.abi.ABIToken;
import org.aion.harness.kernel.Address;
import org.aion.harness.kernel.PrivateKey;
import org.aion.harness.kernel.RawTransaction;
import org.aion.harness.main.LocalNode;
import org.aion.harness.main.Network;
import org.aion.harness.main.NodeConfigurations;
import org.aion.harness.main.NodeConfigurations.DatabaseOption;
import org.aion.harness.main.NodeFactory;
import org.aion.harness.main.NodeFactory.NodeType;
import org.aion.harness.main.NodeListener;
import org.aion.harness.main.ProhibitConcurrentHarness;
import org.aion.harness.main.RPC;
import org.aion.harness.main.event.Event;
import org.aion.harness.main.event.IEvent;
import org.aion.harness.main.event.PrepackagedLogEvents;
import org.aion.harness.main.types.ReceiptHash;
import org.aion.harness.main.types.TransactionReceipt;
import org.aion.harness.result.FutureResult;
import org.aion.harness.result.LogEventResult;
import org.aion.harness.result.Result;
import org.aion.harness.result.RpcResult;
import org.aion.harness.result.TransactionResult;
import org.aion.harness.tests.contracts.avm.AvmCrossCallDispatcher;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

public class CrossCallTest {
    private static final String BUILT_KERNEL = System.getProperty("user.dir") + "/aion";
    private static final String PREMINED_KEY = "4c3c8a7c0292bc55d97c50b4bdabfd47547757d9e5c194e89f66f25855baacd0";
    private static final long ENERGY_LIMIT = 1_234_567L;
    private static final long ENERGY_PRICE = 10_010_020_345L;

    private static LocalNode node;
    private static RPC rpc;
    private static NodeListener listener;
    private static PrivateKey preminedPrivateKey;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(200);

    @BeforeClass
    public static void setup() throws Exception {
        ProhibitConcurrentHarness.acquireTestLock();

        preminedPrivateKey = PrivateKey.fromBytes(Hex.decodeHex(PREMINED_KEY));

        NodeConfigurations configurations = NodeConfigurations.alwaysUseBuiltKernel(Network.CUSTOM, BUILT_KERNEL, DatabaseOption.PRESERVE_DATABASE);
        node = NodeFactory.getNewLocalNodeInstance(NodeType.JAVA_NODE);
        node.configure(configurations);

        Result result = node.initialize();
        System.out.println("Initialize result: " + result);
        assertTrue(result.isSuccess());

        result = node.start();
        System.out.println("Start result: " + result);
        assertTrue(node.isAlive());

        rpc = new RPC("127.0.0.1", "8545");
        listener = NodeListener.listenTo(node);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        System.out.println("Stop result: " + node.stop());
        node = null;
        rpc = null;
        listener = null;
        destroyLogs();
        ProhibitConcurrentHarness.releaseTestLock();
    }

    @Test
    public void testCallingFvmContractFromAvm() throws Exception {
        System.out.println("Deploying fvm contract...");
        Address fvmContract = deployFvmContract();
        System.out.println("Deploying avm contract...");
        Address avmContract = deployAvmDispatcherContract();
        System.out.println("Calling avm contract...");
        callAvmDispatcher(avmContract, fvmContract);
    }

    @Test
    public void testCallingPrecompiledContractFromAvm() throws Exception {
        // This is the token bridge contract address.
        Address precompiledContract = new Address(Hex.decodeHex("0000000000000000000000000000000000000000000000000000000000000200"));

        System.out.println("Deploying avm contract...");
        Address avmContract = deployAvmDispatcherContract();
        System.out.println("Calling avm contract...");
        callAvmDispatcher(avmContract, precompiledContract);
    }

    @Test
    public void testCallingAvmContractFromFvm() throws Exception {
        System.out.println("Deploying avm contract...");
        Address avmContract = deployAvmDispatcherContract();
        System.out.println("Deploying fvm contract...");
        Address fvmContract = deployFvmContract();
        System.out.println("Calling fvm contract...");
        callFvmDispatcher(fvmContract, avmContract);
    }

    private void callFvmDispatcher(Address dispatcher, Address target) throws InterruptedException, DecoderException {
        TransactionResult result = RawTransaction.buildAndSignGeneralTransaction(
            preminedPrivateKey,
            getNonce(),
            dispatcher,
            getFvmCallDispatcherBytes(target),
            ENERGY_LIMIT,
            ENERGY_PRICE,
            BigInteger.ZERO);
        assertTrue(result.isSuccess());

        sendCrossCallToFvm(result.getTransaction());
    }

    private void callAvmDispatcher(Address dispatcher, Address target) throws InterruptedException {
        TransactionResult result = RawTransaction.buildAndSignGeneralTransaction(
            preminedPrivateKey,
            getNonce(),
            dispatcher,
            getAvmCallDispatcherBytes(target),
            ENERGY_LIMIT,
            ENERGY_PRICE,
            BigInteger.ZERO);
        assertTrue(result.isSuccess());

        sendCrossCallToAvm(result.getTransaction());
    }

    private Address deployAvmDispatcherContract() throws InterruptedException {
        TransactionResult result = RawTransaction.buildAndSignAvmCreateTransaction(
            preminedPrivateKey,
            getNonce(),
            getAvmContractBytes(),
            ENERGY_LIMIT,
            ENERGY_PRICE,
            BigInteger.ZERO);
        assertTrue(result.isSuccess());

        TransactionReceipt receipt = sendTransaction(result.getTransaction());
        return receipt.getAddressOfDeployedContract().get();
    }

    private Address deployFvmContract() throws InterruptedException, DecoderException {
        TransactionResult result = RawTransaction.buildAndSignGeneralTransaction(
            preminedPrivateKey,
            getNonce(),
            null,
            getFvmContractBytes(),
            ENERGY_LIMIT,
            ENERGY_PRICE,
            BigInteger.ZERO);
        assertTrue(result.isSuccess());

        TransactionReceipt receipt = sendTransaction(result.getTransaction());
        return receipt.getAddressOfDeployedContract().get();
    }

    private void sendCrossCallToFvm(RawTransaction transaction) throws InterruptedException {
        // we want to ensure that the transaction gets sealed into a block.
        IEvent transactionIsSealed = PrepackagedLogEvents.getTransactionSealedEvent(transaction);
        IEvent transactionIsRejected = PrepackagedLogEvents.getTransactionRejectedEvent(transaction);
        IEvent transactionProcessed = Event.or(transactionIsSealed, transactionIsRejected);

        FutureResult<LogEventResult> future = listener.listenForEvent(transactionProcessed, 5, TimeUnit.MINUTES);

        // Send the transaction off.
        System.out.println("Sending the transaction...");
        RpcResult<ReceiptHash> sendResult = rpc.sendTransaction(transaction);
        assertRpcSuccess(sendResult);

        // Wait on the future to complete and ensure we saw the transaction get sealed.
        System.out.println("Waiting for the transaction to process...");
        LogEventResult listenResult = future.get();
        assertTrue(listenResult.eventWasObserved());

        // Verify it was sealed and not rejected.
        assertFalse(transactionIsRejected.hasBeenObserved());
        assertTrue(transactionIsSealed.hasBeenObserved());
        System.out.println("Transaction was sealed into a block.");

        ReceiptHash hash = sendResult.getResult();

        RpcResult<TransactionReceipt> receiptResult = rpc.getTransactionReceipt(hash);
        assertRpcSuccess(receiptResult);

        // Verify the transaction failed.
        assertFalse(receiptResult.getResult().transactionWasSuccessful());
    }

    private void sendCrossCallToAvm(RawTransaction transaction) throws InterruptedException {
        // we want to ensure that the transaction gets sealed into a block.
        IEvent transactionIsSealed = PrepackagedLogEvents.getTransactionSealedEvent(transaction);
        IEvent transactionIsRejected = PrepackagedLogEvents.getTransactionRejectedEvent(transaction);
        IEvent transactionProcessed = Event.or(transactionIsSealed, transactionIsRejected);

        // we expect to see the foreign virtual machine exception get caught.
        IEvent exception = new Event("Caught exception: java.lang.IllegalArgumentException: Attempt to execute code using a foreign virtual machine");
        IEvent event = Event.and(transactionProcessed, exception);

        FutureResult<LogEventResult> future = listener.listenForEvent(event, 5, TimeUnit.MINUTES);

        // Send the transaction off.
        System.out.println("Sending the transaction...");
        RpcResult<ReceiptHash> sendResult = rpc.sendTransaction(transaction);
        assertRpcSuccess(sendResult);

        // Wait on the future to complete and ensure we saw the transaction get sealed.
        System.out.println("Waiting for the transaction to process...");

        // If it was observed then we know we witnessed the exception.
        LogEventResult listenResult = future.get();
        assertTrue(listenResult.eventWasObserved());

        // Verify it was sealed and not rejected.
        assertFalse(transactionIsRejected.hasBeenObserved());
        assertTrue(transactionIsSealed.hasBeenObserved());

        System.out.println("Transaction was sealed into a block.");

        ReceiptHash hash = sendResult.getResult();

        RpcResult<TransactionReceipt> receiptResult = rpc.getTransactionReceipt(hash);
        assertRpcSuccess(receiptResult);
    }

    private TransactionReceipt sendTransaction(RawTransaction transaction) throws InterruptedException {
        // we want to ensure that the transaction gets sealed into a block.
        IEvent transactionIsSealed = PrepackagedLogEvents.getTransactionSealedEvent(transaction);
        IEvent transactionIsRejected = PrepackagedLogEvents.getTransactionRejectedEvent(transaction);
        IEvent transactionProcessed = Event.or(transactionIsSealed, transactionIsRejected);

        FutureResult<LogEventResult> future = listener.listenForEvent(transactionProcessed, 5, TimeUnit.MINUTES);

        // Send the transaction off.
        System.out.println("Sending the transaction...");
        RpcResult<ReceiptHash> sendResult = rpc.sendTransaction(transaction);
        assertRpcSuccess(sendResult);

        // Wait on the future to complete and ensure we saw the transaction get sealed.
        System.out.println("Waiting for the transaction to process...");
        LogEventResult listenResult = future.get();
        assertTrue(listenResult.eventWasObserved());

        // Verify it was sealed and not rejected.
        assertFalse(transactionIsRejected.hasBeenObserved());
        assertTrue(transactionIsSealed.hasBeenObserved());
        System.out.println("Transaction was sealed into a block.");

        ReceiptHash hash = sendResult.getResult();

        RpcResult<TransactionReceipt> receiptResult = rpc.getTransactionReceipt(hash);
        assertRpcSuccess(receiptResult);

        return receiptResult.getResult();
    }

    /**
     * Returns the bytes of an FVM contract named 'FvmCrossCallDispatcher'.
     *
     * See src/org/aion/harness/tests/contracts/fvm/FvmCrossCallDispatcher.sol for the contract itself.
     *
     * We use the bytes directly here, just because we don't have any good utilities in place for
     * generating these bytes ourselves.
     */
    private byte[] getFvmContractBytes() throws DecoderException {
        return Hex.decodeHex("605060405234156100105760006000fd5b610015565b610105806100246000396000f30060506040526000356c01000000000000000000000000900463ffffffff1680638f2a06d514603157602b565b60006000fd5b3415603c5760006000fd5b605860048080806010013590359091602001909192905050605a565b005b818160405180806f73616d706c652875696e743132382900815260100150600f019050604051809103902090506c0100000000000000000000000090046040518163ffffffff166c01000000000000000000000000028152600401600060405180830381600088885af19350505050151560d45760006000fd5b5b50505600a165627a7a72305820c49126936a14c9e5246af4d9f33d7c62b2178c20b7986799854c73ef0fe047280029");
    }

    private byte[] getAvmContractBytes() {
        return new CodeAndArguments(JarBuilder.buildJarForMainAndClasses(AvmCrossCallDispatcher.class, ABIDecoder.class, ABIToken.class, ABIException.class), new byte[0]).encodeToBytes();
    }

    private byte[] getFvmCallDispatcherBytes(Address target) throws DecoderException {
        byte[] functionHash = Hex.decodeHex("8f2a06d5");
        return joinArrays(functionHash, target.getAddressBytes());
    }

    private byte[] getAvmCallDispatcherBytes(Address target) {
        byte[] encodedAddress = ABIUtil.encodeOneObject(new avm.Address(target.getAddressBytes()));
        byte[] encodedFvmCall = ABIUtil.encodeOneObject(new byte[32]);  // these bytes don't matter
        return joinArrays(encodedAddress, encodedFvmCall);
    }

    private byte[] joinArrays(byte[] array1, byte[] array2) {
        int totalLength = array1.length + array2.length;
        byte[] array = Arrays.copyOf(array1, totalLength);
        System.arraycopy(array2, 0, array, array1.length, array2.length);
        return array;
    }

    private BigInteger getNonce() throws InterruptedException {
        RpcResult<BigInteger> nonceResult = rpc.getNonce(preminedPrivateKey.getAddress());
        assertRpcSuccess(nonceResult);
        return nonceResult.getResult();
    }

    private static void destroyLogs() throws IOException {
        FileUtils.deleteDirectory(new File(System.getProperty("user.dir") + "/logs"));
    }
}
