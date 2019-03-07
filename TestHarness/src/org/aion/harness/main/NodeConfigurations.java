package org.aion.harness.main;

import java.io.File;
import org.aion.harness.util.NodeFileManager;

/**
 * A class that holds configuration details related to a node.
 *
 * This class is immutable.
 */
public final class NodeConfigurations {
    private final Network network;
    private final String kernelSourceDirectory;
    private final String builtKernelFile;
    private final String database;
    private final boolean conditionalBuild;
    private final boolean preserveDatabase;

    /**
     * Constructs an instance of this class using the specified parameters.
     */
    private NodeConfigurations(Network network, String kernelSourceDirectory, String builtKernelFile, boolean conditionalBuild, boolean preserveDatabase) {
        if (network == null) {
            throw new NullPointerException("Cannot construct NodeConfigurations with null network.");
        }

        this.network = network;
        this.kernelSourceDirectory = kernelSourceDirectory;
        this.builtKernelFile = builtKernelFile;
        this.conditionalBuild = conditionalBuild;
        this.preserveDatabase = preserveDatabase;

        // This is an internal detail and should not be customizable.
        this.database = NodeFileManager.getKernelDirectory().getAbsolutePath() + File.separator + this.network.string() + File.separator + "database";
    }

    /**
     * Constructs a node configurations object that is used for "conditional builds". That is, for
     * when the kernel should be built from source only if the provided built file does not yet
     * exist.
     *
     * A conditional build allows for the source build to only occur once, or to only occur when the
     * built file has been removed.
     *
     * @param network The network to connect the node to.
     * @param kernelSourceDirectory The directory that contains the kernel source files.
     * @param builtKernelFile The built kernel file.
     * @return the conditional configurations.
     */
    public static NodeConfigurations conditionalBuild(Network network, String kernelSourceDirectory, String builtKernelFile, boolean preserveDatabase) {
        if (kernelSourceDirectory == null) {
            throw new NullPointerException("Cannot construct NodeConfigurations with null kernelSourceDirectory.");
        }
        if (builtKernelFile == null) {
            throw new NullPointerException("Cannot construct NodeConfigurations with null builtKernelFile.");
        }

        return new NodeConfigurations(network, kernelSourceDirectory, builtKernelFile, true, preserveDatabase);
    }

    /**
     * Constructs a node configurations object that is used for "unconditional builds". That is, for
     * when the kernel should be built from source every single time, regardless of whether or not
     * a build already exists.
     *
     * @param network The network to connect the node to.
     * @param kernelSourceDirectory The directory that contains the kernel source files.
     * @return the unconditional configurations.
     */
    public static NodeConfigurations unconditionalBuild(Network network, String kernelSourceDirectory, boolean preserveDatabase) {
        if (kernelSourceDirectory == null) {
            throw new NullPointerException("Cannot construct NodeConfigurations with null kernelSourceDirectory.");
        }

        return new NodeConfigurations(network, kernelSourceDirectory, null, false, preserveDatabase);
    }

    /**
     * Returns the network that the node should connect to.
     *
     * @return the network.
     */
    public Network getNetwork() {
        return this.network;
    }

    /**
     * Returns the directory of the kernel source code.
     *
     * @return kernel source directory.
     */
    public File getKernelSourceDirectory() {
        return new File(this.kernelSourceDirectory);
    }

    /**
     * Returns the built kernel file.
     *
     * The assumption is that this is a tar.bz2 file.
     *
     * @return the built kernel directory.
     */
    public File getBuiltKernelFile() {
        return new File(this.builtKernelFile);
    }

    /**
     * Returns the database of the current network.
     *
     * @return the database.
     */
    public File getDatabase() {
        return new File(this.database);
    }

    /**
     * Returns {@code true} only if a conditional build was specified.
     *
     * @return whether or not to perform a conditional build.
     */
    public boolean isConditionalBuildSpecified() {
        return this.conditionalBuild;
    }

    /**
     * Returns {@code true} only if the database is specified to be preserved across multiple
     * builds.
     *
     * @return whether or not to preserve the database.
     */
    public boolean isPreserveDatabaseSpecified() {
        return this.preserveDatabase;
    }

}
