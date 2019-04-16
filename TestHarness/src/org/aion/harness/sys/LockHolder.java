package org.aion.harness.sys;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.concurrent.TimeUnit;

/**
 * This class exists solely to facilitate the tests of {@HarnessLock}.
 *
 * It holds a POSIX lock on the file given by the first argument until
 * the program exits.
 */
public class LockHolder {
    public static void main(String args[]) {
        try {
            final String lockFile = args[0];
            System.out.println("[LockHolder] Locking " + lockFile + " until I exit.");
            if(new RandomAccessFile(lockFile, "rw").getChannel().tryLock() == null) {
                System.exit(1);
            }
            for(;;) {
                TimeUnit.SECONDS.sleep(5);
                if(Thread.currentThread().isInterrupted()) {
                    System.out.println("[LockHolder] I was interrupted.  Bye.");
                    System.exit(0);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
