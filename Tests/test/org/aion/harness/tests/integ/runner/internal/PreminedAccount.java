package org.aion.harness.tests.integ.runner.internal;

import java.math.BigInteger;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.TimeoutException;
import org.aion.harness.kernel.Address;
import org.aion.harness.kernel.PrivateKey;
import org.aion.harness.tests.integ.runner.exception.UnexpectedTestRunnerException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A pre-mined account, or an account loaded up with the specified initial amount of funds, to be
 * used in tests that require Aion to send transactions.
 */
public final class PreminedAccount implements TestRule {
    private PrivateKey preminedForCaller;
    private BigInteger initialAmount;
    private BigInteger nonce = BigInteger.ZERO; // this is a new account!

    public PreminedAccount(BigInteger initialAmount) {
        this.initialAmount = initialAmount;
    }

    /**
     * This method is invoked via reflection by the custom runners to initialize the premined private key.
     */
    private void setPrivateKey() {
        try {
            this.preminedForCaller = PrivateKey.random();
        } catch (InvalidKeySpecException e) {
            throw new UnexpectedTestRunnerException("Failed to generated a random pre-mined address!");
        }
    }

    /**
     * Do not invoke.
     */
    @Override
    public Statement apply(Statement statement, Description description) {
        throw new UnsupportedOperationException("Didn't you read my doc?");
    }

    /**
     * Returns the private key of this pre-mined account.
     */
    public PrivateKey getPrivateKey() {
        return this.preminedForCaller;
    }

    /**
     * Returns the address of this pre-mined account.
     */
    public Address getAddress() {
        return this.preminedForCaller.getAddress();
    }

    /**
     * Returns the current nonce of this account but does NOT increment it.
     *
     * This method should only be used for peeking at the current nonce value.
     */
    public BigInteger getNonce() {
        return this.nonce;
    }

    /**
     * Increments the current nonce value by one.
     */
    public void incrementNonce() {
        this.nonce = this.nonce.add(BigInteger.ONE);
    }

    /**
     * Returns the current nonce of this account and increments it so that the next time this method
     * is invoked the nonce will be one larger than what it returned this time.
     */
    public BigInteger getAndIncrementNonce() {
        BigInteger previous = this.nonce;
        this.nonce = this.nonce.add(BigInteger.ONE);
        return previous;
    }

    /**
     * Returns the current nonce of this account and increments it by the given amount.
     */
    public BigInteger getAndIncrementNonceBy(int amountToIncrementBy) {
        BigInteger previous = this.nonce;
        this.nonce = this.nonce.add(BigInteger.valueOf(amountToIncrementBy));
        return previous;
    }

    /**
     * This is called by our custom Runners to load these accounts up with funds to fake up the 'pre-minedness'.
     */
    private void getFundsFromRealPreminedAccount(PreminedAccountFunder dispatcher)
        throws InterruptedException, TimeoutException {
        dispatcher.fundAccount(this.preminedForCaller.getAddress(), this.initialAmount);
    }
}
