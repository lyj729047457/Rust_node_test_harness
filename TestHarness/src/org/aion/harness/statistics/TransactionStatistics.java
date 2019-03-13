package org.aion.harness.statistics;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.aion.harness.main.types.TransactionReceipt;

public final class TransactionStatistics {
    private final List<TransactionReceipt> receipts;

    private TransactionStatistics(List<TransactionReceipt> receipts) {
        if (receipts == null) {
            throw new NullPointerException("Cannot construct TransactionStatistics with null receipts list.");
        }

        this.receipts = new ArrayList<>(receipts);
    }

    public static TransactionStatistics from(List<TransactionReceipt> receipts) {
        return new TransactionStatistics(receipts);
    }

    /**
     * Prints some basic transaction statistics to console.
     *
     * These statistics are simply the number of transactions per block.
     */
    public void printStatistics() {
        Map<BigInteger, Integer> mapBlockNumToTransactionCount = new HashMap<>();

        for (TransactionReceipt receipt : this.receipts) {
            Integer count = mapBlockNumToTransactionCount.get(receipt.getBlockNumber());

            if (count == null) {
                mapBlockNumToTransactionCount.put(receipt.getBlockNumber(), 1);
            } else {
                mapBlockNumToTransactionCount.put(receipt.getBlockNumber(), count + 1);
            }
        }

        StringBuilder builder = new StringBuilder("Block transaction counts = [");

        int sum = 0;
        int index = 0;
        for (Integer count : mapBlockNumToTransactionCount.values()) {
            builder.append(count);

            if (index < mapBlockNumToTransactionCount.size() - 1) {
                builder.append(", ");
            }

            sum += count;
            index++;
        }

        builder.append("] total = ").append(sum);

        System.out.println("---------------------------------------------------------------------");
        System.out.println(builder.toString());
        System.out.println("---------------------------------------------------------------------");
    }

}
