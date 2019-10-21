package org.aion.harness.tests.integ;

import static org.aion.harness.tests.contracts.Assertions.assertRpcSuccess;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.aion.api.IAionAPI;
import org.aion.api.impl.AionAPIImpl;
import org.aion.api.type.ApiMsg;
import org.aion.api.type.BlockDetails;
import org.aion.api.type.TxDetails;
import org.aion.harness.kernel.SignedTransaction;
import org.aion.harness.main.NodeFactory.NodeType;
import org.aion.harness.main.RPC;
import org.aion.harness.main.event.IEvent;
import org.aion.harness.main.types.ReceiptHash;
import org.aion.harness.main.types.TransactionReceipt;
import org.aion.harness.result.FutureResult;
import org.aion.harness.result.LogEventResult;
import org.aion.harness.result.RpcResult;
import org.aion.harness.tests.integ.runner.ExcludeNodeType;
import org.aion.harness.tests.integ.runner.internal.LocalNodeListener;
import org.aion.harness.tests.integ.runner.internal.PreminedAccount;
import org.aion.harness.tests.integ.runner.SequentialRunner;
import org.aion.harness.tests.integ.runner.internal.PrepackagedLogEventsFactory;
import org.aion.harness.util.SimpleLog;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.junit.Ignore;

@Ignore
@RunWith(SequentialRunner.class)
@ExcludeNodeType(NodeType.RUST_NODE)
public class JavaApiSmokeTest {
    private static final long ENERGY_LIMIT = 1_234_567L;
    private static final long ENERGY_PRICE = 10_010_020_345L;

    private final SimpleLog log = new SimpleLog("org.aion.harness.tests.integ.JavaApiSmokeTest");

    private static RPC rpc = RPC.newRpc("127.0.0.1", "8545");
    
    @Rule
    private PreminedAccount preminedAccount = new PreminedAccount(BigInteger.valueOf(1_000_000_000_000_000_000L));
    
    @Rule
    private LocalNodeListener listener = new LocalNodeListener();

    @Rule
    private PrepackagedLogEventsFactory prepackagedLogEventsFactory = new PrepackagedLogEventsFactory();

    @Test
    public void testGetBlockDetailsByRange() throws Exception {
        IAionAPI api = AionAPIImpl.inst();
        ApiMsg connectionMsg = api.connect("tcp://localhost:8547");
        int tries = 0;

        while(connectionMsg.isError() && tries++ <= 10) {
            log.log("trying again after 3 sec");
            connectionMsg = api.connect("tcp://localhost:8547");
        }
        if(connectionMsg.isError()) {
            throw new RuntimeException("error: aion_api can't connect to kernel (after retrying 10 times).");
        }

        // send a transaction that deploys a simple contract and loads it with some funds
        log.log("Sending a transaction");
        BigInteger amount = BigInteger.TEN.pow(13).add(BigInteger.valueOf(2_938_652));
        SignedTransaction transaction = buildTransactionToCreateAndTransferToFvmContract(amount);
        TransactionReceipt createReceipt = sendTransaction(transaction);
        assertTrue(createReceipt.getAddressOfDeployedContract().isPresent());

        final long b0 = createReceipt.getBlockNumber().longValue();
        final long b2 = b0 + 2;
        long bn = b0;

        // wait for two more blocks
        while(bn < b2) {
            log.log("current block number = " + bn + "; waiting to reach block number " + b2);
            TimeUnit.SECONDS.sleep(10); // expected block time
            bn = rpc.blockNumber().getResult();
        }


        log.log(String.format("Calling getBlockDetailsByRange(%d, %d)", b0, b2));
        ApiMsg blockDetailsMsg = api.getAdmin().getBlockDetailsByRange(b0, b2);
        assertThat(blockDetailsMsg.isError(), is(false));

        List<BlockDetails> blockDetails = blockDetailsMsg.getObject();
        assertThat("incorrect number of blocks",
            blockDetails.size(), is(3));
        assertThat("block details has incorrect block number",
            blockDetails.get(0).getNumber(), is(b0));
        assertThat("block details has incorrect block number",
            blockDetails.get(1).getNumber(), is(b0 + 1));
        assertThat("block details has incorrect block number",
            blockDetails.get(2).getNumber(), is(b2));

        long blockNumberOfTransaction = createReceipt.getBlockNumber().longValueExact();

        if (blockNumberOfTransaction == b0) {
            verifyTransactionDetailsOfBlock(blockDetails.get(0), createReceipt.getTransactionHash(), amount);
        } else if (blockNumberOfTransaction == b0 + 1) {
            verifyTransactionDetailsOfBlock(blockDetails.get(1), createReceipt.getTransactionHash(), amount);
        } else if (blockNumberOfTransaction == b2) {
            verifyTransactionDetailsOfBlock(blockDetails.get(2), createReceipt.getTransactionHash(), amount);
        } else {
            fail("Expected block number to be in range [" + b0 + ", " + b2 + "], but was: " + blockNumberOfTransaction);
        }
    }

    private void verifyTransactionDetailsOfBlock(BlockDetails blockDetails, byte[] transactionHash, BigInteger amount) {
        boolean foundTransaction = false;
        for (TxDetails transactionDetails : blockDetails.getTxDetails()) {
            if (Arrays.equals(transactionDetails.getTxHash().toBytes(), transactionHash)) {
                assertThat("block details' tx details incorrect contract address", transactionDetails.getContract(), is(not(nullValue())));
                assertThat("block details' tx details incorrect value", transactionDetails.getValue().equals(amount), is(true));
                foundTransaction = true;
            }
        }
        assertTrue(foundTransaction);
    }

    private TransactionReceipt sendTransaction(SignedTransaction transaction)
        throws InterruptedException, TimeoutException {
        // we want to ensure that the transaction gets sealed into a block.
        IEvent transactionIsSealed = prepackagedLogEventsFactory.build().getTransactionSealedEvent(transaction);
        FutureResult<LogEventResult> future = this.listener.listenForEvent(transactionIsSealed, 5, TimeUnit.MINUTES);

        // Send the transaction off.
        log.log("Sending the transaction...");
        RpcResult<ReceiptHash> sendResult = rpc.sendSignedTransaction(transaction);
        assertRpcSuccess(sendResult);

        // Wait on the future to complete and ensure we saw the transaction get sealed.
        log.log("Waiting for the transaction to process...");
        LogEventResult listenResult = future.get(5, TimeUnit.MINUTES);
        assertTrue(listenResult.eventWasObserved());
        log.log("Transaction was sealed into a block.");

        ReceiptHash hash = sendResult.getResult();

        RpcResult<TransactionReceipt> receiptResult = rpc.getTransactionReceipt(hash);
        assertRpcSuccess(receiptResult);
        return receiptResult.getResult();
    }

    private SignedTransaction buildTransactionToCreateAndTransferToFvmContract(BigInteger amount) throws DecoderException, InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        return SignedTransaction.newGeneralTransaction(
            this.preminedAccount.getPrivateKey(),
            this.preminedAccount.getNonce(),
            null,
            getFvmContractBytes(),
            ENERGY_LIMIT,
            ENERGY_PRICE,
            amount, null);
    }

    /**
     * Returns the bytes of an FVM contract named 'PayableConstructor'.
     *
     * See src/org/aion/harness/tests/contracts/fvm/PayableConstructor.sol for the contract itself.
     *
     * We use the bytes directly here, just because we don't have any good utilities in place for
     * generating these bytes ourselves.
     */
    private byte[] getFvmContractBytes() throws DecoderException {
        return Hex.decodeHex("60506040525b5b600a565b6088806100186000396000f300605060405260"
            + "00356c01000000000000000000000000900463ffffffff1680634a6a740714603b578063fc9ad4331"
            + "46043576035565b60006000fd5b60416056565b005b3415604e5760006000fd5b60546059565b005b"
            + "5b565b5b5600a165627a7a723058206bd6d88e9834838232f339ec7235f108a21441649a2cf876547"
            + "229e6c18c098c0029");
    }
}
