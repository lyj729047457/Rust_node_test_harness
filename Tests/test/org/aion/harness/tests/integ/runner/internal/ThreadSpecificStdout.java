package org.aion.harness.tests.integ.runner.internal;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public final class ThreadSpecificStdout extends PrintStream {
    private ThreadLocal<PrintStream> out;

    public ThreadSpecificStdout() {
        super(new ByteArrayOutputStream());
        this.out = new ThreadLocal<>();
    }

    public void setStdout(PrintStream outStream) {
        this.out.set(outStream);
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        this.out.get().write(buf, off, len);
    }

    @Override
    public void flush() {
        this.out.get().flush();
    }

    @Override
    public void close() {
        this.out.get().close();
    }
}
