package abstraction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import math.Vector;
import math.Matrix;
import abstraction.Image;

public class PCA {
	private int numberOfKeptAxes; // number of axes that are relevant for the ACP
	private Vector meanFace; //mean face found based on our database
	
	/**
	 * Calculate the mean face based on a list of images
	 * 
	 * @param list of images that has been treated before(resized and greyscale)
	 * @return vector representing the mean face
	 * */
	private Vector getMeanFace(List<Image> listImages) {
		double mean;
		for(int j = 0; j < listImages.get(0).getPixel().getDimension(); j++ ) {
			for(int i = 0; i< listImages.size(); i++) {
				/*Sum pixels*/
				mean+=listImages.get(i).getPixel().get(j);
			}
			meanFace.add(mean/listImages.size());
			mean = 0;		
		}
		return meanFace;
	}
	

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

}
	
