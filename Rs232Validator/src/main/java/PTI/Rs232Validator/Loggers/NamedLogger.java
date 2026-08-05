package PTI.Rs232Validator.Loggers;

/**
 * An implementation of {@link ILogger} that has a name and logs certain messages
 */
public abstract class NamedLogger implements ILogger{

    /**
     * Creates a new instance of {@link NamedLogger}
     * @param name {@link #Name}.
     * @param minLogLevel {@link #MinLogLevel}.
     */
    protected NamedLogger(String name, LogLevel minLogLevel) {
        Name = name;
        MinLogLevel = minLogLevel;
    }

    /**
     * The name of this instance
     */
    public String Name;

    /**
     * The minimum log level a message must be to be logged.
     */
    public LogLevel MinLogLevel;

    /**
     * {@inheritDoc}
     */
    public void LogTrace(String format, Object... args) {
        if(MinLogLevel.ordinal() > LogLevel.Trace.ordinal()){
            return;
        }

        Log(Name, LogLevel.Trace, format, args);
    }

    /**
     * {@inheritDoc}
     */
    public void LogDebug(String format, Object... args) {
        if(MinLogLevel.ordinal() > LogLevel.Debug.ordinal()){
            return;
        }

        Log(Name, LogLevel.Debug, format, args);
    }

    /**
     * {@inheritDoc}
     */
    public void LogInfo(String format, Object... args) {
        if(MinLogLevel.ordinal() > LogLevel.Info.ordinal()){
            return;
        }

        Log(Name, LogLevel.Info, format, args);
    }

    /**
     * {@inheritDoc}
     */
    public void LogError(String format, Object... args) {
        if(MinLogLevel.ordinal() > LogLevel.Error.ordinal()){
            return;
        }

        Log(Name, LogLevel.Error, format, args);
    }

    /**
     * Logs a specified message at the specified log level
     * @param Name {@link #Name}.
     * @param level The log level of the message
     * @param format The format of the message
     * @param args An array of objects to format
     */
    protected abstract void Log(String Name, LogLevel level, String format, Object... args);
}


