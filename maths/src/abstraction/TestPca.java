package abstraction;

import math.Vector;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class TestPca {

    public static void main(String[] args) {
        TestMeanFaces();
    }

    public static void TestMeanFaces() {
        try {
            List<Image> images = List.of(
                    new Image("./Celeba_HQ_facial_identity_dataset/test/5/91.jpg", "5"),
                    new Image("./Celeba_HQ_facial_identity_dataset/test/5/822.jpg", "5"),
                    new Image("./Celeba_HQ_facial_identity_dataset/test/5/8222.jpg", "5"),
                    new Image("./Celeba_HQ_facial_identity_dataset/test/5/9770.jpg", "5"),
                    new Image("./Celeba_HQ_facial_identity_dataset/test/5/9787.jpg", "5")
            );

            PCA pca = new PCA();
            Vector meanFace = pca.getMeanFace(images);

            File outputDir = new File("./testImg");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            Image.toImage(meanFace, "./testImg/test.jpg");

            System.out.println("TestMeanFaces passé : image moyenne enregistrée dans ./abstraction/test.jpg");

        } catch (IOException e) {
            System.err.println("TestMeanFaces non passé : erreur d'entrée/sortie");
            e.printStackTrace();
        } catch (RuntimeException e) {
            System.err.println("TestMeanFaces non passé : erreur dans getMeanFace");
            e.printStackTrace();
        }
    }
}