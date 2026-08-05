package PTI.Rs232Validator.Utility;

/**
 * A generic class to create Tuples
 * @param <X> Class of the 1st entry
 * @param <Y> Class of the 2nd entry
 */
public class Tuple<X, Y> {

    public final X x;
    public final Y y;
    public Tuple(X x, Y y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Checks if the Tuple object is equivalent to another
     * @param tuple The Tuple object to check equivalence against
     * @return {@true} if the two Tuples are equivalent; otherwise, {@code false}
     */
    public boolean isEqual(Tuple<X, Y> tuple) {
        return (x == tuple.x) && (y == tuple.y);
    }
}
