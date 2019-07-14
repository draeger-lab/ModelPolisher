/**
 * 
 */
package main.java.edu.ucsd.sbrg.bigg;

import static main.java.edu.ucsd.sbrg.bigg.ModelPolisher.mpMessageBundle;
import static java.text.MessageFormat.format;

import java.util.StringTokenizer;
import java.util.logging.Logger;

import de.zbit.util.Utils;

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
   * 
   */
  private String abbreviation;
  /**
   * 
   */
  private String compartmentCode;
  /**
   * 
   */
  private String prefix;
  /**
   * 
   */
  private String tissueCode;


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
    setConstructorPrefix(prefix);
    setCheckAbbreviation(abbreviation);
    setCheckCompartmentCode(compartmentCode);
    setCheckTissueCode(tissueCode);
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
  public String getAbbreviation() {
    return isSetAbbreviation() ? abbreviation : "";
  }


  /**
   * @return the compartmentCode
   */
  public String getCompartmentCode() {
    return isSetCompartmentCode() ? compartmentCode : "";
  }


  /**
   * @return the prefix
   */
  public String getPrefix() {
    return isSetPrefix() ? prefix : "";
  }


  /**
   * @return the tissueCode
   */
  public String getTissueCode() {
    return isSetTissueCode() ? tissueCode : "";
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
   * @param id
   *        the identifier to be parsed into a bigg_id.
   */
  private void parseBiGGId(String id) {
    String biggId = id;
    id = setParsedPrefix(id);
    id = id.replace("__", "-");
    if (id.matches(".*-[a-z][a-z0-9]?")) {
      id = id.substring(0, id.lastIndexOf('-')) + '-' + biggId.substring(biggId.lastIndexOf('_'));
    }
    StringTokenizer st = new StringTokenizer(id, "_");
    while (st.hasMoreElements()) {
      String elem = st.nextElement().toString();
      if (!isSetAbbreviation()) {
        setAbbreviation(elem.replace("-", "__"));
        continue;
      }
      if (!isSetCompartmentCode()) {
        setCheckCompartmentCode(biggId, elem);
        continue;
      }
      if (!isSetTissueCode()) {
        try {
          setTissueCode(elem);
        } catch (IllegalArgumentException exc) {
          // This is not a problem, we have the chance to fix it right
          // away.
          logger.finer(format(mpMessageBundle.getString("PARSE_ID_FAILED"), biggId, Utils.getMessage(exc)));
          if (isSetCompartmentCode()) {
            setAbbreviation(getAbbreviation() + '_' + getCompartmentCode());
          }
          setCheckCompartmentCode(biggId, elem);
        }
        continue;
      }
      if (elem.length() > 0) {
        throw new IllegalArgumentException(format(mpMessageBundle.getString("ID_COMPONENT_UNKNOWN"), elem));
      }
    }
  }


  /**
   * @param CompartmentCode
   */
  public void setCheckCompartmentCode(String CompartmentCode) {
    try {
      setCompartmentCode(CompartmentCode);
    } catch (IllegalArgumentException exc) {
      logger.finer(format(mpMessageBundle.getString("SET_COMPART_CODE_FAILED"), Utils.getMessage(exc)));
    }
  }


  /**
   * @param biggId
   * @param elem
   * @return
   */
  public void setCheckCompartmentCode(String biggId, String elem) {
    try {
      setCompartmentCode(elem);
    } catch (IllegalArgumentException exc) {
      // this is not a warning because we try to fix the problem right away.
      logger.finer(format(mpMessageBundle.getString("PARSE_ID_FAILED"), biggId, Utils.getMessage(exc)));
      if (isSetCompartmentCode()) {
        unsetCompartmentCode();
      }
      if (elem.endsWith("-")) {
        elem = elem.substring(0, elem.length() - 1) + '_';
      }
      setCheckAbbreviation(getAbbreviation() + "_" + elem.replace("-", "__"));
    }
  }


  /**
   * @param abbreviation
   */
  public void setCheckAbbreviation(String abbreviation) {
    try {
      setAbbreviation(abbreviation);
    } catch (IllegalArgumentException exc) {
      logger.fine(format(mpMessageBundle.getString("SET_ABREV_FAILED"), Utils.getMessage(exc)));
    }
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
    if (abbreviation.matches("[\\w&&[^_]][\\w]*")) {
      this.abbreviation = abbreviation;
    } else {
      throw new IllegalArgumentException(format(mpMessageBundle.getString("ABREV_INVALID"), abbreviation));
    }
  }


  /**
   * One or two characters in length, and contain only lower case letters and
   * numbers, and must begin with a lower case letter. /[a-z][a-z0-9]?/
   * 
   * @param compartmentCode
   *        the compartmentCode to set
   */
  public void setCompartmentCode(String compartmentCode) {
    if (compartmentCode.matches("[a-z][a-z0-9]?")) {
      this.compartmentCode = compartmentCode;
    } else {
      throw new IllegalArgumentException(format(mpMessageBundle.getString("COMPART_CODE_INVALID"), compartmentCode));
    }
  }


  /**
   * @param id
   * @return
   */
  public String setParsedPrefix(String id) {
    String prefix = "";
    if (id.startsWith("R_")) {
      if (id.matches("R_[Ee][Xx]_.*") || id.matches("R_[Dd][Mm]_.*")) {
        prefix = id.substring(0, 4);
        id = id.substring(5);
      } else if (id.matches("R_[Bb][Ii][Oo][Mm][Aa][Ss][Ss]_.*")) {
        prefix = id.substring(0, 9);
        id = id.substring(10);
      } else {
        prefix = "R";
        id = id.substring(2);
      }
    } else if (id.startsWith("M_") || id.startsWith("G_")) {
      prefix = id.substring(0, 1);
      id = id.substring(2);
    } else {
      logger.fine(format(mpMessageBundle.getString("ID_PREFIX_UNKNOWN"), id));
    }
    try {
      setPrefix(prefix);
    } catch (IllegalArgumentException exc) {
      logger.fine(format(mpMessageBundle.getString("SET_PREFIX_FAILED"), id));
    }
    return id;
  }


  /**
   * @param prefix
   * @return
   */
  public void setConstructorPrefix(String prefix) {
    if (prefix.matches("[RMG]|R_[Ee][Xx]|R_[Dd][Mm]|R_[Bb][Ii][Oo][Mm][Aa][Ss][Ss]")) {
      try {
        setPrefix(prefix);
      } catch (IllegalArgumentException exc) {
        logger.fine(format(mpMessageBundle.getString("SET_PREFIX_FAILED"), prefix));
      }
    } else {
      logger.fine(format(mpMessageBundle.getString("PREFIX_UNKNOWN"), prefix));
    }
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
    if (prefix.matches("[RrMmGg](_.*)?")) {
      // TODO refine the pattern.
      this.prefix = prefix;
    } else {
      throw new IllegalArgumentException(format("Invalid prefix: ''{0}''", prefix));
    }
  }


  /**
   * 
   */
  public void setCheckTissueCode(String tissueCode) {
    try {
      setTissueCode(tissueCode);
    } catch (IllegalArgumentException exc) {
      logger.finer(format(mpMessageBundle.getString("SET_TISS_CODE_FAILED"), Utils.getMessage(exc)));
    }
  }


  /**
   * One or two characters in length, and contain only upper case letters and
   * numbers, and must begin with an upper case letter. /[A-Z][A-Z0-9]?/
   * 
   * @param tissueCode
   *        the tissueCode to set
   */
  public void setTissueCode(String tissueCode) {
    if (tissueCode.matches("[A-Z][A-Z0-9]?")) {
      this.tissueCode = tissueCode;
    } else {
      throw new IllegalArgumentException(format(mpMessageBundle.getString("TISS_CODE_INVALID"), tissueCode));
    }
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
