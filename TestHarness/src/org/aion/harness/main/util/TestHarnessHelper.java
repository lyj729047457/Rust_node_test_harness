package org.aion.harness.main.util;

import java.math.BigInteger;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.aion.harness.kernel.Address;
import org.aion.harness.kernel.PrivateKey;
import org.aion.harness.kernel.RawTransaction;
import org.aion.harness.result.FutureResult;
import org.aion.harness.main.types.TransactionReceipt;
import org.aion.harness.result.BulkResult;
import org.aion.harness.result.LogEventResult;
import org.aion.harness.result.RpcResult;

/**
 * A basic utility class that typically extracts objects from other objects or provides other
 * common and useful idioms.
 */
public final class TestHarnessHelper {

    /**
     * Returns a list of {@code transactions.size()} transaction hashes such that the i'th hash in
     * the returned list is the hash of the i'th transaction in the list of transactions.
     *
     * @param transactions The transactions whose hashes are to be extracted.
     * @return the transaction hashes.
     */
    public static List<byte[]> extractTransactionHashes(List<RawTransaction> transactions) {
        if (transactions == null) {
            throw new NullPointerException("Cannot extract hashes from null list of transactions.");
        }

        List<byte[]> hashes = new ArrayList<>();
        for (RawTransaction transaction : transactions) {
            if (transaction == null) {
                throw new NullPointerException("Cannot extract hash from null transaction.");
            }
            hashes.add(transaction.getTransactionHash());
        }
        return hashes;
    }

    /**
     * Returns a list of the specified number of random private keys to create.
     *
     * @param numberOfKeys The number of random keys to create.
     * @return the random private keys.
     */
    public static List<PrivateKey> randomPrivateKeys(int numberOfKeys) throws InvalidKeySpecException {
        if (numberOfKeys < 0) {
            throw new IllegalArgumentException("Cannot create a negative number of private keys!");
        }

        List<PrivateKey> keys = new ArrayList<>();
        for (int i = 0; i < numberOfKeys; i++) {
            keys.add(PrivateKey.random());
        }
        return keys;
    }

    /**
     * Returns a list of addresses corresponding to the provided list of private keys such that the
     * address at index i in the returned list corresponds to the key at index i in the input list.
     *
     * @param keys The private keys.
     * @return the addresses of the private keys.
     */
    public static List<Address> extractAddresses(List<PrivateKey> keys) {
        if (keys == null) {
            throw new NullPointerException("Cannot extract addresses from null list of keys.");
        }

        List<Address> addresses = new ArrayList<>();
        for (PrivateKey key : keys) {
            if (key == null) {
                throw new NullPointerException("Cannot extract address from null key.");
            }
            addresses.add(key.getAddress());
        }
        return addresses;
    }

    /**
     * Returns a list of addresses that are the contract addresses obtained from the list of
     * transaction receipts.
     *
     * All receipts in the list must be contract creation receipts otherwise this method will throw
     * an exception.
     *
     * The address at index i in the returned list corresponds to the receipt at index i in the
     * input list.
     *
     * @param receipts The receipts.
     * @return the contract addresses.
     */
    public static List<Address> extractContractAddresses(List<TransactionReceipt> receipts) {
        if (receipts == null) {
            throw new NullPointerException("Cannot extract contract addresses from null list of receipts.");
        }

        List<Address> addresses = new ArrayList<>();
        for (TransactionReceipt receipt : receipts) {
            if (receipt == null) {
                throw new NullPointerException("Cannot extract address from null receipt.");
            }

            Optional<Address> optionalAddress = receipt.getAddressOfDeployedContract();
            if (!optionalAddress.isPresent()) {
                throw new IllegalArgumentException("Cannot extract address from non-create receipt.");
            }

            addresses.add(optionalAddress.get());
        }
        return addresses;
    }

    /**
     * This method blocks until all of the futures given as inputs are finished and their results
     * are in.
     *
     * @param futures The futures to wait on.
     */
    public static void waitOnFutures(List<FutureResult<LogEventResult>> futures) throws InterruptedException {
        if (futures == null) {
            throw new NullPointerException("Cannot wait on null list of futures.");
        }

        for (FutureResult<LogEventResult> future : futures) {
            if (future == null) {
                throw new NullPointerException("Cannot wait on a null future.");
            }

            future.get();
        }
    }

    /**
     * Returns an array of long values that are the timestamps of the specified results, where these
     * values are in terms of the specified units.
     *
     * @param results The results whose timestamps are to be extracted.
     * @param unit The time unit to extract the timestamps in.
     * @return the timestamps.
     */
    public static <T> long[] extractResultTimestamps(List<RpcResult<T>> results, TimeUnit unit) {
        if (results == null) {
            throw new NullPointerException("Cannot extract timestamps from null list of results.");
        }
        if (unit == null) {
            throw new NullPointerException("Cannot extract timestamps using null time units.");
        }

        long[] timestamps = new long[results.size()];

        int index = 0;
        for (RpcResult<T> result : results) {
            timestamps[index] = result.getTimeOfCall(unit);
            index++;
        }

        return timestamps;
    }

    /**
     * Returns an array of long values that are the timestamps of the specified log event results,
     * where these values are in terms of the specified units.
     *
     * Returns null if one of the futures was not observed (and therefore has no timestamp).
     *
     * @param eventResults The event results.
     * @param unit The unit of time for the returned timestamps.
     * @return the timestamps on the event results.
     */
    public static long[] extractEventTimestamps(List<LogEventResult> eventResults, TimeUnit unit) {
        if (eventResults == null) {
            throw new NullPointerException("Cannot extract timestamps from null list of futures.");
        }
        if (unit == null) {
            throw new NullPointerException("Cannot extract timestamps using null time units.");
        }

        long[] timestamps = new long[eventResults.size()];

        int index = 0;
        for (LogEventResult result : eventResults) {

            if (!result.eventWasObserved()) {
                return null;
            }

            timestamps[index] = result.timeOfObservation(unit);
            index++;
        }

        return timestamps;
    }

    /**
     * Extracts all the block numbers from the corresponding transaction receipts such that the
     * number at index i in the returned list is the block number of the transaction receipt at
     * index i in the input list.
     *
     * @param receipts The receipts.
     * @return the block numbers.
     */
    public static List<BigInteger> extractBlockNumbers(List<TransactionReceipt> receipts) {
        if (receipts == null) {
            throw new NullPointerException("Cannot extract block numbers from null list of receipts");
        }

        List<BigInteger> numbers = new ArrayList<>();

        for (TransactionReceipt receipt : receipts) {
            numbers.add(receipt.getBlockNumber());
        }

        return numbers;
    }

    /**
     * Extracts all of the results from the provided list of rpc results (The results being extracted
     * here are the object obtained from the method: {@link RpcResult#getResult()}.
     *
     * The returned {@link BulkResult} is successful only if every one of the provided rpc results
     * is itself successful, and in this case {@link BulkResult#getResults()} will return a list of
     * these extracted results.
     *
     * Otherwise, if the provided list of rpc results is null, if any one of the individual rpc
     * results is null, or if any one of them is unsuccessful, this method will return an
     * unsuccessful {@link BulkResult}.
     *
     * @param rpcResults The rpc results whose result objects are to be extracted.
     * @param <T> The type of the result object to extract.
     * @return a bulk result holding these extracted objects.
     */
    public static <T> BulkResult<T> extractRpcResults(List<RpcResult<T>> rpcResults) {
        if (rpcResults == null) {
            throw new NullPointerException("Cannot extract results from a null list of rpcResults.");
        }

        List<T> results = new ArrayList<>();

        for (RpcResult<T> rpcResult : rpcResults) {
            if (rpcResult == null) {
                return BulkResult.unsuccessful("Result at index " + results.size() + " was null!");
            }

            if (!rpcResult.isSuccess()) {
                return BulkResult.unsuccessful("Result at index " + results.size() + " was unsuccessful: " + rpcResult.getError());
            }

            results.add(rpcResult.getResult());
        }

        return BulkResult.successful(results);
    }

    /**
     * Extracts all of the results from the provided list of futures (The results being extracted
     * here are the object obtained from the method: {@link FutureResult#get()}.
     *
     * The returned {@link BulkResult} is successful only if every one of the provided log event
     * results has been observed, and in this case {@link BulkResult#getResults()} will return a
     * list of these extracted results.
     *
     * Otherwise, if the provided list of futures is null, if any one of the individual futures
     * is null, or if any one of them was not observed, this method will return an unsuccessful
     * {@link BulkResult}.
     *
     * This method will block on all non-completed futures.
     *
     * @param futures The futures whose result objects are to be extracted.
     * @return a bulk result holding these extracted objects.
     */
    public static BulkResult<LogEventResult> extractFutureResults(List<FutureResult<LogEventResult>> futures) throws InterruptedException {
        if (futures == null) {
            throw new NullPointerException("Cannot extract future results from a null list of futures.");
        }

        List<LogEventResult> results = new ArrayList<>();

        for (FutureResult<LogEventResult> future : futures) {
            if (future == null) {
                return BulkResult.unsuccessful("Result at index " + results.size() + " was null!");
            }

            LogEventResult logResult = future.get();
            if (!logResult.eventWasObserved()) {
                return BulkResult.unsuccessful("Result at index " + results.size() + " was not observed.");
            }

            results.add(logResult);
        }

        return BulkResult.successful(results);
    }

}
