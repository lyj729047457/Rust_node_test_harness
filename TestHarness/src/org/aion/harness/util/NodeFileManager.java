package org.aion.harness.util;

import java.io.File;
import java.io.IOException;
import org.aion.harness.main.Network;
import org.aion.harness.misc.Assumptions;

/**
 * A convenience class that lets us keep all the relevant files and directories relating to a node together in one place
 * and provides descriptively named getter methods for ease of use.
 */
public class NodeFileManager {
    public static final String WORKING_DIR = System.getProperty("user.dir");

    private static final String SANDBOX_DIR = WORKING_DIR + File.separator + "sandbox";
    private static final String LOG_DIR = WORKING_DIR + File.separator + "logs";
    private static final String LOG_ARCHIVE_DIR = LOG_DIR + File.separator + "archive";
    private static final String TEMPORARY_DATABASE = WORKING_DIR + File.separator + "temporary_database";
    private static final String TEMPORARY_TAR_FILE = SANDBOX_DIR + File.separator + "temporary_tar.tar.bz2";

    public static String getSandboxPath() {
        return SANDBOX_DIR;
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

    public static File getTemporaryTarFile() {
        return new File(TEMPORARY_TAR_FILE);
    }

    /**
     * Returns the path to the database for the given network and root directory of the built kernel.
     *
     * @implNote this gives the path of the db used by a {@link org.aion.harness.main.impl.JavaNode},
     *           not the Rust one!  see {@link org.aion.harness.main.NodeConfigurations#getDatabaseRust(String)}
     *           for that.
     */
    public static File getDatabaseOfJava(File builtKernelDirectory, Network network) throws IOException {
        return new File(builtKernelDirectory.getCanonicalPath() + File.separator + network.string() + File.separator + "database");
    }

    /**
     * Returns the directory of the executable for the given built kernel root directory.
     */
    public static String getExecutableDirectoryOf(File builtKernelDirectory) throws IOException {
        return builtKernelDirectory.getCanonicalPath() + File.separator + "rt/bin/java";
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
