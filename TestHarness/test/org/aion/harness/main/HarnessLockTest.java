package org.aion.harness.main;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.aion.harness.sys.HarnessLock;
import org.aion.harness.sys.LockHolder;
import org.aion.harness.util.SimpleLog;
import org.junit.Test;

/**
 * @implNote Test has some timing assumptions; can improve them in the future if it's causing problems.
 */
public class HarnessLockTest {
    private final SimpleLog log = new SimpleLog(getClass().getName());

    @Test(timeout = 90_000 /* 1.5 min */)
    public void test() throws Exception {
        String lockName =  "locktest_" + new Random().nextLong();
        lockName = lockName.substring(0, Math.min(16, lockName.length()));
        log.log("Doing lock test with lock name: " + lockName);

        // make the lock
        HarnessLock unit = new HarnessLock(lockName);

        // start up a program that locks the same file (simulates another test harness
        // running)
        File lockFile = new File(unit.getPrefix() + File.separator + lockName);
        lockFile.deleteOnExit();
        Process otherProgram = runLockingJar(
            unit.getPrefix() + File.separator + lockName);
        Thread.sleep(3000);
        if(! otherProgram.isAlive()) {
            throw new RuntimeException("Test error: lock-holding program died.");
        }

        assertThat(
            "tryAcquire() should return false when another program holds it",
            unit.tryAcquire(), is(false));
        assertThat(
            "tryAcquire(5, SECONDS) should return false after 5 sec when another program holds it",
            unit.tryAcquire(5, TimeUnit.SECONDS),
            is(false));

        // set up some code for tryAcquire() to gate
        class CriticalSection {
            public volatile boolean flag = false;
        }
        CriticalSection criticalSection = new CriticalSection();
        // start up another thread that blocks on lock.tryAcquire and tries to use the critical section
        Thread t = new Thread(() -> {
            try {
                unit.tryAcquire(1, TimeUnit.MINUTES); // should block
                criticalSection.flag = true;
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        });
        t.start();

        assertThat(
            "isAcquired() should still be false since we just failed to obtain lock",
            unit.isAcquired(), is(false));
        assertThat(
            "tryAcquire(1, MINUTES) did not block when we expected it to (could be timing issue if system very slow)",
            criticalSection.flag, is(false));

        // kill otherProgram, now the thread should continue
        otherProgram.destroyForcibly();
        otherProgram.waitFor();
        if(otherProgram.isAlive()) {
            throw new RuntimeException("Test error: could not kill spawned program");
        }
        assertThat(
            "isAcquired() should be true since the program holding the lock died",
            unit.isAcquired(), is(true));
        assertThat("tryAcquire(1, MINUTES) should have stopped blocking",
            criticalSection.flag, is(true));

        unit.acquire(); // should do nothing
        unit.release();
        assertThat(
            "isAcquired() should be false after release()",
            unit.isAcquired(), is(false));
        unit.release(); // should do nothing
        log.log("All done!");
    }

    /**
     * @implNote Acquiring the {@link FileLock}, used by {@link HarnessLock},
     * when the underlying file is already locked, has different behaviours depending
     * on what the other lock-holding process is.
     *
     * If the lock is being held by another OS process (including another
     * JVM), acquire/tryAcquire either block or return false as expected.  However,
     * if the file of the FileLock is held by the same JVM, an exception is thrown
     * instead.  So, to test our HarnessLock, we actually need to launch a separate
     * JVM to hold the same underlying lock (so we get the block or return
     * behaviour).
     *
     * This method runs a jar that will hold a given file lock.
     */
    private static Process runLockingJar(String lockFile) throws IOException  {
        String classpath = System.getProperty("java.class.path");
        String path = System.getProperty("java.home")
            + File.separator + "bin" + File.separator + "java";
        return new ProcessBuilder(
            path, "-cp", classpath, LockHolder.class.getName(), lockFile
        ).inheritIO().start();
    }
}