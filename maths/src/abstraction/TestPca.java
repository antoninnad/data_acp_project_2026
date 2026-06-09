package abstraction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import math.Matrix;
import math.Vector;

public class TestPca {

    private static final String TRAINING_DIR = "../data_filtred/train";
    private static final int MAX_INDIVIDUALS_FOR_TEST = 14;

    public static void main(String[] args) throws IOException {

        testPcaStartsFromTrainingDirectory();
        testChangingBaseImage();
        System.out.println("\nTous les tests PCA sont passes !");
    }




    private static void testPcaStartsFromTrainingDirectory() throws IOException {
        PCA pca = new PCA(TRAINING_DIR, MAX_INDIVIDUALS_FOR_TEST, false);

        if (pca.getFacesCoordinates().getNbColumns() == 0) {
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

        if (projectedFaces.getNbColumns() != pca.getFacesCoordinates().getNbColumns()) {
            throw new AssertionError(
                    "Nombre d'images projetees incorrect : attendu "
                            + pca.getFacesCoordinates().getNbColumns()
                            + ", obtenu "
                            + projectedFaces.getNbColumns()
            );
        }

        Map<String, List<Vector>> mapSign = pca.getMapSign();
        List<Image> expectedImages = loadImages(TRAINING_DIR, MAX_INDIVIDUALS_FOR_TEST);

        assertMapLabelsMatchImages(expectedImages, mapSign);
        assertStoredProjectionMatchesProjectionMethod(expectedImages, pca, projectedFaces);

        System.out.println(
                "testPcaStartsFromTrainingDirectory reussi : "
                        + pca.getFacesCoordinates().getNbColumns()
                        + " images chargees depuis "
                        + TRAINING_DIR
                        + ", "
                        + pca.getNumberOfKeptAxes()
                        + " axes gardes"
        );
    }

    private static List<Image> loadImages(String sourceDir, int maxIndividualsToLoad) throws IOException {
        File root = new File(sourceDir);
        File[] personneFolders = root.listFiles(File::isDirectory);

        if (personneFolders == null) {
            throw new IOException("cannot access '" + sourceDir + "': No such file or directory");
        }

        List<Image> images = new ArrayList<>();
        Arrays.sort(personneFolders, Comparator.comparing(File::getName));
        int loadedIndividuals = 0;

        for (File personneFolder : personneFolders) {
            if (maxIndividualsToLoad > 0 && loadedIndividuals >= maxIndividualsToLoad) {
                break;
            }

            String label = Image.labelFromFolderName(personneFolder.getName());
            if (label.isEmpty()) {
                continue;
            }

            File[] files = personneFolder.listFiles(f ->
                    f.getName().endsWith(".jpg") || f.getName().endsWith(".png")
            );

            if (files == null || files.length == 0) {
                continue;
            }

            Arrays.sort(files, Comparator.comparing(File::getName));

            for (File file : files) {
                images.add(new Image(file.getAbsolutePath(), label));
            }
            loadedIndividuals++;
        }

        return images;
    }

    private static void assertMapLabelsMatchImages(List<Image> expectedImages, Map<String, List<Vector>> mapSign) {
        Map<String, Integer> expectedCountsByLabel = new LinkedHashMap<>();
        int projectedVectorCount = 0;

        for (Image image : expectedImages) {
            expectedCountsByLabel.merge(image.getLabel(), 1, Integer::sum);
        }

        for (Map.Entry<String, List<Vector>> entry : mapSign.entrySet()) {
            projectedVectorCount += entry.getValue().size();
            int expectedCount = expectedCountsByLabel.getOrDefault(entry.getKey(), 0);

            if (entry.getValue().size() != expectedCount) {
                throw new AssertionError(
                        "Nombre de vecteurs incorrect pour le label "
                                + entry.getKey()
                                + " : attendu "
                                + expectedCount
                                + ", obtenu "
                                + entry.getValue().size()
                );
            }
        }

        if (projectedVectorCount != expectedImages.size()) {
            throw new AssertionError(
                    "Nombre total de vecteurs projetes incorrect : attendu "
                            + expectedImages.size()
                            + ", obtenu "
                            + projectedVectorCount
            );
        }
    }

    private static void assertStoredProjectionMatchesProjectionMethod(
            List<Image> expectedImages,
            PCA pca,
            Matrix projectedFaces
    ) throws IOException {
        for (int i = 0; i < expectedImages.size(); i++) {
            Vector recomputedProjection = pca.projectVector(expectedImages.get(i).getPixels());
            Vector storedProjection = projectedFaces.getColumn(i);
            assertVectorAlmostEquals(storedProjection, recomputedProjection, 1e-6);
        }
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

    private static void assertVectorAlmostEquals(Vector expected, Vector result, double tolerance) {
        if (result.getDimension() != expected.getDimension()) {
            throw new AssertionError(
                    "Dimension incorrecte : attendu "
                            + expected.getDimension()
                            + ", obtenu "
                            + result.getDimension()
            );
        }

        for (int i = 0; i < result.getDimension(); i++) {
            if (Math.abs(expected.get(i) - result.get(i)) > tolerance) {
                throw new AssertionError(
                        "Projection incorrecte a l'indice "
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
