package edu.ucsd.sbrg.bigg;

import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class stores the information from BiGG identifiers and provides methods
 * to access all components of the identifier.
 * For a formal description of the structure of BiGG ids see the proposed
 * <a href=
 * "https://github.com/SBRG/bigg_models/wiki/BiGG-Models-ID-Specification-and-Guidelines">
 * BiGG ID specification</a>.
 * 
 * @author Andreas Dr&auml;ger
 */
public class BiGGId {

  /**
   * A {@link Logger} for this class.
   */
  private static final transient Logger logger = Logger.getLogger(BiGGId.class.getName());
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
  /**
   *
   */
  private String id;

  /**
   *
   */
  enum IDPattern {

    ATPM("[Aa][Tt][Pp][Mm]"),
    BIOMASS("([Bb][Ii][Oo][Mm][Aa][Ss][Ss])(?:_(.+))?"),
    COMPARTMENT("[a-zA-Z][a-zA-Z0-9]?"),
    PSEUDO("([Ee][Xx]_)|([Ds][Mm]_)|([Ss][Kk]_)"),
    UNIVERSAL("^(?<prefix>[RMG])_(?<abbreviation>[a-zA-Z0-9][a-zA-Z0-9_]+?)" + "(?:_(?<compartment>[a-z][a-z0-9]?))?"
      + "(?:_(?<tissueCode>[A-Z][A-Z0-9]?))?$");

    /**
     *
     */
    private final Pattern pattern;

    /**
     * @param pattern
     */
    IDPattern(String pattern) {
      this.pattern = Pattern.compile(pattern);
    }


    /**
     * @return
     */
    Pattern get() {
      return pattern;
    }
  }

  /**
   * 
   */
  public BiGGId() {
    super();
  }


  /**
   * @param id
   */
  public BiGGId(String id) {
    this();
    parseBiGGId(id);
  }


  /**
   * @param prefix
   * @param abbreviation
   * @param compartmentCode
   * @param tissueCode
   */
  public BiGGId(String prefix, String abbreviation, String compartmentCode, String tissueCode) {
    this();
    String id = toBiGGId(prefix, abbreviation, compartmentCode, tissueCode);
    parseBiGGId(id);
  }


  /**
   * @param id
   *        the identifier to be parsed into a bigg_id.
   */
  private void parseBiGGId(String id) {
    id = id.replaceAll("-", "__").replaceAll("\\/", "_DASH_").replaceAll("\\.", "_DOT_").replaceAll("\\(", "_LPAREN_")
           .replaceAll("\\)", "_RPAREN_").replaceAll("\\[", "_LBRACKET_").replaceAll("\\]", "_RBRACKET_");
    Matcher matcher = IDPattern.UNIVERSAL.get().matcher(id);
    if (matcher.matches()) {
      handleNormalId(id, matcher);
    } else {
      handleSpecialCases(id);
    }
  }


  /**
   * @param id
   */
  private void handleNormalId(String id, Matcher matcher) {
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
   * @param id
   */
  private void handleSpecialCases(String id) {
    Matcher pseudoreactionMatcher = IDPattern.PSEUDO.get().matcher(id);
    Matcher biomassMatcher = IDPattern.BIOMASS.get().matcher(id);
    Matcher atpmMatcher = IDPattern.ATPM.get().matcher(id);
    Matcher compartmentMatcher = IDPattern.COMPARTMENT.get().matcher(id);
    if (pseudoreactionMatcher.find()) {
      String prefix = id.substring(0, 2);
      setPrefix(prefix.toUpperCase());
      String abbreviation = id.substring(3);
      setAbbreviation(abbreviation);
    } else if (biomassMatcher.matches()) {
      String prefix = biomassMatcher.group(1);
      setPrefix(prefix);
      String abbreviation = biomassMatcher.group(2);
      setAbbreviation(abbreviation);
    } else if (atpmMatcher.matches()) {
      setAbbreviation("ATPM");
    } else if (compartmentMatcher.matches()) {
      setAbbreviation(id);
    } else {
      logger.warning(String.format("Cannot convert to BiGGId, setting as abbreviation: %s", id));
      setAbbreviation(id);
    }
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


  /**
   * @return the abbreviation
   */
  public String getIdString() {
    return id;
  }


  /**
   * @return the abbreviation
   */
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
    this.abbreviation = abbreviation;
  }


  /**
   * @return the compartmentCode
   */
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
    this.compartmentCode = compartmentCode;
  }


  /**
   * @return the prefix
   */
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
    this.prefix = prefix;
  }


  /**
   * @return the tissueCode
   */
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
    this.tissueCode = tissueCode;
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


  /**
   * @return
   */
  public boolean isSetAbbreviation() {
    return abbreviation != null;
  }


  /**
   * @return
   */
  public boolean isSetCompartmentCode() {
    return compartmentCode != null;
  }


  /**
   * @return
   */
  public boolean isSetPrefix() {
    return prefix != null;
  }


  /**
   * @return
   */
  public boolean isSetTissueCode() {
    return tissueCode != null;
  }


  /**
   * Generates an actual BiGG id for this object.
   * 
   * @return
   */
  public String toBiGGId() {
    StringBuilder sb = new StringBuilder();
    if (isSetPrefix()) {
      sb.append(getPrefix());
    }
    if (isSetAbbreviation()) {
      if (sb.length() > 0) {
        sb.append('_');
      }
      sb.append(getAbbreviation());
    }
    if (isSetCompartmentCode()) {
      if (sb.length() > 0) {
        sb.append('_');
      }
      sb.append(getCompartmentCode());
    }
    if (isSetTissueCode()) {
      if (sb.length() > 0) {
        sb.append('_');
      }
      sb.append(getTissueCode());
    }
    return sb.toString();
  }


  /**
   * Generates an actual BiGG id for this object.
   *
   * @return
   */
  public String toBiGGId(String prefix, String abbreviation, String compartmentCode, String tissueCode) {
    StringBuilder sb = new StringBuilder();
    if (!Optional.ofNullable(prefix).orElse("").isEmpty()) {
      sb.append(prefix);
    }
    if (!Optional.ofNullable(abbreviation).orElse("").isEmpty()) {
      if (sb.length() > 0) {
        sb.append('_');
      }
      sb.append(abbreviation);
    }
    if (!Optional.ofNullable(compartmentCode).orElse("").isEmpty()) {
      if (sb.length() > 0) {
        sb.append('_');
      }
      sb.append(compartmentCode);
    }
    if (!Optional.ofNullable(tissueCode).orElse("").isEmpty()) {
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
    StringBuilder builder = new StringBuilder();
    builder.append(getClass().getSimpleName());
    builder.append(" [prefix=");
    builder.append(prefix);
    builder.append(", abbreviation=");
    builder.append(abbreviation);
    builder.append(", compartmentCode=");
    builder.append(compartmentCode);
    builder.append(", tissueCode=");
    builder.append(tissueCode);
    builder.append("]");
    return builder.toString();
  }


  /**
   * 
   */
  public void unsetAbbreviation() {
    abbreviation = null;
  }


  /**
   * 
   */
  public void unsetCompartmentCode() {
    compartmentCode = null;
  }


  /**
   * 
   */
  public void unsetPrefix() {
    prefix = null;
  }


  /**
   * 
   */
  public void unsetTissueCode() {
    tissueCode = null;
  }
}
