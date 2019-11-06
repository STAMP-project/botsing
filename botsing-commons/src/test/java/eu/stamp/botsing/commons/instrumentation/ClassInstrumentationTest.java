package eu.stamp.botsing.commons.instrumentation;

import org.evosuite.classpath.ClassPathHandler;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import com.google.common.util.concurrent.Callables;

import static org.junit.Assert.assertEquals;

public class ClassInstrumentationTest {
    List<String> interestingClasses = new ArrayList<>();
    ClassInstrumentation instrumentation = new ClassInstrumentation();
    @Test
    public void testOneClass()  {
        interestingClasses.clear();
        String className = Integer.class.getName();
        interestingClasses.add(className);
        interestingClasses.add(className);

        List<Class> instrumentedClasses = instrumentation.instrumentClasses(interestingClasses,className);
        assert (instrumentedClasses.size() == 1);
        assertEquals(instrumentedClasses.get(0),Integer.class);
    }

    @Test
    public void testUnknownClass(){
        interestingClasses.clear();
        interestingClasses.add("unknown.class.Unknown");
        List<Class> instrumentedClasses = instrumentation.instrumentClasses(interestingClasses,"unknown.class.Unknown");
        assert (instrumentedClasses.size() == 0);
    }


    @Test
    public void testMultipleClasses()  {
        interestingClasses.clear();
        String className = Integer.class.getName();
        interestingClasses.add(className);
        className = String.class.getName();
        interestingClasses.add(className);

        List<Class> instrumentedClasses = instrumentation.instrumentClasses(interestingClasses,className);
        assert (instrumentedClasses.size() == 2);
        assertEquals(instrumentedClasses.get(0),Integer.class);
        assertEquals(instrumentedClasses.get(1),String.class);
    }


    @Test
    public void Instrumentable(){
        interestingClasses.clear();
        interestingClasses.add(Callables.class.getName());
        ClassPathHandler.getInstance().changeTargetCPtoTheSameAsEvoSuite();
        List<Class> instrumentedClasses = instrumentation.instrumentClasses(interestingClasses,Callables.class.getName());
    }
}
