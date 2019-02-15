package org.aion.harness.util;

import org.aion.harness.misc.Assumptions;
import org.aion.harness.result.Result;
import org.apache.commons.io.input.Tailer;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A class responsible for reading a log file and setting up a {@link LogListener} that eavesdrops on the log file.
 */
public final class LogReader {
    private ExecutorService threadExecutor;
    private Tailer logTailer;
    private LogListener listener;

    public LogReader() {
        this.listener = new LogListener();
    }

    public Result startReading(File log) {
        if (log == null) {
            return Result.unsuccessful(Assumptions.PRODUCTION_ERROR_STATUS, "Output log file does not exist!");
        }

        // Attempt to turn the listener on. If it is already on then pass this "warning/error" to the caller.
        Result result = this.listener.startListening();
        if (!result.success) {
            return result;
        }

        this.threadExecutor = Executors.newSingleThreadExecutor();
        this.logTailer = new Tailer(log, this.listener, 0);
        this.threadExecutor.execute(this.logTailer);

        return Result.successful();
    }

    public void stopReading() throws InterruptedException {
        if (this.listener.isAlive()) {

            this.listener.stopListening();
            this.logTailer.stop();
            this.threadExecutor.shutdownNow();

            if (!this.threadExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                System.out.println(Assumptions.LOGGER_BANNER
                    + "Failed to shut down the log reader thread - timed out!");
            }
        } else {
            // If the listener is dead it cannot be recovered. We need to scrap it entirely.
            this.listener = new LogListener();
        }

        this.threadExecutor = null;
    }

    public LogListener getLogListener() {
        return this.listener;
    }

}
