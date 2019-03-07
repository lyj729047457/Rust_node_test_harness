package org.aion.harness.main.impl;

import org.aion.harness.main.LocalNode;
import org.aion.harness.main.NodeListener;
import org.aion.harness.main.event.Event;
import org.aion.harness.main.event.IEvent;
import org.aion.harness.main.global.SingletonFactory;
import org.aion.harness.main.Network;
import org.aion.harness.main.NodeConfigurations;
import org.aion.harness.misc.Assumptions;
import org.aion.harness.result.Result;
import org.aion.harness.util.*;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

/**
 * A node that wraps the Java kernel.
 *
 * A JavaNode is not thread-safe.
 */
public final class JavaNode implements LocalNode {
    private NodeConfigurations configurations = null;
    private LogReader logReader;
    private LogManager logManager;
    private final int ID;

    // The running instance of the kernel.
    private Process runningKernel = null;

    public JavaNode() {
        this.logReader = new LogReader();
        this.logManager = new LogManager();
        this.ID = SingletonFactory.singleton().nodeWatcher().addReader(this.logReader);
    }

    @Override
    public int getID() {
        return this.ID;
    }

    /**
     * Configures the node with the specified settings.
     *
     * This method must be called prior to any other actionable method in this class.
     *
     * @param configurations the configuration settings.
     */
    @Override
    public void configure(NodeConfigurations configurations) {
        if (isAlive()) {
            throw new IllegalStateException("Cannot set a node's configurations while it is running.");
        }

        if (configurations == null) {
            throw new NullPointerException("Cannot configure node with null configurations.");
        }

        this.configurations = configurations;
    }

    @Override
    public Result initializeVerbose() throws IOException, InterruptedException {
        return (this.configurations.isConditionalBuildSpecified())
            ? doConditionalBuild(true)
            : doUnconditionalBuild(true);
    }

    @Override
    public Result initialize() throws IOException, InterruptedException {
        return (this.configurations.isConditionalBuildSpecified())
            ? doConditionalBuild(false)
            : doUnconditionalBuild(false);
    }

    /**
     * Starts a node.
     *
     * @throws IllegalStateException if the node is already started or the kernel does not exist.
     */
    @Override
    public Result start() throws IOException, InterruptedException {
        if (this.configurations == null) {
            throw new IllegalStateException("Node has not been configured yet! Cannot start kernel.");
        }
        if (isAlive()) {
            throw new IllegalStateException("there is already a kernel running.");
        }

        if (!NodeFileManager.getKernelDirectory().exists()) {
            throw new IllegalStateException("there is no kernel directory!");
        }

        System.out.println(Assumptions.LOGGER_BANNER + "Starting Java kernel node...");

        ProcessBuilder builder = new ProcessBuilder("./aion.sh", "-n", this.configurations.getNetwork().string())
            .directory(NodeFileManager.getKernelDirectory());

        File outputLog = this.logManager.getCurrentOutputLogFile();

        if (outputLog == null) {
            this.logManager.setupLogFiles();
            outputLog = this.logManager.getCurrentOutputLogFile();
        }

        builder.redirectOutput(outputLog);
        builder.redirectError(this.logManager.getCurrentErrorLogFile());

        this.runningKernel = builder.start();

        return waitForRpcServerToStart(outputLog);
    }

    /**
     * Stops the node if it is currently running.
     */
    @Override
    public Result stop() throws InterruptedException {
        if (this.configurations == null) {
            throw new IllegalStateException("Node has not been configured yet! Cannot stop kernel.");
        }

        Result result;

        if (isAlive()) {
            System.out.println(Assumptions.LOGGER_BANNER + "Stopping Java kernel node...");

            this.runningKernel.destroy();
            boolean shutdown = this.runningKernel.waitFor(1, TimeUnit.MINUTES);
            this.runningKernel = null;
            this.logReader.stopReading();

            result = (shutdown) ? Result.successful() : Result.unsuccessfulDueTo("Timed out waiting for node to shut down!");

            System.out.println(Assumptions.LOGGER_BANNER + "Java kernel node stopped.");

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
    public boolean isAlive() {
        return ((this.runningKernel != null) && (this.runningKernel.isAlive()));
    }

    /**
     * Resets the state of the node.
     *
     * @throws IllegalStateException if the node is currently running or there is no kernel.
     */
    @Override
    public Result resetState() throws IOException {
        if (this.configurations == null) {
            throw new IllegalStateException("Node has not been configured yet! Cannot reset kernel state.");
        }
        if (isAlive()){
            throw new IllegalStateException("Cannot reset state while the node is running.");
        }

        if (!NodeFileManager.getKernelDirectory().exists()) {
            throw new IllegalStateException("there is no kernel directory!");
        }

        System.out.println(Assumptions.LOGGER_BANNER + "Resetting the state of the Java kernel node...");

        File database = this.configurations.getDatabase();
        if (database.exists()) {
            FileUtils.deleteDirectory(database);
        }
        return Result.successful();
    }

    /**
     * Returns the network that this node will attempt to connect to when {@code start()} is invoked,
     * or, if the node is running, then the network it has connected to.
     *
     * Returns null if no network has been configured yet.
     *
     * @return the network the node is on.
     */
    @Override
    public Network getNetwork() {
        return (this.configurations == null) ? null : this.configurations.getNetwork();
    }

    /**
     * Attempts to build the kernel from source only if the built kernel file path given does not
     * exist.
     *
     * The kernel is always initialized. If the kernel was built from source then the new build will
     * be initialized, otherwise the given built kernel file will be used and the kernel initialized
     * from there.
     */
    private Result doConditionalBuild(boolean verbose) throws IOException, InterruptedException {
        // If the built kernel file does not exist then build it.
        if (verbose) {
            System.out.println(Assumptions.LOGGER_BANNER + " Checking if file exists: " + this.configurations.getBuiltKernelFile().getAbsolutePath());
        }

        if (!this.configurations.getBuiltKernelFile().exists()) {
            Result result = buildJavaKernel(verbose);
            if (!result.isSuccess()) {
                return result;
            }

            // Move the newly built kernel to the expected built kernel file location.
            File builtKernelDir = NodeFileManager.getBuiltKernelDirectory(this.configurations.getKernelSourceDirectory());
            File builtKernel = NodeFileManager.getBuiltKernel(builtKernelDir);
            if (builtKernel == null) {
                return Result.unsuccessfulDueTo("Failed to find newly built kernel in directory: " + builtKernelDir.getAbsolutePath());
            }

            FileUtils.moveFile(builtKernel, this.configurations.getBuiltKernelFile());
        }

        if (this.configurations.isPreserveDatabaseSpecified()) {
            return initAndPreserveDatabase(this.configurations.getBuiltKernelFile(), verbose);
        } else {
            return init(this.configurations.getBuiltKernelFile(), verbose);
        }
    }

    private Result doUnconditionalBuild(boolean verbose) throws IOException, InterruptedException {
        Result result = buildJavaKernel(verbose);
        if (!result.isSuccess()) {
            return result;
        }

        File builtKernelDir = NodeFileManager.getBuiltKernelDirectory(this.configurations.getKernelSourceDirectory());
        File builtKernel = NodeFileManager.getBuiltKernel(builtKernelDir);
        if (builtKernel == null) {
            return Result.unsuccessfulDueTo("Failed to find newly built kernel in directory: " + builtKernelDir.getAbsolutePath());
        }

        if (this.configurations.isPreserveDatabaseSpecified()) {
            return initAndPreserveDatabase(builtKernel, verbose);
        } else {
            return init(builtKernel, verbose);
        }
    }

    private Result initAndPreserveDatabase(File kernelTarFile, boolean verbose) throws IOException, InterruptedException {
        if (this.configurations == null) {
            throw new IllegalStateException("Node has not been configured yet! Cannot initialize kernel.");
        }

        File nodeDirectory = NodeFileManager.getNodeDirectory();
        File kernelDatabaseDirectory = this.configurations.getDatabase();
        File temporaryDatabaseDirectory = NodeFileManager.getTemporaryDatabase();

        if (nodeDirectory.exists()) {
            if (kernelDatabaseDirectory.exists()) {

                // if preserved database directory already exist, delete it
                if (temporaryDatabaseDirectory.exists()) {
                    throw new IllegalStateException("there is already a database at " + temporaryDatabaseDirectory);
                }

                // move the kernel database
                FileUtils.moveDirectory(kernelDatabaseDirectory, temporaryDatabaseDirectory);

                // delete node directory
                FileUtils.deleteDirectory(nodeDirectory);

                if (!nodeDirectory.mkdir()) {
                    throw new IllegalStateException("Failed to make directory: " + nodeDirectory);
                }

                // move the preserved database back to the node directory
                FileUtils.moveDirectory(temporaryDatabaseDirectory, kernelDatabaseDirectory);
            }

        } else {
            if (!nodeDirectory.mkdir()) {
                throw new IllegalStateException("Failed to make directory: " + nodeDirectory);
            }
        }

        return untarAndSetupKernel(kernelTarFile, verbose);
    }

    private Result init(File kernelTarFile, boolean verbose) throws IOException, InterruptedException {
        if (this.configurations == null) {
            throw new IllegalStateException("Node has not been configured yet! Cannot initialize kernel.");
        }

        File nodeDestination = NodeFileManager.getNodeDirectory();
        if (nodeDestination.exists()) {
            throw new IllegalStateException("node directory already exists.");
        }

        if (!nodeDestination.mkdir()) {
            throw new IllegalStateException("Failed to make directory: " + nodeDestination);
        }

        return untarAndSetupKernel(kernelTarFile, verbose);
    }

    private Result untarAndSetupKernel(File kernelTarFile, boolean verbose) throws IOException, InterruptedException {
        System.out.println(Assumptions.LOGGER_BANNER + "Fetching the built kernel...");

        File tarDestination = new File(NodeFileManager.getNodeDirectory().getPath() + File.separator + Assumptions.NEW_KERNEL_TAR_NAME);
        Files.copy(kernelTarFile.toPath(), tarDestination.toPath());

        if (verbose) {
            System.out.println(Assumptions.LOGGER_BANNER + "Unzipping Java Kernel tar file using command: tar xvjf");
        }

        ProcessBuilder builder = new ProcessBuilder("tar", "xvjf", tarDestination.getName())
            .directory(tarDestination.getParentFile());

        if (verbose) {
            builder.inheritIO();
        }

        int untarStatus = builder.start().waitFor();
        tarDestination.delete();

        if (!this.logManager.setupLogFiles().isSuccess()) {
            return Result.unsuccessfulDueTo("Failed to set up log files!");
        }

        return (untarStatus == 0)
            ? Result.successful()
            : Result.unsuccessfulDueTo("Failed to prepare the kernel");
    }

    /**
     * Builds the Java Kernel from source and places its built tar.bz2 file in the specified built
     * kernel file location.
     *
     * @param verbose Whether or not to display the I/O.
     * @return the result of this build attempt.
     */
    private Result buildJavaKernel(boolean verbose) throws IOException, InterruptedException {
        if (this.configurations == null) {
            throw new IllegalStateException("Node has not been configured yet! Cannot build kernel.");
        }

        System.out.println(Assumptions.LOGGER_BANNER + "Building the Java kernel from source...");

        File sourceDirectory = this.configurations.getKernelSourceDirectory();

        if (verbose) {
            System.out.println(Assumptions.LOGGER_BANNER + "Building Java Kernel from command: ./gradlew clean pack");
            System.out.println(Assumptions.LOGGER_BANNER + "Building Java Kernel at location: " + sourceDirectory);
        }

        ProcessBuilder builder = new ProcessBuilder("./gradlew", "clean", "pack")
            .directory(sourceDirectory);

        if (verbose) {
            builder.inheritIO();
        }

        return (builder.start().waitFor() == 0)
            ? Result.successful()
            : Result.unsuccessfulDueTo("An error occurred building the kernel!");
    }

    /**
     * Waits for the RPC server to start before returning.
     */
    private Result waitForRpcServerToStart(File outputLog) throws InterruptedException {
        // We wait for the rpc event to know we are ok to return. There is a chance that we will miss
        // this event and start listening too late. That is why we timeout after 20 seconds, which
        // should be more than sufficient for the server to activate, and then we check if the node
        // is still live.
        // See issue #1 relating to this decision, which will be refactored in the future.

        if (isAlive()) {
            // We wait for the Rpc event or else 20 seconds, in case we come too late and never see it.
            IEvent rpcEvent = new Event("rpc-server - (UNDERTOW) started");

            Result result = this.logReader.startReading(outputLog);
            if (!result.isSuccess()) {
                return result;
            }

            NodeListener.listenTo(this).listenForEvent(rpcEvent, 20, TimeUnit.SECONDS).get();        }

        return (isAlive())
            ? Result.successful()
            : Result.unsuccessfulDueTo("Node failed to start!");
    }
}
