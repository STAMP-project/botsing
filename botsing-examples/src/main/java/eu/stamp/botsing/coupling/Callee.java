package eu.stamp.botsing.coupling;

public class Callee {

    public boolean isPositive(int number){
        if(number <= 0){
            return false;
        }

        return true;
    }

    public int directCompare(Integer n1, Integer n2){
        return n1.compareTo(n2);
    }
}
