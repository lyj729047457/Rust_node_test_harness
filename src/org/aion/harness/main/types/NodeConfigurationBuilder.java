package org.aion.harness.main.types;

import org.aion.harness.main.Node;
import org.aion.harness.util.NodeFileManager;

/**
 * A builder class used to construct instances of {@link NodeConfigurations}.
 *
 * If a setter method is invoked multiple times, its latest invocation takes precedence.
 *
 * This builder can be reused after calling {@code build()}.
 */
public final class NodeConfigurationBuilder {
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
     *   - network = {@link NodeFileManager#network}
     *   - kernel source directory = {@link NodeFileManager#KERNEL_REPO}
     *   - kernel build directory = {@link NodeFileManager#TAR_SOURCE}
     *
     * @return the built configuration object.
     */
    public NodeConfigurations build() {
        Network network = (this.network == null) ? NodeFileManager.getNetwork() : this.network;

        String kernelSource = (this.kernelSourceDirectory == null)
            ? NodeFileManager.getKernelRepositoryDirectory().getAbsolutePath()
            : this.kernelSourceDirectory;

        String kernelBuild = (this.kernelBuildDirectory == null)
            ? NodeFileManager.getKernelTarSourceDirectory().getAbsolutePath()
            : this.kernelBuildDirectory;

        return new NodeConfigurations(network, kernelSource, kernelBuild, NodeFileManager.getKernelDatabase().getAbsolutePath());
    }

    /**
     * Returns a {@link NodeConfigurations} object with all of the default values specified by
     * {@code build()}.
     *
     * @return a default configurations object.
     */
    public static NodeConfigurations defaultConfigurations() {
        Network network = NodeFileManager.getNetwork();
        String kernelSource = NodeFileManager.getKernelRepositoryDirectory().getAbsolutePath();
        String kernelBuild = NodeFileManager.getKernelTarSourceDirectory().getAbsolutePath();
        
        return new NodeConfigurations(network, kernelSource, kernelBuild, NodeFileManager.getKernelDatabase().getAbsolutePath());
    }

}
