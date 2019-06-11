package org.aion.harness.tests.integ.runner.internal;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.aion.harness.main.NodeFactory.NodeType;
import org.aion.harness.main.NodeListener;
import org.aion.harness.main.event.JavaPrepackagedLogEvents;
import org.aion.harness.main.event.RustPrepackagedLogEvents;
import org.aion.harness.tests.integ.runner.exception.UnexpectedTestRunnerException;
import org.aion.harness.tests.integ.runner.exception.UnsupportedAnnotation;

/**
 * A dedicated thread for executing test methods.
 *
 * This executor can be given a list of N tests to run and it will run them sequentially, where each
 * test is run against a new instance of its test class and the following batch of methods or
 * initializations are run in this order:
 *
 * 1. First running any @Before methods
 * 2. Then initializing any @Rule's
 * 3. Then running the test method
 * 4. Then running any @After methods
 *
 * A list of futures is available for immediate consumption once the constructor call finishes and
 * this class is instantiated. Ignored methods are determined ahead of time and already finished.
 *
 * The consumer can iterate over the returned list of futures and consume each one. The information
 * returned gives enough context to determine how to notify JUnit of the test's progress.
 */
public final class TestExecutor implements Runnable {
    private final TestNodeManager nodeManagerForTests;
    private final TestAndResultQueueManager queueManager;
    private final NodeType nodeType;
    private final PreminedAccountFunder preminedDispatcher;
    private boolean alive;

    public TestExecutor(TestNodeManager nodeManager,
                        PreminedAccountFunder preminedDispatcher,
                        TestAndResultQueueManager queueManager,
                        NodeType nodeType) {
        this.nodeManagerForTests = nodeManager;
        this.queueManager = queueManager;
        this.nodeType = nodeType;
        this.preminedDispatcher = preminedDispatcher;
        this.alive = true;
    }

    @Override
    public void run() {
        while (this.alive) {
            TestContext testContext = this.queueManager.takeTest();

            if (testContext == null) {
                // If the queue returned null then the producer has closed it down, there are no tests
                // left to run, so this thread can exit.
                this.alive = false;
            } else {
                try (ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {
                    try (ByteArrayOutputStream errStream = new ByteArrayOutputStream()) {

                        // Replace the thread-local stdout/stderr with our own out/err streams for this test only.
                        ((ThreadSpecificStdout) System.out).setStdout(new PrintStream(new BufferedOutputStream(outStream)));
                        ((ThreadSpecificStderr) System.err).setStderr(new PrintStream(new BufferedOutputStream(errStream)));

                        printTestStartBanner(testContext);

                        // Run the test and place the result in the outbound queue.
                        TestResult result = runTest(testContext);

                        printTestEndBanner();

                        // Flush the stdout and stderr streams, and save them to the test result.
                        System.out.flush();
                        System.err.flush();
                        result.stdout = outStream.toByteArray();
                        result.stderr = errStream.toByteArray();

                        this.queueManager.putResult(result);
                    }
                } catch (Throwable e) {
                    throw new UnexpectedTestRunnerException("Error running the test: " + testContext.testDescription.getMethodName(), e.getCause());
                }
            }
        }
        this.queueManager.reportThreadIsFinished();
    }

    private TestResult runTest(TestContext testContext) {
        Object instanceOfTestClass;
        Method testMethod;

        // Create a unique instance of the test class to run this test against.
        try {
            instanceOfTestClass = testContext.testClass.getConstructor().newInstance();
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            return TestResult.failed(testContext.testDescription, e);
        }

        // Grab the test method to be run.
        try {
            String fakeMethodName = testContext.testDescription.getMethodName();
            String realMethodName = fakeMethodName.split(":")[1];
            testMethod = testContext.testClass.getDeclaredMethod(realMethodName);
        } catch (NoSuchMethodException e) {
            return TestResult.failed(testContext.testDescription, e);
        }

        if (isMethodIgnored(testMethod)) {
            return TestResult.ignored(testContext.testDescription);
        }

        // Run any @Before methods.
        try {
            runBeforeMethodsIfAnyExist(testContext.testClass, instanceOfTestClass);
        } catch (Throwable e) {
            return TestResult.failed(testContext.testDescription, e);
        }

        // Initialize any @Rule fields.
        try {
            initializeAllDeclaredRuleFields(testContext.testClass, instanceOfTestClass);
        } catch (Throwable e) {
            return TestResult.failed(testContext.testDescription, e);
        }

        // Run the test.
        try {
            testMethod.invoke(instanceOfTestClass);
        } catch (IllegalAccessException | InvocationTargetException e) {

            // Only mark the test failed if the exception thrown was not what it expected to see.
            Class<? extends Throwable> expectedException = getExpectedException(testMethod);
            if ((e.getCause() != null) && (!e.getCause().getClass().equals(expectedException))) {
                return TestResult.failed(testContext.testDescription, e.getCause());
            }
        }

        // Run any @After methods.
        try {
            runAfterMethodsIfAnyExist(testContext.testClass, instanceOfTestClass);
        } catch (Throwable e) {
            return TestResult.failed(testContext.testDescription, e);
        }

        return TestResult.successful(testContext.testDescription);
    }

    private boolean isMethodIgnored(Method method) {
        for (Annotation annotation : method.getAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (annotationType.equals(org.junit.Ignore.class)) {
                return true;
            }
        }
        return false;
    }

    private void runBeforeMethodsIfAnyExist(Class<?> testClass, Object testClassInstance) {
        List<Method> beforeMethods = getBeforeMethods(testClass);

        for (Method beforeMethod : beforeMethods) {
            verifyBeforeMethodAnnotations(beforeMethod);
            try {
                beforeMethod.invoke(testClassInstance);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new UnexpectedTestRunnerException("Error invoking the @Before method: " + beforeMethod.getName(), e);
            }
        }
    }

    private List<Method> getBeforeMethods(Class<?> testClass) {
        List<Method> beforeMethods = new ArrayList<>();
        for (Method method : testClass.getMethods()) {
            if (method.isAnnotationPresent(org.junit.Before.class)) {
                beforeMethods.add(method);
            }
        }
        return beforeMethods;
    }

    private void verifyBeforeMethodAnnotations(Method beforeMethod) {
        for (Annotation annotation : beforeMethod.getAnnotations()) {
            if (!annotation.annotationType().equals(org.junit.Before.class)) {
                throw new UnsupportedAnnotation("This custom runner does not allow a @Before method to have any other annotations.");
            }
        }
    }

    private void initializeAllDeclaredRuleFields(Class<?> testClass, Object testClassInstance) {
        List<Field> ruleFields = getAnyRuleFieldsInTestClass(testClass);

        for (Field ruleField : ruleFields) {
            Class<?> ruleType = ruleField.getType();

            if (ruleType.equals(PreminedAccount.class)) {
                initializePreminedAccountRule(ruleField, testClassInstance);
            } else if (ruleType.equals(LocalNodeListener.class)) {
                initializeNodeListenerRule(ruleField, testClassInstance);
            } else if (ruleType.equals(PrepackagedLogEventsFactory.class)) {
                initializePrepackagdLogEventsFactory(ruleField, testClassInstance);
            } else {
                throw new UnsupportedAnnotation("This custom runner only supports the following @Rule's: PreminedAccount, LocalNodeListener. Found: " + ruleType);
            }
        }
    }

    private List<Field> getAnyRuleFieldsInTestClass(Class<?> testClass) {
        List<Field> ruleFields = new ArrayList<>();

        for (Field field : testClass.getDeclaredFields()) {
            for (Annotation annotation : field.getAnnotations()) {
                if (annotation.annotationType().equals(org.junit.Rule.class)) {
                    ruleFields.add(field);
                }
            }
        }
        return ruleFields;
    }

    private void initializePrepackagdLogEventsFactory(Field ruleField, Object testClassInstance) {
        try {
            // Grab the field instance and invoke the 'setKernel' method.
            ruleField.setAccessible(true);
            Object fieldInstance = ruleField.get(testClassInstance);
            Method setKernelMethod = fieldInstance.getClass().getDeclaredMethod("setKernel", NodeType.class);
            setKernelMethod.setAccessible(true);
            setKernelMethod.invoke(fieldInstance, nodeType);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new UnexpectedTestRunnerException("Failed initializing the PrepackedLogEventsFactory @Rule", e);
        }
    }

    private void initializePreminedAccountRule(Field ruleField, Object testClassInstance) {
        try {
            // Grab the field instance and invoke the 'setPrivateKey' method.
            ruleField.setAccessible(true);
            Object fieldInstance = ruleField.get(testClassInstance);
            Method setKeyMethod = fieldInstance.getClass().getDeclaredMethod("setPrivateKey");
            setKeyMethod.setAccessible(true);
            setKeyMethod.invoke(fieldInstance);

            // Transfer the funds from the real pre-mined account to the account we give the test.
            Method fundAccountMethod = fieldInstance.getClass().getDeclaredMethod("getFundsFromRealPreminedAccount", PreminedAccountFunder.class);
            fundAccountMethod.setAccessible(true);
            fundAccountMethod.invoke(fieldInstance, this.preminedDispatcher);

        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new UnexpectedTestRunnerException("Failed initializing the pre-mined account @Rule", e);
        }
    }

    private void initializeNodeListenerRule(Field ruleField, Object testClassInstance) {
        try {
            // Grab the field instance and invoke the 'setListener' method.
            ruleField.setAccessible(true);
            Object listener = ruleField.get(testClassInstance);
            Method setListenerMethod = listener.getClass().getDeclaredMethod("setListener", NodeListener.class);
            setListenerMethod.setAccessible(true);
            setListenerMethod.invoke(listener, this.nodeManagerForTests.newNodeListener());

        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new UnexpectedTestRunnerException("Failed initializing the node listener @Rule", e);
        }
    }

    private Class<? extends Throwable> getExpectedException(Method method) {
        Annotation annotation = method.getAnnotation(org.junit.Test.class);
        return ((org.junit.Test) annotation).expected();
    }

    private void runAfterMethodsIfAnyExist(Class<?> testClass, Object testClassInstance) {
        List<Method> afterMethods = getAfterMethods(testClass);

        for (Method afterMethod : afterMethods) {
            verifyAfterMethodAnnotations(afterMethod);
            try {
                afterMethod.invoke(testClassInstance);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new UnexpectedTestRunnerException("Error invoking the @After method: " + afterMethod.getName(), e);
            }
        }
    }

    private List<Method> getAfterMethods(Class<?> testClass) {
        List<Method> afterMethods = new ArrayList<>();
        for (Method method : testClass.getMethods()) {
            if (method.isAnnotationPresent(org.junit.After.class)) {
                afterMethods.add(method);
            }
        }
        return afterMethods;
    }

    private void verifyAfterMethodAnnotations(Method afterMethod) {
        for (Annotation annotation : afterMethod.getAnnotations()) {
            if (!annotation.annotationType().equals(org.junit.After.class)) {
                throw new UnsupportedAnnotation("This custom runner does not allow a @After method to have any other annotations.");
            }
        }
    }

    private static final String LINE_BREAK = "-----------------------------------------------------------------------------------------------";

    private void printTestStartBanner(TestContext testContext) {
        String currentTest = "CURRENT TEST [" + nodeType.name() + "]: " + testContext.testClass.getName() + "::" + testContext.testDescription.getMethodName();

        System.out.println(LINE_BREAK);
        System.out.println(currentTest);
        System.out.println();

        System.err.println(LINE_BREAK);
        System.err.println(currentTest);
        System.err.println();
    }

    private void printTestEndBanner() {
        System.out.println(LINE_BREAK);
        System.err.println(LINE_BREAK);
    }
}
