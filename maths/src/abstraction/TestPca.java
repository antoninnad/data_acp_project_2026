package abstraction;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import math.Matrix;
import math.Vector;

/**
 * Integration and unit tests for the {@link PCA} class.
 *
 * <p>Tests verify that PCA loads images correctly from the training directory,
 * that the projection matrix has the expected dimensions, and that projecting
 * individual images with {@link PCA#projectVector} gives the same result as the
 * batch projections stored in the model. A change-of-basis test is also included.</p>
 *
 * @see PCA
 */
public class TestPca {

    private static final String TRAINING_DIR = "../data_filtred/train";
    private static final int MAX_INDIVIDUALS_FOR_TEST = 14;

    
    /**
     * Launch all tests for the PCA and print a message if it's successful
     * 
     *@param args unused
     *@throws IOException if there is a reading error
     * 
     * */    
    public static void main(String[] args) throws IOException {

        testPcaStartsFromTrainingDirectory();
        testChangingBaseImage();
        System.out.println("\nTous les tests PCA sont passes !");
    }


    /**
     * Check the proper execution of a PCA model from training directory : loading of images, calculation of the mean face,
     * dimension of the projection matrix, label consistency and the concordance between stored and recalculated projections
     * via {@link PCA#projectVector}.
     *
     * @throws IOException if the training directory is inaccessible
     * @throws AssertionError if one of the verifications failed
     */

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

    
    /**
     * Load all the images (`.jpg` or `.png`) from {@code sourceDir},
     * by traversing a maximum of {@code maxIndividualsToLoad} subfolders
     * (one subfolder = one individual). Forlders and files are sorted alphabetically to ensure 
     * deterministic order.
     *
     * @param sourceDir  path to the root directory containing the subfolders per individuals
     * @param maxIndividualsToLoad  maximum number of individuals to load (0=unlimited)
     * @return list of loaded images
     * @throws IOException if {sourceDir} is not found or inaccessible
     */
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

    /**
     * Check that labels and the number of vectors in {@code mapSign}
     * corresponod exactly to the expected images : each label must contain as many
     * vectors as associated images and the total must equal to {@code expectedImages.size()}.
     *
     * @param expectedImages reference list of loaded images
     * @param mapSign       list of projected vector from the PCA model
     * @throws AssertionError if a label is incorrectly counted or if the total differs
     */
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

    
    /**
     * Pour chaque image attendue, recalcule sa projection via {@link PCA#projectVector}
     * et la compare colonne par colonne à la projection stockée dans {@code projectedFaces},
     * avec une tolérance numérique de {@code 1e-6}.
     *
     * @param expectedImages  list of the reference images
     * @param pca             PCA model already trained
     * @param projectedFaces  matrix of stored projections
     * @throws IOException    if reading an image failed
     * @throws AssertionError if a recalculated projection is different from the stored one
     */
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
