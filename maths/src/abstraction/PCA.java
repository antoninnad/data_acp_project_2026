package abstraction;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
//reading of the save file
import java.util.Scanner;

import math.Matrix;
import math.Vector;

public class PCA {
	private int maxNbOfKeptAxes = 0;  // maximum number of axes having been computed (use for calculus optimisation)
	private int numberOfKeptAxes; // number of axes that are relevant for the PCA
	private Vector meanFace; //mean face found based on our database
	private Matrix facesCoordinates; // coordinates (pixel value) of every face : must remain untouched after centring 
	private Matrix projectedFaces; // Coordinates of the images in the eigenspace
    private Matrix eigenfaces; // eigenfaces of the database
	private Vector eigenvalues; // Necessary if we want to plot the eigenvalue graph "dynamically"
	private List<Image> images;  // List of all the images considered
	private final static String sourceDir = "";
    private final static String filename = ".PCAsave";
    private final static double eigensumThreshold = 0.8;


	public PCA() throws IOException {
    	
    	// Reading the data from the database (either file or folder)
    	
    	readDB(); // Maybe only if we manually change the number of axes
    	int index = 0;  // number of columns
		
		// Fetching all the pixels of the image and storing them in a Matrix
   		facesCoordinates = new Matrix(images.get(0).getPixels().getDimension(), images.size());    	
		for (Image img : images) {
			facesCoordinates.setColumn(index, img.getPixels());
			index += 1;
		}


		// Computing or reading the PCA data (eigen elements) depending on the save file's existence
    	try {
    		
    		// Trying to load data from the save file if it exists
    		loadFromFile();

    	} catch (IOException file_error) {
    		
    		// If it doesn't exist, compute the data (the new images base)

    		// Centring all the images (faceCoordinates) with the mean face
    		this.centreImages();
			
    		// Creating the .jpg images associated to centred faces
    		Image.centeredVectorToImage(meanFace, "meanface.jpg");
    		for (int i=0; i<facesCoordinates.getNbColumns(); i++) {
    			Image.centeredVectorToImage(facesCoordinates.getColumn(i), i+".jpg");
    		}

    		
    		// Calculating the covariance matrix
    		cov = facesCoordinates.covariateMatrix();
    		
    		// Fetching the eigenvalues and computing the necessary number of axes (depending on eigensumThreshold)
    		eigenvalues = cov.getEigenvalues();
    		computeNumberOfKeptAxes();  // Default value on application start
    		maxNbOfKeptAxes = numberOfKeptAxes;  // Setting the maximum number of axes considered up to now
    		
    		
    		
    		// Calculating the eigenfaces : eigenvectors of the original matrix (the centred faceCoordinates)
    		// We only keep the number of eigenvectors previously computed
    		eigenfaces = facesCoordinates.multiply(cov.getEigenvectors().subMatrixFirstColumns(getNumberOfKeptAxes()-1)); // Computing initial eigenvectors
    		
			// Creating the .jpg eigenfaces
			for (int i=0; i<eigenfaces.getNbColumns(); i++) {
	    		Image.centeredVectorToImage(eigenfaces.getColumn(i), "eigen"+i+".jpg");
	    	}
    		
    		eigenfaces.normColumns(); // norms to one
    		
    		
    		// Projecting every face in the new eigenspace
    		for (int i=0; i<facesCoordinates.getNbRows(); i++) {
    			projectedFaces = eigenfaces.transpose().multiply(facesCoordinates); // [nbOfKeptAxes x nbImages]
    		}
    		
    		
    		// Saving the computed data to the files : should be done on app termination
			//saveToFile();
    		
    	}


	
	public int getNumberOfKeptAxes() {
    	return numberOfKeptAxes;
    }

	public Matrix getProjectedFacesOnKeptAxes() {
    	return projectedFaces;
    }

	public Matrix getFacesCordonates() {
		return this.facesCoordinates;
	}

	public Vector getMeanFace() {
		return meanFace;
	}
	
	
	// TODO
	public void computeNumberOfKeptAxes() {
    	numberOfKeptAxes = 2;    	
    }

	

	/**
     * Reads every image from the database and adds them to images list attribute
     * @throws IOException
     */
    public void readDB() throws IOException {
    	
    	File root = new File(sourceDir);
		File[] personneFolders = root.listFiles(File::isDirectory);

		// Checking the directory isn't empty
		if (personneFolders == null) {
			throw new IOException("cannot access '"+sourceDir+"': No such file or directory");
		}

		images = new ArrayList<>(); // Creating a new object to contain the images

		// Looking in every folder
		for (File personneFolder : personneFolders) {
			if (!personneFolder.getName().matches("\\d+")) {
				continue;
			}

			
			int personneId = Integer.parseInt(personneFolder.getName());

			// Fetching images from a sub-folder
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
				
				// Adding the image to the list
				images.add(new Image(converted.getAbsolutePath(), personneFolder.getName()));
			}
		}
    }

	/**
	 * Centres images with the mean face
	 *
	 * @param list of images (resized and greyscale)
	 * @return matrix of centred images
	 * */
	public void centreImages() throws IOException {
		
		// Computing the mean face
		this.computeMeanFace();
		for(int i=0; i<images.size(); i++) {

				//add in the matrix the new centred image
				 facesCoordinates.setColumn(i, centredVector(facesCoordinates.getColumn(i)));
				
		}
	}
	

	public Vector centredVector(Vector v) {

		if (meanFace == null) {
			throw new RuntimeException("Vector mean face is null");
		}

		return v.difference(meanFace);
	}


	/** Calculate the mean face based on a list of Images, by averaging pixels by pixels
	 *
	 * @param listImages list of images that has been treated before(resized and greyscale)
	 * @return vector representing the mean face
	 * */
	public void computeMeanFace() {


		try {
			meanFace = new Vector(images.getFirst().getNumberOfPixel());
			int numberImg = images.size();

			// 255 * 300 * 30 at worst does not overpass the max of double in java
			for (Image img : images) {
				Vector add = img.getPixels();
				meanFace = meanFace.addition(add);
			}

			//normalise by the number of image
			meanFace = meanFace.multiplicationScalar((double) 1.0/numberImg);


		} catch (IOException e) {
			throw new RuntimeException("Image can be load " + e);
		}

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
			writer.println(this.projectedFaces.getNbRows() + " " + this.projectedFaces.getNbColumns());
			for (int i = 0 ; i < this.projectedFaces.getNbRows(); i++ ) {
				for (int j = 0; j < this.projectedFaces.getNbColumns(); j++) {
					writer.println(this.projectedFaces.get(i,j) + " ");
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
			System.out.println("The informations concerning the PCA have been to " + filename);
		} catch (IOException e) {
			System.err.println("Error while trying to save the informations" + e.getMessage());
		}

	}

	/**
	 * Loads the informations regarding the PCA to avoid recalculating too often
	 * @param filename is the name of the file where the informations are saved
	 */
	public void loadFromFile() {
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
                 this.projectedFaces = new Matrix(rowsOfFaces, colsOfFaces);
                 for (int i = 0; i < rowsOfFaces; i++) {
                     for (int j = 0; j < colsOfFaces; j++) {
                         this.projectedFaces.set(i, j, scanner.nextDouble());
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
