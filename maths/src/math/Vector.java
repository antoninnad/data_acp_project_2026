package math;

/**
 * Vector is an array of one dimension that allows mathematical operations
 */
public class Vector {

	
	// Declaration of a one-dimensional array representing a vector
    private double[] data;

    /**
     * Constructs a new vector with a given dimension
     * @param dimension is the size of the vector
     */
    public Vector(int dimension) {
        this.data = new double[dimension];
    }

    /**
     * Constructs a new vector by copying an array
     * @param data is an array (doubles)
     */
    public Vector(double[] data) {
    	// Make a deep copy instead of a shallow one to avoid memory issues 
    	this.data = data.clone();
    }

    /**
     * Gets the value at the index i
     * @param i is the index 
     * @return the value in the array at index i
     */
    public double get(int i) {
        return data[i];
    }

    /**
     * Sets the value at index i
     * @param i is the index 
     * @param value is the new value at index i
     */
    public void set(int i, double value) {
        data[i] = value;
    }

	/**
	 * Calculates the euclidean norm of the vector
	 * @return the norm 
	 */
	public double norm() {
		double sum = 0;
		for (double val : data) {
			// Add the square of val to the sum
			sum = sum + (val * val);
		}
		// Use of the library Math
		return Math.sqrt(sum);
	}
	
	/**
	 * Normalizes each value of the vector using the norm
	 */
	public void normalise() {
		double vectorNorm = this.norm();
		//Avoid the division by 0
		if (vectorNorm > 0) {
			for (int i = 0 ; i < data.length ; i++) {
				// Divide each value of the vector by the norm of the vector
				data[i] = data[i] / vectorNorm;
			}
		}
	}
	
}
