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
            int personneId = Integer.parseInt(personneFolder.getName());

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
                images.add(new Image(converted.getAbsolutePath(), personneFolder.getName()));
            }
        }

        return images;
    }
}