package abstraction;

import java.util.List;
import math.Vector;
import math.Matrix;
import abstraction.Image;

public class PCA {
	private int numberOfKeptAxes; // number of axes that are relevant for the ACP
	private Vector meanFace; //mean face found based on our database
	
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
	

}
