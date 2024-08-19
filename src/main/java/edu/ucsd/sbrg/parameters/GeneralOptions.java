/*
 * 
 */
package edu.ucsd.sbrg.parameters;

import java.util.ResourceBundle;

import de.zbit.util.ResourceManager;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import edu.ucsd.sbrg.logging.BundleNames;

/**
 * @author Andreas Dr&auml;ger
 */
public interface GeneralOptions extends KeyProvider {

  ResourceBundle MESSAGES = ResourceManager.getBundle(BundleNames.POLISHING_MESSAGES);

  /**
   * Set this option to true if generic top-level annotations, such as 'process'
   * should not be applied. Not using those terms will reduce the size of the
   * resulting output file.
   */
  @SuppressWarnings("unchecked")
  Option<Boolean> ADD_GENERIC_TERMS =
    new Option<>("ADD_GENERIC_TERMS", Boolean.class, MESSAGES.getString("ADD_GENERIC_TERMS_DESC"),
            Boolean.TRUE);

  /**
   * If true, the created SBML file will be validated through the online
   * validator service at <a href="http://sbml.org">http://sbml.org</a>. This option is only used
   * if the output is GZIP compressed.
   */
  @SuppressWarnings("unchecked")
  Option<Boolean> SBML_VALIDATION =
    new Option<>("SBML_VALIDATION", Boolean.class, MESSAGES.getString("SBML_VAL_DESC"), Boolean.FALSE);

}
