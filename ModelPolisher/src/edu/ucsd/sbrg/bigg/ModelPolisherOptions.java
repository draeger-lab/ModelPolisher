/**
 * 
 */
package edu.ucsd.sbrg.bigg;

import java.io.File;

import de.zbit.util.objectwrapper.ValuePairUncomparable;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.Range;


/**
 * @author Andreas Dr&auml;ger
 *
 */
public interface ModelPolisherOptions extends KeyProvider {

  /**
   * 
   * @author Andreas Dr&auml;ger
   *
   */
  public static enum Compression {
    /**
     * 
     */
    GZIP("gz"),
    /**
     * 
     */
    NONE,
    /**
     * 
     */
    ZIP("zip");

    /**
     * 
     */
    private String extension;

    /**
     * 
     */
    private Compression() {
      this(null);
    }

    /**
     * 
     * @param extension
     */
    private Compression(String extension) {
      this.extension = extension;
    }

    /**
     * 
     * @return
     */
    public String getFileExtension() {
      return extension;
    }
  }

  /**
   * When set to true, the mass balance of each reaction will be checked where
   * possible. Reactions that are recognized as peudoreactions are excluded from
   * this check, also are reactions that lack information about elementary
   * composition of their participants.
   */
  @SuppressWarnings("unchecked")
  public static final Option<Boolean> CHECK_MASS_BALANCE = new Option<Boolean>(
      "CHECK_MASS_BALANCE",
      Boolean.class,
      "When set to true, the mass balance of each reaction will be checked where possible. Reactions that are recognized as peudoreactions are excluded from this check, also are reactions that lack information about elementary composition of their participants.",
      Boolean.TRUE);

  /**
   * Decides whether or not the output file should directly be compressed and if
   * so, which archive type should be used.
   */
  @SuppressWarnings("unchecked")
  public static final Option<Compression> COMPRESSION_TYPE = new Option<Compression>(
      "COMPRESSION_TYPE",
      Compression.class,
      "Decides whether or not the output file should directly be compressed and if so, which archive type should be used.",
      Compression.NONE);

  /**
   * This option allows you to define the title of the SBML document's
   * description and hence the head line when the file is displayed in a web
   * browser.
   */
  @SuppressWarnings("unchecked")
  public static final Option<String> DOCUMENT_TITLE_PATTERN = new Option<String>(
      "DOCUMENT_TITLE_PATTERN",
      String.class,
      "This option allows you to define the title of the SBML document's description and hence the head line when the file is displayed in a web browser.",
      "[biggId] - [organism]");

  /**
   * This XHTML file defines alternative model notes and makes them
   * exchangeable.
   */
  public static final Option<File> MODEL_NOTES_FILE = new Option<File>(
      "MODEL_NOTES_FILE",
      File.class,
      "This XHTML file defines alternative model notes and makes them exchangeable.");

  /**
   * Set this option to true if generic top-level annotations, such as 'process'
   * should not be applied. Not using those terms will reduce the size of the
   * resulting output file.
   */
  @SuppressWarnings("unchecked")
  public static final Option<Boolean> OMIT_GENERIC_TERMS = new Option<Boolean>(
      "OMIT_GENERIC_TERMS",
      Boolean.class,
      "Set this option to true if generic top-level annotations, such as 'process' should not be applied. Not using those terms will reduce the size of the resulting output file.",
      Boolean.FALSE);

  /**
   * If true, the created SBML file will be validated through the online
   * validator service at {@link "http://sbml.org"}. This option is only used
   * if the output is GZIP compressed.
   */
  @SuppressWarnings("unchecked")
  public static final Option<Boolean> SBML_VALIDATION = new Option<Boolean>(
      "SBML_VALIDATION",
      Boolean.class,
      "If true, the created SBML file will be validated through the online validator service at http://sbml.org.",
      Boolean.FALSE,
      new ValuePairUncomparable<Option<Compression>, Range<Compression>>(COMPRESSION_TYPE, new Range<Compression>(Compression.class, Compression.GZIP)),
      new ValuePairUncomparable<Option<Compression>, Range<Compression>>(COMPRESSION_TYPE, new Range<Compression>(Compression.class, Compression.ZIP)));

}
