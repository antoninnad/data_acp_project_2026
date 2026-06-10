package abstraction;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import math.Matrix;
import math.Vector;

/**
 * Represents a single face image loaded from disk and converted to a flat greyscale pixel vector.
 *
 * <p>Images from the CelebA-HQ dataset (178×218) are automatically cropped to a
 * 128×128 square centred on the face and then scaled down to 64×64 pixels, producing
 * a 4096-component vector compatible with {@link PCA}. Images that are already at the
 * target resolution are used as-is.</p>
 *
 * <p>Static utility methods allow converting a pixel vector back to a JPEG file:
 * {@link #toImage} for raw [0-255] vectors and {@link #centeredVectorToImage} for
 * centred PCA vectors whose values can be negative.</p>
 *
 * @see PCA
 */
public class Image {

    private static final int CELEBA_RAW_WIDTH = 178;
    private static final int CELEBA_RAW_HEIGHT = 218;
    private static final int CELEBA_FACE_CROP_LEFT = 25;
    private static final int CELEBA_FACE_CROP_TOP = 65;
    private static final int CELEBA_FACE_CROP_SIZE = 128;
    private static final int CELEBA_FACE_OUTPUT_SIZE = 64;

    String pathToImage;
    Matrix matrixChangingBase;
    String label;
    Vector data;

    /**
     * Constructs an Image directly from an existing pixel vector (no file I/O).
     * Intended for unit tests and internal use.
     *
     * @param data pixel vector representing the image
     */
    Image(Vector data) {
        this.data = data;
    }

    /**
     * Returns the raw pixel vector stored in this image.
     *
     * @return pixel vector, or {@code null} if {@link #getPixels()} has not been called yet
     */
    Vector getVector() {
        return data;
    }

    /**
     * Replaces the pixel vector stored in this image.
     *
     * @param data new pixel vector
     */
    void setVector(Vector data) {
        this.data = data;
    }

    /**
     * Constructs an Image from a file on disk.
     * The file is not read immediately; pixels are loaded lazily via {@link #getPixels()}.
     *
     * @param pathToImage absolute or relative path to the image file (JPG or PNG)
     * @param label       identity label of the person shown in the image (e.g. "018")
     * @throws FileNotFoundException if the file does not exist at the given path
     */
    public Image(String pathToImage, String label) throws FileNotFoundException {

        this.pathToImage = pathToImage;
        this.label = label;
        Path folderPath = Path.of(pathToImage);

        if (!Files.exists(folderPath)) {
            throw new FileNotFoundException("The image " + folderPath + " does not exist");
        }

    }

    /**
     * Returns the total number of pixels in the prepared (cropped and resized) image.
     *
     * @return width × height of the prepared image
     * @throws IOException if the image file cannot be read
     */
    int getNumberOfPixel() throws IOException {
        BufferedImage image = readPreparedImage();

        int width = image.getWidth();
        int heigth = image.getHeight();

        return  width * heigth;
    }

    /**
     * Reads the image from disk, applies the CelebA-HQ crop and resize if needed,
     * and returns a flat greyscale pixel vector in row-major order.
     * Each component is the average of the R, G, B channels in [0.0, 255.0].
     *
     * @return flat pixel vector of length width × height (4096 for a 64×64 image)
     * @throws IOException if the image file cannot be read
     */
    public Vector getPixels() throws IOException {

        BufferedImage image = readPreparedImage();

        int width = image.getWidth();
        int heigth = image.getHeight();

        this.data = new Vector(width * heigth);

        for (int y = 0; y < heigth; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);

                double rouge = (rgb >> 16) & 0xFF;
                double vert = (rgb >> 8) & 0xFF;
                double bleu = rgb & 0xFF;

                data.set(y * width + x, (rouge + vert + bleu) / 3);
            }
        }

        return data;

    }

    /**
     * Reads the image file and applies the CelebA-HQ crop and resize when the
     * raw dimensions match the expected 178×218 format.
     *
     * @return prepared BufferedImage ready for pixel extraction
     * @throws IOException if the file cannot be decoded
     */
    private BufferedImage readPreparedImage() throws IOException {
        BufferedImage image = ImageIO.read(new File(this.pathToImage));

        if (image == null) {
            throw new IOException("Cannot read image '" + this.pathToImage + "'");
        }

        if (isRawCelebAImage(image)) {
            image = image.getSubimage(
                    CELEBA_FACE_CROP_LEFT,
                    CELEBA_FACE_CROP_TOP,
                    CELEBA_FACE_CROP_SIZE,
                    CELEBA_FACE_CROP_SIZE
            );
            image = resize(image, CELEBA_FACE_OUTPUT_SIZE, CELEBA_FACE_OUTPUT_SIZE);
        }

        return image;
    }

    /**
     * Checks whether the image has the raw CelebA-HQ dimensions (178×218).
     *
     * @param image image to inspect
     * @return {@code true} if the image is an unprocessed CelebA-HQ file
     */
    private boolean isRawCelebAImage(BufferedImage image) {
        return image.getWidth() == CELEBA_RAW_WIDTH && image.getHeight() == CELEBA_RAW_HEIGHT;
    }

    /**
     * Scales a BufferedImage to the requested dimensions using bilinear interpolation.
     *
     * @param source source image to scale
     * @param width  target width in pixels
     * @param height target height in pixels
     * @return a new greyscale BufferedImage at the requested size
     */
    private BufferedImage resize(BufferedImage source, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = resized.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        return resized;
    }

    /**
     * Sets the identity label of this image.
     *
     * @param label numeric string identifying the person (e.g. "018")
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Returns the identity label of this image.
     *
     * @return numeric string identifying the person shown in the image
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the file path used to load this image.
     *
     * @return absolute or relative path to the source image file
     */
    public String getPathToImage() {
        return pathToImage;
    }

    /**
     * Extracts a numeric label from a dataset folder name.
     * Accepts bare integer folders ("018") and "personne_NNN" folders.
     *
     * @param folderName name of the folder to parse
     * @return the numeric label as a string, or an empty string if the format is not recognised
     */
    static String labelFromFolderName(String folderName) {
        if (folderName.matches("\\d+")) {
            return folderName;
        }

        if (folderName.matches("personne_\\d+")) {
            return folderName.substring("personne_".length());
        }

        return "";
    }

    /**
     * Returns a short string representation of this image showing its label.
     *
     * @return the identity label of the image
     */
    @Override
    public String toString() {
        return this.label;
    }

    /**
     * Writes a raw pixel vector (values in [0, 255]) to a square JPEG file.
     * Values outside [0, 255] are clamped. The image side length is inferred
     * from the square root of the vector dimension.
     *
     * @param imgVectorized flat greyscale pixel vector (length must be a perfect square)
     * @param pathToSave    destination file path (e.g. "debug/face.jpg")
     * @throws IOException if the file cannot be written
     */
    public static void toImage(Vector imgVectorized, String pathToSave) throws IOException {

        int dimension = Math.toIntExact(Math.round(Math.sqrt(imgVectorized.getDimension())));
        BufferedImage image = new BufferedImage(
                dimension,
                dimension,
                BufferedImage.TYPE_BYTE_GRAY
        );

        for (int y = 0; y < dimension; y++) {
            for (int x = 0; x < dimension; x++) {

                int index = y * dimension + x;

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

        ImageIO.write(image, "jpg", new File(pathToSave));

    }

    /**
     * Writes a centred PCA vector (values can be negative) to a square JPEG file.
     * Pixel values are linearly normalised from [min, max] to [0, 255] so that the
     * full dynamic range of the vector is visible.
     *
     * @param imgVectorized centred pixel vector produced by PCA (length must be a perfect square)
     * @param pathToSave    destination file path (e.g. "debug/eigenfaces/eigen0.jpg")
     * @throws IOException if the file cannot be written
     */
    public static void centeredVectorToImage(Vector imgVectorized, String pathToSave) throws IOException {
        int dimension = Math.toIntExact(Math.round(Math.sqrt(imgVectorized.getDimension())));

        BufferedImage image = new BufferedImage(
                dimension,
                dimension,
                BufferedImage.TYPE_BYTE_GRAY
        );

        double min = imgVectorized.get(0);
        double max = imgVectorized.get(0);

        for (int i = 0; i < imgVectorized.getDimension(); i++) {
            double value = imgVectorized.get(i);

            if (value < min) {
                min = value;
            }

            if (value > max) {
                max = value;
            }
        }

        for (int y = 0; y < dimension; y++) {
            for (int x = 0; x < dimension; x++) {
                int index = y * dimension + x;

                double value = imgVectorized.get(index);

                int grey;

                if (max == min) {
                    grey = 0;
                } else {
                    grey = (int) Math.round(255 * (value - min) / (max - min));
                }

                int rgb = (grey << 16) | (grey << 8) | grey;
                image.setRGB(x, y, rgb);
            }
        }

        ImageIO.write(image, "jpg", new File(pathToSave));
    }

    /**
     * Converts an image to 64×64 greyscale JPEG and saves it under a standardised
     * folder/filename convention ({@code data_filtred/test/NNN/img_MM.jpg}).
     *
     * @param inputPath  path to the source image file
     * @param personneId numeric person identifier (formatted as a zero-padded 3-digit folder name)
     * @param imageNum   image sequence number within the person's folder (formatted as 2 digits)
     * @return the converted and saved output file
     * @throws IOException if the source image cannot be read or the output file cannot be written
     */
    private static final String BASE_DIR = "../data_filtred/test";

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

        String personneFolder = String.format("%03d", personneId);
        String fileName = String.format("img_%02d.jpg", imageNum);

        File outputDir = new File(BASE_DIR + File.separator + personneFolder);
        if (!outputDir.exists()) outputDir.mkdirs();

        File output = new File(outputDir, fileName);
        ImageIO.write(grayscale, "jpg", output);

        return output;
    }

    /**
     * Simple smoke test that loads a CelebA image, extracts its pixel vector,
     * and writes it back as a JPEG to verify the round-trip.
     *
     * @param args unused
     * @throws FileNotFoundException if the hard-coded test image is not found
     */
    public static void main(String[] args) throws FileNotFoundException {

        boolean isOkay = true;

        try {
            Image t = new Image("./Celeba_HQ_facial_identity_dataset/test/5/91.jpg", "test");

            try {
                Vector a = t.getPixels();
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

    /**
     * Projects the image's pixel vector into a new basis defined by the given
     * change-of-basis matrix.
     *
     * @param changeOfBasis matrix whose rows are the new basis vectors
     * @return the image's coordinates in the new basis
     */
    Vector changingBaseImage(Matrix changeOfBasis) {

        Matrix matrixReduction = new Matrix(changeOfBasis.getNbRows(), 1);

        matrixReduction = changeOfBasis.multiply(data.VectorToMatrix());

        data = matrixReduction.matrixToVector();

        return data;
    }
}
