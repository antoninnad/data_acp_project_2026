package abstraction;

import java.io.IOException;

import math.Matrix;
import math.Vector;

public class TestPca {

    private static final String TRAINING_DIR = "../data_filtred/train";
    private static final int MAX_IMAGES_FOR_TEST = 20;

    public static void main(String[] args) throws IOException {
        testChangingBaseImage();
        testPcaStartsFromTrainingDirectory();

        System.out.println("\nTous les tests PCA sont passes !");
    }

    private static void testPcaStartsFromTrainingDirectory() throws IOException {
        PCA pca = new PCA(TRAINING_DIR, MAX_IMAGES_FOR_TEST, false);

        if (pca.getFacesCordonates().getNbColumns() == 0) {
            throw new AssertionError("Aucune image chargee depuis " + TRAINING_DIR);
        }

        if (pca.getMeanFace() == null) {
            throw new AssertionError("La moyenne des visages n'a pas ete calculee");
        }

        Matrix projectedFaces = pca.getProjectedFacesOnKeptAxes();

        if (projectedFaces == null) {
            throw new AssertionError("Les images n'ont pas ete projetees dans l'espace ACP");
        }

        if (projectedFaces.getNbRows() != pca.getNumberOfKeptAxes()) {
            throw new AssertionError(
                    "Nombre de lignes projetees incorrect : attendu "
                            + pca.getNumberOfKeptAxes()
                            + ", obtenu "
                            + projectedFaces.getNbRows()
            );
        }

        if (projectedFaces.getNbColumns() != pca.getFacesCordonates().getNbColumns()) {
            throw new AssertionError(
                    "Nombre d'images projetees incorrect : attendu "
                            + pca.getFacesCordonates().getNbColumns()
                            + ", obtenu "
                            + projectedFaces.getNbColumns()
            );
        }

        System.out.println(
                "testPcaStartsFromTrainingDirectory reussi : "
                        + pca.getFacesCordonates().getNbColumns()
                        + " images chargees depuis "
                        + TRAINING_DIR
                        + ", "
                        + pca.getNumberOfKeptAxes()
                        + " axes gardes"
        );
    }

    private static void testChangingBaseImage() {
        Vector data = new Vector(new double[] {
                2, 5, -1, 7, 4, 3
        });

        Image image = new Image(data);

        Matrix changeOfBasis = new Matrix(new double[][] {
                {1, 0, 0, 1, 0, 0},
                {0, 1, 0, 0, 1, 0},
                {0, 0, 1, 0, 0, 1}
        });

        Vector result = image.changingBaseImage(changeOfBasis);
        Vector expected = new Vector(new double[] {
                9, 9, 2
        });

        assertVectorEquals(expected, result);

        System.out.println("testChangingBaseImage reussi");
    }

    private static void assertVectorEquals(Vector expected, Vector result) {
        if (result.getDimension() != expected.getDimension()) {
            throw new AssertionError(
                    "Dimension incorrecte : attendu "
                            + expected.getDimension()
                            + ", obtenu "
                            + result.getDimension()
            );
        }

        for (int i = 0; i < result.getDimension(); i++) {
            if (Double.compare(result.get(i), expected.get(i)) != 0) {
                throw new AssertionError(
                        "Valeur incorrecte a l'indice "
                                + i
                                + " : attendu "
                                + expected.get(i)
                                + ", obtenu "
                                + result.get(i)
                );
            }
        }
    }
}
