package org.aion.harness;

import org.aion.harness.misc.Assumptions;
import org.aion.harness.util.*;
import org.aion.harness.result.Result;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.concurrent.TimeUnit;

/**
 * A node that wraps the Java kernel.
 *
 * A JavaNode is thread-safe.
 */
public final class JavaNode implements Node {
    private LogManager logManager = new LogManager();

    // The running instance of the kernel.
    private Process runningKernel = null;

    /**
     * Builds a new kernel and copies it into a directory named node.
     */
    @Override
    public Result initialize() throws IOException, InterruptedException {
        return initialize(false);
    }

    /**
     * Builds a new kernel and copies it into a directory named node.
     */
    @Override
    public Result initializeVerbose() throws IOException, InterruptedException {
        return initialize(true);
    }

    /**
     * Starts a node.
     *
     * @throws IllegalStateException if the node is already started or the kernel does not exist.
     */
    @Override
    public synchronized Result start() throws IOException, InterruptedException {
        if (isAlive()) {
            throw new IllegalStateException("there is already a kernel running.");
        }

        if (!NodeFileManager.getKernelDirectory().exists()) {
            throw new IllegalStateException("there is no kernel directory!");
        }

        System.out.println(Assumptions.LOGGER_BANNER + "Starting Java kernel node...");
        ProcessBuilder builder = new ProcessBuilder("./aion.sh", "-n", NodeFileManager.NETWORK)
                .directory(NodeFileManager.getKernelDirectory());

        builder.redirectOutput(this.logManager.getCurrentOutputLogFile());
        builder.redirectError(this.logManager.getCurrentErrorLogFile());

        this.runningKernel = builder.start();

        //TODO: probably a better solution than this (scanning logs?)
        Thread.sleep(TimeUnit.SECONDS.toMillis(10));
        Result result = isAlive() ? Result.successful() : Result.unsuccessful(Assumptions.PRODUCTION_ERROR_STATUS, "node has not started");

        if (result.success) {
            result = LogReader.singleton().startReading(this.logManager.getCurrentOutputLogFile());
        }

        return result;
    }

    /**
     * Stops the node if it is currently running.
     */
    @Override
    public synchronized void stop() throws InterruptedException {
        if (isAlive()) {
            System.out.println(Assumptions.LOGGER_BANNER + "Stopping Java kernel node...");
            this.runningKernel.destroy();
            this.runningKernel = null;

            LogReader.singleton().stopReading();

            //TODO: wait until the shutdown message is read or a timeout?
        }

        // Finds the kernel and kills it (above we are killing the aion.sh script,
        // which is not guaranteed to kill the kernel). We find these processes because we know the
        // directory of the executable, so we can hunt it down precisely.
        String executableDir = NodeFileManager.getDirectoryOfExecutableKernel().getAbsolutePath();
        ProcessHandle.allProcesses()
                .filter(process -> process.info().command().toString().contains(executableDir))
                .forEach(kernel -> kernel.destroy());
    }

    /**
     * Returns true if the node is currently running.
     */
    @Override
    public synchronized boolean isAlive() {
        return ((this.runningKernel != null) && (this.runningKernel.isAlive()));
    }

    /**
     * Resets the state of the node.
     *
     * @throws IllegalStateException if the node is currently running or there is no kernel.
     */
    @Override
    public synchronized void resetState() throws IOException {
        if (isAlive()){
            throw new IllegalStateException("Cannot reset state while the node is running.");
        }

        if (!NodeFileManager.getKernelDirectory().exists()) {
            throw new IllegalStateException("there is no kernel directory!");
        }

        System.out.println(Assumptions.LOGGER_BANNER + "Resetting the state of the Java kernel node...");
        File database = NodeFileManager.getKernelDatabase();
        if (database.exists()) {
            FileUtils.deleteDirectory(database);
        }
    }

    private synchronized Result initialize(boolean verbose) throws IOException, InterruptedException {
        if (!buildJavaKernel(verbose)) {
            return Result.unsuccessful(Assumptions.PRODUCTION_ERROR_STATUS, "Kernel source build failed.");
        }

        if (!initializeButSkipKernelBuild(verbose)) {
            return Result.unsuccessful(Assumptions.PRODUCTION_ERROR_STATUS, "Fetching kernel build failed.");
        }

        return this.logManager.setupLogFiles();
    }

    /**
     * FOR TESTING.
     *
     * This skips the process of building the kernel, assumes it is already built and is in the
     * pack directory, and continues the initialization from there.
     *
     * This is mostly just a convenience method for tests to bring down the time it takes to run
     * them all.
     */
    public synchronized boolean initializeButSkipKernelBuild(boolean verbose) throws IOException, InterruptedException {
        File nodeDestination = NodeFileManager.getNodeDirectory();
        if (nodeDestination.exists()) {
            throw new IllegalStateException("node directory already exists.");
        }

        if (!nodeDestination.mkdir()) {
            throw new IllegalStateException("Failed to make directory: " + nodeDestination);
        }

        System.out.println(Assumptions.LOGGER_BANNER + "Fetching the built kernel...");
        File tarSourceDirectory = NodeFileManager.getKernelTarSourceDirectory();
        if (!tarSourceDirectory.isDirectory()) {
            throw new IllegalStateException("Unable to find kernel tar source directory at: " + tarSourceDirectory);
        }

        File kernelTarFile = null;
        File[] entries = tarSourceDirectory.listFiles();
        if (entries == null) {
            throw new NoSuchFileException("Could not find kernel tar file.");
        }

        for (File file : entries) {
            if (Assumptions.KERNEL_TAR_PATTERN.matcher(file.getName()).matches()) {
                kernelTarFile = file;
            }
        }

        if (kernelTarFile == null) {
            throw new NoSuchFileException("Could not find kernel tar file.");
        }

        File tarDestination = new File(nodeDestination.getPath() + File.separator + Assumptions.NEW_KERNEL_TAR_NAME);
        Files.copy(kernelTarFile.toPath(), tarDestination.toPath());

        ProcessBuilder builder = new ProcessBuilder("tar", "xvjf", tarDestination.getName())
                .directory(tarDestination.getParentFile());

        if (verbose) {
            builder.inheritIO();
        }

        int untarStatus = builder.start().waitFor();
        tarDestination.delete();

        if (!this.logManager.setupLogFiles().success) {
            return false;
        }

        return untarStatus == 0;
    }

    private boolean buildJavaKernel(boolean verbose) throws IOException, InterruptedException {
        System.out.println(Assumptions.LOGGER_BANNER + "Building the Java kernel from source...");

        ProcessBuilder builder = new ProcessBuilder("./gradlew", "clean", "pack")
                .directory(NodeFileManager.getKernelRepositoryDirectory());

        if (verbose) {
            builder.inheritIO();
        }

        return builder.start().waitFor() == 0;
    }

}
