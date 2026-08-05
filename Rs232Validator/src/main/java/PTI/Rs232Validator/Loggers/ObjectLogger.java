package PTI.Rs232Validator.Loggers;

/**
 * An implementation of {@link NamedLogger} that uses the name of the generic type as the name of the logger.
 * @param <T> The class to log for
 */
public abstract class ObjectLogger<T> extends NamedLogger {

    /**
     * Initializes a new instance of {@link ObjectLogger}
     * @param c The class to log for
     * @param minLogLevel {@link NamedLogger#MinLogLevel MinLogLevel}
     */
    protected ObjectLogger(Class<T> c, LogLevel minLogLevel) {
        super(c.getSimpleName(), minLogLevel);
    }
}
