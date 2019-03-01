package org.aion.harness.integ;

import org.aion.harness.main.Node;
import org.aion.harness.main.NodeFactory;
import org.aion.harness.main.types.NodeConfigurationBuilder;
import org.aion.harness.main.types.NodeConfigurations;
import org.aion.harness.result.Result;
import org.aion.harness.util.NodeFileManager;
import org.apache.commons.io.FileUtils;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Tests the various aspects of a node's lifecycle.
 */
public class NodeLifecycleTest {
    private static File nodeDirectory = NodeFileManager.getNodeDirectory();
    private static File kernelDirectory = NodeFileManager.getKernelDirectory();

    private Node node;
    private NodeConfigurations configurations;

    /**
     * Set to false for testing convenience, tune to true if starting from scratch.
     */
    private static boolean doFullInitialization = false;

    /**
     * Number of seconds to sleep during the heartbeat test. Set to something more meaningful for
     * temporary verification.
     */
    private static int heartbeatDurationInSeconds = 10;

    @Before
    public void setup() throws IOException {
        deleteInitializationDirectories();
        this.node = NodeFactory.getNewNodeInstance(NodeFactory.NodeType.JAVA_NODE);
        this.configurations = NodeConfigurationBuilder.defaultConfigurations();
        this.node.configure(this.configurations);
    }

    @After
    public void tearDown() throws IOException {
        shutdownNodeIfRunning();
        deleteInitializationDirectories();
        deleteLogs();
        this.node = null;
    }

    @Test
    public void testInitializeNode() {
        Result result = initializeNode();
        assertTrue(result.isSuccess());

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

    @Test
    public void testStart() {
        initializeNodeWithChecks();
        Result result = this.node.start();
        System.out.println("Start result = " + result);

        assertTrue(result.isSuccess());
        assertTrue(this.node.isAlive());
    }

    @Test(expected = IllegalStateException.class)
    public void testStartWhenNoAionKernelBuildExistsInNodeDirectory() throws IOException {
        initializeNodeWithChecks();
        FileUtils.deleteDirectory(kernelDirectory);
        this.node.start();
    }

    @Test(expected = IllegalStateException.class)
    public void testStartWhenNoNodeDirectoryExists() {
        this.node.start();
    }

    @Test(expected = IllegalStateException.class)
    public void testInvokingStartTwice() {
        initializeNodeWithChecks();
        assertTrue(this.node.start().isSuccess());
        this.node.start();
    }

    @Test
    public void testStop() {
        initializeNodeWithChecks();
        Result result = this.node.start();
        System.out.println("Start result = " + result);

        assertTrue(result.isSuccess());
        assertTrue(this.node.isAlive());

        result = this.node.stop();
        System.out.println("Stop result = " + result);

        assertTrue(result.isSuccess());
        assertFalse(this.node.isAlive());
    }

    @Test
    public void testStopWhenKernelIsNotStarted() {
        assertFalse(this.node.isAlive());
        Result result = this.node.stop();
        System.out.println("Stop result = " + result);

        assertFalse(result.isSuccess());
        assertFalse(this.node.isAlive());
    }

    @Test
    public void testInvokingStopTwice() {
        initializeNodeWithChecks();
        Result result = this.node.start();
        System.out.println("Start result = " + result);

        assertTrue(result.isSuccess());
        assertTrue(this.node.isAlive());

        result = this.node.stop();
        System.out.println("Stop result = " + result);

        assertTrue(result.isSuccess());
        assertFalse(this.node.isAlive());

        result = this.node.stop();
        System.out.println("Stop result = " + result);

        assertFalse(result.isSuccess());
        assertFalse(this.node.isAlive());
    }

    @Test
    public void testNodeHeartbeat() throws InterruptedException {
        initializeNodeWithChecks();
        Result result = this.node.start();
        System.out.println("Start result = " + result);

        assertTrue(result.isSuccess());
        assertTrue(this.node.isAlive());

        Thread.sleep(TimeUnit.SECONDS.toMillis(heartbeatDurationInSeconds));
        assertTrue(this.node.isAlive());

        result = this.node.stop();
        System.out.println("Stop result = " + result);

        assertTrue(result.isSuccess());
        assertFalse(this.node.isAlive());
    }

    @Test
    public void testReset() throws IOException, InterruptedException {
        setupDatabase();

        Result result = this.node.resetState();
        System.out.println("Reset result = " + result);
        assertTrue(result.isSuccess());

        assertFalse(this.configurations.getDatabase().exists());
    }

    @Test(expected = IllegalStateException.class)
    public void testResetWhileNodeIsRunning() {
        initializeNodeWithChecks();
        Result result = this.node.start();
        System.out.println("Start result = " + result);

        assertTrue(result.isSuccess());
        assertTrue(this.node.isAlive());

        this.node.resetState();
    }

    /**
     * This tests verifies that multiple calls to resetState do not cause any exceptions to be thrown.
     */
    @Test
    public void testInvokingResetTwice() throws IOException, InterruptedException {
        setupDatabase();

        Result result = this.node.resetState();
        System.out.println("Reset result = " + result);
        assertTrue(result.isSuccess());

        result = this.node.resetState();
        System.out.println("Reset result = " + result);
        assertTrue(result.isSuccess());
    }

    @Test(expected = IllegalStateException.class)
    public void testResetWhenNoNodeDirectoryExists() {
        this.node.resetState();
    }

    /**
     * This tests verifies that a call to resetState do not cause any exceptions to be thrown when the database does not
     * exist.
     */
    @Test
    public void testResetWhenNoDatabaseExists() throws IOException, InterruptedException{
        setupDatabase();
        File database = this.configurations.getDatabase();
        FileUtils.deleteDirectory(database);

        assertTrue(!database.exists());

        Result result = this.node.resetState();
        System.out.println("Reset result = " + result);

        assertTrue(result.isSuccess());
    }

    private void setupDatabase() throws InterruptedException {
        initializeNodeWithChecks();
        Result result = this.node.start();
        System.out.println("Start result = " + result);

        assertTrue(result.isSuccess());
        assertTrue(this.node.isAlive());

        File database = this.configurations.getDatabase();

        int timeoutInSeconds = 30;
        int sleepTime = 2;

        int durationInSeconds = 0;
        while ((durationInSeconds < timeoutInSeconds) && (!database.exists())) {
            Thread.sleep(TimeUnit.SECONDS.toMillis(sleepTime));
            durationInSeconds += sleepTime;
        }

        if (durationInSeconds >= timeoutInSeconds) {
            fail("Timed out waiting for database to be created!");
        }

        result = this.node.stop();
        System.out.println("Stop result = " + result);

        assertTrue(result.isSuccess());
        assertFalse(this.node.isAlive());
    }

    private Result initializeNode() {
        if (doFullInitialization) {
            Result result = this.node.buildKernel();
            if (!result.isSuccess()) {
                return result;
            }
        }

        return this.node.initializeKernel();
    }

    private void initializeNodeWithChecks() {
        Result result = initializeNode();
        assertTrue(result.isSuccess());

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

    private void shutdownNodeIfRunning() {
        if ((this.node != null) && (this.node.isAlive())) {
            Result result = this.node.stop();
            System.out.println("Stop result = " + result);

            assertTrue(result.isSuccess());
            assertFalse(this.node.isAlive());
        }
    }

}
