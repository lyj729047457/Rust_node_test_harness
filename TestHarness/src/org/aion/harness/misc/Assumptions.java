package org.aion.harness.misc;

import java.util.regex.Pattern;

/**
 * We try to document our assumptions here. These are things that we eventually want to eliminate
 * from the harness, but are still crucial to its proper functioning in these early stages.
 */
public final class Assumptions {

    // Other assumptions about the system include:
    // Genesis node has the appropriate pre-mined account set in 'mastery'
    // Config has all peers removed from 'mastery'
    // Config has mining enabled, RPC enabled on port 8545
    // Config has the TX log set to TRACE
    // AionBlockchainImpl broadcasts all transactions, by hash, when successfully sealed into block
    // aion project is in same directory as this project
    // mainnet rpc channel is set active (used by tests)
    // RPC get-receipt knows how to handle Avm addresses
    // aion project must be using node_test_harness branch [ this branch meets all these criteria ]

    public static final Pattern KERNEL_TAR_PATTERN = Pattern.compile("^aion-v(.|\\s)+\\.tar\\.bz2$");

    // Not really an assumption, should eventually be moved some place more meaningful, but no kind
    // of global data class exists yet.
    public static final String LOGGER_BANNER = "TESTING-HARNESS: ";

    public static final String NEW_KERNEL_TAR_NAME = "kernel.tar.bz2";

    public static final String PREMINED_PRIVATE_KEY = "223f19377d95582055bd8972cf3ffd635d2712a7171e4888091a066b9f4f63d5";

}
