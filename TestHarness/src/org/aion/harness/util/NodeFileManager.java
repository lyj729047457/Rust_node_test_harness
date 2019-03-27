package org.aion.harness.util;

import java.io.File;
import org.aion.harness.misc.Assumptions;

/**
 * A convenience class that lets us keep all the relevant files and directories relating to a node together in one place
 * and provides descriptively named getter methods for ease of use.
 */
public class NodeFileManager {
    public static final String WORKING_DIR = System.getProperty("user.dir");

    private static final String SANDBOX_DIR = WORKING_DIR + File.separator + "node";
    private static final String KERNEL_DIR = SANDBOX_DIR + File.separator + "aion";
    private static final String EXECUTABLE_DIR = KERNEL_DIR + File.separator + "rt" + File.separator + "bin" + File.separator + "java";
    private static final String LOG_DIR = WORKING_DIR + File.separator + "logs";
    private static final String LOG_ARCHIVE_DIR = LOG_DIR + File.separator + "archive";
    private static final String TEMPORARY_DATABASE = WORKING_DIR + File.separator + "temporary_database";

    public static String getSandboxPath() {
        return SANDBOX_DIR;
    }

    public static File getKernelDirectory() {
        return new File(KERNEL_DIR);
    }

    public static File getDirectoryOfExecutableKernel() {
        return new File(EXECUTABLE_DIR);
    }

    public static File getLogsDirectory() {
        return new File(LOG_DIR);
    }

    public static File getLogsArchiveDirectory() {
        return new File(LOG_ARCHIVE_DIR);
    }

    public static File getTemporaryDatabase() {
        return new File(TEMPORARY_DATABASE);
    }

    public static File getDirectoryOfBuiltTarFile(File kernelSourceDirectory) {
        if (kernelSourceDirectory == null) {
            throw new NullPointerException("Cannot get built kernel directory from a null source directory.");
        }

        return new File(kernelSourceDirectory.getAbsolutePath() + File.separator + "pack");
    }

    public static File findKernelTarFile(File directory) {
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
