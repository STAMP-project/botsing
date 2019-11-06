package eu.stamp.botsing.coupling;

public class BranchlessCaller {
    int number;
    Callee service = new Callee();
    public BranchlessCaller(int givenNumber){
        number = givenNumber;
    }

    public boolean isPositive(){
        return service.isPositive(this.number);
    }
}
