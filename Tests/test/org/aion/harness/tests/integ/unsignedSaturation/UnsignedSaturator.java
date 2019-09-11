package org.aion.harness.tests.integ.unsignedSaturation;

import java.math.BigInteger;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import org.aion.harness.kernel.Address;
import org.aion.harness.kernel.PrivateKey;
import org.aion.harness.kernel.UnsignedTransaction;
import org.aion.harness.main.RPC;
import org.aion.harness.result.RpcResult;
import org.aion.harness.tests.integ.saturation.SaturationReport;

public final class UnsignedSaturator implements Callable<SaturationReport> {
    private final RPC rpc = RPC.newRpc("127.0.0.1", "8545");
    private final String name;
    private final CyclicBarrier barrier;
    private final Address sender;

    public UnsignedSaturator(int threadID, CyclicBarrier barrier, Address sender) {
        this.name = "[Saturator-#" + threadID + "]";
        this.barrier = barrier;
        this.sender = sender;
        Thread.currentThread().setName(this.name);
    }

    @Override
    public SaturationReport call() throws Exception {

        // Wait for all the sender threads to be ready to send.
        this.barrier.await();

        // We only want to do the next steps if nothing has gone wrong so far.
        SaturationReport report = null;

        // Send the transactions.
        System.out.println(this.name + " sending the " + UnsignedSaturationTest.NUM_TRANSACTIONS + " transactions ...");

        // Unlock the account.
        RpcResult<Boolean> unlockResult = this.rpc.unlockKeystoreAccount(this.sender, UnsignedSaturationTest.PASSWORD, 10, TimeUnit.HOURS);
        if (!unlockResult.isSuccess()) {
            report = SaturationReport.unsuccessful(this.name, "Keystore account unlock attempt failed: " + unlockResult.getError());
        }
        if (!unlockResult.getResult()) {
            report = SaturationReport.unsuccessful(this.name, "Keystore account was not unlocked!");
        }

        Address destination = PrivateKey.random().getAddress();
        if (report == null) {
            for (int i = 0; i < UnsignedSaturationTest.NUM_TRANSACTIONS; i++) {
                BigInteger nonce = BigInteger.valueOf(i);
                UnsignedTransaction transaction = UnsignedTransaction.newNonCreateTransaction(this.sender, destination, nonce, UnsignedSaturationTest.TRANSFER_AMOUNT, new byte[0], UnsignedSaturationTest.ENERGY_LIMIT, UnsignedSaturationTest.ENERGY_PRICE);
                this.rpc.sendUnsignedTransaction(transaction);
            }
            System.out.println(this.name + " all transactions sent!");
        }

        // Verify that all the transactions were sent.
        // We just wait until the destination address has the expected balance or until we time out.
        System.out.println(this.name + " waiting for all the transactions to process ...");

        BigInteger expectedBalance = BigInteger.valueOf(UnsignedSaturationTest.NUM_TRANSACTIONS).multiply(UnsignedSaturationTest.TRANSFER_AMOUNT);

        long startTimeInNanos = System.nanoTime();
        long currentTimeInNanos = startTimeInNanos;
        while ((report == null) && (currentTimeInNanos < (startTimeInNanos + UnsignedSaturationTest.THREAD_TIMEOUT_IN_NANOS))) {

            RpcResult<BigInteger> balanceResult = this.rpc.getBalance(destination);
            if (!balanceResult.isSuccess()) {
                report = SaturationReport.unsuccessful(this.name, "Failed to get destination balance due to: " + balanceResult.getError());
            }

            if (balanceResult.getResult().equals(expectedBalance)) {
                report = SaturationReport.successful(this.name);
            }

            Thread.sleep(UnsignedSaturationTest.THREAD_DELAY_IN_MILLIS);
            currentTimeInNanos = System.nanoTime();
        }

        // If the report is null then we must have timed out.
        if (report == null) {
            report = SaturationReport.unsuccessful(this.name, "Timed out waiting for transactions to process!");
        }

        System.out.println(this.name + " all transactions have been processed!");

        return report;
    }
}
