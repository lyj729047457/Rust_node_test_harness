package org.aion.harness.main.impl.internal;

import java.io.File;
import java.io.IOException;
import org.aion.harness.main.Network;
import org.aion.harness.main.NodeConfigurations;
import org.aion.harness.misc.Assumptions;
import org.aion.harness.result.Result;
import org.aion.harness.util.NodeFileManager;
import org.aion.harness.util.SimpleLog;
import org.apache.commons.io.FileUtils;

/**
 * A class that is responsible for initializing a node (performing any setup required for proper
 * usage).
 */
public final class NodeInitializer {
    private final SimpleLog log;
    private final NodeConfigurations configurations;

    public NodeInitializer(NodeConfigurations configurations) {
        log = new SimpleLog(getClass().getName());
        if (configurations == null) {
            throw new NullPointerException("Cannot construct NodeInitializer with null configurations.");
        }

        this.configurations = configurations;
    }

    public Result initializeJavaKernel(boolean verbose) throws IOException, InterruptedException {
        if (this.configurations.alwaysBuildFromSource()) {
            return buildFromSource(verbose);
        } else {
            return useExistingBuild();
        }
    }

    /**
     * Builds the kernel from the source files in the provided directory and extracts the contents
     * of this build into a new sandbox directory.
     *
     * If the sandbox directory exists prior to this call, it will be destroyed.
     */
    private Result buildFromSource(boolean verbose) throws IOException, InterruptedException {

        // 1. Build the kernel from source.
        Result result = buildJavaKernel(verbose);
        if (!result.isSuccess()) {
            return result;
        }

        // 2. Destroy the sandbox if it exists.
        destroySandbox();

        // 3. Create the sandbox directory.
        result = createSandbox();
        if (!result.isSuccess()) {
            return result;
        }

        // 4. Locate the tar file and move it into the sandbox temporarily.
        result = moveTarFileToSandbox();
        if (!result.isSuccess()) {
            cleanup();
            return result;
        }

        // 5. Extract the contents of the tar file and delete it.
        result = extractTarFile(verbose);
        if (!result.isSuccess()) {
            cleanup();
        }

        destroyTemporaryTarFile();
        return result;
    }

    /**
     * Uses an existing built kernel.
     *
     * If database preservation is not specified then the database corresponding to the current
     * network is deleted.
     *
     * This method does not make use of the sandbox directory at all, but whatever directory the
     * build currently resides in.
     *
     * If the build does not exist then this method is unsuccessful.
     */
    private Result useExistingBuild() throws IOException {
        if (!this.configurations.getDirectoryOfBuiltKernel().exists()) {
            return Result.unsuccessfulDueTo("Could not find built kernel directory at location: " + this.configurations.getDirectoryOfBuiltKernel().getAbsolutePath());
        }
        if (!this.configurations.getDirectoryOfBuiltKernel().isDirectory()) {
            return Result.unsuccessfulDueTo("Found the built kernel, but it is not a directory! Path: " + this.configurations.getDirectoryOfBuiltKernel().getAbsolutePath());
        }

        // If no database preservation specified then delete database if it exists
        if (!this.configurations.preserveDatabase()) {
            destroyDatabaseOf(this.configurations.getDirectoryOfBuiltKernel(), this.configurations.getNetwork());
        }

        return Result.successful();
    }

    /**
     * Builds the Java Kernel from source and places its built tar.bz2 file in the specified built
     * kernel file location.
     *
     * @param verbose Whether or not to display the I/O.
     * @return the result of this build attempt.
     */
    private Result buildJavaKernel(boolean verbose) throws IOException, InterruptedException {
        log.log(Assumptions.LOGGER_BANNER + "Building the Java kernel from source...");

        File sourceDirectory = this.configurations.getKernelSourceDirectory();
        if (!sourceDirectory.exists()) {
            return Result.unsuccessfulDueTo("Could not find source directory: " + this.configurations.getKernelSourceDirectory());
        }
        if (!sourceDirectory.isDirectory()) {
            return Result.unsuccessfulDueTo("Found source but it is not a directory: " + this.configurations.getKernelSourceDirectory());
        }

        if (verbose) {
            log.log(Assumptions.LOGGER_BANNER + "Building Java Kernel from command: ./gradlew clean pack");
            log.log(Assumptions.LOGGER_BANNER + "Building Java Kernel at location: " + sourceDirectory);
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

    private Result extractTarFile(boolean verbose) throws IOException, InterruptedException {
        log.log(Assumptions.LOGGER_BANNER + "Extracting the built kernel...");
        if (verbose) {
            log.log(Assumptions.LOGGER_BANNER + "Extracting contents of Java Kernel tar.bz2 file using command: tar xvjf");
        }

        File temporaryTarFile = NodeFileManager.getTemporaryTarFile();
        ProcessBuilder builder = new ProcessBuilder("tar", "xvjf", temporaryTarFile.getName())
            .directory(temporaryTarFile.getParentFile());

        if (verbose) {
            builder.inheritIO();
        }

        int status = builder.start().waitFor();

        return (status == 0)
            ? Result.successful()
            : Result.unsuccessfulDueTo("Failed to extract the built assets from the tar.bz2 file. Tar command exit code: " + status);
    }

    private Result moveTarFileToSandbox() throws IOException {
        File builtKernel = grabKernelTarFile();
        if (builtKernel == null) {
            return Result.unsuccessfulDueTo("Failed to find newly built kernel in specified source directory: "
                + this.configurations.getKernelSourceDirectory().getAbsolutePath());
        }

        File temporaryTarFile = NodeFileManager.getTemporaryTarFile();
        FileUtils.moveFile(builtKernel, temporaryTarFile);
        return Result.successful();
    }

    private File grabKernelTarFile() {
        File builtKernelDir = NodeFileManager.getDirectoryOfBuiltTarFile(this.configurations.getKernelSourceDirectory());
        return NodeFileManager.findKernelTarFile(builtKernelDir);
    }

    private Result createSandbox() {
        if (!new File(NodeFileManager.getSandboxPath()).mkdir()) {
            return Result.unsuccessfulDueTo("Failed to create the sandbox directory to host the built kernel at location: "
                + NodeFileManager.getSandboxPath());
        }
        return Result.successful();
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
        destroySandbox();
        FileUtils.deleteDirectory(NodeFileManager.getTemporaryDatabase());
    }

    private void destroyDatabaseOf(File builtKernelDirectory, Network network) throws IOException {
        FileUtils.deleteDirectory(NodeFileManager.getDatabaseOf(builtKernelDirectory, network));
    }

    private void destroyTemporaryTarFile() {
        NodeFileManager.getTemporaryTarFile().delete();
    }

    private void destroySandbox() throws IOException {
        FileUtils.deleteDirectory(new File(NodeFileManager.getSandboxPath()));
    }

}
