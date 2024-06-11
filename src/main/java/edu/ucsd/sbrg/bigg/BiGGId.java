package edu.ucsd.sbrg.bigg;

import static java.text.MessageFormat.format;

import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.zbit.util.ResourceManager;


/**
 * Represents a BiGG identifier used to uniquely identify various biological entities
 * such as reactions, metabolites, and genes within the BiGG database. This class provides methods to parse, validate,
 * and manipulate BiGG IDs according to the standards specified in the BiGG database.
 *
 * The BiGG ID typically consists of several parts:
 * - A prefix indicating the type of entity (e.g., 'R' for reaction, 'M' for metabolite, 'G' for gene).
 * - An abbreviation which is the main identifier part.
 * - A compartment code that specifies the cellular location of the metabolite.
 * - A tissue code that indicates the tissue specificity of the identifier, applicable in multicellular organisms.
 *
 * This class also includes methods to create BiGG IDs from strings, validate them against known patterns, and
 * extract specific parts like the compartment code. It supports handling special cases and correcting common
 * formatting issues in BiGG IDs.
 *
 * For a formal description of the structure of BiGG ids see the proposed
 * <a href=
 * "https://github.com/SBRG/bigg_models/wiki/BiGG-Models-ID-Specification-and-Guidelines">
 * BiGG ID specification</a>.
 *
 * @author Andreas Dr&auml;ger
 * @author Thomas Zajac
 */
public class BiGGId {

  /**
   * A {@link Logger} for this class.
   */
  private static final transient Logger logger = Logger.getLogger(BiGGId.class.getName());
  /**
   * Bundle for ModelPolisher logger messages
   */
  private static final transient ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");
  /**
   * First part of BiGG ID, either R, M or G
   */
  private String prefix;
  /**
   * Second part of BiGG ID, matches [a-zA-Z0-9][a-zA-Z0-9_]+[a-zA-Z0-9]
   */
  private String abbreviation;
  /**
   * Third part of BiGG ID, matches [a-z][a-z0-9]? and exists for compartmentalized metabolites
   */
  private String compartmentCode;
  /**
   * Fourth part of BiGG ID, matches [A-Z][A-Z0-9]
   */
  private String tissueCode;


  enum IDPattern {

    ATPM("[Aa][Tt][Pp][Mm]"),
    BIOMASS("(([Rr]_?)?[Bb][Ii][Oo][Mm][Aa][Ss][Ss])(?:_(.+))?"),
    COMPARTMENT("[a-zA-Z][a-zA-Z0-9]?"),
    METABOLITE_SPECIAL("M_(?<abbreviation>[a-zA-Z0-9])_(?<compartment>[a-z][a-z0-9]?)"),
    PSEUDO("(([Rr]_?)?[Ee][Xx]_).*|(([Rr]_?)?[Dd][Mm]_).*|(([Rr]_?)?[Ss]([Ii][Nn])?[Kk]_).*"),
    UNIVERSAL("^(?<prefix>[RMG])_(?<abbreviation>[a-zA-Z0-9][a-zA-Z0-9_]+?)(?:_(?<compartment>[a-z][a-z0-9]?))?"
      + "(?:_(?<tissueCode>[A-Z][A-Z0-9]?))?$");


    private final Pattern pattern;


    IDPattern(String pattern) {
      this.pattern = Pattern.compile(pattern);
    }



    Pattern get() {
      return pattern;
    }
  }


  public BiGGId() {
    super();
  }



  public BiGGId(String id) {
    this();
    parseBiGGId(id);
  }



  public BiGGId(String prefix, String abbreviation, String compartmentCode, String tissueCode) {
    this();
    String id = toBiGGId(prefix, abbreviation, compartmentCode, tissueCode);
    parseBiGGId(id);
  }

  /**
   * Creates a BiGG ID for a metabolite with default correction behavior.
   *
   * @param id The raw metabolite ID string.
   * @return An Optional containing the BiGGId if the ID is non-empty and valid, or an empty Optional otherwise.
   */
  public static Optional<BiGGId> createMetaboliteId(String id) {
    return createMetaboliteId(id, true);
  }


  /**
   * Creates a BiGG ID for a metabolite based on the provided string identifier.
   * This method handles the correction and standardization of the metabolite ID according to BiGG database standards.
   *
   * @param id The raw metabolite ID string.
   * @param correct A boolean flag indicating whether to correct the ID to conform to BiGG standards.
   * @return An Optional containing the BiGGId if the ID is non-empty and valid, or an empty Optional otherwise.
   */
  public static Optional<BiGGId> createMetaboliteId(String id, boolean correct) {
    // Return empty if the input ID is empty
    if (id.isEmpty()) {
      return Optional.empty();
    }
    // Fix the compartment code in the ID
    id = fixCompartmentCode(id);
    // Correct the ID to conform to BiGG standards if required
    if (correct) {
      id = makeBiGGConform(id);
      // Remove leading underscore if present
      if (id.startsWith("_")) {
	id = id.substring(1);
      }
      // Standardize the prefix for metabolites from 'm_' to 'M_'
      if (id.startsWith("m_")) {
	id = id.replaceAll("^m_", "M_");
      } else if (!id.startsWith("M_")) {
	id = "M_" + id;
      }
    }
    // Special handling for one-letter abbreviation metabolites not conforming to the specification, but still
    // present in BiGG
    Matcher metaboliteSpecialCase = IDPattern.METABOLITE_SPECIAL.get().matcher(id);
    if (metaboliteSpecialCase.matches()) {
      BiGGId biggId = new BiGGId();
      biggId.setPrefix("M");
      biggId.setAbbreviation(metaboliteSpecialCase.group("abbreviation"));
      biggId.setCompartmentCode(metaboliteSpecialCase.group("compartment"));
      return Optional.of(biggId);
    } else {
      return Optional.of(new BiGGId(id));
    }
  }


  /**
   * Creates a BiGG ID for a gene using the default correction behavior.
   *
   * @param id The raw gene ID string.
   * @return An Optional containing the BiGGId if the ID is non-empty and valid, or an empty Optional otherwise.
   */
  public static Optional<BiGGId> createGeneId(String id) {
    return createGeneId(id, true);
  }

  /**
   * Creates a BiGG ID for a gene, with an option to correct the ID to conform to BiGG standards.
   *
   * This method first checks if the provided ID is empty, returning an empty Optional if true.
   * If the 'correct' parameter is true, the ID is processed to conform to BiGG standards:
   * - Leading underscores are removed.
   * - The prefix "g_" is replaced with "G_" to standardize the gene ID format.
   * - If the ID does not start with "G_", the prefix "G_" is prepended.
   *
   * @param id The raw gene ID string.
   * @param correct A boolean flag indicating whether to correct the ID to conform to BiGG standards.
   * @return An Optional containing the BiGGId if the ID is non-empty and valid, or an empty Optional otherwise.
   */
  public static Optional<BiGGId> createGeneId(String id, boolean correct) {
    if (id.isEmpty()) {
      return Optional.empty();
    }
    if (correct) {
      id = makeBiGGConform(id);
      if (id.startsWith("_")) {
	id = id.substring(1);
      }
      if (id.startsWith("g_")) {
	id = id.replaceAll("^g_", "G_");
      } else if (!id.startsWith("G_")) {
	id = "G_" + id;
      }
    }
    return Optional.of(new BiGGId(id));
  }

  /**
   * Creates a BiGG ID for a reaction based on the provided string identifier.
   * This method handles the prefix stripping and checks if the reaction is a pseudo-reaction.
   * Depending on these checks, it delegates to the overloaded createReactionId method with appropriate flags.
   *
   * @param id The raw reaction ID string.
   * @return An Optional containing the BiGGId if the ID is non-empty and valid, or an empty Optional otherwise.
   */
  public static Optional<BiGGId> createReactionId(String id) {
    String prefixStripped = "";
    // Strip the prefix if it starts with 'R_' or 'r_'
    if (id.startsWith("R_") || id.startsWith("r_")) {
      prefixStripped = id.substring(2);
    }
    // Check if the original ID is a pseudo-reaction
    if (isPseudo(id)) {
      return createReactionId(id, true, true);
    }
    // Check if the ID without the prefix is a pseudo-reaction
    else if (!prefixStripped.isEmpty() && isPseudo(prefixStripped)) {
      return createReactionId(prefixStripped, true, true);
    }
    // Handle normal reaction ID
    else {
      return createReactionId(id, true, false);
    }
  }


  /**
   * Checks if the given reaction ID corresponds to a pseudo-reaction.
   * Pseudo-reactions are predefined patterns that do not correspond to actual biochemical reactions
   * but are used for modeling purposes. This method checks if the reaction ID matches any of the
   * predefined pseudo-reaction patterns such as ATP maintenance (ATPM), biomass, or generic pseudo-reactions.
   *
   * @param reactionId The reaction ID to be checked.
   * @return true if the reaction ID matches any pseudo-reaction pattern, false otherwise.
   */
  private static boolean isPseudo(String reactionId) {
    return IDPattern.ATPM.get().matcher(reactionId).matches() ||
	   IDPattern.BIOMASS.get().matcher(reactionId).matches() ||
	   IDPattern.PSEUDO.get().matcher(reactionId).matches();
  }


  /**
   * Creates a BiGG ID for a reaction based on the provided string identifier.
   * The method can correct the ID to conform to BiGG standards and adjust the prefix based on whether it is a pseudo-reaction.
   *
   * @param id The raw reaction ID string.
   * @param correct If true, the ID will be corrected to conform to BiGG standards.
   * @param isPseudo If true, the ID is treated as a pseudo-reaction, affecting the prefix handling.
   * @return An Optional containing the BiGGId if the ID is non-empty, or an empty Optional if the ID is empty.
   */
  public static Optional<BiGGId> createReactionId(String id, boolean correct, boolean isPseudo) {
    if (id.isEmpty()) {
      return Optional.empty();
    }
    if (correct) {
      id = makeBiGGConform(id);
      if (id.startsWith("_")) {
	id = id.substring(1);
      }
      if (!isPseudo && id.startsWith("r_")) {
	id = id.replaceAll("^r_", "R_");
      } else if (!isPseudo && !id.startsWith("R_")) {
	id = "R_" + id;
      }
    }
    return Optional.of(new BiGGId(id));
  }


  /**
   * Transforms a given identifier into a format conforming to BiGG ID standards.
   * This method applies several transformations to ensure the ID adheres to the required naming conventions:
   * - Prefixes IDs starting with a digit with an underscore.
   * - Replaces certain characters with specific strings or patterns to avoid conflicts in naming conventions.
   * - Extracts and reformats compartment codes enclosed in parentheses or brackets.
   * - Removes trailing parts of IDs that are marked as copies.
   * - Ensures that only alphanumeric characters and underscores are retained, replacing all other characters with underscores.
   * - Trims any trailing underscores from the final ID.
   *
   * @param id The original identifier that needs to be transformed.
   * @return A string representing the transformed identifier conforming to BiGG standards.
   */
  private static String makeBiGGConform(String id) {
    // Prefix the ID with an underscore if it starts with a digit
    if (Character.isDigit(id.charAt(0))) {
      id = "_" + id;
    }
    // Replace problematic characters with specific strings
    id = id.replaceAll("[-/]", "__").replaceAll("\\.", "__SBML_DOT__").replaceAll("\\(", "_LPAREN_")
	   .replaceAll("\\)", "_RPAREN_").replaceAll("\\[", "_LBRACKET_").replaceAll("]", "_RBRACKET_");
    // Extract and reformat compartment codes enclosed in parentheses
    Pattern parenCompartment = Pattern.compile("_LPAREN_(?<paren>.*?)_RPAREN_");
    Matcher parenMatcher = parenCompartment.matcher(id);
    if (parenMatcher.find()) {
      id = id.replaceAll(parenCompartment.toString(), "_" + parenMatcher.group("paren"));
    }
    // Extract and reformat compartment codes enclosed in brackets
    Pattern bracketCompartment = Pattern.compile("_LBRACKET_(?<bracket>.*)_RBRACKET_");
    Matcher bracketMatcher = bracketCompartment.matcher(id);
    if (bracketMatcher.find()) {
      id = id.replaceAll(bracketCompartment.toString(), "_" + bracketMatcher.group("bracket"));
    }
    // Remove the '_copy' suffix and any trailing digits
    if (id.matches(".*_copy\\d*")) {
      id = id.substring(0, id.lastIndexOf('_'));
    }
    // Retain only alphanumeric characters and underscores, replacing all other characters
    Pattern alphaNum = Pattern.compile("[a-zA-Z0-9_]");
    StringBuilder builder = new StringBuilder(id.length());
    for (char ch : id.toCharArray()) {
      if (alphaNum.matcher(String.valueOf(ch)).matches()) {
	builder.append(ch);
      } else {
	builder.append("_");
      }
    }
    id = builder.toString();
    // Remove any trailing underscores
    if (id.endsWith("_")) {
      id = id.substring(0, id.length() - 1);
    }
    return id;
  }


  public static boolean isValid(String queryId) {
    return IDPattern.ATPM.get().matcher(queryId).matches() || IDPattern.BIOMASS.get().matcher(queryId).matches()
      || IDPattern.COMPARTMENT.get().matcher(queryId).matches()
      || IDPattern.METABOLITE_SPECIAL.get().matcher(queryId).matches()
      || IDPattern.PSEUDO.get().matcher(queryId).matches() || IDPattern.UNIVERSAL.get().matcher(queryId).matches();
  }


  /**
   * Corrects the format of compartment codes in the given identifier string.
   * This method is specifically designed to handle cases where the compartment code is incorrectly
   * formatted with square brackets (e.g., [cc]) instead of the expected underscore format (e.g., _cc_).
   *
   * @param id The identifier string potentially containing incorrectly formatted compartment codes.
   * @return The identifier string with corrected compartment code format.
   */
  private static String fixCompartmentCode(String id) {
    // Define a pattern to identify and extract compartment codes enclosed in square brackets
    Pattern rescueCompartment = Pattern.compile(".*\\[(?<code>[a-z][a-z0-9]?)\\]");
    Matcher rescueMatcher = rescueCompartment.matcher(id);
    // Check if the pattern matches and process accordingly
    if (rescueMatcher.matches()) {
      // Extract the compartment code from the matcher
      String compartmentCode = rescueMatcher.group("code");
      // Replace the bracketed compartment code with the underscore format
      id = id.replaceAll("\\[[a-z][a-z0-9]?\\]", "_" + compartmentCode + "_");
      // Remove trailing underscore if present
      if (id.endsWith("_")) {
	id = id.substring(0, id.length() - 1);
      }
    }
    return id;
  }


  /**
   * Parses the given identifier into a structured BiGG ID. This method first checks if an ID
   * corresponding to a reaction might in fact identify a pseudo-reaction. If it is a pseudo-reaction,
   * it is processed accordingly. Otherwise, it checks against the UNIVERSAL pattern to handle
   * normal BiGG IDs. If neither condition is met, the ID is handled as a special case.
   *
   * @param id the identifier to be parsed into a BiGG ID.
   */
  private void parseBiGGId(String id) {
    Matcher matcher = IDPattern.UNIVERSAL.get().matcher(id);
    // Determine if the ID is a pseudo-reaction, which are special cases like ATP maintenance or biomass reactions
    boolean isPseudoReaction = id.startsWith("R_") && isPseudo(id);
    if (!isPseudoReaction && matcher.matches()) {
      // If it matches the universal pattern and is not a pseudo-reaction, handle it as a normal BiGG ID
      handleNormalId(matcher);
    } else {
      // If it does not match or is a pseudo-reaction, handle it according to its special characteristics
      handleSpecialCases(id);
    }
  }


  /**
   * Processes a Matcher object that has matched a BiGG ID against the UNIVERSAL pattern.
   * This method extracts the components of the BiGG ID from the Matcher and sets the corresponding fields
   * in the BiGGId object.
   *
   * @param matcher The Matcher object containing the groups corresponding to the components of the BiGG ID.
   */
  private void handleNormalId(Matcher matcher) {
    String prefix = matcher.group("prefix");
    setPrefix(prefix);
    String abbreviation = matcher.group("abbreviation");
    setAbbreviation(abbreviation);
    String compartment = matcher.group("compartment");
    setCompartmentCode(compartment);
    String tissueCode = matcher.group("tissueCode");
    setTissueCode(tissueCode);
  }


  /**
   * Handles special cases for BiGG ID parsing where standard parsing fails.
   * This method checks the given ID against several predefined patterns to determine
   * if the ID corresponds to pseudoreactions, biomass, ATP maintenance (ATPM), or compartment identifiers.
   * Depending on the match, it reformats the ID or logs a warning if no known pattern is matched.
   *
   * @param id The BiGG ID string to be evaluated and handled for special cases.
   */
  private void handleSpecialCases(String id) {
    Matcher pseudoreactionMatcher = IDPattern.PSEUDO.get().matcher(id);
    Matcher biomassMatcher = IDPattern.BIOMASS.get().matcher(id);
    Matcher atpmMatcher = IDPattern.ATPM.get().matcher(id);
    Matcher compartmentMatcher = IDPattern.COMPARTMENT.get().matcher(id);
    if (pseudoreactionMatcher.matches()) {
      id = id.replaceAll("^([Rr]_?)?[Ee][Xx]", "EX");
      id = id.replaceAll("^([Rr]_?)?[Dd][Mm]", "DM");
      id = id.replaceAll("^([Rr]_?)?[Ss]([Ii][Nn])?[Kk]", "SK");
      setAbbreviation(id);
    } else if (biomassMatcher.matches()) {
      id = id.replaceAll("^([Rr]_?)?[Bb][Ii][Oo][Mm][Aa][Ss][Ss]", "BIOMASS");
      setAbbreviation(id);
    } else if (atpmMatcher.matches()) {
      setAbbreviation("ATPM");
    } else if (compartmentMatcher.matches()) {
      setAbbreviation(id);
    } else {
      logger.warning(format(MESSAGES.getString("BIGGID_CONVERSION_FAIL"), id));
      setAbbreviation(id);
    }
  }


  /**
   * Extracts the compartment code from a given identifier if it matches the expected pattern.
   * The expected pattern allows an optional prefix "C_" followed by one or two lowercase letters,
   * optionally followed by a digit. If the identifier does not match this pattern, an empty
   * {@link Optional} is returned. If the identifier starts with "C_", this prefix is removed
   * before returning the compartment code.
   *
   * @param id The identifier from which to extract the compartment code.
   * @return An {@link Optional} containing the compartment code if the identifier matches the pattern,
   *         otherwise an empty {@link Optional}.
   */
  public static Optional<String> extractCompartmentCode(String id) {
    if (!Pattern.compile("(C_)?[a-z][a-z0-9]?").matcher(id).matches()) {
      return Optional.empty();
    }
    if (id.startsWith("C_")) {
      id = id.substring(2);
    }
    return Optional.of(id);
  }


  /*
   * (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    BiGGId other = (BiGGId) obj;
    if (abbreviation == null) {
      if (other.abbreviation != null) {
	return false;
      }
    } else if (!abbreviation.equals(other.abbreviation)) {
      return false;
    }
    if (compartmentCode == null) {
      if (other.compartmentCode != null) {
	return false;
      }
    } else if (!compartmentCode.equals(other.compartmentCode)) {
      return false;
    }
    if (prefix == null) {
      if (other.prefix != null) {
	return false;
      }
    } else if (!prefix.equals(other.prefix)) {
      return false;
    }
    if (tissueCode == null) {
      return other.tissueCode == null;
    } else
      return tissueCode.equals(other.tissueCode);
  }



  public String getAbbreviation() {
    return isSetAbbreviation() ? abbreviation : "";
  }


  /**
   * <ul>
   * <li>Only contain upper and lower case letters, numbers, and underscores
   * <li>/[0-9a-zA-Z][a-zA-Z0-9_]+/, only ASCII and don't start with numbers
   * <li>When converting old BIGG IDs to BIGG2 IDs, replace a dash with two
   * underscores. For example, ala-L becomes ala__L.
   * <li>Reactions should be all upper case. Metabolites should be primarily
   * lower case, but upper case letters are allowed (ala__L is preferred to
   * ALA__L).
   * </ul>
   *
   * @param abbreviation
   *        the abbreviation to set
   */
  public void setAbbreviation(String abbreviation) {
    if (null != abbreviation && !abbreviation.isEmpty()) {
      this.abbreviation = abbreviation;
    }
  }



  public String getCompartmentCode() {
    return isSetCompartmentCode() ? compartmentCode : "";
  }


  /**
   * One or two characters in length, and contain only lower case letters and
   * numbers, and must begin with a lower case letter. /[a-z][a-z0-9]?/
   *
   * @param compartmentCode
   *        the compartmentCode to set
   */
  public void setCompartmentCode(String compartmentCode) {
    if (null != compartmentCode && !compartmentCode.isEmpty()) {
      this.compartmentCode = compartmentCode;
    }
  }



  public String getPrefix() {
    return isSetPrefix() ? prefix : "";
  }


  /**
   * <ul>
   * <li>R: reaction
   * <li>M: metabolite /[RM]/
   * <li>NOTE: Do we want to have the id entity use R and M, and just remove
   * them when constructing the model, or have them just as
   * [abbreviation]_[compartment code] and add the prefix when they are put
   * into SBML models? Also SBML id's use capital letters (/[RM]/).
   * </ul>
   *
   * @param prefix
   *        the prefix to set
   */
  public void setPrefix(String prefix) {
    if (null != prefix && !prefix.isEmpty()) {
      this.prefix = prefix;
    }
  }



  public String getTissueCode() {
    return isSetTissueCode() ? tissueCode : "";
  }


  /**
   * One or two characters in length, and contain only upper case letters and
   * numbers, and must begin with an upper case letter. /[A-Z][A-Z0-9]?/
   *
   * @param tissueCode
   *        the tissueCode to set
   */
  public void setTissueCode(String tissueCode) {
    if (null != tissueCode && !tissueCode.isEmpty()) {
      this.tissueCode = tissueCode;
    }
  }


  /*
   * (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((abbreviation == null) ? 0 : abbreviation.hashCode());
    result = prime * result + ((compartmentCode == null) ? 0 : compartmentCode.hashCode());
    result = prime * result + ((prefix == null) ? 0 : prefix.hashCode());
    result = prime * result + ((tissueCode == null) ? 0 : tissueCode.hashCode());
    return result;
  }



  public boolean isSetAbbreviation() {
    return abbreviation != null;
  }



  public boolean isSetCompartmentCode() {
    return compartmentCode != null;
  }



  public boolean isSetPrefix() {
    return prefix != null;
  }



  public boolean isSetTissueCode() {
    return tissueCode != null;
  }


  /**
   * Generates a BiGG ID for this object based on its properties. The BiGG ID is constructed by concatenating
   * the available properties (prefix, abbreviation, compartment code, and tissue code) in that order, each separated
   * by an underscore. Each property is included only if it is set (i.e., not null).
   *
   * @return A string representing the BiGG ID, constructed by concatenating the set properties with underscores.
   *         If none of the properties are set, returns an empty string.
   */
  public String toBiGGId() {
    StringBuilder sb = new StringBuilder();
    // Append prefix if set
    if (isSetPrefix()) {
      sb.append(getPrefix());
    }
    // Append abbreviation if set, prefixed by an underscore if not the first element
    if (isSetAbbreviation()) {
      if (sb.length() > 0) {
	sb.append('_');
      }
      sb.append(getAbbreviation());
    }
    // Append compartment code if set, prefixed by an underscore if not the first element
    if (isSetCompartmentCode()) {
      if (sb.length() > 0) {
	sb.append('_');
      }
      sb.append(getCompartmentCode());
    }
    // Append tissue code if set, prefixed by an underscore if not the first element
    if (isSetTissueCode()) {
      if (sb.length() > 0) {
	sb.append('_');
      }
      sb.append(getTissueCode());
    }
    return sb.toString();
  }


  /**
   * Constructs a BiGG ID using the provided components. Each component is separated by an underscore.
   * If a component is null or empty, it is omitted from the final ID.
   *
   * @param prefix The first part of the BiGG ID, typically representing the type of entity (e.g., 'R', 'M', 'G').
   * @param abbreviation The main identifier, usually an alphanumeric string that uniquely describes the entity.
   * @param compartmentCode A code indicating the compartmentalization, relevant for compartmentalized entities.
   * @param tissueCode A code representing the tissue specificity, applicable to certain biological models.
   * @return A string representing the constructed BiGG ID, formed by concatenating the provided components with underscores.
   */
  public String toBiGGId(String prefix, String abbreviation, String compartmentCode, String tissueCode) {
    StringBuilder sb = new StringBuilder();
    if (prefix != null && !prefix.isEmpty()) {
      sb.append(prefix);
    }
    if (abbreviation != null && !abbreviation.isEmpty()) {
      if (sb.length() > 0) {
	sb.append('_');
      }
      sb.append(abbreviation);
    }
    if (compartmentCode != null && !compartmentCode.isEmpty()) {
      if (sb.length() > 0) {
	sb.append('_');
      }
      sb.append(compartmentCode);
    }
    if (tissueCode != null && !tissueCode.isEmpty()) {
      if (sb.length() > 0) {
	sb.append('_');
      }
      sb.append(tissueCode);
    }
    return sb.toString();
  }


  /*
   * (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return getClass().getSimpleName() + " [prefix=" + prefix + ", abbreviation=" + abbreviation + ", compartmentCode="
      + compartmentCode + ", tissueCode=" + tissueCode + "]";
  }
}
