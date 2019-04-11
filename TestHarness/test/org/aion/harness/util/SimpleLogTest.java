package org.aion.harness.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Date;
import org.junit.Test;

public class SimpleLogTest {

    @Test
    public void test() throws Exception {
        String name = getClass().getName();
        Date date = new Date(1231006505000l);
        String message = "Chancellor on brink of second bailout for banks";

        String expected =
            "[2009-01-03T18:15:05.000Z org.aion.harness.util.SimpleLogTest] Chancellor on brink of second bailout for banks\n";

        ByteArrayOutputStream underlying = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(underlying);

        SimpleLog unit = new SimpleLog(name, stream);
        unit.log(message, date);

        String actual = new String(underlying.toString("UTF-8"));
        assertThat(actual, is(expected));
    }
}