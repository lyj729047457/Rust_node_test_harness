package org.aion.harness.tests.integ.runner.internal;

import java.io.File;
import java.io.IOException;
import org.aion.harness.main.LocalNode;
import org.aion.harness.main.Network;
import org.aion.harness.main.NodeConfigurations;
import org.aion.harness.main.NodeConfigurations.DatabaseOption;
import org.aion.harness.main.NodeFactory;
import org.aion.harness.main.NodeFactory.NodeType;
import org.aion.harness.main.NodeListener;
import org.aion.harness.main.ProhibitConcurrentHarness;
import org.aion.harness.result.Result;
import org.aion.harness.tests.integ.runner.exception.TestRunnerInitializationException;
import org.apache.commons.io.FileUtils;

/**
 * This class is responsible for the lifecycle of the node that all tests are running against.
 *
 * The node is never exposed. The caller has the ability to start and stop it.
 */
public final class TestNodeManager {
    private static final String WORKING_DIR = System.getProperty("user.dir");
    private static final String EXPECTED_KERNEL_LOCATION = WORKING_DIR + "/aion";
    private static final String HANDWRITTEN_CONFIGS = WORKING_DIR + "/test_resources/custom/config";
    private LocalNode localNode;

    /**
     * Starts up a local node of the specified type if no node has currently been started.
     *
     * If anything goes wrong this method will throw an exception to halt the runner.
     */
    public void startLocalNode(NodeType nodeType) throws Exception {
        if (this.localNode == null) {

            // Verify the kernel is in the expected location and overwrite its config & genesis files.
            checkKernelExistsAndOverwriteConfigs();

            // Acquire the system-wide lock.
            ProhibitConcurrentHarness.acquireTestLock();

            // Initialize the node.
            NodeConfigurations configurations = NodeConfigurations.alwaysUseBuiltKernel(Network.CUSTOM, EXPECTED_KERNEL_LOCATION, DatabaseOption.PRESERVE_DATABASE);
            LocalNode node = NodeFactory.getNewLocalNodeInstance(nodeType);
            node.configure(configurations);

            Result result = node.initialize();
            if (!result.isSuccess()) {
                throw new TestRunnerInitializationException("Failed to initialize the node: " + result.getError());
            }

            // Start the node.
            result = node.start();
            if (!result.isSuccess()) {
                throw new TestRunnerInitializationException("Failed to start the node: " + result.getError());
            }

            this.localNode = node;
        } else {
            throw new IllegalStateException("Attempted to start running a local node but one is already running!");
        }
    }

    /**
     * Stops a local node if one is currently running.
     */
    public void shutdownLocalNode() throws Exception {
        if (this.localNode != null) {
            try {
                this.localNode.stop();
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                ProhibitConcurrentHarness.releaseTestLock();
            }
        } else {
            throw new IllegalStateException("Attempted to stop running a local node but no node is currently running!");
        }
    }

    /**
     * Returns a newly created node listener that is listening to the current running local node.
     */
    public NodeListener newNodeListener() {
        if (this.localNode != null) {
            return NodeListener.listenTo(this.localNode);
        } else {
            throw new IllegalStateException("Attempted to get a new listener but no local node is currently running!");
        }
    }

    private static void checkKernelExistsAndOverwriteConfigs() throws IOException {
        if (!kernelExists()) {
            throw new TestRunnerInitializationException("Expected to find a kernel at: " + EXPECTED_KERNEL_LOCATION);
        }
        overwriteConfigAndGenesis();
    }

    private static boolean kernelExists() {
        File kernel = new File(EXPECTED_KERNEL_LOCATION);
        return kernel.exists() && kernel.isDirectory();
    }

    private static void overwriteConfigAndGenesis() throws IOException {
        FileUtils.copyDirectory(new File(HANDWRITTEN_CONFIGS), new File(EXPECTED_KERNEL_LOCATION + "/config/custom"));
        overwriteIfTargetDirExists(new File(HANDWRITTEN_CONFIGS), new File(EXPECTED_KERNEL_LOCATION + "/custom/config"));
    }

    private static void overwriteIfTargetDirExists(File source, File target) throws IOException {
        if (target.exists()) {
            FileUtils.copyDirectory(source, target);
        }
    }
}
