package eu.stamp.botsing.coupling;

public class Caller {

    int number1;
    int number2;
    Callee service = new Callee();

    public Caller(int givenNumber1, int givenNumber2) {
        number1 = givenNumber1;
        number2 = givenNumber2;
    }

    public boolean firstNumberIsBigger(){
        if (number1>0 && number2<=0){
            return true;
        }else if (number1 <=0 && number2>0){
            return false;
        }
        int compareResult = service.directCompare(number1,number2);

        if (compareResult > 0){
            return true;
        }

        return false;
    }

}
