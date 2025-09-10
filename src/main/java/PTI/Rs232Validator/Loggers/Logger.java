package PTI.Rs232Validator.Loggers;


public abstract class Logger implements ILogger{


    protected Logger(String name, LogLevel minLogLevel) {
        Name = name;
        MinLogLevel = minLogLevel;
    }


    public String Name;


    public LogLevel MinLogLevel;


    public void LogTrace(String format, Object... args) {
        if(MinLogLevel.ordinal() > LogLevel.Trace.ordinal()){
            return;
        }

        Log(Name, LogLevel.Trace, format, args);
    }


    public void LogDebug(String format, Object... args) {
        if(MinLogLevel.ordinal() > LogLevel.Debug.ordinal()){
            return;
        }

        Log(Name, LogLevel.Debug, format, args);
    }


    public void LogInfo(String format, Object... args) {
        if(MinLogLevel.ordinal() > LogLevel.Info.ordinal()){
            return;
        }

        Log(Name, LogLevel.Info, format, args);
    }


    public void LogError(String format, Object... args) {
        if(MinLogLevel.ordinal() > LogLevel.Error.ordinal()){
            return;
        }

        Log(Name, LogLevel.Error, format, args);
    }


    protected abstract void Log(String Name, LogLevel level, String format, Object... args);
}


