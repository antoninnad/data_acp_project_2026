package abstraction;

import math.Matrix;
import math.Vector;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class TestPca {

    public static void main(String[] args) {
        TestChangingBaseImage();
        TestcentredVector();
    }

    public static void TestcentredVector() {
        PCA pca = new PCA();

        try {
            Image img = new Image("./data_filtred/meanface.jpg", "test");
            img.getPixel();

            pca.setMeanFace(img.getVector());

            System.out.println("meanFace = " + img.getVector());

            Image imgToCentralize = new Image("./data_filtred/test/005/img_01.jpg", "test2");
            imgToCentralize.getPixel(); // IMPORTANT

            System.out.println("v = " + imgToCentralize.getVector());

            Image.centeredVectorToImage(
                    pca.centredVector(imgToCentralize.getVector()),
                    "./test/imgCentralize.jpg"
            );

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public  static  void TestChangingBaseImage() {
        try {

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

            boolean isOkay = true;

            if (result.getDimension() != expected.getDimension()) {
                isOkay = false;
            } else {
                for (int i = 0; i < result.getDimension(); i++) {
                    if (result.get(i) != expected.get(i)) {
                        isOkay = false;
                    }
                }
            }

            if (isOkay) {
                System.out.println("TestchangingBaseImage passé");
            } else {
                System.out.println("TestchangingBaseImage non passé");
                System.out.println("Résultat attendu : " + expected);
                System.out.println("Résultat obtenu  : " + result);
            }

        } catch (RuntimeException e) {
            System.err.println("TestchangingBaseImage non passé : erreur pendant le changement de base");
            e.printStackTrace();
        }
    }

    public static void TestMeanFaces() {
        try {


            PCA pca = new PCA();

            List<Image> images = pca.getFacesCordonates("./data_filtred/test");
            Vector meanFace = pca.getMeanFace(images);

            File outputDir = new File("./data_filtred");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            Image.toImage(meanFace, "./data_filtred/meanface_test.jpg");

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