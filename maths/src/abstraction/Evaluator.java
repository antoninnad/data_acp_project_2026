package abstraction;

import math.Matrix;
import math.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class Evaluator {

    private static final String TESTING_DIR = "data_filtred3/test";
    private static final String TRAINING_DIR = "data_filtred3/train";
    private static final int MAX_INDIVIDUALS_FOR_TRAINING = 30;
    private static final int MAX_UNKNOWN_INDIVIDUALS_FOR_TEST = 10;
    private static final double DEFAULT_THRESHOLD = 0.10;
    private PCA pca;
    private Map<String, List<Vector>> dataBase;
    private Query query;


    /**
     * to get information for the quality of acp
     */
    public void getMatrixConfusion() {

        try {
            setANewPca();
            int knownCorrect = 0;
            int knownRejected = 0;
            int knownMisclassified = 0;
            int unknownAccepted = 0;
            int unknownRejected = 0;
            int nearestLabelCorrect = 0;
            int expectedPassesThreshold = 0;
            int expectedPassesButAnotherAcceptedCloser = 0;
            int rejectedDespiteExpectedPassingThreshold = 0;
            int rejectedBecauseExpectedFailsThreshold = 0;

            List<Image> imagesToEvaluate = loadImages(
                    TESTING_DIR,
                    MAX_INDIVIDUALS_FOR_TRAINING + MAX_UNKNOWN_INDIVIDUALS_FOR_TEST
            );
            Set<String> knownLabels = dataBase.keySet();

            //lauching the test

            for (Image image : imagesToEvaluate) {
                String expectedLabel = image.getLabel();
                Vector projectedImage = pca.projectVector(image.getPixels());
                String predictedLabel = query.findBestMatch(projectedImage, dataBase);
                boolean isPredictedKnown = !predictedLabel.isEmpty();

                if (!knownLabels.contains(expectedLabel)) {


                    if (isPredictedKnown) {
                        System.out.println("[Test] accepted but should be not");
                        unknownAccepted++;
                    } else {
                        unknownRejected++;
                    }
                } else {
                    Query.MatchDiagnostic diagnostic = query.diagnoseMatch(projectedImage, dataBase, expectedLabel);

                    if (diagnostic.nearestLabelIsExpected(expectedLabel)) {
                        nearestLabelCorrect++;
                    }

                    if (diagnostic.expectedPassesThreshold()) {
                        expectedPassesThreshold++;
                    }

                    if (expectedLabel.equals(predictedLabel)) {
                        knownCorrect++;
                    } else if (isPredictedKnown) {
                        knownMisclassified++;
                        logMismatch(image, predictedLabel, diagnostic);
                        if (diagnostic.expectedPassesThreshold()
                                && !diagnostic.acceptedLabelIsExpected(expectedLabel)) {
                            expectedPassesButAnotherAcceptedCloser++;
                        }
                    } else {
                        knownRejected++;
                        if (diagnostic.expectedPassesThreshold()) {
                            rejectedDespiteExpectedPassingThreshold++;
                        } else {
                            rejectedBecauseExpectedFailsThreshold++;
                        }
                    }
                }
            }

            // all the metrics

            int total = knownCorrect + knownRejected + knownMisclassified;
            int unknownTotal = unknownAccepted + unknownRejected;
            double accuracy = total == 0 ? 0.0 : knownCorrect / total;
            double discriminationRate = total == 0 ? 0.0 : (double) nearestLabelCorrect / total;
            double openSetAccuracy = total + unknownTotal == 0 ? 0.0 : (double) (knownCorrect + unknownRejected) / (total + unknownTotal);
            int predictedKnownTotal = knownCorrect + knownMisclassified + unknownAccepted;
            double precision = predictedKnownTotal == 0 ? 0.0 : (double) knownCorrect / predictedKnownTotal;
            double recall = total == 0 ? 0.0 : (double) knownCorrect / total;
            double f1Score = precision + recall == 0.0
                    ? 0.0
                    : 2.0 * precision * recall / (precision + recall);


            System.out.println("Evaluation terminee : " + (total + unknownTotal) + " images testees depuis " + resolveExistingDirectory(TESTING_DIR));
            System.out.println("trainingIndividuals=" + MAX_INDIVIDUALS_FOR_TRAINING);
            System.out.println("knownEvaluated=" + total);
            System.out.println("knownCorrect=" + knownCorrect);
            System.out.println("knownRejected=" + knownRejected);
            System.out.println("knownMisclassified=" + knownMisclassified);
            System.out.println("unknownEvaluated=" + unknownTotal);
            System.out.println("unknownAccepted=" + unknownAccepted);
            System.out.println("unknownRejected=" + unknownRejected);
            System.out.println("nearestLabelCorrect=" + nearestLabelCorrect);
            System.out.println("expectedPassesThreshold=" + expectedPassesThreshold);
            System.out.println("expectedPassesButAnotherAcceptedCloser=" + expectedPassesButAnotherAcceptedCloser);
            System.out.println("rejectedDespiteExpectedPassingThreshold=" + rejectedDespiteExpectedPassingThreshold);
            System.out.println("rejectedBecauseExpectedFailsThreshold=" + rejectedBecauseExpectedFailsThreshold);
            System.out.printf(Locale.US, "accuracy=%.2f%%%n", accuracy * 100.0);
            System.out.printf(Locale.US, "discriminationRate=%.2f%%%n", discriminationRate * 100.0);
            System.out.printf(Locale.US, "openSetAccuracy=%.2f%%%n", openSetAccuracy * 100.0);
            System.out.printf(Locale.US, "precision=%.2f%%%n", precision * 100.0);
            System.out.printf(Locale.US, "recall=%.2f%%%n", recall * 100.0);
            System.out.printf(Locale.US, "f1Score=%.2f%%%n", f1Score * 100.0);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (pca == null) {
            throw new AssertionError("Pca wasn't set");
        }

    }

    /**
     * to show when we get a mismatch only for debug
     * @param image
     * @param predictedLabel
     * @param diagnostic
     */

    private void logMismatch(Image image, String predictedLabel, Query.MatchDiagnostic diagnostic) {
        System.out.printf(
                Locale.US,
                "[MISMATCH] expected=%s predicted=%s nearest=%s nearestDistance=%.3f expectedDistance=%.3f expectedThreshold=%.3f image=%s%n",
                image.getLabel(),
                predictedLabel,
                diagnostic.nearestLabel,
                diagnostic.nearestDistance,
                diagnostic.expectedDistance,
                diagnostic.expectedThreshold,
                image.getPathToImage()
        );
    }

    public static void main(String[] args) {
        Evaluator E = new Evaluator();
        E.getMatrixConfusion();
    }


    /**
     * to get the new PCa and normalize if pca change
     * @throws IOException
     */

    private void setANewPca() throws IOException {

        PCA pca = new PCA(resolveExistingDirectory(TRAINING_DIR), MAX_INDIVIDUALS_FOR_TRAINING, false);

        if (pca.getFacesCoordinates().getNbColumns() == 0) {
            throw new AssertionError("Aucune image chargee depuis " + TRAINING_DIR);
        }

        if (pca.getMeanFace() == null) {
            throw new AssertionError("La moyenne des visages n'a pas ete calculee");
        }

        Matrix projectedFaces = pca.getProjectedFacesOnKeptAxes();

        if (projectedFaces == null) {
            throw new AssertionError("Les images n'ont pas ete projetées dans l'espace ACP");
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

        Map<String, List<Vector>> dataBase = pca.getMapSign();

        System.out.println(
                "testPcaStartsFromTrainingDirectory reussi : "
                        + pca.getFacesCoordinates().getNbColumns()
                        + " images chargees depuis "
                        + TRAINING_DIR
                        + " avec "
                        + MAX_INDIVIDUALS_FOR_TRAINING
                        + " individus max"
                        + ", "
                        + pca.getNumberOfKeptAxes()
                        + " axes gardes"
        );

        this.pca = pca;
        this.dataBase = dataBase;
        this.query = new Query(DEFAULT_THRESHOLD);
    }

    private String predict(Image image) throws IOException {
        if (pca == null || dataBase == null) {
            throw new IllegalStateException("PCA and database must be initialized before prediction");
        }

        Vector projectedImage = pca.projectVector(image.getPixels());
        return query.findBestMatch(projectedImage, dataBase);
    }

    /**
     * to load of the personn
     * @param sourceDir
     * @param maxIndividualsToLoad
     * @return
     * @throws IOException
     */

    private List<Image> loadImages(String sourceDir, int maxIndividualsToLoad) throws IOException {
        File root = new File(resolveExistingDirectory(sourceDir));
        File[] personneFolders = root.listFiles(File::isDirectory);

        if (personneFolders == null) {
            throw new IOException("cannot access '" + sourceDir + "': No such file or directory");
        }

        List<Image> images = new ArrayList<>();
        //sort he person folders to get the same then in pca at 0 we get the person with id 1 etc ..
        Arrays.sort(personneFolders, Comparator.comparingInt(this::personFolderSortValue).thenComparing(File::getName));
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

        if (images.isEmpty()) {
            throw new IOException("No images found in '" + sourceDir + "'");
        }

        return images;
    }

    /**
     *
     * @param folder
     * @return
     */

    private int personFolderSortValue(File folder) {
        String label = Image.labelFromFolderName(folder.getName());
        return label.isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(label);
    }

    /**
     * to get a compatibility between intelij and eclipse some one does not set the source of project
     * @param path
     * @return the true existing path
     * @throws IOException
     */

    private String resolveExistingDirectory(String path) throws IOException {
        File fromCurrentDirectory = new File(path);
        if (fromCurrentDirectory.isDirectory()) {
            return fromCurrentDirectory.getPath();
        }

        //because for someone we have in ..

        File fromParentDirectory = new File("..", path);
        if (fromParentDirectory.isDirectory()) {
            return fromParentDirectory.getPath();
        }

        // ADD A NEW CONDITION IF IS NOT WORKING

        throw new IOException("cannot access '" + path + "': No such file or directory PLEASE RESOLVE IT IN Evaluator.resolveExistingDirectory");
    }
}
