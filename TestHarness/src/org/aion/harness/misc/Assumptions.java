package org.aion.harness.misc;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public final class Assumptions {

    // Other assumptions about the system include:
    // Genesis node has the appropriate pre-mined account set in 'mastery'
    // Config has all peers removed from 'mastery'
    // Config has mining enabled, RPC enabled on port 8545
    // Config has the TX log set to TRACE
    // AionBlockchainImpl broadcasts all transactions, by hash, when successfully sealed into block
    // aion project is in same directory as this project
    // aion project must be using node_test_harness branch
    // mainnet rpc channel is set active (used by tests)

    public static final Pattern KERNEL_TAR_PATTERN = Pattern.compile("^aion-v(.|\\s)+\\.tar\\.bz2$");

    public static final String LOGGER_BANNER = "TESTING-HARNESS: ";

    public static final String NEW_KERNEL_TAR_NAME = "kernel.tar.bz2";

    public static final String PREMINED_PRIVATE_KEY = "223f19377d95582055bd8972cf3ffd635d2712a7171e4888091a066b9f4f63d5";

    public static final int PRODUCTION_ERROR_STATUS = -9999;

    public static final int TESTING_ERROR_STATUS = -7777;

    public static final long LISTENER_TIMEOUT_SECS = 60;

    public static final long REQUEST_MANAGER_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(60);

    public static final long SLEEP_TIME_MILLIS = TimeUnit.SECONDS.toMillis(2);

}
