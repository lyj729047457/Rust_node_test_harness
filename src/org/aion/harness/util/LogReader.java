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
    private boolean isCurrentlyReading = false;

    public LogReader() {}

    public Result startReading(File log) {
        if (this.isCurrentlyReading) {
            return Result.unsuccessful(Assumptions.PRODUCTION_ERROR_STATUS, "LogReader is already reading a log.");
        }

        if (log == null) {
            return Result.unsuccessful(Assumptions.PRODUCTION_ERROR_STATUS, "Output log file does not exist!");
        }

        this.threadExecutor = Executors.newSingleThreadExecutor();
        this.listener = new LogListener();
        this.logTailer = new Tailer(log, this.listener, 0);
        this.threadExecutor.execute(this.logTailer);

        this.isCurrentlyReading = true;
        return Result.successful();
    }

    public void stopReading() throws InterruptedException {
        this.logTailer.stop();
        this.threadExecutor.shutdownNow();

        if (!this.threadExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
            System.out.println(Assumptions.LOGGER_BANNER + "Failed to shut down the log reader thread - timed out!");
        }

        this.isCurrentlyReading = false;
        this.threadExecutor = null;
    }

    public LogListener getLogListener() {
        return this.listener;
    }

}
