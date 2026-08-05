package PTI.Rs232Validator.Loggers;

/**
 * An implementation of {@link ILogger} that does nothing
 */
public class NullLogger implements ILogger{

    /**
     * {@inheritDoc}
     */
    @Override
    public void LogTrace(String format, Object... args) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void LogDebug(String format, Object... args) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void LogInfo(String format, Object... args) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void LogError(String format, Object... args) {

    }
}
