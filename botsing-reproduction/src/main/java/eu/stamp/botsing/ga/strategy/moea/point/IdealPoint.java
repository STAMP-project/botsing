package eu.stamp.botsing.ga.strategy.moea.point;


/**d
 * Class representing an ideal point (minimization is assumed)
 *
 * @author Antonio J.Nebro <antonio@lcc.uma.es>
 */
public class IdealPoint extends ArrayPoint {

    public IdealPoint(int dimension) {
        super(dimension) ;
        for (int i = 0; i < dimension; i++) {
            point[i] = Double.POSITIVE_INFINITY;
        }
    }

    @Override
    public void update(double[] point) {
        if (point.length != this.point.length) {
            throw new RuntimeException("The point to be update have a dimension of " + point.length + " "
                    + "while the parameter point has a dimension of " + point.length) ;
        }

        for (int i = 0; i < point.length; i++) {
            if (this.point[i] > point[i]) {
                this.point[i] = point[i];
            }
        }
    }

}
