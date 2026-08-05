package PTI.Rs232Validator.Loggers;

/**
 * A generic logging interface
 */
public interface ILogger {

    /**
     * Logs a message at the trace level
     * @param format The format of the message
     * @param args An array of objects to format
     */
    public void LogTrace(String format, Object... args);

    /**
     * Logs a message at the debug level
     * @param format The format of the message
     * @param args An array of objects to format
     */
    public void LogDebug(String format, Object... args);

    /**
     * Logs a message at the info level
     * @param format The format of the message
     * @param args An array of objects to format
     */
    public void LogInfo(String format, Object... args);

    /**
     * Logs a message at the error level
     * @param format The format of the message
     * @param args An array of objects to format
     */
    public void LogError(String format, Object... args);
}
