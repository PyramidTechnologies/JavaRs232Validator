package PTI.Rs232Validator.Utility;

@FunctionalInterface
public interface Action{
    void accept(Object... args);
}
