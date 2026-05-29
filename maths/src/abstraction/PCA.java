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

	public void setMeanFace(Vector meanFace) {
		this.meanFace = meanFace;
	}

	public Vector getMeanFace() {
		return meanFace;
	}

	public Vector centredVector(Vector v) {
		Vector meanFace = getMeanFace();

		if (meanFace == null) {
			throw new RuntimeException("Vector mean face is null");
		}

		return v.difference(meanFace);
	}

	/**
	 * Center images with the mean face
	 *
	 * @param list of images (rezised and greyscale)
	 * @return matrix of centered images
	 * */
//	public Matrix centeredImages(List<Image> listImages) {
//
//
//
//
//	}


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
	 * Calculate the covariate matrix
	 *
	 * @param Matrix containing centered images (dimension = nxp where n>p)
	 * @return covariate matrix (dimension = pxp)
	 * */
	public Matrix covariateMatrix(Matrix imagesMatrix) {
		int p = imagesMatrix.getNbRows();
		//create a square matrix to stock the covariate matrix
		Matrix covariateMatrix = new Matrix(p,p);
		//calculate the transposed matrix
		Matrix transposedMatrix = imagesMatrix.transpose();
		//calculate the covariate matrix by multiplying imagesMatrix with its transposed matrix
		covariateMatrix = transposedMatrix.multiply(imagesMatrix);
		return covariateMatrix;
	}




	/**
	 * Saves the informations regarding the PCA to avoid recalculating too often
	 * @param filename is the name of the file where the informations are saved
	 */
	public void saveToFile(String filename) {
		try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
			//Save the number of axes kept
			writer.println(this.numberOfKeptAxes);
			writer.println();

			//Save the mean face
			writer.println(this.meanFace.getDimension());
			for (int i = 0 ; i < this.meanFace.getDimension() ; i++) {
				writer.println(this.meanFace.get(i) + " ");

			}
			writer.println();

			//Save the coordinates of the faces
			writer.println();
			writer.println(this.facesCoordinates.getNbRows() + " " + this.facesCoordinates.getNbColumns());
			for (int i = 0 ; i < this.facesCoordinates.getNbRows(); i++ ) {
				for (int j = 0; j < this.facesCoordinates.getNbColumns(); j++) {
					writer.println(this.facesCoordinates.get(i,j) + " ");
				}
				writer.println();
			}

			writer.println();

			//Save the eigenfaces
			writer.println(this.eigenfaces.getNbRows() + " " + this.eigenfaces.getNbColumns());
            for (int i = 0; i < this.eigenfaces.getNbRows(); i++) {
                for (int j = 0; j < this.eigenfaces.getNbColumns(); j++) {
                    writer.print(this.eigenfaces.get(i, j) + " ");
                }
                writer.println();
            }
            writer.println();
			System.out.println("The informations concerning the PCA are saved to " + filename);
		} catch (IOException e) {
			System.err.println("Error while trying to save the informations" + e.getMessage());
		}

	}
	

	/**
	 * Calculate the covariate matrix 
	 * 
	 * @param matrix containing centered images
	 * @return covariate matrix (different dimension
	 * */
	
//	private Matrix covariateMatrix(Matrix images) {
//
//	}
	

	/**
	 * Loads the informations regarding the PCA to avoid recalculating too often
	 * @param filename is the name of the file where the informations are saved
	 */
	public void loadFromFile(String filename) {
		try (Scanner scanner = new Scanner(new BufferedReader(new FileReader(filename)))) {
			if (scanner.hasNextInt()) {
				//Load numberOfKeptAxes (int)
                this.numberOfKeptAxes = scanner.nextInt();
			}

            //Load dimension of vector mean face and the values
            if (scanner.hasNextInt()) {
                 int meanFaceDim = scanner.nextInt();
                 this.meanFace = new Vector(meanFaceDim);
                 for (int i = 0; i < meanFaceDim; i++) {
                     this.meanFace.set(i, scanner.nextDouble());
                 }
             }

             //Load facesCoordinates matrix
             if (scanner.hasNextInt()) {
                 int rowsOfFaces = scanner.nextInt();
                 int colsOfFaces = scanner.nextInt();
                 this.facesCoordinates = new Matrix(rowsOfFaces, colsOfFaces);
                 for (int i = 0; i < rowsOfFaces; i++) {
                     for (int j = 0; j < colsOfFaces; j++) {
                         this.facesCoordinates.set(i, j, scanner.nextDouble());
                     }
                 }
             }

             //Load eigenfaces
             if (scanner.hasNextInt()) {
                 int rowsOfEigenfaces = scanner.nextInt();
                 int colsOfEigenfaces = scanner.nextInt();
                 this.eigenfaces = new Matrix(rowsOfEigenfaces, colsOfEigenfaces);
                 for (int i = 0; i < rowsOfEigenfaces; i++) {
                     for (int j = 0; j < colsOfEigenfaces; j++) {
                         this.eigenfaces.set(i, j, scanner.nextDouble());
                     }
                 }
             }

             System.out.println("The informations concerning the PCA are successfully loaded from " + filename);

		} catch (IOException e) {
			System.err.println("Error while trying to load the informations" + e.getMessage());
		}
	}

}
