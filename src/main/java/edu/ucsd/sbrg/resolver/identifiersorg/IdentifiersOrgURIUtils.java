package edu.ucsd.sbrg.resolver.identifiersorg;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IdentifiersOrgURIUtils {

    public static String addJavaRegexCaptureGroup(String pattern) {
        String idPattern = pattern.replaceAll("\\^|\\$", "");
        idPattern = "(?<id>" + idPattern + ")";
        return idPattern;
    }

    public static String removeHttpProtocolFromUrl(String query) {
        if (query.startsWith("http://") || query.startsWith("https://")) {
            Matcher protocolMatcher = Pattern.compile("^https?://").matcher(query);
            if (protocolMatcher.find()) {
                query = query.replaceAll(protocolMatcher.pattern().pattern(), "");
            }
        }
        return query;
    }


    /**
     * Replaces the identifier placeholder "{$id}" in a URL pattern with a specified regex pattern.
     * This method is designed to facilitate the matching of URLs against a dynamic regex pattern that represents
     * an identifier within an identifiers.org namespace or its child resources.
     * <p> <p>
     * The method first attempts to find the "{$id}" placeholder within the provided URL. If found, it splits the URL
     * around this placeholder and reassembles it with the given regex pattern in place of the placeholder. If the
     * placeholder is not found, the URL is returned as is, but quoted to ensure it is treated as a literal string in regex
     * operations.
     * <p> <p>
     * Note: The placeholder "{$id}" can optionally be surrounded by curly braces, which are considered during the
     * replacement but do not affect the functionality.
     *
     * @param url The URL pattern containing the "{$id}" placeholder. This pattern represents a identifiers.org namespace or a related child namespace.
     * @param pattern The regex pattern that should replace the "{$id}" placeholder in the URL pattern.
     * @return A string representing the URL with the "{$id}" placeholder replaced by the provided regex pattern. If no placeholder is found, the URL is returned unchanged but quoted.
     */
    public static String replaceIdTag(String url, String pattern) {
        Pattern id = Pattern.compile("\\{?\\{\\$id}}?");
        Matcher matcher = id.matcher(url);
        if (!matcher.find()) {
            return Pattern.quote(url);
        }
        String[] parts = url.split(id.pattern());
        String result = Pattern.quote(parts[0]) + pattern;
        if (parts.length == 2) {
            result += Pattern.quote(parts[1]);
        }
        return result;
    }


}
