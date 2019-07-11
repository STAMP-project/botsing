package eu.stamp.botsing.coupling.analyze.calls;

import java.util.Arrays;
import java.util.List;

public class ClassPair implements Comparable<ClassPair> {

    // In hierarchy analysis, class1 is sub class and class2 is super class
    protected String class1;
    protected String class2;
    protected int callScore1 = 0;
    protected int callScore2 = 0;
    protected int numberOfBranchesInClass1 = 0;
    protected int numberOfBranchesInClass2 = 0;
    protected int totalScore = 0;

    public ClassPair(String class1, String class2, int score1, int score2){
        this.class1 = class1;
        this.class2 = class2;
        this.callScore1 = score1;
        this.callScore2 = score2;
        this.totalScore = score1+score2;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ClassPair)){
            return false;
        }

        ClassPair otherPair = (ClassPair) obj;
        if(! otherPair.class1.equals(this.class1) && !otherPair.class1.equals(this.class2)){
            return false;
        }

        if(! otherPair.class2.equals(this.class1) && !otherPair.class2.equals(this.class2)){
            return false;
        }

        return true;
    }

    @Override
    public int compareTo(ClassPair otherPair) {
        List<Integer> ownObjectives = this.getListOfObjectivesValue();
        List<Integer> otherObjectives = otherPair.getListOfObjectivesValue();

        int value = 0;
        for (int i =0 ; i<ownObjectives.size(); i++){
            int ownValue = ownObjectives.get(i);
            int otherValue = otherObjectives.get(i);
            if(i==0){
                value =  Integer.compare(ownValue,otherValue);
            }else{
                int tempValue = Integer.compare(ownValue,otherValue);
                int mult = value * tempValue;
                if(mult < 0){
                    // better in one objective worst in the other one!
                    return 0;
                }else if (mult == 0){
                    if(value == 0){
                        value = tempValue;
                    }
                    continue;
                }else{
                    continue;
                }
            }
        }

        return value;
    }


    public String getClass1() {
        return class1;
    }

    public String getClass2() {
        return class2;
    }

    public int getCallScore1() {
        return callScore1;
    }

    public int getCallScore2() {
        return callScore2;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public int getNumberOfBranchesInClass1() {
        return numberOfBranchesInClass1;
    }

    public int getNumberOfBranchesInClass2() {
        return numberOfBranchesInClass2;
    }

    public void addTonumberOfBranches(String className, int value){
        if(className.equals(class1)){
            numberOfBranchesInClass1+=value;
        }else if (className.equals(class2)){
            numberOfBranchesInClass2+=value;
        }else{
            throw new IllegalStateException("Class is not available!");
        }
    }

    public List<Integer> getListOfObjectivesValue(){
        List<Integer> result = Arrays.asList(new Integer[]{this.callScore1, this.callScore2, this.numberOfBranchesInClass1, this.numberOfBranchesInClass2 });
        return result;
    }
}
