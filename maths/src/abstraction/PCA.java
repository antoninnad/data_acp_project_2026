package abstraction;


import java.util.ArrayList;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
//reading of the save file
import java.util.Scanner;

import math.Vector;
import math.Matrix;
import abstraction.Image;

public class PCA {
	private int numberOfKeptAxes; // number of axes that are relevant for the PCA
	private Vector meanFace; //mean face found based on our database
	private Matrix facesCoordinates; // coordinates of every face projected into the PCA
    private Matrix eigenfaces; // eigenfaces of the database


	/**
	 * Center images with the mean face
	 *
	 * @param list of images (rezised and greyscale)
	 * @return matrix of centered images
	 * */
	public Matrix centeredImages(List<Image> listImages) {
		Matrix centeredMatrix = new Matrix(listImages.get(0).getPixel().getDimension(), listImages.size());
		for(int i=0; i<listImages.size(); i++) {
			try {
				//substracts pixels to get a centered vector and add to centeredList
				Vector centeredVector = listImages.get(i).getPixel().difference(meanFace);
				//add in the matrix the new centered image
				 for (int j = 0; j < centeredVector.getDimension(); j++) {
					 centeredMatrix.set(j, i, centeredVector.get(j));
				 }
			} catch (IOException e) {
				//case if the file was not found
				e.printStackTrace();
            }
		}
		return centeredMatrix;
	}


	 /** Calculate the mean face based on a list of Images, by averaging pixels by pixels
	 *
	 * @param listImages list of images that has been treated before(resized and greyscale)
	 * @return vector representing the mean face
	 * */
	public Vector getMeanFace(List<Image> listImages) {


		try {
			Vector mean = new Vector(listImages.getFirst().getNumberOfPixel());
			int numberImg = listImages.size();

			// 255 * 300 * 30 a worst do not overpass the max of double in java
			for (Image img : listImages) {
				Vector add = img.getPixel();
				mean = mean.addition(add);
			}

			//normalyse by the number of image
			mean = mean.multiplicationScalar((double) 1.0/numberImg);

			meanFace = mean;
			return mean;

		} catch (IOException e) {
			throw new RuntimeException("Image can be load " + e);
		}

	}

	public static List<Image> getFacesCordonates(String sourceDir) throws IOException {
		File root = new File(sourceDir);
		File[] personneFolders = root.listFiles(File::isDirectory);

		if (personneFolders == null) {
			throw new IOException("Répertoire introuvable : " + sourceDir);
		}

		List<Image> images = new ArrayList<>();

		for (File personneFolder : personneFolders) {
			if (!personneFolder.getName().matches("\\d+")) {
				continue;
			}

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


	/**
	 * Saves the informations regarding the PCA to avoid recalculating too often
	 * @param filename is the name of the file where the informations are saved
	 */

}
