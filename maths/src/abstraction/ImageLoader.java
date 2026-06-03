package abstraction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImageLoader {

    /**
     * convert a directory to 64x64 and grey scale
     * return the directory as a list converted
     * @param sourceDir source directory (ex: "./Celeba_HQ_facial_identity_dataset/train")
     */
    public static List<Image> loadFromDirectory(String sourceDir) throws IOException {
        File root = new File(sourceDir);
        File[] personneFolders = root.listFiles(File::isDirectory);

        if (personneFolders == null) {
            throw new IOException("Répertoire introuvable : " + sourceDir);
        }

        List<Image> images = new ArrayList<>();

        for (File personneFolder : personneFolders) {
            String label = Image.labelFromFolderName(personneFolder.getName());
            if (label.isEmpty()) {
                continue;
            }

            int personneId = Integer.parseInt(label);

            File[] files = personneFolder.listFiles(f ->
                f.getName().endsWith(".jpg") || f.getName().endsWith(".png")
            );

            if (files == null) continue;

            for (int i = 0; i < files.length; i++) {
                // utilise convertAndSave de Image
                File converted = Image.convertAndSave(
                    files[i].getAbsolutePath(),
                    personneId,
                    i + 1
                );
                images.add(new Image(converted.getAbsolutePath(), label));
            }
        }

        return images;
    }

    /**
     * Add a person with their images from a folder to an existing filtered image list
     * @param personFolder path to the folder containing images for one person
     * @param personneId ID of the person
     * @param filteredImages list of images to add the person's images to
     * @return number of images added
     * @throws IOException if the folder cannot be read
     */
    public static int addPerson(String personFolder, int personneId, List<Image> filteredImages) throws IOException {
        File folder = new File(personFolder);
        
        if (!folder.isDirectory()) {
            throw new IOException("Le dossier n'existe pas ou n'est pas valide : " + personFolder);
        }

        File[] files = folder.listFiles(f ->
            f.getName().endsWith(".jpg") || f.getName().endsWith(".png")
        );

        if (files == null) {
            return 0;
        }

        int count = 0;
        for (int i = 0; i < files.length; i++) {
            try {
                // Convert and save the image
                File converted = Image.convertAndSave(
                    files[i].getAbsolutePath(),
                    personneId,
                    i + 1
                );
                // Add to filtered images list
                filteredImages.add(new Image(converted.getAbsolutePath(), String.format("%03d", personneId)));
                count++;
            } catch (IOException e) {
                System.err.println("Erreur lors du traitement de l'image : " + files[i].getAbsolutePath());
                e.printStackTrace();
            }
        }

        return count;
    }
}
