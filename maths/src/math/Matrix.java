package math;

import math.exception.DimensionMismatchException;
import math.exception.NoDataException;
import math.exception.NullArgumentException;
import math.exception.OutOfRangeException;
import math.linear.EigenDecomposition;
import math.linear.MatrixUtils;
import math.linear.RealMatrix;



/**
 * The class Matrix represents a mathematical real matrix and allows to perform some basic operation on this object.
 * Matrix actually is implemented as a translator between our application and the Appache Common Maths (ACM) library.
 * It also allows to access the eigen decomposition, if it exists, of this matrix. However, the eigen decomposition
 * is computed (only once) only if asked for.
 */
public class Matrix {
	
	
	private RealMatrix matrix;						// Associated (ACM) matrix
	private EigenDecomposition diagonalisation;		// ACM object to compute and store diagonalisaton data
	
	/**
     * Creates a Matrix of the specified dimensions.
     * It initialises the ACM objects that this object will communicate with.
     * @param rows number of rows of the matrix
     * @param columns number of columns of the matrix
     */
	public Matrix(int nbRows, int nbCol) {
		matrix = MatrixUtils.createRealMatrix(nbRows, nbCol);
		diagonalisation = null;
	}
 
	


	/**
     * Creates a matrix whose entries are the the values in the
     * the input array.
     * It initialises the ACM objects that this object will communicate with.
     * @param data input array
     */
    public Matrix(double[][] data) throws NullArgumentException,
    		DimensionMismatchException, NoDataException {
    	matrix = MatrixUtils.createRealMatrix(data);
    	diagonalisation = null;
    }
    
    
    /**
     * Creates a matrix whose entries are the the values in the
     * the input array.
     * It initialises the ACM objects that this object will communicate with.
     * @param m associated RealMatrix object
     */
    public Matrix(RealMatrix m) throws NullArgumentException {
    	if (m == null) {
    		throw new NullArgumentException();
    	}
		this.matrix = m;
		diagonalisation = null;
	}
    
    
    /**
     * Getter for the RealMatrix associated to this object
     * @return matrix matrix attribut of this object
     */
    public RealMatrix getRealMatrix() {
    	return this.matrix;
    }
	
    
    /**
     * Performs the matrix multiplication between this matrix (considered being
     * on the left side) and the parsed one (considered being on the right side).
     * @param m A matrix object to be on the right of the multiplication.
     * @return
     * @throws DimensionMismatchException
     */
    public Matrix multiply(Matrix m) throws DimensionMismatchException {
    	return new Matrix(this.matrix.multiply(m.getRealMatrix()));
    }

    
    /**
     * Computes the transpose matrix of this object
     * @return A Matrix of switched dimensions
     */
    public Matrix transpose() {
    	return new Matrix(this.matrix.transpose());
    }
    
    /**
     * Computes the trace of this matrix object
     * @return A double being the sum of the diagonal
     */
    public double trace() {
    	return (this.matrix.getTrace());
    }
    
    
    /**
     * Predicat checking if this is a square matrix
     * @return A boolean indicating if it's a square matrix.
     */
    public boolean isSquare() {
    	return (this.matrix.isSquare());
    }
    
    
    /**
     * Fetched and returns the number of rows in this matrix.
     * @return An integer being the number of rows
     */
    public int getNbRows() {
    	return (this.matrix.getRowDimension());
    }
    
    
    /**
     * Fetched and returns the number of columns in this matrix.
     * @return An integer being the number of columns
     */
    public int getNbColumns() {
    	return (this.matrix.getColumnDimension());
    }
    
   
    
    /**
     * Fetched and returns the element of the matrix of coordinate (i,j)
     * @param i Row of the element to return
     * @param j Column of the element to return
     * @return A double, the (i,j) coefficient of the matrix.
     */
    public double get(int i, int j) throws OutOfRangeException {
    	return (this.matrix.getEntry(i, j));
    }
    
    
    /**
     * Sets the element of the matrix of coordinate (i,j) to the given value
     * @param i Row of the element to return
     * @param j Column of the element to return
     * @param value Value to insert at the position (i,j) of the matrix.
     */
    public void set(int i, int j, double value) throws OutOfRangeException {
    	this.matrix.setEntry(i, j, value);
    	diagonalisation = null;
    }
    
    
    /**
     * Initialises the decomposition attribut of the matrix, computing its eigen elements
     * @throws DimensionMismatchException
     * if the matrix isn't a square matrix, the eigen decomposition doesn't exist
     */
    private void eigenDecompose() throws DimensionMismatchException {
    	if (!isSquare()) {
    		throw new DimensionMismatchException(getNbRows(), getNbColumns());
    	}
    	diagonalisation = new EigenDecomposition(this.matrix);
    }
    
    
    /**
     * Returns, if they exist, the eigenvalues of the matrix.
     * @return A Vector containing the eigen values sorted from greatest to smallest
     */
    public Vector getEigenvalues() throws DimensionMismatchException {
    	eigenDecompose();
    	return new Vector(diagonalisation.getRealEigenvalues());
    }
    
    
    /**
     * Returns, if they exist, the eigenvectors of the matrix.
     * @return A Matrix the eigen vof which the columns are the eigen vectors sorted
     * in the same order as their corresponding eigen values on {@link getEigenvalues()}
     */
    public Matrix getEigenvectors() throws DimensionMismatchException {
    	eigenDecompose();
    	return new Matrix(diagonalisation.getV());
    }
    
    
    /**
     * Extracts and returns a column of the matrix.
     * @param i index of the Column to extract
     * @return A Vector object containing the elements of the i-th columns
     * @throws OutOfRangeException
     * if the index is equal or greater than the number of columns
     */
    public Vector getColumn(int i) throws OutOfRangeException {
    	if (i >= getNbColumns()) {
    		throw new OutOfRangeException(i, 0, getNbColumns()-1);
    	}
    	return new Vector(this.matrix.getColumn(i));
    }
    
    
    /**
     * Extracts and returns a row of the matrix.
     * @param i index of the row to extract
     * @return A Vector object containing the elements of the i-th row
     * @throws OutOfRangeException
     * if the index is equal or greater than the number of rows
     */
    public Vector getRow(int i) throws OutOfRangeException {
    	if (i >= getNbRows()) {
    		throw new OutOfRangeException(i, 0, getNbRows()-1);
    	}
    	return new Vector(this.matrix.getRow(i));
    }
    
    
    public void setColumn(int i, Vector vector) throws OutOfRangeException {
    	this.matrix.setColumn(i, vector.getData());
    }
    
    public void setColumn(int i, double[] column) throws OutOfRangeException {
    	this.matrix.setColumn(i, column);
    }


	/**
	 * Calculate the covariate matrix
	 * @param Matrix containing centered images (dimension = nxp where n>p)
	 * @return covariate matrix (dimension = pxp)
	 * */
	public Matrix covariateMatrix() throws DimensionMismatchException {
		// Computes the transposed matrix
		Matrix transposedMatrix = this.transpose();
		// Computes the covariate matrix by multiplying imagesMatrix with its transposed matrix
		return transposedMatrix.multiply(this);

	}	
    
    /**
	 * Norms the columns of the Matrix (of RealMatrix) to 1
	 * */
    public void normColumns() {
    	normColumns(0,getNbColumns()-1);
    }

	/**
	 * Norms the given columns of the Matrix (of RealMatrix) to 1
	 * @param start Column where to start norming
	 * @param end Column where to stop norming
	 * */
	public void normColumns(int start, int end) throws OutOfRangeException {
    	for (int i=start; i<end; i++) {
    		setColumn(i, getColumn(i).normalise());
    	}
    }

	/**
	 * Concantenates a Matrix to the right of this Matrix.
	 * @param m Matrix to concatenate to the right
	 * @result matrix (RealMatrix) now contains the columns of m
	 * @throws DimensionMismatchException
	 * if the number of rows of the two matrices are different, conatenation
	 * is impossible.
	 * */
    public void addColumns(Matrix m) throws DimensionMismatchException {
    	
    	if (m.getNbRows() != this.getNbRows()){
    		throw new DimensionMismatchException(m.getNbRows(),  this.getNbRows());
    	}
    	
    	RealMatrix extendedMatrix = matrix.createMatrix(this.getNbRows(), this.getNbColumns()+m.getNbColumns());
    	extendedMatrix.setSubMatrix(this.matrix.getData(), 0, 0);
    	extendedMatrix.setSubMatrix(m.getRealMatrix().getData(), 0, this.getNbColumns());
    	this.matrix = extendedMatrix;
    }

	/**
	 * Concantenates a Matrix to the bottom of this Matrix.
	 * @param m Matrix to concatenate to the bottom
	 * @result matrix (RealMatrix) now contains the rows of m
	 * @throws DimensionMismatchException
	 * if the number of columns of the two matrices are different, conatenation
	 * is impossible.
	 * */
    public void addRows(Matrix m) throws DimensionMismatchException {
    	
    	if (m.getNbColumns() != this.getNbColumns()){
    		throw new DimensionMismatchException(m.getNbColumns(),  this.getNbColumns());
    	}
    	
    	RealMatrix extendedMatrix = matrix.createMatrix(this.getNbRows()+m.getNbRows(), this.getNbColumns());
    	extendedMatrix.setSubMatrix(this.matrix.getData(), 0, 0);
    	extendedMatrix.setSubMatrix(m.getRealMatrix().getData(), this.getNbRows(), 0);
    	this.matrix = extendedMatrix;
    }
    
    /**
	 * Creates a submatrix containing the rows a this matrix whose index are
	 * in the range given by the parameter.
	 * @param start Index (from 0) from which we start copying rows
	 * @param start Index (from 0) at which we stop copying rows
	 * @return A Matrix object containing the given rows is returned
	 * @throws DimensionMismatchException
	 * if the indexes are incompatible, an error is thrown
	 * */
    public Matrix getSubRows(int start, int end) throws OutOfRangeException {
    	return new Matrix(matrix.getSubMatrix(start, end, 0, this.getNbColumns()-1));
    }

	
	/**
	 * Creates a submatrix containing the columns a this matrix whose index are
	 * in the range given by the parameter.
	 * @param start Index (from 0) from which we start copying columns
	 * @param start Index (included, from 0) at which we stop copying columns
	 * @return A Matrix object containing the given columns is returned
	 * @throws DimensionMismatchException
	 * if the indexes are incompatible, an error is thrown
	 * */
    public Matrix getSubColumns(int start, int end) throws OutOfRangeException {
    	return new Matrix(matrix.getSubMatrix(0,  this.getNbColumns()-1, start, end));
    }
    

	/**
	 * Creates a submatrix containing the columns a this matrix up to a given index
	 * @param start Index (included, from 0) at which we stop copying columns
	 * @return A Matrix object containing the given columns is returned
	 * @throws DimensionMismatchException
	 * if the indexes are incompatible, an error is thrown
	 * */
    public Matrix subMatrixFirstColumns(int colLimit) throws OutOfRangeException {
    	return this.getSubColumns(0, colLimit);
    }

	
    /**
     * Converts a Matrix to a String
     * @return A String object containing the printed Matrix
     */
    @Override
    public String toString() {
    	return this.matrix.toString();
    }
    
    
    /**
     * Computes and returns the hash code of this Matrix.
     * @return int the hash code of this object
     */
    @Override
    public int hashCode() {
    	return (this.matrix.hashCode()+this.diagonalisation.hashCode())/2;
    }
    


    /**
     * Converts a column Matrix (dimensions being (n,1) to a Vector object
     * @return a vector dimension n
     */
    public Vector matrixToVector() {

        if (getNbColumns() != 1) {
            throw new RuntimeException("Matrix shold be nx1 to be transform to a vector");
        }

        Vector result = new Vector(getNbRows());

        for (int i = 0; i < getNbRows(); i++) {
            result.set(i, get(i, 0));
        }

        return result;
    }



}







































