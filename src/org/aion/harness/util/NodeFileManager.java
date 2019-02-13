package org.aion.harness.util;

import java.io.File;

/**
 * A convenience class that lets us keep all the relevant files and directories relating to a node together in one place
 * and provides descriptively named getter methods for ease of use.
 */
public class NodeFileManager {
    public static final String NETWORK = "mastery";

    private static final String WORKING_DIR = System.getProperty("user.dir");
    private static final File KERNEL_REPO = new File(WORKING_DIR + File.separator + ".." + File.separator + "aion");
    private static final File TAR_SOURCE = new File(KERNEL_REPO + File.separator + "pack");
    private static final File NODE_DIR = new File(WORKING_DIR + File.separator + "node");
    private static final File KERNEL_DIR = new File(NODE_DIR + File.separator + "aion");
    private static final File DATABASE = new File(KERNEL_DIR + File.separator + NETWORK + File.separator + "database");
    private static final File EXECUTABLE_DIR = new File(KERNEL_DIR.getAbsolutePath() + "/rt/bin/java");
    private static final File LOG_DIR = new File(WORKING_DIR + File.separator + "logs");
    private static final File LOG_ARCHIVE_DIR = new File(LOG_DIR.getAbsolutePath() + File.separator + "archive");

    public static File getKernelRepositoryDirectory() {
        return KERNEL_REPO;
    }

    public static File getKernelTarSourceDirectory() {
        return TAR_SOURCE;
    }

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

}
