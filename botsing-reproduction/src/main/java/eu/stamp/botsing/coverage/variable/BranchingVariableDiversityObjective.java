package eu.stamp.botsing.coverage.variable;

import org.evosuite.Properties;
import org.evosuite.ga.archive.Archive;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;
import org.objectweb.asm.Type;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

public class BranchingVariableDiversityObjective extends TestFitnessFunction {
    private static final long serialVersionUID = -8943148780270724898L;

    String className;
    private String methodName;
    int lineNumber;
    String variableName;
    Type variableType;
    DiversityObjective condition;

    static BranchingVariableDiversityObjective getTestFitness(String className, String methodName, int lineNumber,
                                                              String variableName, Type variableType,
                                                              DiversityObjective condition) {
        BranchingVariableDiversityObjective testFitness = new BranchingVariableDiversityObjective();
        testFitness.className = className;
        testFitness.methodName = methodName;
        testFitness.lineNumber = lineNumber;
        testFitness.variableName = variableName;
        testFitness.variableType = variableType;
        testFitness.condition = condition;
        return testFitness;
    }

    @Override
    public String getTargetClass() {
        return className;
    }

    @Override
    public String getTargetMethod() {
        return methodName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    String getVariableName() {
        return variableName;
    }

    Type getVariableType() {
        return variableType;
    }

    DiversityObjective getCondition() {
        return condition;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getFitness(TestChromosome individual, ExecutionResult result) {
        double fitness = 1;

        Map<Integer, Map<String, Object>> inClass = result.getTrace().getBranchingVariables(className);
        if (inClass != null) {
            Map<String, Object> atLine = inClass.get(getLineNumber());
            if (atLine != null) {
                Object variable = atLine.get(getVariableName());
                if (variable != null) {
                    double distance = calculateDistance(variable);
                    if (distance >= 0) {
                        fitness = distance;
                    }
                }
            }
        }

        assert fitness >= 0;
        updateIndividual(this, individual, fitness);

        if (fitness == 0) {
            individual.getTestCase().addCoveredGoal(this);
        }

        if (Properties.TEST_ARCHIVE) {
            Archive.getArchiveInstance().updateArchive(this, individual, fitness);
        }

        return fitness;
    }

    double calculateDistance(Object variable) {
        switch (getCondition()) {
            case REF_NULL:
                return variable == null ? 0 : 1;
            case REF_NONNULL:
                return variable == null ? 1 : 0;
            case NUM_NEGATIVE:
            case NUM_ZERO:
            case NUM_POSITIVE:
                assert variable instanceof Number;
                double doubleValue = ((Number) variable).doubleValue();
                switch (getCondition()) {
                    case NUM_NEGATIVE:
                        return doubleValue < 0 ? 0 : doubleValue + 1;
                    case NUM_ZERO:
                        return Math.abs(doubleValue);
                    case NUM_POSITIVE:
                        return doubleValue > 0 ? 0 : 1 - doubleValue;
                }
            case CHAR_ALPHA:
            case CHAR_DIGIT:
            case CHAR_OTHER:
                assert variable instanceof Character;
                char charValue = (Character) variable;
                switch (getCondition()) {
                    case CHAR_ALPHA:
                        if (charValue < 'A') {
                            return 'A' - charValue;
                        } else if (charValue > 'z') {
                            return charValue - 'z';
                        } else if (charValue < 'a' && charValue > 'Z') {
                            return Math.min('a' - charValue, charValue - 'Z');
                        } else {
                            return 0;
                        }
                    case CHAR_DIGIT:
                        if (charValue < '0') {
                            return '0' - charValue;
                        } else if (charValue > '9') {
                            return charValue - '9';
                        } else {
                            return 0;
                        }
                    case CHAR_OTHER:
                        if (charValue >= '0' && charValue <= '9') {
                            return Math.min(charValue - '0', '9' - charValue) + 1;
                        } else if (charValue >= 'A' && charValue <= 'Z') {
                            return Math.min(charValue - 'A', 'Z' - charValue) + 1;
                        } else if (charValue >= 'a' && charValue <= 'z') {
                            return Math.min(charValue - 'a', 'z' - charValue) + 1;
                        } else {
                            return 0;
                        }
                }
            case BOOL_TRUE:
                assert variable instanceof Boolean;
                return ((Boolean) variable) ? 0 : 1;
            case BOOL_FALSE:
                assert variable instanceof Boolean;
                return ((Boolean) variable) ? 1 : 0;
            case ARRAY_EMPTY:
                return Array.getLength(variable);
            case ARRAY_NONEMPTY:
                return Array.getLength(variable) == 0 ? 1 : 0;
            case STRING_EMPTY:
                return ((String) variable).length();
            case STRING_NONEMPTY:
                return ((String) variable).length() == 0 ? 1 : 0;
            case LIST_EMPTY:
            case SET_EMPTY:
                assert variable instanceof Collection;
                return ((Collection) variable).size();
            case LIST_NONEMPTY:
            case SET_NONEMPTY:
                assert variable instanceof Collection;
                return ((Collection) variable).size() == 0 ? 1 : 0;
            case MAP_EMPTY:
                assert variable instanceof Map;
                return ((Map) variable).size();
            case MAP_NONEMPTY:
                assert variable instanceof Map;
                return ((Map) variable).size() == 0 ? 1 : 0;
            default:
                throw new IllegalStateException("Unknown condition!!!");
        }
    }

    @Override
    public String toString() {
        return className + " @L" + lineNumber + " : " + variableName + " : " + condition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BranchingVariableDiversityObjective that = (BranchingVariableDiversityObjective) o;

        if (!className.equals(that.className)) {
            return false;
        }
        if (lineNumber != that.lineNumber) {
            return false;
        }
        if (!variableName.equals(that.variableName)) {
            return false;
        }
        return condition == that.condition;
    }

    @Override
    public int hashCode() {
        int result = className.hashCode();
        result = 31 * result + lineNumber;
        result = 31 * result + variableName.hashCode();
        return 31 * result + condition.hashCode();
    }

    @Override
    public int compareTo(TestFitnessFunction testFitnessFunction) {
        int classCompare = className.compareTo(testFitnessFunction.getTargetClass());
        if (classCompare == 0) {
            if (testFitnessFunction instanceof BranchingVariableDiversityObjective) {
                BranchingVariableDiversityObjective other = (BranchingVariableDiversityObjective) testFitnessFunction;
                int lineCompare = Integer.compare(lineNumber, other.lineNumber);
                if (lineCompare == 0) {
                    int nameCompare = variableName.compareTo(other.variableName);
                    if (nameCompare == 0) {
                        return condition.compareTo(other.condition);
                    }
                    return nameCompare;
                }
                return lineCompare;
            }
        }
        return classCompare;
    }
}
