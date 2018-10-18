package eu.stamp.botsing;

public class Fraction {

    int numerator;
    int denominator;

    public Fraction(int numerator, int denominator){
        this.numerator = numerator;
        this.denominator = denominator;
    }

    public double getValue(){
        if (denominator == 0) {
            throw new IllegalArgumentException();
        }
        return ((double) this.numerator)/this.denominator;
    }

    public double getShiftedValue(int shift){
        if (denominator == 0) {
            throw new IllegalArgumentException();
        }
        return this.numerator/(this.denominator + shift);
    }
}
