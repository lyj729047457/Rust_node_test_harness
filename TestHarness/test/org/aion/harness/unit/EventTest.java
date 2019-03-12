package org.aion.harness.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.aion.harness.main.event.Event;
import org.aion.harness.main.event.IEvent;
import org.junit.Assert;
import org.junit.Test;

public class EventTest {

    @Test
    public void testObservationTimes() throws InterruptedException {
        String eventString1 = "one";
        String eventString2 = "two";
        IEvent event1 = new Event(eventString1);
        IEvent event2 = new Event(eventString2);
        IEvent fullEvent = Event.and(event1, event2);

        long observedTimeString1 = System.nanoTime();
        assertFalse(fullEvent.isSatisfiedBy("one", observedTimeString1, TimeUnit.NANOSECONDS));

        assertTrue(event1.hasBeenObserved());
        assertFalse(event2.hasBeenObserved());
        assertFalse(fullEvent.hasBeenObserved());

        // Just to really make sure the two times differ...
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));

        long observedTimeString2 = System.nanoTime();
        assertTrue(fullEvent.isSatisfiedBy("two", observedTimeString2, TimeUnit.NANOSECONDS));

        assertTrue(event1.hasBeenObserved());
        assertTrue(event2.hasBeenObserved());
        assertTrue(fullEvent.hasBeenObserved());

        assertEquals(observedTimeString1, event1.observedAt(TimeUnit.NANOSECONDS));
        assertEquals(observedTimeString2, event2.observedAt(TimeUnit.NANOSECONDS));
    }

    @Test
    public void testBareEventIsObserved() {
        String eventString = "me";
        IEvent event = new Event(eventString);
        assertFalse(event.isSatisfiedBy("em", System.nanoTime(), TimeUnit.NANOSECONDS));
        assertFalse(event.hasBeenObserved());

        List<String> observed = event.getAllObservedEvents();
        assertTrue(observed.isEmpty());

        List<String> logs = event.getAllObservedLogs();
        assertTrue(logs.isEmpty());

        long observedAt = System.nanoTime();
        assertTrue(event.isSatisfiedBy("me2", observedAt, TimeUnit.NANOSECONDS));
        assertTrue(event.hasBeenObserved());

        observed = event.getAllObservedEvents();
        assertEquals(1, observed.size());
        assertEquals(eventString, observed.get(0));
        assertEquals(observedAt, event.observedAt(TimeUnit.NANOSECONDS));
    }

    @Test
    public void testAndEventIsObserved() {
        String condition1string = "lucky";
        String condition2string = "news";

        IEvent event1 = new Event(condition1string);
        IEvent event2 = new Event(condition2string);

        IEvent event = Event.and(event1, event2);

        assertFalse(event.isSatisfiedBy("chucky crews", System.nanoTime(), TimeUnit.NANOSECONDS));

        assertFalse(event1.hasBeenObserved());
        assertFalse(event2.hasBeenObserved());
        assertFalse(event.hasBeenObserved());

        List<String> observed = event.getAllObservedEvents();
        assertTrue(observed.isEmpty());

        List<String> logs = event.getAllObservedLogs();
        assertTrue(logs.isEmpty());

        // The condition is not satisfied by this, but one part of it is.
        String logLine = "i read the news today";
        assertFalse(event.isSatisfiedBy(logLine, System.nanoTime(), TimeUnit.NANOSECONDS));

        assertFalse(event1.hasBeenObserved());
        assertTrue(event2.hasBeenObserved());
        assertFalse(event.hasBeenObserved());

        observed = event.getAllObservedEvents();
        assertEquals(1, observed.size());
        assertEquals(condition2string, observed.get(0));

        logs = event.getAllObservedLogs();
        assertEquals(1, logs.size());
        assertEquals(logLine, logs.get(0));

        String logLine2 = "news l ucky news lucky";
        long observedAt = System.nanoTime();
        assertTrue(event.isSatisfiedBy(logLine2, observedAt, TimeUnit.NANOSECONDS));

        assertTrue(event1.hasBeenObserved());
        assertTrue(event2.hasBeenObserved());
        assertTrue(event.hasBeenObserved());

        observed = event.getAllObservedEvents();
        assertEquals(2, observed.size());

        logs = event.getAllObservedLogs();
        assertEquals(2, logs.size());
        assertEquals(logLine2, logs.get(0));
        assertEquals(observedAt, event.observedAt(TimeUnit.NANOSECONDS));

        boolean saw1 = false;
        boolean saw2 = false;

        for (String seen : observed) {
            if (seen.equals(condition1string)) {
                saw1 = true;
            } else if (seen.equals(condition2string)) {
                saw2 = true;
            } else {
                fail("Not one of the expected strings: " + seen);
            }
        }

        assertTrue(saw1 && saw2);
    }

    @Test
    public void testOrEventIsObserved() {
        String condition1string = "lucky";
        String condition2string = "news";

        IEvent event1 = new Event(condition1string);
        IEvent event2 = new Event(condition2string);

        IEvent event = Event.or(event1, event2);

        assertFalse(event.isSatisfiedBy("chucky crews", System.nanoTime(), TimeUnit.NANOSECONDS));

        assertFalse(event1.hasBeenObserved());
        assertFalse(event2.hasBeenObserved());
        assertFalse(event.hasBeenObserved());

        List<String> observed = event.getAllObservedEvents();
        assertTrue(observed.isEmpty());

        List<String> logs = event.getAllObservedLogs();
        assertTrue(logs.isEmpty());

        // This satisfies one condition and therefore the whole thing.
        String logLine = "i read the news today";
        long observedAt = System.nanoTime();
        assertTrue(event.isSatisfiedBy(logLine, observedAt, TimeUnit.NANOSECONDS));

        assertFalse(event1.hasBeenObserved());
        assertTrue(event2.hasBeenObserved());
        assertTrue(event.hasBeenObserved());

        observed = event.getAllObservedEvents();
        assertEquals(1, observed.size());
        assertEquals(condition2string, observed.get(0));
        assertEquals(observedAt, event.observedAt(TimeUnit.NANOSECONDS));

        logs = event.getAllObservedLogs();
        assertEquals(1, logs.size());
        assertEquals(logLine, logs.get(0));
    }

    @Test
    public void testOrEventBothConditionsObservedAtOnce() {
        String condition1string = "lucky";
        String condition2string = "news";

        IEvent event1 = new Event(condition1string);
        IEvent event2 = new Event(condition2string);

        IEvent event = Event.or(event1, event2);

        assertFalse(event.isSatisfiedBy("chucky crews", System.nanoTime(), TimeUnit.NANOSECONDS));

        assertFalse(event1.hasBeenObserved());
        assertFalse(event2.hasBeenObserved());
        assertFalse(event.hasBeenObserved());

        List<String> observed = event.getAllObservedEvents();
        assertTrue(observed.isEmpty());

        Assert.assertTrue(event.getAllObservedLogs().isEmpty());

        String logLine = "news l ucky news lucky";
        long observedAt = System.nanoTime();
        assertTrue(event.isSatisfiedBy(logLine, observedAt, TimeUnit.NANOSECONDS));

        assertTrue(event1.hasBeenObserved());
        assertTrue(event2.hasBeenObserved());
        assertTrue(event.hasBeenObserved());

        observed = event.getAllObservedEvents();
        assertEquals(2, observed.size());
        assertEquals(observedAt, event.observedAt(TimeUnit.NANOSECONDS));

        List<String> logs = event.getAllObservedLogs();
        assertEquals(2, logs.size());

        boolean saw1 = false;
        boolean saw2 = false;

        for (String seen : observed) {
            if (seen.equals(condition1string)) {
                saw1 = true;
            } else if (seen.equals(condition2string)) {
                saw2 = true;
            } else {
                fail("Not one of the expected strings: " + seen);
            }
        }

        assertTrue(saw1 && saw2);

        int count = 0;

        for (String seen : logs) {
            if (seen.equals(logLine)) {
                count++;
            } else {
                fail("Not one of the expected strings: " + seen);
            }
        }

        assertEquals(2, count);
    }

    @Test
    public void testComplexConditionalIsObservedInOneLine() {
        String hammerString = "hammer";
        String nailsString = "nails";
        String chalkString = "chalk";
        String drillString = "drill";
        String timberString = "timber";
        String millString = "mill";

        IEvent chalk = new Event(chalkString);

        IEvent hammerOrNails = Event.or(hammerString, nailsString);
        IEvent drillAndTimberOrChalk = Event.or(Event.and(drillString, timberString), chalk);
        IEvent chalkAndMill = Event.and(chalkString, millString);

        IEvent total = Event.or(Event.and(hammerOrNails, drillAndTimberOrChalk), chalkAndMill);

        // --------------------------
        String logLine = "millchalk";
        long observedAt = System.nanoTime();
        assertTrue(total.isSatisfiedBy(logLine, observedAt, TimeUnit.NANOSECONDS));
        List<String> observed = total.getAllObservedEvents();

        assertFalse(hammerOrNails.hasBeenObserved());
        assertTrue(drillAndTimberOrChalk.hasBeenObserved());
        assertTrue(chalkAndMill.hasBeenObserved());
        assertTrue(total.hasBeenObserved());

        // 3 because there are 2 "chalk" events in the logic.
        assertEquals(3, observed.size());

        List<String> logs = total.getAllObservedLogs();
        assertEquals(3, logs.size());
        assertEquals(observedAt, total.observedAt(TimeUnit.NANOSECONDS));

        int sawChalkCount = 0;
        boolean sawMill = false;

        for (String seen : observed) {
            if (seen.equals(chalkString)) {
                sawChalkCount++;
            } else if (seen.equals(millString)) {
                sawMill = true;
            } else {
                fail("Unexpected observed string: " + seen);
            }
        }

        assertTrue((sawChalkCount == 2) && sawMill);
        int sawLogCount = 0;

        for (String seen : logs) {
            if (seen.equals(logLine)) {
                sawLogCount++;
            } else {
                fail("Unexpected observed string: " + seen);
            }
        }

        assertEquals(3, sawLogCount);
    }

    @Test
    public void testComplexConditionalIsObserved() {
        String hammerString = "hammer";
        String nailsString = "nails";
        String chalkString = "chalk";
        String drillString = "drill";
        String timberString = "timber";
        String millString = "mill";

        IEvent chalk = new Event(chalkString);

        IEvent hammerOrNails = Event.or(hammerString, nailsString);
        IEvent drillAndTimberOrChalk = Event.or(Event.and(drillString, timberString), chalk);
        IEvent chalkAndMill = Event.and(chalkString, millString);

        IEvent total = Event.or(Event.and(hammerOrNails, drillAndTimberOrChalk), chalkAndMill);

        // --------------------------

        List<String> expectedStrings = new ArrayList<>();
        List<String> expectedLogs = new ArrayList<>();

        // Satisfy the "hammer or nails" clause.
        String logLine = "ammerorails - nails!";
        assertFalse(total.isSatisfiedBy(logLine, System.nanoTime(), TimeUnit.NANOSECONDS));
        assertEquals(1, total.getAllObservedEvents().size());

        assertTrue(hammerOrNails.hasBeenObserved());
        assertFalse(drillAndTimberOrChalk.hasBeenObserved());
        assertFalse(chalkAndMill.hasBeenObserved());
        assertFalse(total.hasBeenObserved());

        // we observed the event: nails
        List<String> logs = total.getAllObservedLogs();
        assertEquals(1, logs.size());

        expectedStrings.add(nailsString);
        assertEquals(expectedStrings, total.getAllObservedEvents());
        expectedLogs.add(logLine);
        assertEquals(expectedLogs, total.getAllObservedLogs());

        // Observe "drill", have still not satisfied: "(drill and timber) or chalk"
        String logLine2 = "drilll";
        assertFalse(total.isSatisfiedBy(logLine2, System.nanoTime(), TimeUnit.NANOSECONDS));
        assertEquals(2, total.getAllObservedEvents().size());

        assertTrue(hammerOrNails.hasBeenObserved());
        assertFalse(drillAndTimberOrChalk.hasBeenObserved());
        assertFalse(chalkAndMill.hasBeenObserved());
        assertFalse(total.hasBeenObserved());

        // we observed the events: nails, drill
        logs = total.getAllObservedLogs();
        assertEquals(2, logs.size());

        expectedStrings.add(drillString);
        assertEquals(expectedStrings, total.getAllObservedEvents());
        expectedLogs.add(logLine2);
        assertEquals(expectedLogs, total.getAllObservedLogs());

        // Observe "hammer", this changes nothing because that clause was satisfied
        // and cannot be 're-satisfied'.
        assertFalse(total.isSatisfiedBy("hammer", System.nanoTime(), TimeUnit.NANOSECONDS));
        assertEquals(2, total.getAllObservedEvents().size());
        assertEquals(expectedStrings, total.getAllObservedEvents());

        assertTrue(hammerOrNails.hasBeenObserved());
        assertFalse(drillAndTimberOrChalk.hasBeenObserved());
        assertFalse(chalkAndMill.hasBeenObserved());
        assertFalse(total.hasBeenObserved());

        logs = total.getAllObservedLogs();
        assertEquals(2, logs.size());
        assertEquals(expectedLogs, total.getAllObservedLogs());

        // Observe "chalk", now "(drill and timber) or chalk" is satisfied and
        // thus the AND is too and thus the top-level OR also.
        String logLine3 = "chalk";
        long observedAt = System.nanoTime();
        assertTrue(total.isSatisfiedBy(logLine3, observedAt, TimeUnit.NANOSECONDS));

        assertTrue(hammerOrNails.hasBeenObserved());
        assertTrue(drillAndTimberOrChalk.hasBeenObserved());
        assertFalse(chalkAndMill.hasBeenObserved());
        assertTrue(total.hasBeenObserved());

        // 4 because there are 2 "chalk" events.
        assertEquals(4, total.getAllObservedEvents().size());

        expectedStrings.add(chalkString);
        expectedStrings.add(chalkString);
        assertEquals(expectedStrings, total.getAllObservedEvents());
        assertEquals(observedAt, total.observedAt(TimeUnit.NANOSECONDS));

        logs = total.getAllObservedLogs();
        assertEquals(4, logs.size());
        expectedLogs.add(logLine3);
        expectedLogs.add(logLine3);
        assertEquals(expectedLogs, total.getAllObservedLogs());

        // Now observe "mill", which should 're-satisfy' the top level clause.
        // But this is not possible, so nothing changes.
        assertTrue(total.isSatisfiedBy("mill", System.nanoTime(), TimeUnit.NANOSECONDS));

        // Nothing here should change.
        assertTrue(hammerOrNails.hasBeenObserved());
        assertTrue(drillAndTimberOrChalk.hasBeenObserved());
        assertFalse(chalkAndMill.hasBeenObserved());
        assertTrue(total.hasBeenObserved());

        assertEquals(4, total.getAllObservedEvents().size());
        assertEquals(expectedStrings, total.getAllObservedEvents());

        logs = total.getAllObservedLogs();
        assertEquals(4, logs.size());
        assertEquals(expectedLogs, total.getAllObservedLogs());
        assertEquals(observedAt, total.observedAt(TimeUnit.NANOSECONDS));

        // check for each observed event

        int sawHammerCounter = 0;
        int sawNailsCounter = 0;
        int sawChalkCounter = 0;
        int sawDrillCounter = 0;
        int sawTimberCounter = 0;
        int sawMillCounter = 0;

        for (String seen : total.getAllObservedEvents()) {
            if (seen.equals(hammerString)) {
                sawHammerCounter++;
            } else if (seen.equals(nailsString)) {
                sawNailsCounter++;
            } else if (seen.equals(chalkString)) {
                sawChalkCounter++;
            } else if (seen.equals(drillString)) {
                sawDrillCounter++;
            } else if (seen.equals(timberString)) {
                sawTimberCounter++;
            } else if (seen.equals(millString)) {
                sawMillCounter++;
            } else {
                fail("Unexpected observed string: " + seen);
            }
        }

        assertEquals(0, sawHammerCounter);
        assertEquals(1, sawNailsCounter);
        assertEquals(2, sawChalkCounter);
        assertEquals(1, sawDrillCounter);
        assertEquals(0, sawTimberCounter);
        assertEquals(0, sawMillCounter);

        // check for each observed log

        int sawLogLineCounter = 0;
        int sawLogLine2Counter = 0;
        int sawLogLine3Counter = 0;

        for (String seen : logs) {
            if (seen.equals(logLine)) {
                sawLogLineCounter++;
            } else if (seen.equals(logLine2)) {
                sawLogLine2Counter++;
            } else if (seen.equals(logLine3)) {
                sawLogLine3Counter++;
            } else {
                fail("Unexpected observed string: " + seen);
            }
        }

        assertEquals(1, sawLogLineCounter);
        assertEquals(1, sawLogLine2Counter);
        assertEquals(2, sawLogLine3Counter);
    }

}
