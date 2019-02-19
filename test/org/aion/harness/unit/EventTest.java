package org.aion.harness.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import org.aion.harness.main.event.Event;
import org.aion.harness.main.event.IEvent;
import org.junit.Test;

public class EventTest {

    @Test
    public void testBareEventIsObserved() {
        String eventString = "me";
        IEvent event = new Event(eventString);
        assertFalse(event.isSatisfiedBy("em"));

        List<String> observed = event.getAllObservedEvents();
        assertTrue(observed.isEmpty());

        assertTrue(event.isSatisfiedBy("me2"));

        observed = event.getAllObservedEvents();
        assertEquals(1, observed.size());
        assertEquals(eventString, observed.get(0));
    }

    @Test
    public void testAndEventIsObserved() {
        String condition1string = "lucky";
        String condition2string = "news";

        IEvent event = new Event(condition1string);
        event = event.and(new Event(condition2string));

        assertFalse(event.isSatisfiedBy("chucky crews"));

        List<String> observed = event.getAllObservedEvents();
        assertTrue(observed.isEmpty());

        // The condition is not satisfied by this, but one part of it is.
        assertFalse(event.isSatisfiedBy("i read the news today"));

        observed = event.getAllObservedEvents();
        assertEquals(1, observed.size());
        assertEquals(condition2string, observed.get(0));

        assertTrue(event.isSatisfiedBy("news l ucky news lucky"));

        observed = event.getAllObservedEvents();
        assertEquals(2, observed.size());

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

        IEvent event = new Event(condition1string);
        event = event.or(new Event(condition2string));

        assertFalse(event.isSatisfiedBy("chucky crews"));

        List<String> observed = event.getAllObservedEvents();
        assertTrue(observed.isEmpty());

        // This satisfies one condition and therefore the whole thing.
        assertTrue(event.isSatisfiedBy("i read the news today"));

        observed = event.getAllObservedEvents();
        assertEquals(1, observed.size());
        assertEquals(condition2string, observed.get(0));
    }

    @Test
    public void testOrEventBothConditionsObservedAtOnce() {
        String condition1string = "lucky";
        String condition2string = "news";

        IEvent event = new Event(condition1string);
        event = event.or(new Event(condition2string));

        assertFalse(event.isSatisfiedBy("chucky crews"));

        List<String> observed = event.getAllObservedEvents();
        assertTrue(observed.isEmpty());

        assertTrue(event.isSatisfiedBy("news l ucky news lucky"));

        observed = event.getAllObservedEvents();
        assertEquals(2, observed.size());

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
        assertTrue(total.isSatisfiedBy("millchalk"));
        List<String> observed = total.getAllObservedEvents();

        // 3 because there are 2 "chalk" events in the logic.
        assertEquals(3, observed.size());

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

        // Satisfy the "hammer or nails" clause.
        assertFalse(total.isSatisfiedBy("ammerorails - nails!"));
        assertEquals(1, total.getAllObservedEvents().size());

        expectedStrings.add(nailsString);
        assertEquals(expectedStrings, total.getAllObservedEvents());

        // Observe "drill", have still not satisfied: "(drill and timber) or chalk"
        assertFalse(total.isSatisfiedBy("drilll"));
        assertEquals(2, total.getAllObservedEvents().size());

        expectedStrings.add(drillString);
        assertEquals(expectedStrings, total.getAllObservedEvents());

        // Observe "hammer", this changes nothing because that clause was satisfied
        // and cannot be 're-satisfied'.
        assertFalse(total.isSatisfiedBy("hammer"));
        assertEquals(2, total.getAllObservedEvents().size());
        assertEquals(expectedStrings, total.getAllObservedEvents());

        // Observe "chalk", now "(drill and timber) or chalk" is satisfied and
        // thus the AND is too and thus the top-level OR also.
        assertTrue(total.isSatisfiedBy("chalk"));

        // 4 because there are 2 "chalk" events.
        assertEquals(4, total.getAllObservedEvents().size());

        expectedStrings.add(chalkString);
        expectedStrings.add(chalkString);
        assertEquals(expectedStrings, total.getAllObservedEvents());

        // Now observe "mill", which should 're-satisfy' the top level clause.
        // But this is not possible, so nothing changes.
        assertTrue(total.isSatisfiedBy("mill"));

        assertEquals(4, total.getAllObservedEvents().size());
        assertEquals(expectedStrings, total.getAllObservedEvents());
    }

}
