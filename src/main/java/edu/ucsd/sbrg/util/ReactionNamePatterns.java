package edu.ucsd.sbrg.util;

import java.util.regex.Pattern;

/**
 * Defines an enumeration for regex patterns that are used to categorize reactions based on their ID strings.
 * Each enum constant represents a specific type of reaction and is associated with a regex pattern that matches
 * reaction IDs corresponding to that type.
 */
public enum ReactionNamePatterns {

    /**
     * Pattern for ATP maintenance reactions, which are typically denoted by IDs containing 'ATPM' in any case.
     */
    ATP_MAINTENANCE(".*[Aa][Tt][Pp][Mm]"),

    /**
     * Case-insensitive pattern for biomass reactions, matching IDs that include the word 'biomass' in any case.
     */
    BIOMASS_CASE_INSENSITIVE(".*[Bb][Ii][Oo][Mm][Aa][Ss][Ss].*"),

    /**
     * Case-sensitive pattern for biomass reactions, matching IDs that specifically contain 'BIOMASS'.
     */
    BIOMASS_CASE_SENSITIVE(".*BIOMASS.*"),

    /**
     * Pattern for default flux bound reactions, matching IDs that typically start with a prefix followed by 'default_'.
     */
    DEFAULT_FLUX_BOUND("(.*_)?[Dd][Ee][Ff][Aa][Uu][Ll][Tt]_.*"),

    /**
     * Pattern for demand reactions, identified by IDs starting with 'DM_'.
     */
    DEMAND_REACTION("(.*_)?[Dd][Mm]_.*"),

    /**
     * Pattern for exchange reactions, identified by IDs starting with 'EX_'.
     */
    EXCHANGE_REACTION("(.*_)?[Ee][Xx]_.*"),

    /**
     * Pattern for sink reactions, which are reactions that remove metabolites from the system, identified by IDs starting with 'SK_' or 'SINK_'.
     */
    SINK_REACTION("(.*_)?[Ss]([Ii][Nn])?[Kk]_.*");

    /**
     * The compiled regex pattern used for matching reaction IDs.
     */
    private final Pattern pattern;

    /**
     * Constructs a new enum constant with the specified regex pattern.
     *
     * @param regex The regex pattern to compile.
     */
    ReactionNamePatterns(String regex) {
        pattern = Pattern.compile(regex);
    }

    /**
     * Retrieves the compiled Pattern object for this enum constant.
     *
     * @return The compiled Pattern object.
     */
    public Pattern getPattern() {
        return pattern;
    }
}
