package PTI.Rs232Validator.Utility;

public class StringExtensions {

    public static String AddSpacesToCamelCase(String str) {
        return str.replaceAll("(?<=[a-z])(?=[A-Z0-9])|(?<=[A-Z])(?=[A-Z][a-z])", " ").toLowerCase();
    }
}
