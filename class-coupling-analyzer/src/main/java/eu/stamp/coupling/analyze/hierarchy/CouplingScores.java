package eu.stamp.coupling.analyze.hierarchy;

public class CouplingScores {

    public int superClassScore;
    public int subClassScore;

    public CouplingScores(int superClassScore, int subClassScore){
        this.subClassScore = subClassScore;
        this.superClassScore = superClassScore;
    }

    public void increaseSuperClassScore(){
        superClassScore++;
    }

    public void increaseSubClassScore(){
        subClassScore++;
    }

    public boolean isZero(){
        if(subClassScore+superClassScore < 5){
            return true;
        }

        return false;
    }
}
