package org.aion.harness.tests.integ.runner.internal;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public final class ThreadSpecificStderr extends PrintStream {
    private ThreadLocal<PrintStream> err;

    public ThreadSpecificStderr() {
        super(new ByteArrayOutputStream());
        this.err = new ThreadLocal<>();
    }

    public void setStderr(PrintStream errStream) {
        this.err.set(errStream);
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        this.err.get().write(buf, off, len);
    }

    @Override
    public void flush() {
        this.err.get().flush();
    }

    @Override
    public void close() {
        this.err.get().close();
    }
}
