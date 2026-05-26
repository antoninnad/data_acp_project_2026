package math;

/**
 * Vector is an array of one dimension that allows mathematical operations
 */
public class Vector {

	
	// Declaration of a one-dimensional array representing a vector
    private double[] data;
    // Declaration of the size of the vector for security
    private int dimension;

    /**
     * Constructs a new vector with a given dimension
     * @param dimension is the size of the vector
     */
    public Vector(int dimension) {
        this.data = new double[dimension];
        this.dimension = dimension;
    }

    /**
     * Constructs a new vector by copying an array
     * @param data is an array (doubles)
     */
    public Vector(double[] data) {
    	// Make a deep copy instead of a shallow one to avoid memory issues 
    	this.data = data.clone();
    	this.dimension = data.length;
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
     * Gets the size of the vector 
     * @return an integer
     */
    public int getDimension() {
    	return this.dimension;
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
	
	/**
	 * Calculates the difference between two vector 
	 * @param v the other vector
	 * @return a new vector who is the difference between the vectors
	 */
	public Vector difference(Vector v) {
		
		if (this.data.length != v.data.length) {
			//throw();
		}
		
		Vector diffVector = new Vector(this.data.length);
		for (int i = 0 ; i < this.data.length ; i++) {
			diffVector.set(i, this.get(i) - v.get(i));
		}
		
		return diffVector;
	}
	
	/**
	 * Calculates the euclidean distance between two vectors
	 * @param v the other vector
	 * @return the distance 
	 */
	public double distance(Vector v) {
        // the norm of the difference 
        return this.difference(v).norm();
    }
	
	/**
	 * Calculates the dot product between the current vector and an other
	 * @param v the other vector
	 * @return the scalar result
	 */
	public double dotProduct(Vector v) {
		
		if (this.data.length != v.data.length) {
			//throw();
		}
		
		double result = 0 ;
		for (int i = 0 ; i < this.data.length ; i++) {
			result = result + this.data[i] * v.get(i);
		}
		return result;
	}
	
	/**
	 * Returns a vector
	 * @return a display of a vector
	 */
	@Override
    public String toString() {
		String display = "[";
    	
    	for (int i = 0 ; i < this.data.length ; i++) {
			display += data[i];
			
			// Add a comma between the numbers except the last one
			if (i < this.data.length - 1) {
				display += ", ";
			}
		}
		
		display += "]";
		return display;
    }
	
}
