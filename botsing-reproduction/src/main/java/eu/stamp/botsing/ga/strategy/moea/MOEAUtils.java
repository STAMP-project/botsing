package eu.stamp.botsing.ga.strategy.moea;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.fitnessfunction.FitnessFunctionHelper;
import eu.stamp.botsing.ga.strategy.moea.point.IdealPoint;
import org.evosuite.ga.Chromosome;
import org.evosuite.utils.Randomness;

import java.util.List;

public class MOEAUtils {


    public static <T extends Chromosome> void updateIdealPoint(IdealPoint idealPoint, List<T> population) {
        for (Chromosome individual: population){
            idealPoint.update(getPoints(individual));
        }
    }

    public static double[] getPoints(Chromosome individual){
        double[] points = new double[CrashProperties.fitnessFunctions.length];
        int index = 0;
        for (CrashProperties.FitnessFunction objective: CrashProperties.fitnessFunctions){
            points[index] = FitnessFunctionHelper.getFitnessValue(individual,objective);
            index++;
        }

        return points;
    }


    public static void randomPermutation(int[] perm, int size){
        int[] index = new int[size];
        boolean[] flag = new boolean[size];

        for (int n = 0; n < size; n++) {
            index[n] = n;
            flag[n] = true;
        }

        int num = 0;
        while (num < size) {
            int start = Randomness.nextInt(0, size);
            while (true) {
                if (flag[start]) {
                    perm[num] = index[start];
                    flag[start] = false;
                    num++;
                    break;
                }
                if (start == (size - 1)) {
                    start = 0;
                } else {
                    start++;
                }
            }
        }
    }


    public static double distVector(double[] vector1, double[] vector2) {
        int dim = vector1.length;
        double sum = 0;
        for (int n = 0; n < dim; n++) {
            sum += (vector1[n] - vector2[n]) * (vector1[n] - vector2[n]);
        }
        return Math.sqrt(sum);
    }

    public static void minFastSort(double x[], int idx[], int n, int m) {
        for (int i = 0; i < m; i++) {
            for (int j = i + 1; j < n; j++) {
                if (x[i] > x[j]) {
                    double temp = x[i];
                    x[i] = x[j];
                    x[j] = temp;
                    int id = idx[i];
                    idx[i] = idx[j];
                    idx[j] = id;
                }
            }
        }
    }


}
