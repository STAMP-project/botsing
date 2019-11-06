package eu.stamp.botsing.poly;

public class SuperClass {

    protected Integer number1;
    protected Integer number2;

    public SuperClass(int givenNumber1, int givenNumber2){
        number1 = new Integer(givenNumber1);
        number2 = new Integer(givenNumber2);
    }

    public int directCompare(){
        return number1.compareTo(number2);
    }

    public boolean AreNumbersOk(){
        if(number2>0 && number1 > 0){
            return true;
        }

        return false;
    }

    public boolean valid(){
        if (AreNumbersOk()){
            if (number1+number2 <10){
                return false;
            }
            return true;
        }
        return false;
    }


}
