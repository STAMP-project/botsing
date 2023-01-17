package eu.stamp.botsing.commons;

import org.evosuite.Properties;
import org.evosuite.coverage.TestFitnessFactory;
import org.evosuite.result.TestGenerationResult;
import org.evosuite.setup.TestCluster;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.factories.AllMethodsTestChromosomeFactory;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.utils.generic.GenericClass;
import org.evosuite.utils.generic.GenericMethod;
import org.junit.Before;
import org.junit.Test;
import org.evosuite.shaded.org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.ArrayList;

import static eu.stamp.botsing.commons.PostProcessUtility.writeJUnitFailingTests;
import static eu.stamp.botsing.commons.PostProcessUtility.writeJUnitTestsAndCreateResult;


public class PostProcessUtilityTest {

    private TestSuiteChromosome testSuite;

    @Before
    public void generateTestSuite() throws NoSuchMethodException {
        Object obj = new String();
        GenericClass gc = Mockito.mock(GenericClass.class);
        Mockito.when(gc.hasWildcardOrTypeVariables()).thenReturn(false);
        Method m = obj.getClass().getMethod("equals", Object.class);
        GenericMethod call = Mockito.mock(GenericMethod.class,Mockito.RETURNS_DEEP_STUBS);
        Mockito.doReturn(obj.getClass()).when(call).getDeclaringClass();
        Mockito.when(call.getName()).thenReturn("equals");
        Mockito.when(call.getOwnerClass()).thenReturn(gc);
        Mockito.when(call.isMethod()).thenReturn(true);
        Mockito.when(call.getOwnerType()).thenReturn(String.class);
        Mockito.when(call.getMethod()).thenReturn(m);
        Mockito.when(call.getParameterTypes()).thenReturn(m.getParameterTypes());
        Mockito.when(call.getReturnType()).thenReturn(Boolean.TYPE);
        Mockito.when(call.isPublic()).thenReturn(true);
        Mockito.doReturn(boolean.class).when(call).getRawGeneratedType();


        Mockito.when(call.copy()).thenReturn(call);
        TestCluster.getInstance().addTestCall(call);

        AllMethodsTestChromosomeFactory testChromosomeFactory = new AllMethodsTestChromosomeFactory();
        TestChromosome test =testChromosomeFactory.getChromosome();
        testSuite = new TestSuiteChromosome();
        testSuite.addTest(test);
    }

    @Test
    public void testWithoutSearchObjective(){
        TestFitnessFactory testFitnessFactory = Mockito.mock(TestFitnessFactory.class);
        ArrayList testFitnessFactoryList = new ArrayList();
        testFitnessFactoryList.add(testFitnessFactory);
        PostProcessUtility.postProcessTests(testSuite, testFitnessFactoryList,false);
        // Minimization suppose to remove the test because there is no search objective
        assert(testSuite.getTests().size() == 0);
    }

    @Test
    public void testWithSearchObjective(){
        TestFitnessFunction testFF = Mockito.mock(TestFitnessFunction.class);
        Mockito.when(testFF.isCovered(testSuite.getTestChromosome(0))).thenReturn(true);
        ArrayList<TestFitnessFunction> coverageGoals =  new ArrayList<>();
        coverageGoals.add(testFF);

        Properties.MINIMIZE = false;
        TestFitnessFactory testFitnessFactory = Mockito.mock(TestFitnessFactory.class);
        Mockito.when(testFitnessFactory.getCoverageGoals()).thenReturn(coverageGoals);
        Mockito.when(testFitnessFactory.getCoverageGoals()).thenReturn(coverageGoals);
        ArrayList testFitnessFactoryList = new ArrayList();
        testFitnessFactoryList.add(testFitnessFactory);
        PostProcessUtility.postProcessTests(testSuite, testFitnessFactoryList,true);
        assert(testSuite.getTests().size() == 1);
    }

    @Test
    public void testWriteTest(){
        TestGenerationResult writingTest = writeJUnitTestsAndCreateResult(testSuite, Properties.JUNIT_SUFFIX);
        String interestingPart = writingTest.getTestSuiteCode().split("public void test0\\(\\)  throws Throwable  \\{\n")[1];
        interestingPart = interestingPart.replaceAll("      ","");
        assert (interestingPart.startsWith(testSuite.getTests().get(0).toCode()));

        writeJUnitFailingTests();

        Properties.CHECK_CONTRACTS = true;
        writeJUnitFailingTests();
    }
}
