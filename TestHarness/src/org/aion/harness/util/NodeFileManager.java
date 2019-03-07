package org.aion.harness.util;

import java.io.File;
import org.aion.harness.misc.Assumptions;

/**
 * A convenience class that lets us keep all the relevant files and directories relating to a node together in one place
 * and provides descriptively named getter methods for ease of use.
 */
public class NodeFileManager {
    public static final String WORKING_DIR = System.getProperty("user.dir");

    private static final File NODE_DIR = new File(WORKING_DIR + File.separator + "node");
    private static final File KERNEL_DIR = new File(NODE_DIR + File.separator + "aion");
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

    public static File getBuiltKernelDirectory(File kernelSourceDirectory) {
        if (kernelSourceDirectory == null) {
            throw new NullPointerException("Cannot get built kernel directory from a null source directory.");
        }

        return new File(kernelSourceDirectory.getAbsolutePath() + File.separator + "pack");
    }

    public static File getBuiltKernel(File directory) {
        if (directory == null) {
            throw new NullPointerException("Cannot find a file in a null directory.");
        }

        File[] entries = directory.listFiles();
        if (entries == null) {
            throw new IllegalArgumentException("Given an empty directory!");
        }

        for (File entry : entries) {
            if (Assumptions.KERNEL_TAR_PATTERN.matcher(entry.getName()).matches()) {
                return entry;
            }
        }

        return null;
    }

}
