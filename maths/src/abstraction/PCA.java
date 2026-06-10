package abstraction;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import java.util.*;
//reading of the save file

import math.Matrix;
import math.Vector;


/**
 * PCA is the class responsible for setting up the "work environment" we will use to compare images.
 * It does this by simply making the Principal Component Analysis and switching the database's images' coordinates
 * to the new basis. Its attributes represent the necessary data for further image analysis (especially eigen elements
 * and image coordinates). It will read the database itself and save the computed data in a file to spare the cost
 * of recalculating everything on launch of the application.
 */
public class PCA {
	private int maxNbOfKeptAxes = 0;  // Maximum number of axes having been computed (use for calculus optimisation)
	private int numberOfKeptAxes;     // Number of axes to be considered in the image analysis process
	private Vector meanFace;          // Mean face of every image in the database
	private Matrix facesCoordinates;  // Coordinates (pixel value) of every known face (Note : must remain untouched after centring)
	private Matrix projectedFaces;    // Coordinates of the images in the considered eigenspace
	private Matrix eigenfaces;        // Eigenfaces of the database
	private List<Image> images;       // List of all the images considered
	private Matrix cov;
	private Vector pixelMeans;
	private Vector pixelStandardDeviations;
	private final String sourceDir;
	private final int maxIndividualsToLoad;
	private final boolean writeDebugImages;
	private final static String defaultSourceDir = "data_filtred3/train";
	private final static String filename = ".PCAsave";
	private final static double keptInertiaThreshold = 0.85;
	private final static int maxPowerIterations = 100;
	private final static double powerIterationTolerance = 1e-6;
	private final static int skippedLeadingAxes = 3;


	/**
	 * Default constructor. Starts the PCA using the default folder path.
	 * It loads saved data if it exists, or calculates everything from scratch.
	 * @throws IOException If the files or folders cannot be read.
	 */
	public PCA() throws IOException {
		this(resolveExistingDirectory(defaultSourceDir));
	}

	/**
	 * Constructor that sets up the PCA using a  folder path.
	 * @param sourceDir The path to the folder containing the images.
	 * @throws IOException If the folder cannot be accessed.
	 */
	public PCA(String sourceDir) throws IOException {
		this(sourceDir, 0, true);
	}

	/**
	 * Constructor that lets you limit how many people/folders to load from the database
	 * @param sourceDir The path to the folder containing the images
	 * @param maxIndividualsToLoad The maximum number of people to load (use 0 for all)
	 * @throws IOException If reading files goes wrong
	 */
	public PCA(String sourceDir, int maxIndividualsToLoad) throws IOException {
		this(sourceDir, maxIndividualsToLoad, true);
	}

	/**
	 * The main constructor that runs the entire PCA process
	 * It reads the images, standardizes the pixels, finds the best mathematical 
	 * features (eigenfaces), and projects the images into the new coordinate system
	 * @param sourceDir The path to the folder containing the images
	 * @param maxIndividualsToLoad The maximum number of people to load (use 0 for all)
	 * @param writeDebugImages Set to true to save visual helper images (like the mean face) to disk
	 * @throws IOException If reading folders or writing debug files fails
	 */
	public PCA(String sourceDir, int maxIndividualsToLoad, boolean writeDebugImages) throws IOException {
		this.sourceDir = sourceDir;
		this.maxIndividualsToLoad = maxIndividualsToLoad;
		this.writeDebugImages = writeDebugImages;

		/****** Reading the data from the database ******/
		reportProgress("lecture du dossier source : " + sourceDir);
		readDB();
		reportProgress(images.size() + " images trouvees");

		/****** Fetching all the pixels of the images and storing them in a Matrix ******/
		reportProgress("chargement des pixels");
		Vector firstImagePixels = images.get(0).getPixels();
		facesCoordinates = new Matrix(firstImagePixels.getDimension(), images.size());
		facesCoordinates.setColumn(0, firstImagePixels);
		reportItemProgress("images chargees", 1, images.size());

		for (int index = 1; index < images.size(); index++) {
			Image img = images.get(index);
			facesCoordinates.setColumn(index, img.getPixels());
			reportItemProgress("images chargees", index + 1, images.size());
		}
		reportProgress("pixels charges dans la matrice " + facesCoordinates.getNbRows() + "x" + facesCoordinates.getNbColumns());
		reportProgress("centrage-reduction des pixels sur le train");
		standardizeTrainingPixels();



		/****** Computing or reading the PCA data (eigen elements) depending on the save file's existence ******/
		try {
			if (maxIndividualsToLoad > 0) {
				throw new IOException("Partial PCA test run: skip save file");
			}

			/*--- Trying to load data from the save file if it exists ---*/
			reportProgress("chargement des donnees ACP depuis " + filename);
			loadFromFile();

		} catch (IOException file_error) {

			/*--- If the save file cannot be read, we compute the PCA data (the new images basis) ---*/
			reportProgress("calcul ACP complet");

			// Centring all the images (facesCoordinates) with the mean face
			centreImages();

			// Creating the .jpg images associated to centred faces
			if (writeDebugImages) {
				new File("debug/visages").mkdirs();
				Image.centeredVectorToImage(meanFace, "debug/meanface.jpg");
				for (int i=0; i<facesCoordinates.getNbColumns(); i++) {
					Image.centeredVectorToImage(facesCoordinates.getColumn(i), "debug/visages/" + i + ".jpg");
				}
			}
			// Computing the covariance matrix
			reportProgress("calcul de la matrice de covariance");
			cov = facesCoordinates.covariateMatrixWithProgress(128);

			// Fetching the dominant eigenvectors and computing the necessary number of axes.
			reportProgress("calcul iteratif des meilleurs axes propres");
			EigenSelection eigenSelection = computeDominantEigenvectors();
			numberOfKeptAxes = eigenSelection.eigenvectors.getNbColumns();
			maxNbOfKeptAxes = numberOfKeptAxes;  // Setting the maximum number of axes considered up to now
			reportProgress(numberOfKeptAxes + " axes gardes");



			/****** Calculating the eigenfaces : eigenvectors of the centred facesCoordinates ******/

			// Computing the original eigenvectors
			reportProgress("calcul des eigenfaces");
			eigenfaces = facesCoordinates.multiply(eigenSelection.eigenvectors); // We only keep the number of eigenvectors previously computed

			// Creating the .jpg eigenfaces
			if (writeDebugImages) {
				new File("debug/eigenfaces").mkdirs();
				for (int i=0; i<eigenfaces.getNbColumns(); i++) {
					Image.centeredVectorToImage(eigenfaces.getColumn(i), "debug/eigenfaces/eigen" + i + ".jpg");
				}
			}

			// Norming the eigenvectors
			reportProgress("normalisation des eigenfaces");
			eigenfaces.normColumns();


			// Projecting every face in the new eigenspace
			reportProgress("projection des images d'entrainement");
			projectedFaces = eigenfaces.transpose().multiply(facesCoordinates); // [nbOfKeptAxes x nbImages]
			reportProgress("ACP terminee");


			// Saving the computed data to the files : should be done on app termination
			//saveToFile();

		}
	}

	/**
	 * Prints a status check to the console
	 * @param message Description text summarizing the currently executed state
	 */
	private void reportProgress(String message) {
		System.out.println("[ACP] " + message);
	}

	/**
	 * Prints a step-by-step counter to the console (e.g., "images loaded : 100/500")
	 * @param label The name of what we are tracking
	 * @param done The current progress number
	 * @param total The target end number
	 */
	private void reportItemProgress(String label, int done, int total) {
		if (done == total || done % 100 == 0) {
			System.out.println("[ACP] " + label + " : " + done + "/" + total);
		}
	}

	/**
	 * Finds the most important mathematical directions (eigenvectors) from the 
	 * covariance matrix. It loops repeatedly until it captures enough image information
	 * @return An EigenSelection object holding the important features
	 */
	private EigenSelection computeDominantEigenvectors() {
		int dimension = cov.getNbColumns();
		int maxAxes = Math.min(100, dimension);
		double totalEnergy = Math.max(cov.trace(), 0.0);
		Matrix eigenvectors = new Matrix(dimension, maxAxes);
		double[][] keptVectors = new double[maxAxes][];
		double cumulativeEnergy = 0.0;
		int keptAxes = 0;
		Random random = new Random(0);

		if (totalEnergy == 0.0) {
			eigenvectors.setColumn(0, new Vector(new double[dimension]));
			return new EigenSelection(eigenvectors.subMatrixFirstColumns(0));
		}

		for (int axis = 0; axis < maxAxes; axis++) {
			double[] vector = randomUnitVector(dimension, random);
			orthogonalize(vector, keptVectors, keptAxes);
			normalise(vector);
			int iterations = 0;

			for (; iterations < maxPowerIterations; iterations++) {
				double[] nextVector = cov.getRealMatrix().operate(vector);
				orthogonalize(nextVector, keptVectors, keptAxes);

				double norm = norm(nextVector);
				if (norm == 0.0) {
					break;
				}

				scale(nextVector, 1.0 / norm);
				double alignment = Math.abs(dot(vector, nextVector));
				vector = nextVector;

				if (1.0 - alignment < powerIterationTolerance) {
					break;
				}

				if ((iterations + 1) % 10 == 0) {
					double progress = 100.0 * (axis + (iterations + 1.0) / maxPowerIterations) / maxAxes;
					System.out.printf(Locale.US,
							"[ACP] axes propres : %.2f%% (axe %d/%d, iteration %d/%d)%n",
							progress,
							axis + 1,
							maxAxes,
							iterations + 1,
							maxPowerIterations
					);
				}
			}

			double eigenvalue = dot(vector, cov.getRealMatrix().operate(vector));
			if (eigenvalue <= 0.0) {
				break;
			}

			keptVectors[keptAxes] = vector;
			eigenvectors.setColumn(keptAxes, new Vector(vector));
			keptAxes++;
			cumulativeEnergy += eigenvalue;

			double energyPercent = 100.0 * cumulativeEnergy / totalEnergy;
			double progress = 100.0 * keptAxes / maxAxes;
			System.out.printf(Locale.US,
					"[ACP] axes propres : %.2f%% (axe %d/%d calcule, energie %.2f%%, iterations %d)%n",
					progress,
					keptAxes,
					maxAxes,
					energyPercent,
					iterations + 1
			);

			if (cumulativeEnergy / totalEnergy >= keptInertiaThreshold) {
				break;
			}
		}

		return new EigenSelection(eigenvectors.subMatrixFirstColumns(Math.max(0, keptAxes - 1)));
	}

	/**
	 * Creates a starting vector filled with random numbers
	 * @param dimension How long the vector needs to be (number of elements)
	 * @param random The random number generator to use
	 * @return An array of doubles representing the random unit vector
	 */
	private double[] randomUnitVector(int dimension, Random random) {
		double[] vector = new double[dimension];

		for (int i = 0; i < vector.length; i++) {
			vector[i] = random.nextDouble() - 0.5;
		}

		normalise(vector);
		return vector;
	}

	/**
	 * Cleans up a vector by removing components that overlap with features 
	 * we already calculated. This keeps all features separate and unique.
	 * @param vector      The vector we want to clean up.
	 * @param keptVectors The list of unique vectors we already saved.
	 * @param keptAxes    How many vectors are currently in the saved list.
	 */
	private void orthogonalize(double[] vector, double[][] keptVectors, int keptAxes) {
		for (int axis = 0; axis < keptAxes; axis++) {
			double projection = dot(vector, keptVectors[axis]);

			for (int i = 0; i < vector.length; i++) {
				vector[i] -= projection * keptVectors[axis][i];
			}
		}
	}

	/**
	 * its mathematical length equals exactly 1.0
	 * @param vector The vector array to rescale in place
	 */
	private void normalise(double[] vector) {
		double norm = norm(vector);

		if (norm > 0.0) {
			scale(vector, 1.0 / norm);
		}
	}

	/**
	 * Multiplies every single number inside a vector by a specific scaling multiplier
	 * @param vector The vector array to modify
	 * @param scale The number to multiply by
	 */
	private void scale(double[] vector, double scale) {
		for (int i = 0; i < vector.length; i++) {
			vector[i] *= scale;
		}
	}

	/**
	 * Calculates the average values and variations for every pixel across all training images
	 * This math is needed to standardize the data before comparing images
	 */
	private void standardizeTrainingPixels() {
		int rows = facesCoordinates.getNbRows();
		int columns = facesCoordinates.getNbColumns();
		double[] means = new double[rows];
		double[] variances = new double[rows];

		for (int row = 0; row < rows; row++) {
			double sum = 0.0;

			for (int column = 0; column < columns; column++) {
				sum += facesCoordinates.get(row, column);
			}

			means[row] = sum / columns;
		}

		for (int row = 0; row < rows; row++) {
			double variance = 0.0;

			for (int column = 0; column < columns; column++) {
				double centeredValue = facesCoordinates.get(row, column) - means[row];
				variance += centeredValue * centeredValue;
			}

			variances[row] = Math.sqrt(variance / columns);
		}

		pixelMeans = new Vector(means);
		pixelStandardDeviations = new Vector(variances);

		for (int column = 0; column < columns; column++) {
			facesCoordinates.setColumn(column, standardizeWithTrainingPixels(facesCoordinates.getColumn(column)));
			reportItemProgress("images standardisees", column + 1, columns);
		}
	}

	/**
	 * Standardizes an incoming image vector using the average stats calculated from the training data
	 * @param pixels The raw image pixels vector to adjust
	 * @return A new normalized vector where pixel values are balanced
	 * @throws RuntimeException If the training statistics averages are missing
	 */
	private Vector standardizeWithTrainingPixels(Vector pixels) {
		if (pixelMeans == null || pixelStandardDeviations == null) {
			throw new RuntimeException("Pixel standardization statistics are not initialized");
		}

		double[] standardizedData = new double[pixels.getDimension()];

		for (int i = 0; i < pixels.getDimension(); i++) {
			double standardDeviation = pixelStandardDeviations.get(i);
			standardizedData[i] = standardDeviation == 0.0
					? 0.0
					: (pixels.get(i) - pixelMeans.get(i)) / standardDeviation;
		}

		return new Vector(standardizedData);
	}

	/**
	 * Calculates the norm of a vector
	 * @param vector The vector to measure
	 * @return The calculated length as a double
	 */
	private double norm(double[] vector) {
		return Math.sqrt(dot(vector, vector));
	}

	/**
	 * Calculates the dot product of two vectors 
	 * @param first  The first vector array
	 * @param second The second vector array
	 * @return The final combined scalar sum
	 */
	private double dot(double[] first, double[] second) {
		double result = 0.0;

		for (int i = 0; i < first.length; i++) {
			result += first[i] * second[i];
		}

		return result;
	}

	/**
	 * A simple private helper class used to temporarily group and return 
	 * a matrix of calculated features.
	 */
	private static class EigenSelection {
		private final Matrix eigenvectors;

		private EigenSelection(Matrix eigenvectors) {
			this.eigenvectors = eigenvectors;
		}
	}



	/**
	 * Getter for the numberOfKeptAxes attribute
	 * @return Returns the number of considered axes of the new basis
	 */
	public int getNumberOfKeptAxes() {
		return Math.max(0, this.numberOfKeptAxes - getSkippedLeadingAxes());
	}

	/**
	 * Getter for the keptInertiaThreshold attribute
	 * @return Returns the inertia threshold
	 */
	public static double getKeptinertiathreshold() {
		return keptInertiaThreshold;
	}

	/**
	 * Getter for the projectedFaces attribute
	 * @return Returns the matrix containing the coordinate of the faces in the new basis
	 */
	public Matrix getProjectedFacesOnKeptAxes() {
		int firstKeptAxis = getSkippedLeadingAxes();
		return projectedFaces.getSubRows(firstKeptAxis, numberOfKeptAxes-1);
	}

	private int getSkippedLeadingAxes() {
		return Math.min(skippedLeadingAxes, Math.max(0, numberOfKeptAxes - 1));
	}


	/**
	 * Getter for the facesCoordinates attribute
	 * @return Returns the matrix containing the coordinate of the faces in the original basis
	 */
	public Matrix getFacesCoordinates() {
		return this.facesCoordinates;
	}

	/**
	 * Getter for the meanFace attribute
	 * @return Returns the mean face of the database
	 */
	public Vector getMeanFace() {
		return this.meanFace;
	}


	/**
	 * To get the map with key it's the label of the personn and the values are the projection of the person with the matrix
	 * @return A linked mapping matching people labels to lists of their projected face vectors.
	 */
	public Map<String, List<Vector>> getMapSign() {

		Map<String, List<Vector>> resultat = new LinkedHashMap<>();
		Matrix projectedFacesOnKeptAxes = getProjectedFacesOnKeptAxes();

		for (int i = 0;i != projectedFacesOnKeptAxes.getNbColumns();i++) {
			String label = images.get(i).getLabel();
			resultat.computeIfAbsent(label, key -> new ArrayList<>()).add(projectedFacesOnKeptAxes.getColumn(i));
		}


		return resultat;

	}


	/**
	 * Getter for the eigenvalues attribute
	 * @return Returns a vector containing the eigenvalues ordered in
	 * descending order
	 */
	public Vector getEigenValues() {
		return this.cov.getEigenvalues();
	}


	/**
	 * Computes the number axes needed to reach at least the kept inertia threshold.
	 * @return Returns the mean face of the database
	 */
	public void computeNumberOfKeptAxes() {
		computeNumberOfKeptAxes(cov.getEigenvalues());
	}

	/**
	 * Computes the number axes needed to reach at least the kept inertia threshold.
	 * @param eigenvalues
	 */

	public void computeNumberOfKeptAxes(Vector eigenvalues) {
		int maxAxes = Math.min(50, eigenvalues.getDimension());
		double total = 0.0;

		for (int i = 0; i < eigenvalues.getDimension(); i++) {
			total += Math.max(0.0, eigenvalues.get(i));
		}

		if (total == 0.0) {
			numberOfKeptAxes = Math.min(1, maxAxes);
			return;
		}

		Integer[] indexes = getEigenvalueIndexesByDescendingValue(eigenvalues);
		double cumulative = 0.0;
		numberOfKeptAxes = maxAxes;

		for (int i = 0; i < maxAxes; i++) {
			cumulative += Math.max(0.0, eigenvalues.get(indexes[i]));

			if (cumulative / total >= keptInertiaThreshold) {
				numberOfKeptAxes = i + 1;
				return;
			}
		}
	}

	/**
	 * to get The eigneis values sorted to match descending eigenvalues sorting.
	 * @param eigenvectors
	 * @param eigenvalues
	 * @return  A new sorted matrix containing principal components arranged by variance weight.
	 */

	private Matrix getSortedEigenvectors(Matrix eigenvectors, Vector eigenvalues) {
		Integer[] indexes = getEigenvalueIndexesByDescendingValue(eigenvalues);
		Matrix sortedEigenvectors = new Matrix(eigenvectors.getNbRows(), numberOfKeptAxes);

		for (int i = 0; i < numberOfKeptAxes; i++) {
			sortedEigenvectors.setColumn(i, eigenvectors.getColumn(indexes[i]));
		}

		return sortedEigenvectors;
	}

	
	/**
	 * to sort the eigeinvalues from largest eigenvalue to smallest.
	 * @param eigenvalues
	 * @return An array of original indexes sorted by their values in descending order.
	 */

	private Integer[] getEigenvalueIndexesByDescendingValue(Vector eigenvalues) {
		Integer[] indexes = new Integer[eigenvalues.getDimension()];

		for (int i = 0; i < indexes.length; i++) {
			indexes[i] = i;
		}

		Arrays.sort(indexes, (first, second) ->
				Double.compare(eigenvalues.get(second), eigenvalues.get(first))
		);

		return indexes;
	}

	public void setMeanFace(Vector meanFace) {
		this.meanFace = meanFace;
	}

	/**
	 * Reads every image from the database and adds them to images list attribute
	 * @throws IOException
	 */
	public void readDB() throws IOException {

		File root = new File(sourceDir);		// Parent directory of the database
		File[] personneFolders = root.listFiles(File::isDirectory);   // Folders containing the images

		// Checking that the directory isn't empty
		if (personneFolders == null) {
			throw new IOException("cannot access '"+sourceDir+"': No such file or directory");
		}

		images = new ArrayList<>(); // Creating a new object to contain the images
		Arrays.sort(personneFolders, Comparator.comparingInt(this::personFolderSortValue).thenComparing(File::getName));

		int loadedIndividuals = 0;

		// Looking in every folder
		for (File personneFolder : personneFolders) {
			if (maxIndividualsToLoad > 0 && loadedIndividuals >= maxIndividualsToLoad) {
				break;
			}

			String label = Image.labelFromFolderName(personneFolder.getName());
			if (label.isEmpty()) {
				continue;
			}
			// Fetching images from a sub-folder
			File[] files = personneFolder.listFiles(f ->
					f.getName().endsWith(".jpg") || f.getName().endsWith(".png")
			);

			if (files == null || files.length == 0) continue;
			Arrays.sort(files, Comparator.comparing(File::getName));


			for (int i = 0; i < files.length; i++) {
				// Adding the image to the list
				images.add(new Image(files[i].getAbsolutePath(), label));
			}
			loadedIndividuals++;
		}

		if (images.isEmpty()) {
			throw new IOException("No images found in '" + sourceDir + "'");
		}
	}

	private int personFolderSortValue(File folder) {
		String label = Image.labelFromFolderName(folder.getName());
		return label.isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(label);
	}

	/**
	 * Centres images with the mean face
	 *
	 *@throws IOException If file operations inside centering pipeline fail.
	 * */
	public void centreImages() throws IOException {

		// Computing the mean face
		reportProgress("calcul du visage moyen");
		this.computeMeanFace();
		for(int i=0; i<images.size(); i++) {

			//add in the matrix the new centred image
			facesCoordinates.setColumn(i, centredVector(facesCoordinates.getColumn(i)));
			reportItemProgress("images centrees", i + 1, images.size());

		}
	}

	
	/**
	 * Centres a given vector with the mean face
	 * @param v The vector to center
	 * @return The meanFace vector has been substracted from v
	 * */
	public Vector centredVector(Vector v) {

		if (meanFace == null) {
			throw new RuntimeException("Vector mean face is null");
		}

		return v.difference(meanFace);
	}

	/**
	 * Projects a vectorized image into the PCA eigenspace.
	 * @param v image pixels in the original basis
	 * @return image coordinates on the kept PCA axes
	 */
	public Vector projectVector(Vector v) {
		if (eigenfaces == null) {
			throw new RuntimeException("Eigenfaces are not initialized");
		}

		Matrix projectedVector = eigenfaces.transpose().multiply(centredVector(standardizeWithTrainingPixels(v)).VectorToMatrix());
		return projectedVector.getSubRows(getSkippedLeadingAxes(), numberOfKeptAxes - 1).matrixToVector();
	}


	/** Calculate the mean face based on a list of Images, by averaging pixels by pixels
	 * 
	 */
	public void computeMeanFace() {
		if (facesCoordinates == null) {
			throw new RuntimeException("Faces coordinates matrix is null");
		}

		int numberImg = facesCoordinates.getNbColumns();
		double[] meanData = new double[facesCoordinates.getNbRows()];

		for (int column = 0; column < numberImg; column++) {
			Vector imagePixels = facesCoordinates.getColumn(column);
			for (int row = 0; row < imagePixels.getDimension(); row++) {
				meanData[row] += imagePixels.get(row);
			}
		}

		for (int i = 0; i < meanData.length; i++) {
			meanData[i] /= numberImg;
		}

		meanFace = new Vector(meanData);
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
	 * Loads the PCA configuration and calculated data from a file.
	 * @throws IOException if an error occurs while reading the file
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

	}

	/**
	 * Resolves a directory path by checking its existence relative to the current or parent directory
	 * @param path the relative path to resolve
	 * @return the valid existing directory path
	 * @throws IOException if the directory cannot be found in either location
	 */
	private static String resolveExistingDirectory(String path) throws IOException {
		File fromCurrentDirectory = new File(path);
		if (fromCurrentDirectory.isDirectory()) {
			return fromCurrentDirectory.getPath();
		}

		File fromParentDirectory = new File("..", path);
		if (fromParentDirectory.isDirectory()) {
			return fromParentDirectory.getPath();
		}

		throw new IOException("cannot access '" + path + "': No such file or directory");
	}

	/**
	 * Gets the complete raw matrix containing all calculated eigenfaces
	 * @return the full eigenfaces matrix
	 */
	public Matrix getEigenfaces() {
		return eigenfaces;
	}

	/**
	 * Extracts the subset of eigenfaces corresponding to the kept principal components
	 * @return the matrix of active/retained eigenfaces
	 */
	public Matrix getKeptEigenfaces() {
		int first = getSkippedLeadingAxes();
		return eigenfaces.getSubColumns(first, numberOfKeptAxes - 1);
	}

	/**
	 * Gets the average pixel values vector calculated across the training set.
	 * @return the mean pixel values vector
	 */
	public Vector getPixelMeans() {
		return pixelMeans;
	}

	/**
	 * Generates a mapping between unique image labels and their respective file paths.
	 * @return a map linking each unique image label to its image path
	 */
	public Map<String, String> getLabelToImagePath() {
		Map<String, String> result = new LinkedHashMap<>();
		for (Image img : images) {
			result.putIfAbsent(img.getLabel(), img.getPathToImage());
		}
		return result;
	}

	/**
	 * Sets the number of PCA axes to retain
	 * @param userAxes the desired number of principal components to keep
	 */
	public void setNumberOfKeptAxes(int userAxes) {
		int available = maxNbOfKeptAxes > 0 ? maxNbOfKeptAxes : numberOfKeptAxes;
		int maxEffective = Math.max(0, available - skippedLeadingAxes);
		if (maxEffective == 0) return;
		int clamped = Math.max(1, Math.min(userAxes, maxEffective));
		this.numberOfKeptAxes = skippedLeadingAxes + clamped;
	}

	/**
	 * Calculates the maximum allowable number of components that can be safely kept.
	 * @return the maximum number of effective principal axes
	 */
	public int getMaxNumberOfKeptAxes() {
		int available = maxNbOfKeptAxes > 0 ? maxNbOfKeptAxes : numberOfKeptAxes;
		return Math.max(0, available - skippedLeadingAxes);
	}

	/**
	 * The main execution entry point for the PCA application. Resolves the target image 
	 * directory, initializes the Principal Component Analysis, and logs the execution summary
	 *
	 * @param args Command-line arguments
	 * @throws IOException If an error occurs while locating directories or reading files.
	 */
	public static void main(String[] args) throws IOException {
		String sourceDirectory = args.length > 0 ? args[0] : resolveExistingDirectory(defaultSourceDir);
		PCA pca = new PCA(sourceDirectory);
		System.out.println(
				"ACP terminee : "
						+ pca.getFacesCoordinates().getNbColumns()
						+ " images chargees depuis "
						+ sourceDirectory
						+ ", "
						+ pca.getNumberOfKeptAxes()
						+ " axes gardes"
		);
	}

}
