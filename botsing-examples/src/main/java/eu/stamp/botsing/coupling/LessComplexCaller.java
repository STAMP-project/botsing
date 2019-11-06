package eu.stamp.botsing.coupling;

public class LessComplexCaller {


    int number1;
    int number2;
    Callee service = new Callee();

    public LessComplexCaller(int givenNumber1, int givenNumber2) {
        number1 = givenNumber1;
        number2 = givenNumber2;
    }

    public boolean firstNumberIsBigger(){
        int compareResult = service.directCompare(number1,number2);

        if (compareResult > 0){
            return true;
        }

        return false;
    }
}
