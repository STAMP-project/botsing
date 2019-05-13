package eu.stamp.botsing.fitnessfunction;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.fitnessfunction.calculator.CrashCoverageFitnessCalculator;
import org.evosuite.coverage.exception.ExceptionCoverageHelper;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;

public class IntegrationTestingFF extends TestFitnessFunction {
    private static final Logger LOG = LoggerFactory.getLogger(IntegrationTestingFF.class);
    @Resource
    CrashCoverageFitnessCalculator fitnessCalculator;

    public IntegrationTestingFF(){
        fitnessCalculator = new CrashCoverageFitnessCalculator();
    }
    @Override
    public double getFitness(TestChromosome testChromosome, ExecutionResult executionResult) {
        int targetFrame = CrashProperties.getInstance().getStackTrace(0).getTargetFrameLevel();
        double fitnessValue=0;
        boolean covering = true;
        for(int frameLevel = targetFrame; frameLevel > 0 ; frameLevel--){
            if(covering){
                double lineCoverageFitness = fitnessCalculator.getLineCoverageForFrame(0, executionResult,frameLevel);
                if(lineCoverageFitness != 0 && !CrashProperties.getInstance().getStackTrace(0).isIrrelevantFrame(frameLevel)){
                    fitnessValue = lineCoverageFitness;
                    covering=false;
                }
            }else{
                fitnessValue++;
            }
        }

        if(fitnessValue == 0.0){
            // We have reached to the deepest frame target line. So, need to check if the target exception has been thrown.
            fitnessValue = exceptionCoverage(executionResult);
        }else {
            // We have not reached to the deepest frame target line. So, we set the target exception to 1 as the penalty.
            fitnessValue++;
        }

//        double fitnessValue = lineCoverageFitness;
        LOG.debug("Fitness Function: "+fitnessValue);
        testChromosome.setFitness(this,fitnessValue);
        testChromosome.increaseNumberOfEvaluations();
        return fitnessValue;
    }

    private double exceptionCoverage(ExecutionResult executionResult) {
        double exceptionCoverage = 1.0;
        for (Integer ExceptionLocator : executionResult.getPositionsWhereExceptionsWereThrown()) {
            if(ExceptionCoverageHelper.isExplicit(executionResult,ExceptionLocator)){
                String thrownException = ExceptionCoverageHelper.getExceptionClass(executionResult, ExceptionLocator).getName();
                if (thrownException.equals(CrashProperties.getInstance().getStackTrace(0).getExceptionType())){
                    exceptionCoverage = 0.0;
                    break;
                }
            }
        }
        return exceptionCoverage;
    }

    @Override
    public int compareTo(TestFitnessFunction testFitnessFunction) {
        return 0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( CrashProperties.getInstance().getStackTrace(0).hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        return getClass() == obj.getClass();
    }

    @Override
    public String getTargetClass() {
        return CrashProperties.getInstance().getStackTrace(0).getFrame(1).getClassName();
    }

    @Override
    public String getTargetMethod() {
        return CrashProperties.getInstance().getStackTrace(0).getFrame(1).getMethodName();
    }
}
