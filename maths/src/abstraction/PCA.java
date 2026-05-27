package abstraction;

import java.util.List;
//reading of the save file
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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
	 * Calculate the mean face based on a list of Images, by averaging pixels by pixels
	 * 
	 * @param listImages list of images that has been treated before(resized and greyscale)
	 * @return vector representing the mean face
	 * */
	public Vector getMeanFace(List<Image> listImages) {
		double mean; // mean of pixels 
		for(int j = 0; j < Image.getDimension() ; j++) { //j : index for pixels
			for(int i = 0; i < listImages.size(); i++) { // i : index for images 
				/*Sum pixels*/
				mean+=listImages.get(i).getPixel().get(j);
			}
			/*add the mean in the of the pixels in meanFace */
			meanFace.add(mean/listImages.size());
			mean = 0;			
		}
		return meanFace
	}
	
	centeredImages()
	

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
