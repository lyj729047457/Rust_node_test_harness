package org.aion.harness.main.impl.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.aion.harness.main.Node;
import org.aion.harness.main.NodeConfigurations;
import org.aion.harness.misc.Assumptions;
import org.aion.harness.result.Result;
import org.aion.harness.util.NodeFileManager;
import org.apache.commons.io.FileUtils;

/**
 * A class that is responsible for initializing a node (performing any setup required for proper
 * usage).
 */
public final class NodeInitializer {
    private final NodeConfigurations configurations;

    public NodeInitializer(NodeConfigurations configurations) {
        if (configurations == null) {
            throw new NullPointerException("Cannot construct NodeInitializer with null configurations.");
        }

        this.configurations = configurations;
    }

    /**
     * Attempts to build the kernel from source and then set up the new build for the {@link Node}
     * to launch directly from.
     *
     * If the build kernel already exists then this method does not build the kernel from source.
     * Instead, it simply untars the built kernel and puts its contents in the expected place.
     *
     * Otherwise, if no build exists in the specified location, this method builds the kernel from
     * source and puts the new tar file in the location of the build file, so that next time it can
     * skip this step, and then extracts the built assets from the tar file into the newly created
     * sandbox.
     *
     * Note that if there is no specification to preserve the database and there exists a sandbox
     * directory then this method will fail.
     */
    public Result doConditionalBuild(boolean verbose) throws IOException, InterruptedException {

        // If the built tar file does not exist, then build from source.
        if (!this.configurations.getBuiltKernelTarFile().exists()) {
            Result result = buildFromSource(verbose);
            if (!result.isSuccess()) {
                return result;
            }
        }

        if (this.configurations.isPreserveDatabaseSpecified()) {
            return createSandboxAndExtractTarFilePreservingDatabase(verbose);
        } else {
            return createSandboxAndExtractTarFile(verbose);
        }
    }

    /**
     * Always builds the kernel from source and puts the new tar file in the location of the build
     * file, so that next time it can skip this step, and then extracts the built assets from the
     * tar file into the newly created sandbox.
     *
     * Note that if there is no specification to preserve the database and there exists a sandbox
     * directory then this method will fail.
     */
    public Result doUnconditionalBuild(boolean verbose) throws IOException, InterruptedException {
        Result result = buildFromSource(verbose);
        if (!result.isSuccess()) {
            return result;
        }

        if (this.configurations.isPreserveDatabaseSpecified()) {
            return createSandboxAndExtractTarFilePreservingDatabase(verbose);
        } else {
            return createSandboxAndExtractTarFile(verbose);
        }
    }

    /**
     * Builds the kernel from source and moves the new tar file to the built kernel location.
     *
     * ASSUMPTION: the built kernel location is empty.
     */
    private Result buildFromSource(boolean verbose) throws IOException, InterruptedException {

        // If building the java kernel fails then return immediately.
        Result result = buildJavaKernel(verbose);
        if (!result.isSuccess()) {
            return result;
        }

        // Move the newly built kernel to the expected built kernel file location.
        File builtKernelDir = NodeFileManager.getDirectoryOfBuiltTarFile(this.configurations.getKernelSourceDirectory());
        File builtKernel = NodeFileManager.findKernelTarFile(builtKernelDir);
        if (builtKernel == null) {
            return Result.unsuccessfulDueTo("Failed to find newly built kernel in directory: " + builtKernelDir.getAbsolutePath());
        }

        FileUtils.moveFile(builtKernel, this.configurations.getBuiltKernelTarFile());
        return result;
    }

    /**
     * Builds the Java Kernel from source and places its built tar.bz2 file in the specified built
     * kernel file location.
     *
     * @param verbose Whether or not to display the I/O.
     * @return the result of this build attempt.
     */
    private Result buildJavaKernel(boolean verbose) throws IOException, InterruptedException {
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
     * Checks first if there already exists a sandboxed node and if so preserves its directory.
     * The sandbox will be destroyed and re-built using the built tar file assets and the database
     * will be reinstalled so as to preserve it.
     */
    private Result createSandboxAndExtractTarFilePreservingDatabase(boolean verbose) throws IOException, InterruptedException {
        File sandbox = NodeFileManager.getNodeSandboxDirectory();
        File database = this.configurations.getDatabase();
        File temporaryDatabase = NodeFileManager.getTemporaryDatabase();

        if (temporaryDatabase.exists()) {
            throw new IllegalStateException("Cannot create the temporary directory to preserve the database, it already exists at: "
                + temporaryDatabase.getAbsolutePath());
        }

        // Then there is no database to preserve.
        if (!sandbox.exists()) {
            return createSandboxAndExtractTarFile(verbose);
        }

        // Move the database out of the sandbox and destroy the sandbox.
        FileUtils.moveDirectory(database, temporaryDatabase);
        FileUtils.deleteDirectory(sandbox);

        // Create the sandbox and extract the built assets into it.
        Result result = createSandboxAndExtractTarFile(verbose);
        if (!result.isSuccess()) {
            cleanup();
            return result;
        }

        // Reinstall the database.
        FileUtils.moveDirectory(temporaryDatabase, database);
        return result;
    }

    /**
     * Attempts to extract the contents of the kernel tar file to the sandbox directory so that a
     * node can be launched.
     *
     * This method is unsuccessful only if one of the following is true:
     *
     * 1. An old node already exists in the sandbox.
     * 2. This method fails to create the sandbox directory.
     * 3. The tar file extraction fails.
     * 4. The log manager fails to set up the log files.
     *
     * If this method fails due to any of the above reasons, it will leave the file system in the
     * same state in which it was prior to invoking this method.
     */
    private Result createSandboxAndExtractTarFile(boolean verbose) throws IOException, InterruptedException {
        File sandbox = NodeFileManager.getNodeSandboxDirectory();
        if (sandbox.exists()) {
            return Result.unsuccessfulDueTo("An old node build currently exists at: " + sandbox.getAbsolutePath() + ". Cannot re-initialize.");
        }

        if (!sandbox.mkdir()) {
            return Result.unsuccessfulDueTo("Failed to make sandboxed node directory: " + sandbox.getAbsolutePath());
        }

        Result result = extractKernelTarFileIntoSandbox(verbose);
        if (!result.isSuccess()) {
            cleanup();
        }
        return result;
    }

    /**
     * Attempts to extract the contents of the kernel tar file to the sandbox directory so that a
     * node can be launched.
     *
     * This method is unsuccessful only if either the extracting itself fails or the log manager
     * fails to set up the log files for the node.
     */
    private Result extractKernelTarFileIntoSandbox(boolean verbose) throws IOException, InterruptedException {
        System.out.println(Assumptions.LOGGER_BANNER + "Extracting the built kernel...");

        File tarDestination = new File(NodeFileManager.getNodeSandboxDirectory().getPath() + File.separator + Assumptions.NEW_KERNEL_TAR_NAME);
        Files.copy(this.configurations.getBuiltKernelTarFile().toPath(), tarDestination.toPath());

        if (verbose) {
            System.out.println(Assumptions.LOGGER_BANNER + "Extracting contents of Java Kernel tar.bz2 file using command: tar xvjf");
        }

        ProcessBuilder builder = new ProcessBuilder("tar", "xvjf", tarDestination.getName())
            .directory(tarDestination.getParentFile());

        if (verbose) {
            builder.inheritIO();
        }

        int status = builder.start().waitFor();
        tarDestination.delete();

        return (status == 0)
            ? Result.successful()
            : Result.unsuccessfulDueTo("Failed to extract the built assets from the tar.bz2 file. Tar command exit code: " + status);
    }

    /**
     * Cleans up in the event of a failure.
     *
     * Currently this means:
     *
     * 1. Deletes the sandbox directory.
     * 2. Deletes the temporary database directory.
     */
    private void cleanup() throws IOException {
        FileUtils.deleteDirectory(NodeFileManager.getNodeSandboxDirectory());
        FileUtils.deleteDirectory(NodeFileManager.getTemporaryDatabase());
    }

}
