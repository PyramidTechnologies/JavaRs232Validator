package PTI.Rs232Validator.Utility;

/**
 * A container of utility methods for {@link String}.
 */
public class StringUtils {

    /**
     * Adds spaces between words in the specified camelCase or PascalCase String
     * @param str The String to mutate
     * @return The mutated String
     */
    public static String AddSpacesToCamelCase(String str) {
        return str.replaceAll("(?<=[a-z])(?=[A-Z0-9])|(?<=[A-Z])(?=[A-Z][a-z])", " ").toLowerCase();
    }
}
