package org.aion.harness.tests.integ.runner.internal;

import org.junit.runner.Description;

/**
 * Contextual information to be handed off to a {@link TestExecutor} thread.
 *
 * The description contains the description of the method to be tested, and testClass is the class
 * in which this method is defined.
 */
public final class TestContext {
    public final Class<?> testClass;
    public final Description testDescription;

    public TestContext(Class<?> testClass, Description testDescription) {
        this.testClass = testClass;
        this.testDescription = testDescription;
    }
}
