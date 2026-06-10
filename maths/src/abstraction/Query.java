package abstraction;

import math.Vector;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Performs face recognition queries against a labelled database of projected face vectors.
 *
 * <p>Given a query vector (a face projected into the PCA eigenspace), {@link #findBestMatch}
 * searches the database for the closest known individual using cosine distance. The acceptance
 * decision uses a per-person adaptive threshold: the threshold is computed from the mean and
 * standard deviation of intra-class distances so that tighter clusters get stricter thresholds
 * (bounded by {@code MAX_THRESHOLD}).</p>
 *
 * @see PCA
 */
public class Query {

    protected double threshold_similarity;
    private final Map<List<Vector>, Double> thresholdCache = new IdentityHashMap<>();
    private static final double ALPHA_SURVARIANCE = 2;
    private static final double MAX_THRESHOLD = 0.30;

    /**
     * Constructs a Query with a custom base similarity threshold.
     *
     * @param threshold_similarity minimum cosine distance below which a match is accepted
     *                             (used as a floor when computing per-person thresholds)
     */
    public Query(double threshold_similarity) {
        this.threshold_similarity = threshold_similarity;
    }

    /**
     * Constructs a Query with a default similarity threshold of 0.01.
     */
    public Query() {
        this(0.01);
    }

    /**
     * Computes the squared Euclidean distance between two vectors.
     *
     * @param a first vector
     * @param b second vector, must have the same dimension as {@code a}
     * @return sum of squared component-wise differences ‖a − b‖²
     * @throws DimensionVectorException if {@code a} and {@code b} have different dimensions
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
     * Returns the cosine distance between two vectors.
     * This is the metric used to compare face projections.
     *
     * @param a first vector
     * @param b second vector, must have the same dimension as {@code a}
     * @return cosine distance in [0, 2] (0 = identical direction, 1 = orthogonal)
     */
    private double distance(Vector a, Vector b) {
        double cos = cosineDistance(a, b);

        double euclidean = Math.sqrt(squaredNorme(a, b));
        double normalizedEuclidean = euclidean / (norm(a) + norm(b) + 1e-12);

        return cos;
    }

    /**
     * Computes the L2 norm (Euclidean length) of a vector.
     *
     * @param v input vector
     * @return ‖v‖₂
     */
    private double norm(Vector v) {
        double result = 0.0;

        for (int i = 0; i < v.getDimension(); i++) {
            result += v.get(i) * v.get(i);
        }

        return Math.sqrt(result);
    }

    /**
     * Computes the cosine distance between two vectors: 1 − (a·b) / (‖a‖ ‖b‖).
     * Returns 1.0 (maximum distance) if either vector is the zero vector.
     *
     * @param a first vector
     * @param b second vector, must have the same dimension as {@code a}
     * @return cosine distance in [0, 2]
     * @throws DimensionVectorException if the vectors have different dimensions
     */
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

        if (normA == 0.0 || normB == 0.0) {
            return 1.0;
        }

        return 1.0 - dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Computes or retrieves the cached adaptive acceptance threshold for a specific person's
     * training vectors. The threshold equals mean intra-class distance + α × standard deviation,
     * floored at {@code threshold_similarity} and capped at {@code MAX_THRESHOLD}.
     *
     * @param dataSetPersonne list of projected training vectors for one person
     * @return acceptance threshold to use when querying against this person
     */
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

        double[] distances = new double[dataSetPersonne.size()];
        double sumDistances = 0.0;
        for (int i = 0; i < dataSetPersonne.size(); i++) {
            distances[i] = distance(dataSetPersonne.get(i), centroid);
            sumDistances += distances[i];
        }
        double meanDistance = sumDistances / dataSetPersonne.size();

        double variance = 0.0;
        for (double d : distances) {
            variance += (d - meanDistance) * (d - meanDistance);
        }
        double stdDev = Math.sqrt(variance / dataSetPersonne.size());

        double threshold = meanDistance + ALPHA_SURVARIANCE * stdDev;
        threshold = Math.max(threshold, threshold_similarity);
        threshold = Math.min(threshold, MAX_THRESHOLD);

        thresholdCache.put(dataSetPersonne, threshold);
        return threshold;
    }

    /**
     * Searches the database for the best matching identity for a given query vector.
     * The query vector must have been projected into the same PCA eigenspace as the database.
     * Returns an empty string when no person passes their adaptive threshold.
     *
     * @param target   query face vector projected into the PCA eigenspace
     * @param dataBase map from person label to their list of projected training vectors
     * @return the label of the closest accepted person, or {@code ""} if no match is found
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

    /**
     * Runs a full diagnostic match for a known query, returning distances and thresholds
     * for the nearest person, the nearest accepted person, and the expected person.
     * Used by {@link Evaluator} to produce detailed confusion-matrix statistics.
     *
     * @param target        projected query vector
     * @param dataBase      map from label to projected training vectors
     * @param expectedLabel ground-truth label of the query image
     * @return a {@link MatchDiagnostic} containing all relevant distances and thresholds
     */
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

    /**
     * Holds diagnostic data for a single query: distances to the nearest person, the
     * nearest accepted person, and the expected person, together with the expected
     * person's acceptance threshold.
     */
    static class MatchDiagnostic {
        final String nearestLabel;
        final String nearestAcceptedLabel;
        final double nearestDistance;
        final double nearestAcceptedDistance;
        final double expectedDistance;
        final double expectedThreshold;

        /**
         * Constructs a MatchDiagnostic with all diagnostic values.
         *
         * @param nearestLabel          label of the person with the smallest cosine distance
         * @param nearestAcceptedLabel  label of the person with the smallest distance that passes their threshold
         * @param nearestDistance       smallest cosine distance across all persons
         * @param nearestAcceptedDistance smallest accepted cosine distance
         * @param expectedDistance      cosine distance to the ground-truth person
         * @param expectedThreshold     acceptance threshold of the ground-truth person
         */
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

        /**
         * Returns {@code true} if the nearest person (by distance) is the expected person.
         *
         * @param expectedLabel ground-truth label to compare against
         * @return {@code true} when the nearest label matches {@code expectedLabel}
         */
        boolean nearestLabelIsExpected(String expectedLabel) {
            return expectedLabel.equals(nearestLabel);
        }

        /**
         * Returns {@code true} if the expected person's distance is within their acceptance threshold.
         *
         * @return {@code true} when {@code expectedDistance <= expectedThreshold}
         */
        boolean expectedPassesThreshold() {
            return expectedDistance <= expectedThreshold;
        }

        /**
         * Returns {@code true} if the nearest accepted person is the expected person.
         *
         * @param expectedLabel ground-truth label to compare against
         * @return {@code true} when the accepted label matches {@code expectedLabel}
         */
        boolean acceptedLabelIsExpected(String expectedLabel) {
            return expectedLabel.equals(nearestAcceptedLabel);
        }
    }

}
