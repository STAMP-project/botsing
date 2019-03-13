package eu.stamp.botsing.model.generation.testcase.execution;

import eu.stamp.botsing.commons.BotsingTestGenerationContext;
import org.evosuite.Properties;
import org.evosuite.ga.stoppingconditions.MaxTestsStoppingCondition;
import org.evosuite.runtime.Runtime;
import org.evosuite.runtime.sandbox.PermissionStatistics;
import org.evosuite.runtime.sandbox.Sandbox;
import org.evosuite.runtime.util.JOptionPaneInputs;
import org.evosuite.runtime.util.SystemInUtil;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.execution.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class TestExecutor implements ThreadFactory {
    private static final Logger LOG = LoggerFactory.getLogger(TestExecutor.class);

    public static final String BOTSING_TEST_EXECUTION_THREAD_GROUP = "Botsing_Test_Execution_Group";
    public static final String BOTSING_TEST_EXECUTION_THREAD = "BOTSING_TEST_EXECUTION_THREAD";

    private Thread currentThread;

    private final Set<Thread> stalledThreads =  new HashSet<Thread>();
    private ThreadGroup threadGroup = null;

    private Set<ExecutionObserver> observers;

    private ExecutorService executor;

    private static TestExecutor instance = null;

    public volatile int threadCounter;

    private TestExecutor() {
        executor = Executors.newSingleThreadExecutor(this);
        newObservers();
    }

    public static synchronized TestExecutor getInstance() {
        if (instance == null){
            instance = new TestExecutor();
        }

        return instance;
    }


    public ExecutionResult execute(TestCase tc, int timeout) {
        Scope scope = new Scope();
        ExecutionResult result = execute(tc, scope, timeout);
        return result;
    }

    private ExecutionResult execute(TestCase tc, Scope scope, int timeout){
        ExecutionTracer.getExecutionTracer().clear();
        resetObservers();
        ExecutionObserver.setCurrentTest(tc);
        MaxTestsStoppingCondition.testExecuted();
        Runtime.getInstance().resetRuntime();

        TestRunnable callableTest = new TestRunnable(tc, scope, observers);
        callableTest.storeCurrentThreads();

        TimeoutHandler<ExecutionResult> handler = new TimeoutHandler<ExecutionResult>();
        try {
            ExecutionResult result = null;
            SystemInUtil.getInstance().initForTestCase();
            JOptionPaneInputs.getInstance().initForTestCase();

            Sandbox.goingToExecuteSUTCode();
            BotsingTestGenerationContext.getInstance().goingToExecuteSUTCode();
            try {
                result = handler.execute(callableTest, executor, timeout, Properties.CPU_TIMEOUT);
            } finally {
                Sandbox.doneWithExecutingSUTCode();
                BotsingTestGenerationContext.getInstance().doneWithExecutingSUTCode();
            }

            return result;

        }catch (Exception e){
            LOG.warn("Exception during executing the generated tests for class loading");
            return null;
            }
        }



    private void resetObservers() {
        for (ExecutionObserver observer : observers) {
            observer.clear();
        }
    }

    @Override
    public Thread newThread(Runnable r) {
        if (currentThread != null && currentThread.isAlive()) {
            currentThread.setPriority(Thread.MIN_PRIORITY);
            stalledThreads.add(currentThread);
            updateStalledThreads();
            LOG.info("Number of stalled threads: " + stalledThreads.size());
        } else {
            LOG.info("Number of stalled threads: " + 0);
        }

        if (threadGroup != null) {
            PermissionStatistics.getInstance().countThreads(threadGroup.activeCount());
        }
            threadGroup = new ThreadGroup(BOTSING_TEST_EXECUTION_THREAD_GROUP);
            currentThread = new Thread(threadGroup, r);
            currentThread.setName(BOTSING_TEST_EXECUTION_THREAD + "_" + threadCounter);
            threadCounter++;
            currentThread.setContextClassLoader(BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
            ExecutionTracer.setThread(currentThread);
            return currentThread;

    }

    private void updateStalledThreads(){
        Iterator<Thread> iterator = stalledThreads.iterator();
        while (iterator.hasNext()) {
            Thread currentThread = iterator.next();
            if(!currentThread.isAlive()){
                iterator.remove();
            }
        }
    }


    public void newObservers() {
        observers = new LinkedHashSet<>();
    }
}
