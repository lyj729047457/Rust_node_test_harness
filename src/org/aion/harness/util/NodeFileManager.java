package org.aion.harness.util;

import org.aion.harness.main.types.Network;
import java.io.File;

/**
 * A convenience class that lets us keep all the relevant files and directories relating to a node together in one place
 * and provides descriptively named getter methods for ease of use.
 */
public class NodeFileManager {
    public static final String WORKING_DIR = System.getProperty("user.dir");

    private static Network network = Network.MASTERY;

    private static final File NODE_DIR = new File(WORKING_DIR + File.separator + "node");
    private static final File KERNEL_DIR = new File(NODE_DIR + File.separator + "aion");
    private static final File DATABASE = new File(KERNEL_DIR + File.separator + network.string() + File.separator + "database");
    private static final File EXECUTABLE_DIR = new File(KERNEL_DIR.getAbsolutePath() + "/rt/bin/java");
    private static final File LOG_DIR = new File(WORKING_DIR + File.separator + "logs");
    private static final File LOG_ARCHIVE_DIR = new File(LOG_DIR.getAbsolutePath() + File.separator + "archive");
    private static final File TEMPORARY_DATABASE = new File(WORKING_DIR + File.separator + "database");

    public static File getNodeDirectory() {
        return NODE_DIR;
    }

    public static File getKernelDirectory() {
        return KERNEL_DIR;
    }

    public static File getKernelDatabase() {
        return DATABASE;
    }

    public static File getDirectoryOfExecutableKernel() {
        return EXECUTABLE_DIR;
    }

    public static File getLogsDirectory() {
        return LOG_DIR;
    }

    public static File getLogArchiveDirectory() {
        return LOG_ARCHIVE_DIR;
    }

    public static File getTemporaryDatabase() {
        return TEMPORARY_DATABASE;
    }

    public static String getNetworkAsString() {
        return network.string();
    }

    public static Network getNetwork() {
        return network;
    }

    public static void setNetwork(Network newNetwork) {
        if (newNetwork == null) {
            throw new NullPointerException("Cannot set a null network.");
        }
        network = newNetwork;
    }
}
