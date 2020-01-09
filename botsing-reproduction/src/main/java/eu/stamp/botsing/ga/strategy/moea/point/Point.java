package eu.stamp.botsing.ga.strategy.moea.point;

public interface Point {
    int getDimension();

    double[] getValues();

    double getValue(int var1);

    void setValue(int var1, double var2);

    void update(double[] var1);
}
