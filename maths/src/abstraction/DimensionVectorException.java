package abstraction;

/**
 * Unchecked exception thrown when two vectors are expected to have the same dimension
 * but do not. Used to guard arithmetic operations such as dot products or distance
 * computations that require operands of identical size.
 */
public class DimensionVectorException extends RuntimeException {

    /**
     * Constructs a new exception with an explanatory message.
     *
     * @param message human-readable description of the dimension mismatch
     */
    public DimensionVectorException(String message) {
        super(message);
    }
}
