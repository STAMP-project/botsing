package eu.stamp.botsing;

public class PrivateFraction {


    public static double getShiftedValue(int a, int b){
        if (b == 0) {
            throw new IllegalArgumentException();
        }
        return getValue(a,b);
    }

    private static double getValue(int a , int b){
        return a/(b+1);
    }
}
