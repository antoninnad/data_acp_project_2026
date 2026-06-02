package abstraction;

import math.Matrix;
import math.Vector;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class Evaluator {

    private static final String TRAINING_DIR = "./data_filtred/train";
    private static final int MAX_IMAGES_FOR_TEST = 20;
    private PCA pca;


    public void getMatrixConfusion() {

        try {
            setANewPca();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (pca == null) {
            throw new AssertionError("Pca wasn't set");
        }

    }

    public static void main(String[] args) {
        Evaluator E = new Evaluator();
        E.getMatrixConfusion();
    }


    private void setANewPca() throws IOException {
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

        this.pca = pca;
    }
}
