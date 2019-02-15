package org.aion.harness.integ;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.aion.harness.main.Node;
import org.aion.harness.main.NodeFactory;
import org.aion.harness.main.NodeListener;
import org.aion.harness.main.impl.JavaNode;
import org.aion.harness.misc.Assumptions;
import org.aion.harness.result.EventRequestResult;
import org.aion.harness.result.Result;
import org.aion.harness.util.NodeFileManager;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NodeListenerLifecycleTest {
    private static File nodeDirectory = NodeFileManager.getNodeDirectory();
    private static File kernelDirectory = NodeFileManager.getKernelDirectory();

    private Node node;

    /**
     * Set to false for testing convenience, tune to true if starting from scratch.
     */
    private static boolean doFullInitialization = false;

    @Before
    public void setup() throws IOException {
        deleteInitializationDirectories();
        this.node = NodeFactory.getNewNodeInstance(NodeFactory.NodeType.JAVA_NODE);
    }

    @After
    public void tearDown() throws IOException, InterruptedException {
        shutdownNodeIfRunning();
        deleteInitializationDirectories();
        deleteLogs();
        this.node = null;
    }

    @Test
    public void testNodeListenerBeforeStartingNode() {
        NodeListener listener = new NodeListener();

        // Time out should be irrelevant in this situation.
        EventRequestResult result = listener.waitForHeartbeat(0);
        System.out.println("Result = " + result);
        assertTrue(result.eventHasBeenRejected());
        assertEquals("Listener is not currently listening to a log file.", result.causeOfRejection());
    }

    @Test
    public void testNodeListenerAfterShuttingDownNode() throws IOException, InterruptedException {
        initializeNodeWithChecks();

        Result result = this.node.start();
        System.out.println("Start result = " + result);
        assertTrue(result.success);

        NodeListener listener = new NodeListener();

        Thread.sleep(TimeUnit.SECONDS.toMillis(10));

        this.node.stop();

        EventRequestResult eventResult = listener.waitForHeartbeat(0);
        System.out.println("Result = " + eventResult);
        assertTrue(eventResult.eventHasBeenRejected());
        assertEquals("Listener is not currently listening to a log file.", eventResult.causeOfRejection());
    }

    private void initializeNodeWithChecks() throws IOException, InterruptedException {
        Result result = initializeNode();
        assertTrue(result.success);

        // verify the node directory was created.
        assertTrue(nodeDirectory.exists());
        assertTrue(nodeDirectory.isDirectory());

        // veirfy the node directory contains the aion directory.
        File[] nodeDirectoryEntries = nodeDirectory.listFiles();
        assertNotNull(nodeDirectoryEntries);
        assertEquals(1, nodeDirectoryEntries.length);
        assertEquals(kernelDirectory, nodeDirectoryEntries[0]);
        assertTrue(nodeDirectoryEntries[0].isDirectory());
    }

    private Result initializeNode() throws IOException, InterruptedException {
        if (doFullInitialization) {
            return this.node.initialize();
        } else {
            boolean status = ((JavaNode) this.node).initializeButSkipKernelBuild(false);
            return (status) ? Result.successful() : Result.unsuccessful(Assumptions.TESTING_ERROR_STATUS, "Failed partial initialization in test");
        }
    }

    private static void deleteInitializationDirectories() throws IOException {
        if (nodeDirectory.exists()) {
            FileUtils.deleteDirectory(nodeDirectory);
        }
        if (kernelDirectory.exists()) {
            FileUtils.deleteDirectory(kernelDirectory);
        }
    }

    private static void deleteLogs() throws IOException {
        FileUtils.deleteDirectory(NodeFileManager.getLogsDirectory());
    }

    private void shutdownNodeIfRunning() throws InterruptedException {
        if ((this.node != null) && (this.node.isAlive())) {
            this.node.stop();
        }
    }

}
