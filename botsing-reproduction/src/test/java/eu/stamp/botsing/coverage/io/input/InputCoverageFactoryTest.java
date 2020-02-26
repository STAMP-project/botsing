package eu.stamp.botsing.coverage.io.input;

import eu.stamp.botsing.coverage.CoverageUtility;
import org.evosuite.coverage.io.input.InputCoverageTestFitness;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class InputCoverageFactoryTest {
    @Spy
    private CoverageUtility utility;

    @InjectMocks
    InputCoverageFactory inputCoverageFactory = new InputCoverageFactory();


    @Test
    public void testEmptyMethodAndConstructors(){
        Mockito.when(utility.getStackTraceMethods()).thenReturn(new ArrayList<>());
        Mockito.when(utility.getStackTraceConstructors()).thenReturn(new ArrayList<>());
        List<InputCoverageTestFitness> goals =inputCoverageFactory.getCoverageGoals();
        assert (goals.isEmpty());
    }


    @Test
    public void testWithRegularClass() throws NoSuchMethodException {

        // Prepare constructor
        Class cls[] = new Class[] { int.class };
        Constructor constructor = Integer.class.getConstructor(cls);
        List<Constructor> constructorsList = new ArrayList<>();
        constructorsList.add(constructor);
        Mockito.when(utility.getStackTraceConstructors()).thenReturn(constructorsList);

        // Prepare methods
        Method method1 = Integer.class.getMethod("equals",Object.class);
        List<Method> methods = new ArrayList<>();
        methods.add(method1);
        Mockito.when(utility.getStackTraceMethods()).thenReturn(methods);

        List<InputCoverageTestFitness> goals = inputCoverageFactory.getCoverageGoals();

        assert(goals.size() == 5);
        // 3 goals for <init> and 2 goals for equals
        int initCount = 0;
        int equalCount = 0;
        for (InputCoverageTestFitness goal : goals){
            if(goal.getMethod().contains("<init>")){
                initCount++;
            }else if(goal.getMethod().contains("equals")){
                equalCount++;
            }
        }

        assert (initCount == 3);
        assert (equalCount == 2);
    }


    @Test
    public void testDetectGoals_boolean(){
        List<InputCoverageTestFitness> goals = new ArrayList<>();
        inputCoverageFactory.detectGoals("ClassA","method1",new Class[]{boolean.class},goals);
        assert (goals.size() == 2);
    }


    @Test
    public void testDetectGoals_char(){
        List<InputCoverageTestFitness> goals = new ArrayList<>();
        inputCoverageFactory.detectGoals("ClassA","method1",new Class[]{char.class},goals);
        assert (goals.size() == 3);
    }


    @Test
    public void testDetectGoals_array(){
        String[] arr = new String[2];
        List<InputCoverageTestFitness> goals = new ArrayList<>();
        inputCoverageFactory.detectGoals("ClassA","method1",new Class[]{arr.getClass()},goals);
        assert (goals.size() == 3);
    }

    @Test
    public void testDetectGoals_strings(){
        List<InputCoverageTestFitness> goals = new ArrayList<>();
        inputCoverageFactory.detectGoals("ClassA","method1",new Class[]{String.class},goals);
        assert (goals.size() == 3);
    }


    @Test
    public void testDetectGoals_list(){
        List<InputCoverageTestFitness> goals = new ArrayList<>();
        inputCoverageFactory.detectGoals("ClassA","method1",new Class[]{List.class},goals);
        assert (goals.size() == 3);
    }

    @Test
    public void testDetectGoals_set(){
        List<InputCoverageTestFitness> goals = new ArrayList<>();
        inputCoverageFactory.detectGoals("ClassA","method1",new Class[]{Set.class},goals);
        assert (goals.size() == 3);
    }

    @Test
    public void testDetectGoals_map(){
        List<InputCoverageTestFitness> goals = new ArrayList<>();
        inputCoverageFactory.detectGoals("ClassA","method1",new Class[]{Map.class},goals);
        assert (goals.size() == 3);
    }


    @Test
    public void testDetectGoals_object(){
        List<InputCoverageTestFitness> goals = new ArrayList<>();
        inputCoverageFactory.detectGoals("ClassA","method1",new Class[]{Method.class},goals);
        assert (goals.size() == 12);
    }
}
