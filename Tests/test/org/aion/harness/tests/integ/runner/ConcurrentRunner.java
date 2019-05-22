package org.aion.harness.tests.integ.runner;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.aion.harness.main.NodeFactory.NodeType;
import org.aion.harness.tests.integ.runner.exception.TestRunnerInitializationException;
import org.aion.harness.tests.integ.runner.exception.UnexpectedTestRunnerException;
import org.aion.harness.tests.integ.runner.exception.UnsupportedAnnotation;
import org.aion.harness.tests.integ.runner.internal.FailedClass;
import org.aion.harness.tests.integ.runner.internal.TestExecutor;
import org.aion.harness.tests.integ.runner.internal.PreminedAccountFunder;
import org.aion.harness.tests.integ.runner.internal.TestAndResultQueueManager;
import org.aion.harness.tests.integ.runner.internal.TestContext;
import org.aion.harness.tests.integ.runner.internal.TestNodeManager;
import org.aion.harness.tests.integ.runner.internal.TestResult;
import org.apache.commons.io.FileUtils;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Suite.SuiteClasses;

public final class ConcurrentRunner extends Runner {
    // Maximum number of threads to be used to run the tests. Our max is high because our tests are IO-bound.
    private static final int MAX_NUM_THREADS = 50;

    private final Class<?> suiteClass;
    private final Description testSuiteDescription;
    private final TestNodeManager nodeManagerForTests;
    private final PreminedAccountFunder preminedAccountFunder;
    private final Map<Class<?>, Description> testClassesToDescriptions = new HashMap<>();

    // The type of node that this runner will start up.
    private NodeType nodeType = NodeType.JAVA_NODE;

    public ConcurrentRunner(Class<?> suiteClass) {
        this.suiteClass = suiteClass;
        this.testSuiteDescription = getFullSuiteDescriptionAndPopulateMap(suiteClass); // populates the map above
        this.nodeManagerForTests = new TestNodeManager();
        this.preminedAccountFunder = new PreminedAccountFunder(this.nodeManagerForTests);
    }

    @Override
    public Description getDescription() {
        return this.testSuiteDescription;
    }

    @Override
    public void run(RunNotifier runNotifier) {
        // This is the method JUnit invokes to run all of the tests in our test class.

        // Verify the test classes themselves are only using annotations this runner knows how to handle.
        verifyAnnotationsOfTestClasses();
        verifyAnnotationsOfAllMethodsAndFields();

        if (isTestSuiteIgnored()) {
            // Ignore every test in the whole suite.
            for (Description testClassDescription : this.testSuiteDescription.getChildren()) {
                for (Description testDescription : testClassDescription.getChildren()) {
                    runNotifier.fireTestIgnored(testDescription);
                }
            }
        } else {
            // Start up the local node before any tests are run.
            initializeAndStartNode();

            try {
                // Run every @BeforeClass method in any of the test classes.
                List<FailedClass> failedClasses = runAllBeforeClassMethodsAndReturnFailedClasses();

                // If any @BeforeClass methods failed, then every test in that class must be marked failed.
                notifyRunnerAllTestsInAllFailedClassesAreFailed(failedClasses, runNotifier);

                // Grab all of the test classes whose @BeforeClass methods did not fail, we will run these now.
                List<Class<?>> allNonFailedTests = getAllNonFailedTestClassesRemaining(failedClasses);

                // Run all of the test methods now. No exceptions will be thrown here.
                runAllTests(allNonFailedTests, runNotifier);

                // Run any @AfterClass method in any of the test classes.
                failedClasses = runAllAfterClassMethodsAndReturnFailedClasses(allNonFailedTests);

                // If any @AfterClass methods failed, then we mark the test class itself as failed in JUnit to display this information.
                notifyRunnerAfterClassMethodsFailed(failedClasses, runNotifier);

            } catch (Throwable e) {
                // We do not expect any exceptions to ever make it here. This is just to ensure our node
                // gets stopped in the finally block.
                e.printStackTrace();
                throw new UnexpectedTestRunnerException("Unexpected throwable!", e);
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

    /**
     * Grabs all of the N test methods from the given test classes and hands them out to the threads.
     * The minimum of (N, MAX_NUM_THREADS) threads will be used to run the tests.
     *
     * This method will wait on the results of all the tests and notify JUnit of their progress.
     *
     * When this method returns all tests will be done being processed.
     */
    private void runAllTests(List<Class<?>> testClasses, RunNotifier runNotifier) {
        List<TestContext> allTestContexts = getTestContextsForAllTests(testClasses);
        int numThreads = Math.min(MAX_NUM_THREADS, allTestContexts.size());

        TestAndResultQueueManager queueManager = new TestAndResultQueueManager(numThreads);

        List<TestExecutor> testExecutors = createTestExecutors(numThreads, queueManager);
        List<Thread> executorThreads = createExecutorThreads(testExecutors);

        // Start the threads.
        for (Thread executorThread : executorThreads) {
            executorThread.start();
        }

        // Dynamically dispatch all of the tests to the threads.
        for (TestContext testContext : allTestContexts) {
            queueManager.putTest(testContext);
            runNotifier.fireTestStarted(testContext.testDescription);
        }

        // Close the queue to notify threads that all tests have been submitted.
        queueManager.reportAllTestsSubmitted();

        // Collect the results of the tests and notify JUnit.
        TestResult result = queueManager.takeResult();
        while (result != null) {
            if (result.ignored) {
                runNotifier.fireTestIgnored(result.description);
            } else {
                if (!result.success) {
                    runNotifier.fireTestFailure(new Failure(result.description, result.error));
                }
                runNotifier.fireTestFinished(result.description);
            }
            result = queueManager.takeResult();
        }

        // Wait for all the threads to exit.
        for (Thread executorThread : executorThreads) {
            try {
                executorThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private List<TestExecutor> createTestExecutors(int num, TestAndResultQueueManager queueManager) {
        List<TestExecutor> threads = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            threads.add(new TestExecutor(this.nodeManagerForTests, this.preminedAccountFunder, queueManager));
        }
        return threads;
    }

    private List<Thread> createExecutorThreads(List<TestExecutor> testExecutors) {
        List<Thread> threads = new ArrayList<>();
        for (TestExecutor testExecutor : testExecutors) {
            threads.add(new Thread(testExecutor));
        }
        return threads;
    }

    private List<TestContext> getTestContextsForAllTests(List<Class<?>> testClasses) {
        List<TestContext> contexts = new ArrayList<>();
        for (Class<?> testClass : testClasses) {
            Description testClassDescription = this.testClassesToDescriptions.get(testClass);

            for (Description testDescription : testClassDescription.getChildren()) {
                contexts.add(new TestContext(testClass, testDescription));
            }
        }
        return contexts;
    }

    private List<Class<?>> getAllNonFailedTestClassesRemaining(List<FailedClass> failedClasses) {
        List<Class<?>> nonFailedTestClass = new ArrayList<>();
        nonFailedTestClass.addAll(this.testClassesToDescriptions.keySet());

        for (FailedClass failedClass : failedClasses) {
            nonFailedTestClass.remove(failedClass);
        }

        return nonFailedTestClass;
    }

    /**
     * Note that we do not want to mark the individual tests themselves in the failed class as failed,
     * it would be misleading, so we create a 'task' for the test class and mark it failed. This is just
     * so that JUnit passes this information along and we actually see that something went wrong.
     */
    private void notifyRunnerAfterClassMethodsFailed(List<FailedClass> failedClasses, RunNotifier runNotifier) {
        for (FailedClass failedClass : failedClasses) {
            Description testClassDescription = this.testClassesToDescriptions.get(failedClass.failedClass);
            runNotifier.fireTestStarted(testClassDescription);
            runNotifier.fireTestFailure(new Failure(testClassDescription, failedClass.failure));
        }
    }

    private void notifyRunnerAllTestsInAllFailedClassesAreFailed(List<FailedClass> failedClasses, RunNotifier runNotifier) {
        for (FailedClass failedClass : failedClasses) {
            notifyRunnerAllTestsInClassFailed(failedClass, runNotifier);
        }
    }

    private void notifyRunnerAllTestsInClassFailed(FailedClass failedClass, RunNotifier runNotifier) {
        Description testClassDescription = this.testClassesToDescriptions.get(failedClass.failedClass);
        for (Description testDescription : testClassDescription.getChildren()) {
            runNotifier.fireTestStarted(testDescription);
            runNotifier.fireTestFailure(new Failure(testDescription, failedClass.failure));
        }
    }

    private List<FailedClass> runAllBeforeClassMethodsAndReturnFailedClasses() {
        List<FailedClass> failedClasses = new ArrayList<>();

        for (Class<?> testClass : this.testClassesToDescriptions.keySet()) {
            try {
                runBeforeClassMethodsIfAnyExists(testClass);
            } catch (Throwable e) {
                failedClasses.add(new FailedClass(testClass, e));
            }
        }

        return failedClasses;
    }

    private void runBeforeClassMethodsIfAnyExists(Class<?> testClass) {
        List<Method> beforeClassMethods = getBeforeClassMethods(testClass);

        for (Method beforeClassMethod : beforeClassMethods) {
            verifyBeforeClassMethodAnnotations(beforeClassMethod);
            try {
                beforeClassMethod.invoke(null);
            } catch (Throwable e) {
                throw new TestRunnerInitializationException("Error encountered in the @BeforeClass method: " + beforeClassMethod.getName(), e);
            }
        }
    }

    private List<Method> getBeforeClassMethods(Class<?> testClass) {
        List<Method> beforeClassMethods = new ArrayList<>();
        for (Method method : testClass.getMethods()) {
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

    private List<FailedClass> runAllAfterClassMethodsAndReturnFailedClasses(List<Class<?>> testClasses) {
        List<FailedClass> failedClasses = new ArrayList<>();

        for (Class<?> testClass : testClasses) {
            try {
                runAfterClassMethodsIfAnyExists(testClass);
            } catch (Throwable e) {
                failedClasses.add(new FailedClass(testClass, e));
            }
        }

        return failedClasses;
    }

    private void runAfterClassMethodsIfAnyExists(Class<?> testClass) {
        List<Method> afterClassMethods = getAfterClassMethods(testClass);

        for (Method afterClassMethod : afterClassMethods) {
            verifyAfterClassMethodAnnotations(afterClassMethod);
            try {
                afterClassMethod.invoke(null);
            } catch (Throwable e) {
                throw new TestRunnerInitializationException("Error encountered in the @AfterClass method: " + afterClassMethod.getName(), e);
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

    private List<Method> getAfterClassMethods(Class<?> testClass) {
        List<Method> afterClassMethods = new ArrayList<>();
        for (Method method : testClass.getMethods()) {
            if (method.isAnnotationPresent(org.junit.AfterClass.class)) {
                afterClassMethods.add(method);
            }
        }
        return afterClassMethods;
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
     * This is the description of the full test suite used by your IDE etc.
     */
    private Description getFullSuiteDescriptionAndPopulateMap(Class<?> suiteClass) {
        Description suiteDescription = Description.createSuiteDescription(suiteClass);

        List<Class<?>> testClasses = getSuiteTestClasses(suiteClass);
        for (Class<?> testClass : testClasses) {
            Description description = deriveTestDescription(testClass);
            suiteDescription.addChild(description);
            this.testClassesToDescriptions.put(testClass, description);
        }

        return suiteDescription;
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

    private static List<Class<?>> getSuiteTestClasses(Class<?> suiteClass) {
        List<Class<?>> classes = new ArrayList<>();

        Annotation annotation = suiteClass.getAnnotation(org.junit.runners.Suite.SuiteClasses.class);
        for (Class<?> klazz : ((SuiteClasses) annotation).value()) {
            classes.add(klazz);
        }

        return classes;
    }

    private void verifyAnnotationsOfTestClasses() {
        for (Class<?> testClass : this.testClassesToDescriptions.keySet()) {
            verifyTestClassAnnotations(testClass);
        }
    }

    private void verifyTestClassAnnotations(Class<?> testClass) {
        for (Annotation annotation : testClass.getAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (!annotationType.equals(org.junit.Ignore.class) && !annotationType.equals(org.junit.runner.RunWith.class)) {
                throw new UnsupportedAnnotation("This custom runner does not support the annotation: " + annotation);
            }
        }
    }

    private void verifyAnnotationsOfAllMethodsAndFields() {
        for (Class<?> testClass : this.testClassesToDescriptions.keySet()) {
            verifyMethodAndFieldAnnotations(testClass);
        }
    }

    private void verifyMethodAndFieldAnnotations(Class<?> testClass) {
        for (Method method : testClass.getDeclaredMethods()) {
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
        for (Field field : testClass.getDeclaredFields()) {
            for (Annotation annotation : field.getAnnotations()) {
                Class<? extends Annotation> annotationType = annotation.annotationType();
                if (!annotationType.equals(org.junit.Rule.class)) {
                    throw new UnsupportedAnnotation("This custom runner only supports the following field annotations: Rule. Found: " + annotation);
                }
            }
        }
    }

    private boolean isTestSuiteIgnored() {
        for (Annotation annotation : this.suiteClass.getAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (annotationType.equals(org.junit.Ignore.class)) {
                return true;
            }
        }
        return false;
    }
}
