package abstraction;

import math.Vector;

import java.util.List;
import java.util.Map;
import java.util.Set;

class Query {

    protected double threshold_similarity;

    Query(double threshold_similarity) {
        this.threshold_similarity = threshold_similarity;
    }

    Query() {
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
            for (Vector img : vectorListOfIndividu) {
                if (distance(target, img) < threshold_similarity) {
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