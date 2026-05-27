package abstraction;

import java.io.IOException;
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

			return mean;

		} catch (IOException e) {
			throw new RuntimeException("Image can be load ");
		}

	}



}
