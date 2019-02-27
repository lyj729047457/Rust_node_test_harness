package org.aion.harness.main.types;

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
    private final String kernelBuildDirectory;
    private final String database;

    /**
     * Constructs an instance of this class using the specified parameters.
     */
    public NodeConfigurations(Network network, String kernelSourceDirectory, String kernelBuildDirectory) {
        if (network == null) {
            throw new NullPointerException("Cannot construct NodeConfigurations with null network.");
        }
        if (kernelSourceDirectory == null) {
            throw new NullPointerException("Cannot construct NodeConfigurations with null kernelSourceDirectory.");
        }
        if (kernelBuildDirectory == null) {
            throw new NullPointerException("Cannot construct NodeConfigurations with null kernelBuildDirectory.");
        }

        this.network = network;
        this.kernelSourceDirectory = kernelSourceDirectory;
        this.kernelBuildDirectory = kernelBuildDirectory;

        // This is an internal detail and should not be customizable.
        this.database = NodeFileManager.getKernelDirectory().getAbsolutePath() + File.separator + this.network.string() + File.separator + "database";
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
     * Returns the directory of the built kernel.
     *
     * @return the built kernel directory.
     */
    public File getKernelBuildDirectory() {
        return new File(this.kernelBuildDirectory);
    }

    /**
     * Returns the database of the current network.
     *
     * @return the database.
     */
    public File getDatabase() {
        return new File(this.database);
    }

}
