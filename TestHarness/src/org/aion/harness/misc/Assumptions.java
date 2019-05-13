package org.aion.harness.misc;

import java.util.regex.Pattern;

/**
 * We try to document our assumptions here. These are things that we eventually want to eliminate
 * from the harness, but are still crucial to its proper functioning in these early stages.
 */
public final class Assumptions {

    public static final Pattern KERNEL_TAR_PATTERN = Pattern.compile("^aion-v(.|\\s)+\\.tar\\.bz2$");

    // Not really an assumption, should eventually be moved some place more meaningful, but no kind
    // of global data class exists yet.
    public static final String LOGGER_BANNER = "TESTING-HARNESS: ";

    public static final String PREMINED_PRIVATE_KEY = "4c3c8a7c0292bc55d97c50b4bdabfd47547757d9e5c194e89f66f25855baacd0";

    public static final String TEST_HARNESS_LOCK_NAME = "TestHarness";

}
