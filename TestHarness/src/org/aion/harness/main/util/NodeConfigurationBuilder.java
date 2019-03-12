package org.aion.harness.main.util;

import java.io.File;
import org.aion.harness.main.Network;
import org.aion.harness.main.Node;
import org.aion.harness.main.NodeConfigurations;

/**
 * A builder class used to construct instances of {@link NodeConfigurations}.
 *
 * If a setter method is invoked multiple times, its latest invocation takes precedence.
 *
 * This builder can be reused after calling {@code build()}.
 */
public final class NodeConfigurationBuilder {
    private static final String PROJECT_DIR = System.getProperty("user.dir") + File.separator + "..";
    
    public static final Network DEFAULT_NETWORK = Network.MASTERY;
    public static final String DEFAULT_KERNEL_SOURCE_DIR = PROJECT_DIR + File.separator + ".." + File.separator + "aion";
    public static final String DEFAULT_KERNEL_BUILD_DIR = DEFAULT_KERNEL_SOURCE_DIR + File.separator + "pack";

    private Network network;
    private String kernelSourceDirectory;
    private String kernelBuildDirectory;

    /**
     * The network that the node will attempt to connect to.
     *
     * @param network The network the node will connect to.
     * @return this builder.
     */
    public NodeConfigurationBuilder network(Network network) {
        this.network = network;
        return this;
    }

    /**
     * The root directory of the kernel source code.
     *
     * This is the source that the node will build the kernel from if {@link Node#buildKernel()} is
     * invoked.
     *
     * @param kernelSourceDirectory The kernel source code.
     * @return this builder.
     */
    public NodeConfigurationBuilder kernelSourceDirectory(String kernelSourceDirectory) {
        this.kernelSourceDirectory = kernelSourceDirectory;
        return this;
    }

    /**
     * The directory that contains the built kernel (as a .tar.bz2 file).
     *
     * This field is set when no build is desired, then the build is assumed to exist in the provided
     * directory.
     *
     * The {@link Node#initializeKernel()} method uses this.
     *
     * @param kernelBuildDirectory The build kernel.
     * @return this builder.
     */
    public NodeConfigurationBuilder directoryOfBuiltKernel(String kernelBuildDirectory) {
        this.kernelBuildDirectory = kernelBuildDirectory;
        return this;
    }

    /**
     * Builds the {@link NodeConfigurations} object.
     *
     * The following default values are supplied if the fields are not set:
     *   - network = {@link Network#MASTERY}
     *   - kernel source directory = {@link NodeConfigurationBuilder#DEFAULT_KERNEL_SOURCE_DIR}
     *   - kernel build directory = {@link NodeConfigurationBuilder#DEFAULT_KERNEL_BUILD_DIR}
     *
     * The database
     *
     * @return the built configuration object.
     */
    public NodeConfigurations build() {
        Network network = (this.network == null) ? DEFAULT_NETWORK : this.network;

        String kernelSource = (this.kernelSourceDirectory == null)
            ? DEFAULT_KERNEL_SOURCE_DIR
            : this.kernelSourceDirectory;

        String kernelBuild = (this.kernelBuildDirectory == null)
            ? DEFAULT_KERNEL_BUILD_DIR
            : this.kernelBuildDirectory;

        return new NodeConfigurations(network, kernelSource, kernelBuild);
    }

    /**
     * Returns a {@link NodeConfigurations} object with all of the default values specified by
     * {@code build()}.
     *
     * @return a default configurations object.
     */
    public static NodeConfigurations defaultConfigurations() {
        return new NodeConfigurations(DEFAULT_NETWORK, DEFAULT_KERNEL_SOURCE_DIR, DEFAULT_KERNEL_BUILD_DIR);
    }

}
