package abstraction;

import math.Vector;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class Query {

    protected double threshold_similarity;
    private static final double ALPHA_SURVARIANCE = 1.05;
    private final Map<List<Vector>, Double> thresholdCache = new IdentityHashMap<>();

    public Query(double threshold_similarity) {
        this.threshold_similarity = threshold_similarity;
    }

    public Query() {
        this(0.01);
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
            result += (a.get(i) - b.get(i))* (a.get(i) - b.get(i));
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
        double cos = cosineDistance(a, b);

        double euclidean = Math.sqrt(squaredNorme(a, b));
        double normalizedEuclidean = euclidean / (norm(a) + norm(b) + 1e-12);

        return cos;
    }

    private double norm(Vector v) {
        double result = 0.0;

        for (int i = 0; i < v.getDimension(); i++) {
            result += v.get(i) * v.get(i);
        }

        return Math.sqrt(result);
    }

    private double cosineDistance(Vector a, Vector b) {
        if (a.getDimension() != b.getDimension()) {
            throw new DimensionVectorException("Vector does not have same dimension. ");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.getDimension(); i++) {
            dotProduct += a.get(i) * b.get(i);
            normA += a.get(i) * a.get(i);
            normB += b.get(i) * b.get(i);
        }

        // to avoid division

        if (normA == 0.0 || normB == 0.0) {
            return 1.0;
        }

        return 1.0 - dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private double getThreshold_PersonnalizedForAnImage(List<Vector> dataSetPersonne) {
        if (dataSetPersonne == null || dataSetPersonne.isEmpty()) {
            return threshold_similarity;
        }

        Double cachedThreshold = thresholdCache.get(dataSetPersonne);
        if (cachedThreshold != null) {
            return cachedThreshold;
        }

        if (dataSetPersonne.size() == 1) {
            thresholdCache.put(dataSetPersonne, threshold_similarity);
            return threshold_similarity;
        }

        // Calcul du centroïde
        int dim = dataSetPersonne.get(0).getDimension();
        double[] centroidCoords = new double[dim];
        for (Vector v : dataSetPersonne) {
            for (int d = 0; d < dim; d++) {
                centroidCoords[d] += v.get(d);
            }
        }
        for (int d = 0; d < dim; d++) {
            centroidCoords[d] /= dataSetPersonne.size();
        }
        Vector centroid = new Vector(centroidCoords);

        // Distances au centroïde
        double[] distances = new double[dataSetPersonne.size()];
        double sumDistances = 0.0;
        for (int i = 0; i < dataSetPersonne.size(); i++) {
            distances[i] = distance(dataSetPersonne.get(i), centroid);
            sumDistances += distances[i];
        }
        double meanDistance = sumDistances / dataSetPersonne.size();

        // Écart-type des distances
        double variance = 0.0;
        for (double d : distances) {
            variance += (d - meanDistance) * (d - meanDistance);
        }
        double stdDev = Math.sqrt(variance / dataSetPersonne.size());

        // Seuil = moyenne + k * écart-type  (couvre ~95% des images connues avec k=2)
        double threshold = Math.max(meanDistance + ALPHA_SURVARIANCE * stdDev, threshold_similarity);
        thresholdCache.put(dataSetPersonne, threshold);
        return threshold;
    }

    /**
     * to find the best match with all the dataBase build by PCA
     * @param target the image vectorized centred reduced by the base in pca
     * @param dataBase the ensemble of all image vectorized centred and reduced by the PCA
     * @return the label of the image
     */

    public String findBestMatch(Vector target, Map<String, List<Vector>> dataBase) {

        String bestLabel = "";
        double bestAcceptedDistance = Double.POSITIVE_INFINITY;
        Set<String> keys = dataBase.keySet();

        for (String key : keys) {
            List<Vector> vectorListOfIndividu = dataBase.get(key);
            double thresholdPersonnalized = getThreshold_PersonnalizedForAnImage(vectorListOfIndividu);
            double bestDistanceForLabel = Double.POSITIVE_INFINITY;

            for (Vector img : vectorListOfIndividu) {
                bestDistanceForLabel = Math.min(bestDistanceForLabel, distance(target, img));
            }

            if (bestDistanceForLabel <= thresholdPersonnalized
                    && bestDistanceForLabel < bestAcceptedDistance) {
                bestAcceptedDistance = bestDistanceForLabel;
                System.out.println("[Query] Distance acceptée threshold =" + thresholdPersonnalized + " distance=" + bestDistanceForLabel);
                bestLabel = key;
            }
        }

        if (bestLabel.isEmpty()) {
            return "";
        }

        return bestLabel;
    }

    public MatchDiagnostic diagnoseMatch(Vector target, Map<String, List<Vector>> dataBase, String expectedLabel) {
        String nearestLabel = "";
        String nearestAcceptedLabel = "";
        double nearestDistance = Double.POSITIVE_INFINITY;
        double nearestAcceptedDistance = Double.POSITIVE_INFINITY;
        double expectedDistance = Double.POSITIVE_INFINITY;
        double expectedThreshold = Double.POSITIVE_INFINITY;

        for (String key : dataBase.keySet()) {
            List<Vector> vectorListOfIndividu = dataBase.get(key);
            double thresholdPersonnalized = getThreshold_PersonnalizedForAnImage(vectorListOfIndividu);
            double bestDistanceForLabel = Double.POSITIVE_INFINITY;

            for (Vector img : vectorListOfIndividu) {
                bestDistanceForLabel = Math.min(bestDistanceForLabel, distance(target, img));
            }

            if (bestDistanceForLabel < nearestDistance) {
                nearestDistance = bestDistanceForLabel;
                nearestLabel = key;
            }

            if (bestDistanceForLabel <= thresholdPersonnalized
                    && bestDistanceForLabel < nearestAcceptedDistance) {
                nearestAcceptedDistance = bestDistanceForLabel;
                nearestAcceptedLabel = key;
            }

            if (key.equals(expectedLabel)) {
                expectedDistance = bestDistanceForLabel;
                expectedThreshold = thresholdPersonnalized;
            }
        }

        return new MatchDiagnostic(
                nearestLabel,
                nearestAcceptedLabel,
                nearestDistance,
                nearestAcceptedDistance,
                expectedDistance,
                expectedThreshold
        );
    }

    static class MatchDiagnostic {
        final String nearestLabel;
        final String nearestAcceptedLabel;
        final double nearestDistance;
        final double nearestAcceptedDistance;
        final double expectedDistance;
        final double expectedThreshold;

        MatchDiagnostic(
                String nearestLabel,
                String nearestAcceptedLabel,
                double nearestDistance,
                double nearestAcceptedDistance,
                double expectedDistance,
                double expectedThreshold
        ) {
            this.nearestLabel = nearestLabel;
            this.nearestAcceptedLabel = nearestAcceptedLabel;
            this.nearestDistance = nearestDistance;
            this.nearestAcceptedDistance = nearestAcceptedDistance;
            this.expectedDistance = expectedDistance;
            this.expectedThreshold = expectedThreshold;
        }

        boolean nearestLabelIsExpected(String expectedLabel) {
            return expectedLabel.equals(nearestLabel);
        }

        boolean expectedPassesThreshold() {
            return expectedDistance <= expectedThreshold;
        }

        boolean acceptedLabelIsExpected(String expectedLabel) {
            return expectedLabel.equals(nearestAcceptedLabel);
        }
    }



}
