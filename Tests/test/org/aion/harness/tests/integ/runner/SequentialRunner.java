package org.aion.harness.tests.integ.runner;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.aion.harness.main.NodeFactory.NodeType;
import org.aion.harness.tests.integ.runner.exception.TestRunnerInitializationException;
import org.aion.harness.tests.integ.runner.exception.UnsupportedAnnotation;
import org.aion.harness.tests.integ.runner.internal.FutureExecutionResult;
import org.aion.harness.tests.integ.runner.internal.PreminedAccountFunder;
import org.aion.harness.tests.integ.runner.internal.TestContext;
import org.aion.harness.tests.integ.runner.internal.TestExecutor;
import org.aion.harness.tests.integ.runner.internal.TestNodeManager;
import org.apache.commons.io.FileUtils;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

public final class SequentialRunner extends Runner {
    private final Class<?> testClass;
    private final Description testClassDescription;
    private final TestNodeManager nodeManagerForTests;
    private final PreminedAccountFunder preminedAccountFunder;

    // The type of node that this runner will start up.
    private NodeType nodeType = NodeType.JAVA_NODE;

    public SequentialRunner(Class<?> testClass) {
        this.testClass = testClass;
        this.testClassDescription = deriveTestDescription(testClass);
        this.nodeManagerForTests = new TestNodeManager();
        this.preminedAccountFunder = new PreminedAccountFunder(this.nodeManagerForTests);
    }

    @Override
    public Description getDescription() {
        return this.testClassDescription;
    }

    @Override
    public void run(RunNotifier runNotifier) {
        // This is the method JUnit invokes to run all of the tests in our test class.

        // Verify the test class itself is only using annotations this runner knows how to handle.
        verifyTestClassAnnotations();
        verifyMethodAndFieldAnnotations();

        if (isTestClassIgnored()) {
            // This class is marked Ignore so we mark all tests ignored and finish up right here.
            for (Description testDescription : this.testClassDescription.getChildren()) {
                runNotifier.fireTestIgnored(testDescription);
            }
        } else {
            // Start up the local node before any tests are run.
            initializeAndStartNode();

            boolean beforeClassFailed = true;
            try {
                // If the class has a @BeforeClass method then run it now.
                runBeforeClassMethodsIfAnyExists();
                beforeClassFailed = false;

                // Run all of the test methods now. No exceptions will be thrown here.
                runTests(runNotifier);

                // If the class has a @AfterClass method then run it now.
                runAfterClassMethodsIfAnyExists();

            } catch (Throwable e) {

                // Any exceptions that make it here occurred in the @BeforeClass or @AfterClass blocks.
                if (beforeClassFailed) {
                    for (Description description : this.testClassDescription.getChildren()) {
                        runNotifier.fireTestStarted(description);
                        runNotifier.fireTestFailure(new Failure(description, e));
                        runNotifier.fireTestFinished(description);
                    }
                } else {
                    // Document the afterClass error as if the test class itself failed, so we can show the error.
                    runNotifier.fireTestStarted(this.testClassDescription);
                    runNotifier.fireTestFailure(new Failure(this.testClassDescription, e));
                    runNotifier.fireTestFinished(this.testClassDescription);
                }

            } finally {
                // Ensure that the node gets shut down properly.
                stopNode();

                // Delete the log files unless specified not to.
                if (System.getProperty("skipCleanLogs") == null) {
                    try {
                        FileUtils.deleteDirectory(new File(System.getProperty("user.dir") + "/logs"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void runTests(RunNotifier runNotifier) {
        List<TestContext> descriptions = new ArrayList<>();
        for (Description testDescription : this.testClassDescription.getChildren()) {
            descriptions.add(new TestContext(this.testClass, testDescription));
        }

        // Run all of the tests on a single thread.
        TestExecutor testExecutor = new TestExecutor(this.nodeManagerForTests, this.preminedAccountFunder, descriptions);
        Thread executorThread = new Thread(testExecutor);
        executorThread.start();

        try {
            for (FutureExecutionResult future : testExecutor.getFutureExecutionResults()) {
                if (future.ignored) {
                    runNotifier.fireTestIgnored(future.testDescription);
                } else {

                    runNotifier.fireTestStarted(future.testDescription);

                    // Block until the future is ready.
                    future.waitUntilFinished();
                    if (!future.wasSuccessful()) {
                        runNotifier.fireTestFailure(new Failure(future.testDescription, future.getTestError()));
                    }

                    runNotifier.fireTestFinished(future.testDescription);
                }
            }

            // Wait for the thread to finish now that we've collected all of its results.
            executorThread.join();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean isTestClassIgnored() {
        for (Annotation annotation : this.testClass.getAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (annotationType.equals(org.junit.Ignore.class)) {
                return true;
            }
        }
        return false;
    }

    private void initializeAndStartNode() {
        try {
            this.nodeManagerForTests.startLocalNode(this.nodeType);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new TestRunnerInitializationException("Error setting up the local node", e);
        }
    }

    private void runAfterClassMethodsIfAnyExists() {
        List<Method> afterClassMethods = getAfterClassMethods();

        for (Method afterClassMethod : afterClassMethods) {
            verifyAfterClassMethodAnnotations(afterClassMethod);
            try {
                afterClassMethod.invoke(null);
            } catch (Throwable e) {
                throw new TestRunnerInitializationException("Error encountered in the @AfterClass method: " + afterClassMethod.getName(), e);
            }
        }
    }

    private void runBeforeClassMethodsIfAnyExists() {
        List<Method> beforeClassMethods = getBeforeClassMethods();

        for (Method beforeClassMethod : beforeClassMethods) {
            verifyBeforeClassMethodAnnotations(beforeClassMethod);
            try {
                beforeClassMethod.invoke(null);
            } catch (Throwable e) {
                throw new TestRunnerInitializationException("Error encountered in the @BeforeClass method: " + beforeClassMethod.getName(), e);
            }
        }
    }

    private void stopNode() {
        try {
            this.nodeManagerForTests.shutdownLocalNode();
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new TestRunnerInitializationException("Error shutting down the local node", e);
        }
    }

    /**
     * This is the description of all the test methods in the test class. JUnit uses the description
     * to display info in your IDE etc. about which test is which.
     */
    private Description deriveTestDescription(Class<?> testClass) {
        Description description = Description.createTestDescription(testClass, testClass.getName());
        for (Method testMethod : testClass.getMethods()) {
            if (testMethod.isAnnotationPresent(org.junit.Test.class)) {
                description.addChild(Description.createTestDescription(testClass, testMethod.getName()));
            }
        }
        return description;
    }

    private List<Method> getAfterClassMethods() {
        List<Method> afterClassMethods = new ArrayList<>();
        for (Method method : this.testClass.getMethods()) {
            if (method.isAnnotationPresent(org.junit.AfterClass.class)) {
                afterClassMethods.add(method);
            }
        }
        return afterClassMethods;
    }

    private List<Method> getBeforeClassMethods() {
        List<Method> beforeClassMethods = new ArrayList<>();
        for (Method method : this.testClass.getMethods()) {
            if (method.isAnnotationPresent(org.junit.BeforeClass.class)) {
                beforeClassMethods.add(method);
            }
        }
        return beforeClassMethods;
    }

    private void verifyBeforeClassMethodAnnotations(Method beforeClassMethod) {
        for (Annotation annotation : beforeClassMethod.getAnnotations()) {
            if (!annotation.annotationType().equals(org.junit.BeforeClass.class)) {
                throw new UnsupportedAnnotation("This custom runner does not allow a @BeforeClass method to have any other annotations.");
            }
        }
    }

    private void verifyAfterClassMethodAnnotations(Method afterClassMethod) {
        for (Annotation annotation : afterClassMethod.getAnnotations()) {
            if (!annotation.annotationType().equals(org.junit.AfterClass.class)) {
                throw new UnsupportedAnnotation("This custom runner does not allow a @AfterClass method to have any other annotations.");
            }
        }
    }

    private void verifyTestClassAnnotations() {
        for (Annotation annotation : this.testClass.getAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (!annotationType.equals(org.junit.Ignore.class) && !annotationType.equals(org.junit.runner.RunWith.class)) {
                throw new UnsupportedAnnotation("This custom runner does not support the annotation: " + annotation);
            }
        }
    }

    private void verifyMethodAndFieldAnnotations() {
        for (Method method : this.testClass.getDeclaredMethods()) {
            for (Annotation annotation : method.getAnnotations()) {
                Class<? extends Annotation> annotationType = annotation.annotationType();
                if (!annotationType.equals(org.junit.Test.class) && !annotationType.equals(org.junit.Before.class)
                    && !annotationType.equals(org.junit.After.class) && !annotationType.equals(org.junit.BeforeClass.class)
                    && !annotationType.equals(org.junit.AfterClass.class) && !annotationType.equals(org.junit.Ignore.class)) {
                    throw new UnsupportedAnnotation("This custom runner only supports the following method annotations:"
                        + " Ignore, Test, Before, After, BeforeClass, AfterClass. Found: " + annotation);
                }
            }
        }
        for (Field field : this.testClass.getDeclaredFields()) {
            for (Annotation annotation : field.getAnnotations()) {
                Class<? extends Annotation> annotationType = annotation.annotationType();
                if (!annotationType.equals(org.junit.Rule.class)) {
                    throw new UnsupportedAnnotation("This custom runner only supports the following field annotations: Rule. Found: " + annotation);
                }
            }
        }
    }
}
