package org.aion.harness.main;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.aion.harness.misc.Assumptions;
import org.aion.harness.sys.HarnessLock;
import org.aion.harness.util.SimpleLog;

/**
 * TODO Turn this into a JUnit @Rule
 * TODO (maybe put in the @BeforeClass from the existing tests into here too)
 * TODO Not thread-safe, but right now our JUnits don't run concurrently so that's fine
 * */
public class ProhibitConcurrentHarness {
    private static final long MAX_WAIT = TimeUnit.MINUTES.toNanos(10);
    private static final SimpleLog log = new SimpleLog(ProhibitConcurrentHarness.class.getName());

    private static HarnessLock lock;
    static {
        try {
            lock = new HarnessLock(Assumptions.TEST_HARNESS_LOCK_NAME);
        } catch (IOException ioe) {
            throw new RuntimeException();
        }
    }

    public static synchronized void acquireTestLock() throws Exception {
        boolean acquired = false;
        long t0 = System.nanoTime();
        while(! acquired && System.nanoTime() - t0 < MAX_WAIT) {
            log.log(String.format(
                "Waiting to acquire test harness lock. [waited so far: ~%s min, limit: ~%s min]",
                TimeUnit.NANOSECONDS.toMinutes(System.nanoTime() - t0),
                TimeUnit.NANOSECONDS.toMinutes(MAX_WAIT)));
            acquired = lock.tryAcquire(30, TimeUnit.SECONDS);
        }
        if(!acquired) {
            throw new RuntimeException("Failed to obtain lock.  Giving up.");
        }
    }

    public static synchronized void releaseTestLock() throws Exception {
        lock.release();
    }
}
