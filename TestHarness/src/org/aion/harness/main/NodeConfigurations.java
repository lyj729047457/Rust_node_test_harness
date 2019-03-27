package org.aion.harness.main;

import java.io.File;
import java.io.IOException;
import org.aion.harness.util.NodeFileManager;

/**
 * A class that holds configuration details related to a node.
 *
 * This class is immutable.
 */
public final class NodeConfigurations {
    private final Network network;
    private final String kernelSourceDirectory;
    private final String builtKernelDir;
    private final DatabaseOption databaseNodeOption;
    private final BuildOption buildOption;
    private final String buildDirectory;

    private enum BuildOption { ALWAYS_FROM_SOURCE, USE_BUILD }

    public enum DatabaseOption { PRESERVE_DATABASE, DO_NOT_PRESERVE_DATABASE }

    /**
     * Constructs an instance of this class using the specified parameters.
     */
    private NodeConfigurations(Network network, String kernelSourceDirectory, String builtKernelDirectory, DatabaseOption databaseOption, BuildOption buildOption) {
        if (network == null) {
            throw new NullPointerException("Cannot construct NodeConfigurations with null network.");
        }
        if (databaseOption == null) {
            throw new NullPointerException("Cannot construct NodeConfigurations with null database option specified.");
        }
        if (buildOption == null) {
            throw new NullPointerException("Cannot construct NodeConfigurations with null build option specified.");
        }

        this.network = network;
        this.kernelSourceDirectory = kernelSourceDirectory;
        this.builtKernelDir = builtKernelDirectory;
        this.databaseNodeOption = databaseOption;
        this.buildOption = buildOption;

        this.buildDirectory = (kernelSourceDirectory == null) ? builtKernelDirectory : NodeFileManager.getSandboxPath();
    }

    public static NodeConfigurations alwaysBuildFromSource(Network network, String kernelSourceDirectory) {
        if (kernelSourceDirectory == null) {
            throw new NullPointerException("Cannot construct NodeConfigurations with null kernelSourceDirectory.");
        }

        return new NodeConfigurations(network, kernelSourceDirectory, null, DatabaseOption.DO_NOT_PRESERVE_DATABASE, BuildOption.ALWAYS_FROM_SOURCE);
    }

    public static NodeConfigurations alwaysUseBuiltKernel(Network network, String builtKernelDirectory, DatabaseOption databaseOption) {
        if (builtKernelDirectory == null) {
            throw new NullPointerException("Cannot construct NodeConfigurations with null builtKernelDirectory.");
        }

        return new NodeConfigurations(network, null, builtKernelDirectory, databaseOption, BuildOption.USE_BUILD);
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
        return (this.kernelSourceDirectory == null) ? null : new File(this.kernelSourceDirectory);
    }

    /**
     * Returns the database of the current network.
     *
     * @return the database.
     */
    public File getDatabase() throws IOException {
        return NodeFileManager.getDatabaseOf(getActualBuildLocation(), this.network);
    }

    /**
     * Returns {@code true} only if the database is specified to be preserved across multiple
     * builds.
     *
     * @return whether or not to preserve the database.
     */
    public boolean preserveDatabase() {
        return this.databaseNodeOption == DatabaseOption.PRESERVE_DATABASE;
    }

    public boolean alwaysBuildFromSource() {
        return this.buildOption == BuildOption.ALWAYS_FROM_SOURCE;
    }

    public File getDirectoryOfBuiltKernel() {
        return new File(this.builtKernelDir);
    }

    public File getActualBuildLocation() {
        return new File(this.buildDirectory);
    }

}
