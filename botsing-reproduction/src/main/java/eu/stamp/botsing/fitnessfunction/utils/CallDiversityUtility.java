package eu.stamp.botsing.fitnessfunction.utils;

import org.evosuite.testcase.statements.ConstructorStatement;
import org.evosuite.testcase.statements.MethodStatement;
import org.evosuite.testcase.statements.Statement;

public class CallDiversityUtility {

    public static boolean isInteresting(Statement statement, String targetClass){
        boolean interesting = false;
        // In this objective, we only care about calls to methods and constructors
        if(statement instanceof MethodStatement){
            MethodStatement currentMethodStatement = (MethodStatement) statement;
            // We also care only about the method calls to the target class
            if(currentMethodStatement.getDeclaringClassName().equals(targetClass)){
                interesting=true;
            }
        }else if( statement instanceof ConstructorStatement){
            ConstructorStatement currentConstructorStatement = (ConstructorStatement) statement;
            // We also care only about the constructors of the target class
            if(currentConstructorStatement.getDeclaringClassName().equals(targetClass)){
                interesting=true;
            }
        }

        return interesting;
    }
}
