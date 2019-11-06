package eu.stamp.botsing.poly;

public class LessComplexSubClass extends SuperClass {

    public LessComplexSubClass(int givenNumber1, int givenNumber2) {
        super(givenNumber1, givenNumber2);
    }

    public boolean firstNumberIsBigger(){
        int compareResult = this.directCompare();

        if (compareResult > 0){
            return true;
        }

        return false;
    }
}
