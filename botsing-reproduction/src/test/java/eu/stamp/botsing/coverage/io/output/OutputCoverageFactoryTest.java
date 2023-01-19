package eu.stamp.botsing.coverage.io.output;

import eu.stamp.botsing.coverage.io.IOCoverageUtility;
import org.evosuite.coverage.io.output.OutputCoverageTestFitness;
import org.junit.Before;
import org.junit.Test;
import org.evosuite.shaded.org.mockito.Mockito;


import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

//@RunWith(MockitoJUnitRunner.class)
public class OutputCoverageFactoryTest {
//    @Spy
    private IOCoverageUtility utility;

    OutputCoverageFactory outputCoverageFactory;
    @Before
    public void prepareOutPutCoverageFactory(){
        utility = Mockito.spy(new IOCoverageUtility());
        outputCoverageFactory =  new OutputCoverageFactory(utility);
    }

    @Test
    public void testWithRegularClass() throws NoSuchMethodException {


        // Prepare methods
        Method method1 = Integer.class.getMethod("equals",Object.class);
        List<Method> methods = new ArrayList<>();
        methods.add(method1);
        Mockito.when(utility.getStackTraceMethods()).thenReturn(methods);

        List<OutputCoverageTestFitness> goals = outputCoverageFactory.getCoverageGoals();
        // Two goals: True False
        assert(goals.size() == 2);

    }


    @Test
    public void testDetectGoals_char() throws NoSuchMethodException {
        Method method = String.class.getMethod("charAt",int.class);
        List<OutputCoverageTestFitness> goals = new ArrayList<>();
        outputCoverageFactory.detectGoals(method,goals);
        assert (goals.size() == 3);
    }


    @Test
    public void testDetectGoals_double() throws NoSuchMethodException {
        Method method = Double.class.getMethod("parseDouble",String.class);
        List<OutputCoverageTestFitness> goals = new ArrayList<>();
        outputCoverageFactory.detectGoals(method,goals);
        assert (goals.size() == 3);
    }



    @Test
    public void testDetectGoals_array() throws NoSuchMethodException {
        Method method = ArrayList.class.getMethod("toArray");
        List<OutputCoverageTestFitness> goals = new ArrayList<>();
        outputCoverageFactory.detectGoals(method,goals);
        assert (goals.size() == 3);
    }




    @Test
    public void testDetectGoals_string() throws NoSuchMethodException {
        Method method = String.class.getMethod("toString");
        List<OutputCoverageTestFitness> goals = new ArrayList<>();
        outputCoverageFactory.detectGoals(method,goals);
        assert (goals.size() == 3);
    }



    @Test
    public void testDetectGoals_object() throws NoSuchMethodException {
        Method method = Class.class.getMethod("getEnclosingMethod");
        List<OutputCoverageTestFitness> goals = new ArrayList<>();
        outputCoverageFactory.detectGoals(method,goals);
        assert (goals.size() == 12);
    }
//
//    @Test
//    public void testDetectGoals_list(){
//        Type type = Type.getType(List.class);
//        List<OutputCoverageTestFitness> goals = new ArrayList<OutputCoverageTestFitness>();
//        outputCoverageFactory.detectGoals("ClassA","method1",new Class[]{List.class},new Type[]{type},goals);
//        assert (goals.size() == 3);
//    }
//
//    @Test
//    public void testDetectGoals_set(){
//        Type type = Type.getType(Set.class);
//        List<OutputCoverageTestFitness> goals = new ArrayList<OutputCoverageTestFitness>();
//        outputCoverageFactory.detectGoals("ClassA","method1",new Class[]{Set.class},new Type[]{type},goals);
//        assert (goals.size() == 3);
//    }
//
//    @Test
//    public void testDetectGoals_map(){
//        Type type = Type.getType(Map.class);
//        List<OutputCoverageTestFitness> goals = new ArrayList<OutputCoverageTestFitness>();
//        outputCoverageFactory.detectGoals("ClassA","method1",new Class[]{Map.class},new Type[]{type},goals);
//        assert (goals.size() == 3);
//    }
//
//
//    @Test
//    public void testDetectGoals_object(){
//        Type type = Type.getType(Method.class);
//        List<OutputCoverageTestFitness> goals = new ArrayList<OutputCoverageTestFitness>();
//        outputCoverageFactory.detectGoals("ClassA","method1",new Class[]{Method.class},new Type[]{type},goals);
//        assert (goals.size() == 12);
//    }
}
