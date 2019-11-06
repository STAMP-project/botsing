package eu.stamp.botsing.poly;

public class SubClass extends SuperClass {

    public SubClass(int givenNumber1, int givenNumber2) {
        super(givenNumber1, givenNumber2);
    }

    public boolean shouldConsider(){
        if(!valid()){
            return false;
        }

        if (secondNumberIsBigger()){
            return false;
        }

        return true;
    }

    public boolean secondNumberIsBigger(){
        if(!valid()){
            return false;
        }

        return !firstNumberIsBigger();
    }

    public boolean firstNumberIsBigger(){
        if(!valid()){
            return false;
        }
        if (number1>0 && number2<=0){
            return true;
        }else if (number1 <=0 && number2>0){
            return false;
        }

        int compareResult = this.directCompare();

        if (compareResult > 0){
            return true;
        }

        return false;
    }

    @Override
    public boolean AreNumbersOk(){
        return true;
    }


}

