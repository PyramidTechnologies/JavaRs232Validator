package PTI.Rs232Validator.Loggers;

public interface ILogger {


    public void LogTrace(String format, Object... args);


    public void LogDebug(String format, Object... args);


    public void LogInfo(String format, Object... args);


    public void LogError(String format, Object... args);
}
