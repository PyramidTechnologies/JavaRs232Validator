package PTI.Rs232Validator.Loggers;

public abstract class NamedLogger<T> extends Logger {

    protected NamedLogger(Class<T> c, LogLevel minLogLevel) {
        super(c.getName(), minLogLevel);
    }
}
