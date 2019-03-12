package org.aion.harness.integ;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.aion.harness.main.LocalNode;
import org.aion.harness.main.NodeFactory;
import org.aion.harness.main.NodeListener;
import org.aion.harness.main.event.Event;
import org.aion.harness.main.event.IEvent;
import org.aion.harness.main.util.NodeConfigurationBuilder;
import org.aion.harness.result.LogEventResult;
import org.aion.harness.result.Result;
import org.aion.harness.util.NodeFileManager;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Note that we are unable to latch on to any set of X,Y,Z events that are guaranteed to occur exactly
 * once (so that we don't witness them out of order), always in that order, and late enough after
 * the start so that they are witnessed.
 *
 * What we have below meets the first two requirements, but sometimes these events have been missed
 * by the time the listener starts up.
 *
 * This is why these tests are excluded from 'ant test'.
 */
public class ComplexEventTest {
    private static File nodeDirectory = NodeFileManager.getNodeDirectory();
    private static File kernelDirectory = NodeFileManager.getKernelDirectory();

    private static String sslString = "SSL";
    private static String corsString = "CORS";
    private static String workerString = "Worker Thread Count";

    private static IEvent event1 = Event.or(Event.and(sslString, corsString), new Event(workerString));
    private static IEvent event2 = Event.or(Event.or(sslString, corsString), new Event(workerString));
    private static IEvent event3 = Event.or(Event.and(sslString, "I do not exist"), Event.and(corsString, workerString));

    private LocalNode node;

    /**
     * Set to false for testing convenience, tune to true if starting from scratch.
     */
    private static boolean doFullInitialization = false;

    @Before
    public void setup() throws IOException {
        deleteInitializationDirectories();
        this.node = NodeFactory.getNewLocalNodeInstance(NodeFactory.NodeType.JAVA_NODE);
        this.node.configure(NodeConfigurationBuilder.defaultConfigurations());
    }

    @After
    public void tearDown() throws IOException {
        shutdownNodeIfRunning();
        deleteInitializationDirectories();
        deleteLogs();
        this.node = null;
    }

    /**
     * The event is of the form: ((X and Y) or Z)
     *
     * We expect that the order in which the individual strings occur is: X, Y, Z.
     *
     * Therefore we expect the observed strings to be: X, Y.
     */
    @Test
    public void testComplexLogic1() throws InterruptedException {
        NodeListener listener = NodeListener.listenTo(this.node);

        List<String> expectedObservedEvents = new ArrayList<>();
        expectedObservedEvents.add(sslString);
        expectedObservedEvents.add(corsString);

        initializeNodeWithChecks();

        Result result = this.node.start();
        System.out.println("Start result = " + result);

        assertTrue(result.isSuccess());
        assertTrue(this.node.isAlive());

        LogEventResult eventResult = listener.listenForEvent(event1, 1, TimeUnit.MINUTES).get();

        assertTrue(eventResult.eventWasObserved());
        assertEquals(expectedObservedEvents, eventResult.getAllObservedEvents());

        result = this.node.stop();
        System.out.println("Stop result = " + result);

        assertTrue(result.isSuccess());
        assertFalse(this.node.isAlive());
    }

    /**
     * The event is of the form: (X or Y or Z)
     *
     * We expect that the order in which the individual strings occur is: X, Y, Z.
     *
     * Therefore we expect the observed strings to be: X.
     */
    @Test
    public void testComplexLogic2() throws InterruptedException {
        NodeListener listener = NodeListener.listenTo(this.node);

        List<String> expectedObservedEvents = new ArrayList<>();
        expectedObservedEvents.add(sslString);

        initializeNodeWithChecks();

        Result result = this.node.start();
        System.out.println("Start result = " + result);

        assertTrue(result.isSuccess());
        assertTrue(this.node.isAlive());

        LogEventResult eventResult = listener.listenForEvent(event2, 1, TimeUnit.MINUTES).get();

        assertTrue(eventResult.eventWasObserved());
        assertEquals(expectedObservedEvents, eventResult.getAllObservedEvents());

        result = this.node.stop();
        System.out.println("Stop result = " + result);

        assertTrue(result.isSuccess());
        assertFalse(this.node.isAlive());
    }

    /**
     * The event is of the form: ((W and X) or (Y and Z))
     *
     * We expect that the order in which the individual strings occur is: W, Y, Z.
     *
     * We expect string X to never occur.
     *
     * Therefore we expect the observed strings to be: W, Y, Z.
     */
    @Test
    public void testComplexLogic3() throws InterruptedException {
        NodeListener listener = NodeListener.listenTo(this.node);

        List<String> expectedObservedEvents = new ArrayList<>();
        expectedObservedEvents.add(sslString);
        expectedObservedEvents.add(corsString);
        expectedObservedEvents.add(workerString);

        initializeNodeWithChecks();

        Result result = this.node.start();
        System.out.println("Start result = " + result);

        assertTrue(result.isSuccess());
        assertTrue(this.node.isAlive());

        LogEventResult eventResult = listener.listenForEvent(event3, 1, TimeUnit.MINUTES).get();

        assertTrue(eventResult.eventWasObserved());
        assertEquals(expectedObservedEvents, eventResult.getAllObservedEvents());

        result = this.node.stop();
        System.out.println("Stop result = " + result);

        assertTrue(result.isSuccess());
        assertFalse(this.node.isAlive());
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

    private Result initializeNode() {
        if (doFullInitialization) {
            Result result = this.node.buildKernel();
            if (!result.isSuccess()) {
                return result;
            }
        }

        return this.node.initializeKernel();
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
