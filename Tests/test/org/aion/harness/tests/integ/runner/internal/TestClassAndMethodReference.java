package org.aion.harness.tests.integ.runner.internal;

import java.lang.reflect.Method;

/**
 * Holds an instance of testClass, where testClass is the class in which the testMethod is defined in,
 * and testMethod is a method bound explicitly to this test instance.
 *
 * This data is required by the {@link TestExecutor} internally, so that it runs each test on a fresh
 * instance of the class.
 */
public final class TestClassAndMethodReference {
    public final Class<?> testClass;
    public final Object instanceOfTestClass;
    public final Method instanceOfTestMethod;

    public TestClassAndMethodReference(Class<?> testClass, Object instanceOfTestClass, Method testMethod) {
        this.testClass = testClass;
        this.instanceOfTestClass = instanceOfTestClass;
        this.instanceOfTestMethod = testMethod;
    }
}
