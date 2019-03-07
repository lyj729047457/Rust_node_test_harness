package org.aion.harness.main.impl;

import org.aion.harness.main.Node;
import org.aion.harness.main.global.SingletonFactory;
import org.aion.harness.result.Result;
import org.aion.harness.util.LogReader;

import java.io.File;

public final class RemoteNode implements Node {
    private final LogReader logReader;
    private final int ID;
    private boolean isConnected;

    public RemoteNode() {
        logReader = new LogReader();
        this.ID = SingletonFactory.singleton().nodeWatcher().addReader(this.logReader);
        this.isConnected = false;
    }

    @Override
    public int getID() {
        return this.ID;
    }

    public Result connect(File logFile) {
        if (this.isConnected) {
            throw new IllegalStateException("the remote node is already connected");
        }

        if (logFile == null) {
            throw new NullPointerException("log file cannot be null");
        }

        if (!logFile.isFile()) {
            throw new IllegalArgumentException(logFile + " is not a file");
        }

        this.isConnected = true;
        return this.logReader.startReading(logFile);
    }

    public Result disconnect() throws InterruptedException {
        if (this.isConnected) {
            this.logReader.stopReading();
            this.isConnected = false;
        }

        return Result.successful();
    }
}
