package abstraction;

import math.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

class Query {

    protected double threshold_similarity;
    private static final double ALPHA_SURVARIANCE = 1.05;

    public Query(double threshold_similarity) {
        this.threshold_similarity = threshold_similarity;
    }

    public Query() {
        this(1.2);
    }

    /**
     * squared norme
     * @param a vecteur a
     * @param b vecteur b de même dimension que b
     * @return norme of the two vector
     */

    public static double squaredNorme(Vector a, Vector b) {
        double result = 0;

        if (a.getDimension() != b.getDimension()) {
            throw new DimensionVectorException("Vector does not have same dimension. ");
        }

        for (int i = 0; i != a.getDimension(); i++) {
            result +=  Math.pow((a.get(i) - b.get(i)), 2);
        }

        return result;
    }

    /**
     * to set the use of a distance for the match
     * @param a
     * @param b
     * @return the distance between vect a and b
     */
    private double distance(Vector a, Vector b) {
        return Query.squaredNorme(a, b);
    }

    /**
     * to get a threshol personnalizedForAnImage reading recherche.md to understand wy
     * @param dataSetPersonne
     * @return
     */

    private double getThreshold_PersonnalizedForAnImage(List<Vector> dataSetPersonne) {
        if (dataSetPersonne == null || dataSetPersonne.isEmpty()) {
            return threshold_similarity;
        }

        List<Vector> pointsSaves = new ArrayList<>(dataSetPersonne);
        boolean isAPointDeleted;

        do {

            // wa take the moy to compute the distance between all points beetween the epicentre
            Vector epicentre = computeEpicentre(pointsSaves);
            List<Double> distances = computeDistances(pointsSaves, epicentre);
            double limiteTukey = computeTukeyLimit(distances);

            //calculing the outlier
            List<Vector> pointsFiltres = new ArrayList<>();
            for (int i = 0; i < pointsSaves.size(); i++) {
                if (distances.get(i) <= limiteTukey) {
                    pointsFiltres.add(pointsSaves.get(i));
                }
            }

            //we save the points admissible
            isAPointDeleted = pointsFiltres.size() < pointsSaves.size();
            if (!pointsFiltres.isEmpty()) {
                pointsSaves = pointsFiltres;
            }
        } while (isAPointDeleted);

        Vector epicentre = computeEpicentre(pointsSaves);

        //getting the final distance between the epicentre
        double seuilPersonnalise = 0.0;
        for (Vector point : pointsSaves) {
            seuilPersonnalise = Math.max(seuilPersonnalise, distance(point, epicentre));
        }

        return Math.max(ALPHA_SURVARIANCE * seuilPersonnalise, threshold_similarity);
    }

    /**
     * to get the epicente
     * @param points list of vector
     * @return the epicenter
     */

    private Vector computeEpicentre(List<Vector> points) {
        Vector epicentre = new Vector(points.get(0).getDimension());

        for (Vector point : points) {
            epicentre = epicentre.addition(point);
        }

        return epicentre.multiplicationScalar(1.0 / points.size());
    }

    /**
     * to get the lis of distance between the epicenter
     * @param points list of all Vecctor
     * @param epicentre
     * @return
     */

    private List<Double> computeDistances(List<Vector> points, Vector epicentre) {
        List<Double> distances = new ArrayList<>();

        for (Vector point : points) {
            distances.add(distance(point, epicentre));
        }

        return distances;
    }

    /**
     * get the limit of tukey see the recherche.md to understand why
     * @param distances
     * @return
     */

    private double computeTukeyLimit(List<Double> distances) {
        List<Double> sortedDistances = new ArrayList<>(distances);
        Collections.sort(sortedDistances);

        double q1 = percentile(sortedDistances, 0.25);
        double q3 = percentile(sortedDistances, 0.75);
        double iqr = q3 - q1;

        return q3 + 1.5 * iqr;
    }

    /**
     * to get the percentil for limit of tukey
     * @param sortedValues
     * @param percentile
     * @return
     */

    private double percentile(List<Double> sortedValues, double percentile) {
        if (sortedValues.size() == 1) {
            return sortedValues.get(0);
        }

        double index = percentile * (sortedValues.size() - 1);
        int lowerIndex = (int) Math.floor(index);
        int upperIndex = (int) Math.ceil(index);

        if (lowerIndex == upperIndex) {
            return sortedValues.get(lowerIndex);
        }

        double ratio = index - lowerIndex;
        return sortedValues.get(lowerIndex) * (1.0 - ratio) + sortedValues.get(upperIndex) * ratio;
    }

    /**
     * to find the best match with all the dataBase build by PCA
     * @param target the image vectorized centred reduced by the base in pca
     * @param dataBase the ensemble of all image vectorized centred and reduced by the PCA
     * @return the label of the image
     */

    public String findBestMatch(Vector target, Map<String, List<Vector>> dataBase) {

        //recovering all keys to explore the dataset
        String result = "";
        Set<String> keys = dataBase.keySet();
        boolean estTrouve = false;

        //finding the best match with threshold
        for (String key : keys) {
            List<Vector> vectorListOfIndividu = dataBase.get(key);
            double thresholdPersonnalized = getThreshold_PersonnalizedForAnImage(vectorListOfIndividu);

            for (Vector img : vectorListOfIndividu) {
                if (distance(target, img) < thresholdPersonnalized) {
                    estTrouve = true;
                    result = key;
                    break;
                }
            }

            if (estTrouve) {
                break;
            }

        }


        return result;
    }



}
