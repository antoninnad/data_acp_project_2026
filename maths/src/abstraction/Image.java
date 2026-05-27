package abstraction;

import app.Main;
import math.Matrix;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import math.Vector;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.imageio.ImageIO;

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
     * to get numbe of pixel
     * @return number of pixel
     * @throws IOException exception lauch by ImageIo
     */

    int getNumberOfPixel() throws IOException {
        BufferedImage image = ImageIO.read(new File(this.pathToImage));

        int width = image.getWidth();
        int heigth = image.getHeight();

        return  width * heigth;
    }

    /**
     * méthode qui parcours chaque pixel d'une image et les mets dans un vecteur
     * @return vecteur avec chaque composante qui correspont à un pixel
     */
    Vector getPixel() throws IOException {


        //reading the image
        BufferedImage image = ImageIO.read(new File(this.pathToImage));

        int width = image.getWidth();
        int heigth = image.getHeight();

        //for example if image is 10x10 we have 100 pixels
        Vector result = new Vector(width * heigth);

        // to obtain a vector with all the coposante represanting a pixel of image
        for (int y = 0; y < heigth; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);

                double rouge = (rgb >> 16) & 0xFF;
                double vert = (rgb >> 8) & 0xFF;
                double bleu = rgb & 0xFF;

                result.set(y * width + x, (rouge + vert + bleu) / 3);


            }
        }

        return result;

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

    /**
     * Vector to image jpeg
     * @param imgVectorized
     * @param pathToSave
     * @throws IOException
     */

    public static void toImage(Vector imgVectorized, String pathToSave) throws IOException {

        int dimension = Math.toIntExact(Math.round(Math.sqrt(imgVectorized.getDimension())));
        BufferedImage image = new BufferedImage(
                dimension,
                dimension,
                BufferedImage.TYPE_BYTE_GRAY
        );

        //set pixel by pixel
        for (int y = 0; y < dimension; y++) {
            for (int x = 0; x < dimension; x++) {

                int index = y * dimension + x;

                //grey to 0 to 255
                int grey = (int) Math.round(imgVectorized.get(index));

                if (grey < 0) {
                    grey = 0;
                }

                if (grey > 255) {
                    grey = 255;
                }

                int rgb = (grey << 16) | (grey << 8) | grey;

                image.setRGB(x, y, rgb);
            }
        }

        //saving

        ImageIO.write(image, "jpg", new File(pathToSave));


    }

    /**
     * Converts an image to 64x64 grayscale PNG format
     * and saves it using the convention: person_XXX/img_YY.png
     *
     * @param inputPath   path to the source image
     * @param personneId    person identifier (e.g. 1 → "001")
     * @param imageNum    image number (e.g. 3 → "03")
     * @return the converted and saved image file
     */
    private static final String BASE_DIR = "./Celeba_HQ_facial_identity_dataset/train";

    public static File convertAndSave(String inputPath, int personneId, int imageNum) throws IOException {

        BufferedImage original = ImageIO.read(new File(inputPath));
        if (original == null) {
            throw new IOException("Impossible de lire l'image : " + inputPath);
        }

        BufferedImage grayscale = new BufferedImage(64, 64, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = grayscale.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(original, 0, 0, 64, 64, null);
        g2d.dispose();

        String personneFolder = String.format("personne_%03d", personneId);
        String fileName = String.format("img_%02d.jpg", imageNum);  // ← .jpg

        File outputDir = new File(BASE_DIR + File.separator + personneFolder);
        if (!outputDir.exists()) outputDir.mkdirs();

        File output = new File(outputDir, fileName);
        ImageIO.write(grayscale, "jpg", output);  // ← "jpg"

    return output;
}

    public static void main(String[] args) throws FileNotFoundException {

        boolean isOkay = true;

        try {
            Image t = new Image("./Celeba_HQ_facial_identity_dataset/test/5/91.jpg", "test");

            try {
                Vector a = t.getPixel();
                Image.toImage(a, "./test.jpg");

            } catch (IOException e) {
                isOkay = false;
            }
        } catch (FileNotFoundException f) {
            isOkay = false;
        }

        if (!isOkay) {
            System.out.println("Test non passé");
        }
    }
}
