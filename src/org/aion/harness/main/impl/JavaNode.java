package org.aion.harness.main.impl;

import org.aion.harness.main.Node;
import org.aion.harness.main.NodeListener;
import org.aion.harness.main.event.Event;
import org.aion.harness.main.event.IEvent;
import org.aion.harness.main.global.SingletonFactory;
import org.aion.harness.misc.Assumptions;
import org.aion.harness.result.Result;
import org.aion.harness.result.StatusResult;
import org.aion.harness.util.*;
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

    // The running instance of the kernel.
    private Process runningKernel = null;

    /**
     * Builds a new kernel and copies it into a directory named node.
     */
    @Override
    public StatusResult initialize() throws IOException, InterruptedException {
        return initialize(false);
    }

    /**
     * Builds a new kernel and copies it into a directory named node.
     */
    @Override
    public StatusResult initializeVerbose() throws IOException, InterruptedException {
        return initialize(true);
    }

    /**
     * Builds the kernel from source.
     *
     * Displays the I/O of the build process.
     *
     * @return a result indicating the success of failure of this method.
     */
    @Override
    public Result buildKernelVerbose() {
        return buildJavaKernel(true);
    }

    /**
     * Builds the kernel from source.
     *
     * @return a result indicating the success of failure of this method.
     */
    @Override
    public Result buildKernel() {
        return buildJavaKernel(false);
    }

    /**
     * Starts a node.
     *
     * @throws IllegalStateException if the node is already started or the kernel does not exist.
     */
    @Override
    public synchronized Result start() {
        if (isAlive()) {
            throw new IllegalStateException("there is already a kernel running.");
        }

        if (!NodeFileManager.getKernelDirectory().exists()) {
            throw new IllegalStateException("there is no kernel directory!");
        }

        System.out.println(Assumptions.LOGGER_BANNER + "Starting Java kernel node...");

        try {
            ProcessBuilder builder = new ProcessBuilder("./aion.sh", "-n", NodeFileManager.NETWORK)
                .directory(NodeFileManager.getKernelDirectory());

            LogManager logManager = SingletonFactory.singleton().logManager();
            File outputLog = logManager.getCurrentOutputLogFile();

            builder.redirectOutput(outputLog);
            builder.redirectError(logManager.getCurrentErrorLogFile());

            this.runningKernel = builder.start();

            return waitForRpcServerToStart(outputLog);

        } catch (Exception e) {
            return Result.unsuccessfulDueToException(e);
        }
    }

    /**
     * Stops the node if it is currently running.
     */
    @Override
    public synchronized Result stop() {
        Result result;

        if (isAlive()) {
            System.out.println(Assumptions.LOGGER_BANNER + "Stopping Java kernel node...");

            try {

                this.runningKernel.destroy();
                boolean shutdown = this.runningKernel.waitFor(1, TimeUnit.MINUTES);
                this.runningKernel = null;
                SingletonFactory.singleton().logReader().stopReading();

                result = (shutdown) ? Result.successful() : Result.unsuccessfulDueTo("Timed out waiting for node to shut down!");

            } catch (Exception e) {
                result = Result.unsuccessfulDueToException(e);
            } finally {
                System.out.println(Assumptions.LOGGER_BANNER + "Java kernel node stopped.");
            }

        } else {
            result = Result.unsuccessfulDueTo("Node is not currently alive!");
        }

        // Finds the kernel and kills it (above we are killing the aion.sh script,
        // which is not guaranteed to kill the kernel). We find these processes because we know the
        // directory of the executable, so we can hunt it down precisely.
        String executableDir = NodeFileManager.getDirectoryOfExecutableKernel().getAbsolutePath();
        ProcessHandle.allProcesses()
            .filter(process -> process.info().command().toString().contains(executableDir))
            .forEach(kernel -> kernel.destroy());

        return result;
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
    public synchronized Result resetState() {
        if (isAlive()){
            throw new IllegalStateException("Cannot reset state while the node is running.");
        }

        if (!NodeFileManager.getKernelDirectory().exists()) {
            throw new IllegalStateException("there is no kernel directory!");
        }

        System.out.println(Assumptions.LOGGER_BANNER + "Resetting the state of the Java kernel node...");

        try {

            File database = NodeFileManager.getKernelDatabase();
            if (database.exists()) {
                FileUtils.deleteDirectory(database);
            }
            return Result.successful();

        } catch (Exception e) {
            return Result.unsuccessfulDueToException(e);
        }
    }

    private synchronized StatusResult initialize(boolean verbose) throws IOException, InterruptedException {
        if (!buildJavaKernel(verbose).success) {
            return StatusResult.unsuccessful(Assumptions.PRODUCTION_ERROR_STATUS, "Kernel source build failed.");
        }

        if (!initializeButSkipKernelBuild(verbose)) {
            return StatusResult.unsuccessful(Assumptions.PRODUCTION_ERROR_STATUS, "Fetching kernel build failed.");
        }

        return SingletonFactory.singleton().logManager().setupLogFiles();
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

        if (!SingletonFactory.singleton().logManager().setupLogFiles().success) {
            return false;
        }

        return untarStatus == 0;
    }

    private Result buildJavaKernel(boolean verbose) {
        System.out.println(Assumptions.LOGGER_BANNER + "Building the Java kernel from source...");

        try {

            ProcessBuilder builder = new ProcessBuilder("./gradlew", "clean", "pack")
                .directory(NodeFileManager.getKernelRepositoryDirectory());

            if (verbose) {
                builder.inheritIO();
            }

            return (builder.start().waitFor() == 0)
                ? Result.successful()
                : Result.unsuccessfulDueTo("An error occurred building the kernel!");

        } catch (Exception e) {
            return Result.unsuccessfulDueToException(e);
        }
    }

    /**
     * Waits for the RPC server to start before returning.
     */
    private Result waitForRpcServerToStart(File outputLog) {
        // We wait for the rpc event to know we are ok to return. There is a chance that we will miss
        // this event and start listening too late. That is why we timeout after 20 seconds, which
        // should be more than sufficient for the server to activate, and then we check if the node
        // is still live.
        // See issue #1 relating to this decision, which will be refactored in the future.

        if (isAlive()) {
            // We wait for the Rpc event or else 20 seconds, in case we come too late and never see it.
            IEvent rpcEvent = new Event("rpc-server - (UNDERTOW) started");

            StatusResult result = SingletonFactory.singleton().logReader().startReading(outputLog);
            if (!result.success) {
                return Result.unsuccessfulDueTo(result.error);
            }

            new NodeListener().waitForEvent(rpcEvent, TimeUnit.SECONDS.toMillis(20));
        }

        return (isAlive())
            ? Result.successful()
            : Result.unsuccessfulDueTo("Node failed to start!");
    }

}
