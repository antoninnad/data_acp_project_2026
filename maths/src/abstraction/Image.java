package abstraction;

import math.Matrix;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Vector;

public class Image {


    String pathToImage;
    Matrix matrixChangingBase;
    String label;



    /**
     * @param pathToImage path to the dataset that we want to use
     */
    Image(String pathToImage, String label) throws FileNotFoundException {

        this.pathToImage = pathToImage;
        this.label = label;
        Path folderPath = Path.of(pathToImage);

        if (!Files.exists(folderPath)) {
            throw new FileNotFoundException("The image " + folderPath + " does not exist");
        }

    }

    /**
     * méthode qui parcours chaque pixel d'une image et les mets dans un vecteur
     * @return vecteur avec chaque composante qui correspont à un pixel
     */
    Vector<Double> getPixel() {

    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return this.label;
    }
}
