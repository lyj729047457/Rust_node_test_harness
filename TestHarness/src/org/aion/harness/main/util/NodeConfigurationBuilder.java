package org.aion.harness.main.util;

import java.io.File;
import org.aion.harness.main.Network;
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

    private Network network;
    private String kernelSourceDirectory;
    private String builtKernelFile;
    private boolean isConditionalBuild;
    private boolean preserveDatabase = false;

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
     * Tell the node to perform a conditional build.
     *
     * A conditional build will first check whether or not the file whose path is
     * {@code builtKernelFile} exists, and if not, it will build the kernel from the sources at
     * the given source directory.
     *
     * Otherwise, if the built kernel file does exist, no build will occur and the kernel will be
     * fetched directly from the built file.
     *
     * Assumption: the built kernel file is a tar.bz2 file.
     *
     * @param kernelSourceDirectory The path to the root directory of the kernel source files.
     * @param builtKernelFile The path to the built kernel file.
     * @return this builder.
     */
    public NodeConfigurationBuilder conditionalBuild(String kernelSourceDirectory, String builtKernelFile) {
        this.kernelSourceDirectory = kernelSourceDirectory;
        this.builtKernelFile = builtKernelFile;
        this.isConditionalBuild = true;
        return this;
    }

    /**
     * Tell the node to perform an unconditional build.
     *
     * An unconditional build will build the kernel from the sources at the given source directory
     * every time the node is initialized.
     *
     * Assumption: the built kernel file is a tar.bz2 file.
     *
     * @param kernelSourceDirectory The path to the root directory of the kernel source files.
     * @return this builder.
     */
    public NodeConfigurationBuilder unconditionalBuild(String kernelSourceDirectory) {
        this.kernelSourceDirectory = kernelSourceDirectory;
        this.isConditionalBuild = false;
        return this;
    }

    /**
     * Tell the node to preserve the database if it exists. This command is only relevant if a node
     * will be run multiple times. In this case, the same database will be carried over across each
     * of these runs.
     *
     * @return this builder.
     */
    public NodeConfigurationBuilder preserveDatabase() {
        this.preserveDatabase = true;
        return this;
    }

    /**
     * Tell the node not to preserve the database if it exists. This command is only relevant if a
     * node will be run multiple times. In this case, a new empty database will be created for each
     * of these runs.
     *
     * @return this builder.
     */
    public NodeConfigurationBuilder doNotPreserveDatabase() {
        this.preserveDatabase = false;
        return this;
    }

    /**
     * Builds the node configurations from the fields that were set in this builder.
     *
     * If database preservation is not specified then the default value will be to preserve the
     * database.
     *
     * @return the node configurations.
     */
    public NodeConfigurations build() {
        if (this.isConditionalBuild) {
            return NodeConfigurations.conditionalBuild(this.network, this.kernelSourceDirectory, this.builtKernelFile, this.preserveDatabase);
        } else {
            return NodeConfigurations.unconditionalBuild(this.network, this.kernelSourceDirectory, this.preserveDatabase);
        }
    }

    /**
     * Constructs node configurations that will use the default values and will specify a conditonal
     * build.
     *
     * The default network is: {@link NodeConfigurationBuilder#DEFAULT_NETWORK}
     * The default kernel source directory is: {@link NodeConfigurationBuilder#DEFAULT_KERNEL_SOURCE_DIR}
     *
     * @param preserveDatabase whether or not to preserve the database.
     * @return a default conditional build configuration.
     */
    public static NodeConfigurations defaultConditionalBuildConfigurations(String builtKernelFile, boolean preserveDatabase) {
        return NodeConfigurations.conditionalBuild(DEFAULT_NETWORK, DEFAULT_KERNEL_SOURCE_DIR, builtKernelFile, preserveDatabase);
    }

    /**
     * Constructs node configurations that will use the default values and will specify an
     * unconditonal build.
     *
     * The default network is: {@link NodeConfigurationBuilder#DEFAULT_NETWORK}
     * The default kernel source directory is: {@link NodeConfigurationBuilder#DEFAULT_KERNEL_SOURCE_DIR}
     *
     * @param preserveDatabase whether or not to preserve the database.
     * @return a default unconditional build configuration.
     */
    public static NodeConfigurations defaultUnconditionalBuildConfigurations(boolean preserveDatabase) {
        return NodeConfigurations.unconditionalBuild(DEFAULT_NETWORK, DEFAULT_KERNEL_SOURCE_DIR, preserveDatabase);
    }

}