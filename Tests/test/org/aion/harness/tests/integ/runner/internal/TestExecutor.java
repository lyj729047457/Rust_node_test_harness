package org.aion.harness.tests.integ.runner.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.aion.harness.main.NodeListener;
import org.aion.harness.tests.integ.runner.exception.UnexpectedTestRunnerException;
import org.aion.harness.tests.integ.runner.exception.UnsupportedAnnotation;
import org.junit.runner.Description;

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
    private final PreminedAccountFunder preminedDispatcher;
    private final Map<Description, TestClassAndMethodReference> testDescriptionToInstanceMap = new HashMap<>();
    private final List<FutureExecutionResult> futureResults = new ArrayList<>();

    public TestExecutor(TestNodeManager nodeManager, PreminedAccountFunder preminedDispatcher, List<TestContext> testContexts) {
        this.nodeManagerForTests = nodeManager;
        this.preminedDispatcher = preminedDispatcher;

        // We initialize these TestInfo objects all at once here and set up the mapping to save time, there
        // are a few situations where different pieces of information are needed or different related pieces, and
        // this mapping helps us deal with that.
        try {
            this.testDescriptionToInstanceMap.putAll(produceDescriptionToInstanceMap(testContexts));
            this.futureResults.addAll(createFutures(this.testDescriptionToInstanceMap));
        } catch (Throwable e) {
            // This means we were unable to instantiate the test class or grab its test method -- mark all tests as failed.
            this.futureResults.addAll(createFuturesAllFailed(testContexts, e));
        }
    }

    @Override
    public void run() {
        for (FutureExecutionResult executionResult : this.futureResults) {
            runTestMethodAndFinishFuture(executionResult);
        }
    }

    /**
     * Returns a list of futures corresponding to all of the results this thread will be responsible
     * for running.
     */
    public List<FutureExecutionResult> getFutureExecutionResults() {
        return new ArrayList<>(this.futureResults);
    }

    /**
     * Runs the test method corresponding to the given future, and finishes that future once the
     * method has finished running, only if this future has not already been finished.
     *
     * If the future is already finished this method does nothing and returns immediately. The future
     * may be finished if the test is marked @Ignore or if a fatal exception occurred initializing
     * the test class or grabbing the test method reference, for example.
     */
    private void runTestMethodAndFinishFuture(FutureExecutionResult futureExecutionResult) {
        if (!futureExecutionResult.resultIsFinished()) {
            // Grab the unique test class instance and method to be run for this test.
            TestClassAndMethodReference testInstance = this.testDescriptionToInstanceMap.get(futureExecutionResult.testDescription);
            Object testClassInstance = testInstance.instanceOfTestClass;
            Method testMethod = testInstance.instanceOfTestMethod;

            try {
                // 2. Run any @Before methods.
                runBeforeMethodsIfAnyExist(testInstance.testClass, testClassInstance);

                // 3. Initialize any @Rule fields.
                initializeAllDeclaredRuleFields(testInstance.testClass, testClassInstance);

                // 4. Run the test method.
                testMethod.invoke(testClassInstance);

            } catch (Throwable e) {

                // Only mark the test failed if the exception thrown was not what it expected to see.
                Class<? extends Throwable> expectedException = getExpectedException(testMethod);
                if ((e.getCause() != null) && (!e.getCause().getClass().equals(expectedException))) {

                    futureExecutionResult.markAsFailed(e.getCause());
                }

            } finally {
                // Ensure that any @After methods get run.
                try {
                    runAfterMethodsIfAnyExist(testInstance.testClass, testClassInstance);
                } catch (Throwable e) {
                    // Only notify of a failure in the @After methods if we haven't already seen a failure yet.
                    // Otherwise we might overwrite the original source of the error.
                    if (!futureExecutionResult.resultIsFinished()) {
                        futureExecutionResult.markAsFailed(e);
                    }
                }
            }

            // If the future has not been finished yet then there were no errors encountered, it was a success.
            if (!futureExecutionResult.resultIsFinished()) {
                futureExecutionResult.markAsSuccess();
            }
        }
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

    private Map<Description, TestClassAndMethodReference> produceDescriptionToInstanceMap(List<TestContext> testContexts) throws NoSuchMethodException, InstantiationException, InvocationTargetException, IllegalAccessException {
        Map<Description, TestClassAndMethodReference> descriptionToInfoMap = new HashMap<>();
        for (TestContext testContext : testContexts) {
            Object instanceOfTestClass = testContext.testClass.getConstructor().newInstance();
            Method testMethod = testContext.testClass.getDeclaredMethod(testContext.testDescription.getMethodName());

            descriptionToInfoMap.put(testContext.testDescription, new TestClassAndMethodReference(testContext.testClass, instanceOfTestClass, testMethod));
        }
        return descriptionToInfoMap;
    }

    private List<FutureExecutionResult> createFutures(Map<Description, TestClassAndMethodReference> testDescriptionToInfoMap) {
        List<FutureExecutionResult> futures = new ArrayList<>();
        for (Description testDescription : testDescriptionToInfoMap.keySet()) {

            if (isMethodIgnored(testDescriptionToInfoMap.get(testDescription).instanceOfTestMethod)) {
                futures.add(new FutureExecutionResult(testDescription, true));
            } else {
                futures.add(new FutureExecutionResult(testDescription, false));
            }

        }
        return futures;
    }

    private List<FutureExecutionResult> createFuturesAllFailed(List<TestContext> testContexts, Throwable error) {
        List<FutureExecutionResult> futures = new ArrayList<>();
        for (TestContext testContext : testContexts) {
            FutureExecutionResult futureExecutionResult = new FutureExecutionResult(testContext.testDescription, false);
            futureExecutionResult.markAsFailed(error);
            futures.add(futureExecutionResult);
        }
        return futures;
    }
}
