package PTI.Rs232Validator.Utility;

public class Tuple<X, Y> {

    public final X x;
    public final Y y;
    public Tuple(X x, Y y) {
        this.x = x;
        this.y = y;
    }

    public boolean isEqual(Tuple<X, Y> tuple) {
        return x == tuple.x && y == tuple.y;
    }
}
