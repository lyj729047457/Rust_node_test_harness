package org.aion.harness.util;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Very simple logger with hardcoded timezone (UTC) and format (ISO-8601).
 */
public class SimpleLog {
    private final String name;
    private final PrintStream out;

    public static final TimeZone TZ;
    public static final DateFormat FMT;

    static {
        TZ  = TimeZone.getTimeZone("UTC");
        // ISO-8601 with hardcoded 'Z' because we're always UTC
        FMT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        FMT.setTimeZone(TZ);
    }

    /** constructor */
    public SimpleLog(String name) {
        this(name, System.out);
    }

    /** constructor */
    SimpleLog(String name, PrintStream out) {
        this.name = name;
        this.out = out;
    }

    /** log the date, logger name, and the toString of an object */
    public void log(Object object) {
        log(object.toString(), new Date() /* now */);
    }

    /** log the date, logger name, and a message */
    public void log(String message) {
        log(message, new Date() /* now */);
    }

    /* VisibleForTesting*/ void log(String message, Date when) {
        String now = FMT.format(when);
        out.println(String.format("[%s %s] %s", now, name, message));
    }

}