package eu.stamp.botsing.coverage.io.input;

import eu.stamp.botsing.coverage.io.IOCoverageUtility;
import org.evosuite.coverage.io.input.InputCoverageTestFitness;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.evosuite.shaded.org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class InputCoverageFactoryTest {
    @Spy
    private IOCoverageUtility utility;

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

        assert(goals.size() == 6);
        // 3 goals for <init> and 3 goals for equals
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
        assert (equalCount == 3);
    }


    @Test
    public void testDetectGoals_boolean(){
        Type type = Type.getType(boolean.class);
        List<InputCoverageTestFitness> goals = new ArrayList<InputCoverageTestFitness>();
        inputCoverageFactory.detectGoals("ClassA","method1",new Class[]{boolean.class},new Type[]{type},goals);
        assert (goals.size() == 2);
    }


    @Test
    public void testDetectGoals_char(){
        Type type = Type.getType(char.class);
        List<InputCoverageTestFitness> goals = new ArrayList<InputCoverageTestFitness>();
        inputCoverageFactory.detectGoals("ClassA","method1",new Class[]{char.class},new Type[]{type},goals);
        assert (goals.size() == 3);
    }


    @Test
    public void testDetectGoals_array(){
        String[] arr = new String[2];
        Type type = Type.getType(arr.getClass());
        List<InputCoverageTestFitness> goals = new ArrayList<InputCoverageTestFitness>();
        inputCoverageFactory.detectGoals("ClassA","method1",new Class[]{arr.getClass()},new Type[]{type},goals);
        assert (goals.size() == 3);
    }

    @Test
    public void testDetectGoals_strings(){
        Type type = Type.getType(String.class);
        List<InputCoverageTestFitness> goals = new ArrayList<InputCoverageTestFitness>();
        inputCoverageFactory.detectGoals("ClassA","method1",new Class[]{String.class},new Type[]{type},goals);
        assert (goals.size() == 3);
    }


    @Test
    public void testDetectGoals_list(){
        Type type = Type.getType(List.class);
        List<InputCoverageTestFitness> goals = new ArrayList<InputCoverageTestFitness>();
        inputCoverageFactory.detectGoals("ClassA","method1",new Class[]{List.class},new Type[]{type},goals);
        assert (goals.size() == 3);
    }

    @Test
    public void testDetectGoals_set(){
        Type type = Type.getType(Set.class);
        List<InputCoverageTestFitness> goals = new ArrayList<InputCoverageTestFitness>();
        inputCoverageFactory.detectGoals("ClassA","method1",new Class[]{Set.class},new Type[]{type},goals);
        assert (goals.size() == 3);
    }

    @Test
    public void testDetectGoals_map(){
        Type type = Type.getType(Map.class);
        List<InputCoverageTestFitness> goals = new ArrayList<InputCoverageTestFitness>();
        inputCoverageFactory.detectGoals("ClassA","method1",new Class[]{Map.class},new Type[]{type},goals);
        assert (goals.size() == 3);
    }


    @Test
    public void testDetectGoals_object(){
        Type type = Type.getType(Method.class);
        List<InputCoverageTestFitness> goals = new ArrayList<InputCoverageTestFitness>();
        inputCoverageFactory.detectGoals("ClassA","method1",new Class[]{Method.class},new Type[]{type},goals);
        assert (goals.size() == 12);
    }
}
