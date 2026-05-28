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
	private int maxNbOfKeptAxes;
	private int numberOfKeptAxes; // number of axes that are relevant for the PCA
	private Vector meanFace; //mean face found based on our database
	private Matrix facesCoordinates; // coordinates of every face projected into the PCA
	private Matrix projectedFaces;  // REMINDER : vector i in projectedFaces is associated to image i from images (to get label)
    private Matrix eigenfaces; // eigenfaces of the database
    private List<Image> images;
    private final static String sourceDir = "";
    private final static String filename = "";
    
    public PCA() throws IOException {
    	
    	// Reading the data from the database (either file or folder)
    	
    	readDB();
    	
    	try {
    		loadFromFile();
    		
    	} catch (IOException file_error) {
    		
    		int index = 0;
    		getMeanFace();
    		centerImages();
    		facesCoordinates = new Matrix(images.get(0).data.getDimension(), images.size());
    		for (Image img : images) {
    			facesCoordinates.setColumn(i, img.data.difference(meanFace));
    			index += 1;
    		}
    		
    		Matrix cov = facesCoordinates.covariateMatrix();
    		computeNbOfKeptAxes();  // Automatic method
    		
    		maxNbOfKeptAxes = numberOfKeptAxes;
    		
    		
    	
    		
    		
    		
    		// We only keep the number of eigenvectors previously decided
    		eigenfaces = facesCoordinates.multiply(cov.getEigenvectors().subMatrixFirstColumns(getNumberOfKeptAxes())); // Computing initial eigenvectors
    		eigenfaces.normColumns(); // (1) default
    		for (int i=0; i<facesCoordinates.getNbRows(); i++) {
    			projectedFaces = eigenfaces.transpose().multiply(facesCoordinates); // [nbOfKeptAxes x nbImages]
    			// Do a setRows(Matrix) in order to keep  the rest of the facesCoordinates data, allowing to quickly change the number
    			// of axes one want's to consider
    		}
    		
    		
    		
    	}
    	
    	
    }
    
    public void updateToNewAxes() {
    	if (maxNbOfKeptAxes < numberOfKeptAxes) {
    		eigenfaces.addColumns(facesCoordinates.multiply(cov.getEigenvectors().subMatrixFirstColumns(subRows(fromMaxNbAxes, toNbAxes)))); // Computing initial eigenvectors
    		eigenfaces.normColumns(from, to);
    		projectedFaces.addRows(eigenfaces.transpose().subRows(fromMaxNbAxes, toNbAxes).multiply(facesCoordinates));
    		
    		maxNbOfKeptAxes = numberOfKeptAxes;
    	}
    }
    
    
    
    
    public Matrix getProjectedFacesOnKeptAxes() {
    	return facesCoordinates.sub(nbOfKeptAxes);
    }
    
    
    
    public void getNumberOfKeptAxes() {
    	return (numberOfKeptAxes);
    }
    
    public void readDB() throws IOException {
    	File root = new File(sourceDir);
		File[] personneFolders = root.listFiles(File::isDirectory);

		if (personneFolders == null) {
			throw new IOException("Répertoire introuvable : " + sourceDir);
		}

		images = new ArrayList<>();

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
    }
    
    
    
	/**
	 * Center images with the mean face
	 *
	 * @param list of images (rezised and greyscale)
	 * @return matrix of centered images
	 * */
	public Matrix centerImages() {
		Matrix centeredMatrix = new Matrix(images.get(0).getPixel().getDimension(), images.size());
		for(int i=0; i<images.size(); i++) {
			try {
				//substracts pixels to get a centered vector and add to centeredList
				Vector centeredVector = images.get(i).getPixel().difference(meanFace);
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
	public Vector getMeanFace() {


		try {
			Vector mean = new Vector(images.getFirst().getNumberOfPixel());
			int numberImg = images.size();

			// 255 * 300 * 30 a worst do not overpass the max of double in java
			for (Image img : images) {
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

	public Matrix getFacesCordonates() {
		return this.facesCoordinates;
	}


	/**
	 * Saves the informations regarding the PCA to avoid recalculating too often
	 * @param filename is the name of the file where the informations are saved
	 */
	public void saveToFile() {
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
	 * Loads the informations regarding the PCA to avoid recalculating too often
	 * @param filename is the name of the file where the informations are saved
	 */
	public void loadFromFile() throws IOException {
		Scanner scanner = new Scanner(new BufferedReader(new FileReader(filename)));
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

	}

}