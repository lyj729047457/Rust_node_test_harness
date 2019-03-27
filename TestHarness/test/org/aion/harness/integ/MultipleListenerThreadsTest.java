package org.aion.harness.integ;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.aion.harness.integ.resources.Eavesdropper;
import org.aion.harness.integ.resources.Eavesdropper.Gossip;
import org.aion.harness.main.LocalNode;
import org.aion.harness.integ.resources.TarFileFinder;
import org.aion.harness.main.util.NodeConfigurationBuilder;
import org.aion.harness.main.NodeFactory;
import org.aion.harness.result.Result;
import org.aion.harness.util.NodeFileManager;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MultipleListenerThreadsTest {
    private static File nodeDirectory = new File(NodeFileManager.getSandboxPath());
    private static File kernelDirectory = NodeFileManager.getKernelDirectory();

    private static final int NUM_THREADS = 20;

    private static final long TEST_DURATION = TimeUnit.SECONDS.toMillis(45);

    private LocalNode node;

    @Before
    public void setup() throws IOException {
        deleteInitializationDirectories();
        this.node = NodeFactory.getNewLocalNodeInstance(NodeFactory.NodeType.JAVA_NODE);

        File packDir = TarFileFinder.getPackDirectory(NodeConfigurationBuilder.DEFAULT_KERNEL_SOURCE_DIR);
        String builtKernel = packDir.getAbsolutePath() + File.separator + "javaKernel.tar.bz2";

        this.node.configure(NodeConfigurationBuilder.defaultConditionalBuildConfigurations(builtKernel,false));
    }

    @After
    public void tearDown() throws Exception {
        shutdownNodeIfRunning();
        deleteInitializationDirectories();
        deleteLogs();
        this.node = null;
    }

    @Test
    public void testMultipleThreadsRequestingHeartbeatEvents() throws IOException, InterruptedException {
        // Start the node.
        this.node.initialize();

        Result result = this.node.start();
        System.out.println("Start result = " + result);

        assertTrue(result.isSuccess());
        assertTrue(this.node.isAlive());

        // Create the thread pool and all of our eavesdroppers.
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

        List<Eavesdropper> eavesdroppers = new ArrayList<>();
        for (int i = 0; i < NUM_THREADS; i++) {
            eavesdroppers.add(Eavesdropper.createEavesdropperThatListensFor(Gossip.HEARTBEAT, i, this.node));
        }

        // Start running all of our eavesdropping threads.

        for (Eavesdropper eavesdropper : eavesdroppers) {
            executor.execute(eavesdropper);
        }

        // Sleep for the specified duration and then wake up and shut down the threads.
        Thread.sleep(TEST_DURATION);
        for (Eavesdropper eavesdropper : eavesdroppers) {
            eavesdropper.kill();
        }
        executor.shutdownNow();

        // Shut down the node and wait for the threads to finish.
        result = this.node.stop();
        System.out.println("Stop result = " + result);

        assertTrue(result.isSuccess());
        assertFalse(this.node.isAlive());

        executor.awaitTermination(30, TimeUnit.SECONDS);
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

    private void shutdownNodeIfRunning() throws IOException, InterruptedException {
        if ((this.node != null) && (this.node.isAlive())) {
            Result result = this.node.stop();
            System.out.println("Stop result = " + result);

            assertTrue(result.isSuccess());
            assertFalse(this.node.isAlive());
        }
    }

}
