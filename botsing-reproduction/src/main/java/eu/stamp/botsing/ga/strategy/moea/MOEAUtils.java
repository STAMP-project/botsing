package eu.stamp.botsing.ga.strategy.moea;

import eu.stamp.botsing.CrashProperties;
import eu.stamp.botsing.fitnessfunction.FitnessFunctionHelper;
import org.evosuite.ga.Chromosome;
import org.evosuite.utils.Randomness;
import org.uma.jmetal.util.point.impl.IdealPoint;

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
}
