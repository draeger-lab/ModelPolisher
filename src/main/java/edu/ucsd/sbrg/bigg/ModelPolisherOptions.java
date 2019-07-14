/*
 * 
 */
package main.java.edu.ucsd.sbrg.bigg;

import static main.java.edu.ucsd.sbrg.bigg.ModelPolisher.mpMessageBundle;

import java.io.File;

import de.zbit.util.objectwrapper.ValuePairUncomparable;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.Range;

/**
 * @author Andreas Dr&auml;ger
 */
public interface ModelPolisherOptions extends KeyProvider {

  /**
   * @author Andreas Dr&auml;ger
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
     * @param extension
     */
    private Compression(String extension) {
      this.extension = extension;
    }


    /**
     * @return
     */
    public String getFileExtension() {
      return extension;
    }
  }

  /**
   * This switch allows users to specify if also those database cross-links
   * should be extracted from BiGG Models database for which currently no entry
   * in the MIRIAM exists. If set to true, ModelPolisher also includes URIs that
   * do not contain the pattern identifiers.org.
   */
  @SuppressWarnings("unchecked")
  public static final Option<Boolean> INCLUDE_ANY_URI =
  new Option<Boolean>("INCLUDE_ANY_URI", Boolean.class,
      mpMessageBundle.getString("INCLUDE_ANY_URI_DESC"), Boolean.FALSE);
  /**
   * If set to true, the model will be annotated with data from BiGG Models
   * database. If set to false, the resulting model will not receive annotation
   * or correction from BiGG Models database
   */
  @SuppressWarnings("unchecked")
  public static final Option<Boolean> ANNOTATE_WITH_BIGG =
  new Option<Boolean>("ANNOTATE_WITH_BIGG", Boolean.class,
      mpMessageBundle.getString("ANNOTATE_WITH_BIGG_DESC"), Boolean.FALSE);
  /**
   * If set to true, no web content will be inserted in the SBML container nor
   * into the model within the SBML file.
   */
  @SuppressWarnings("unchecked")
  public static final Option<Boolean> NO_MODEL_NOTES = new Option<Boolean>(
      "NO_MODEL_NOTES", Boolean.class, mpMessageBundle.getString("NO_MODEL_NOTES"), Boolean.FALSE);
  /**
   * When set to true, the mass balance of each reaction will be checked where
   * possible. Reactions that are recognized as peudoreactions are excluded from
   * this check, also are reactions that lack information about elementary
   * composition of their participants.
   */
  @SuppressWarnings("unchecked")
  public static final Option<Boolean> CHECK_MASS_BALANCE =
  new Option<Boolean>("CHECK_MASS_BALANCE", Boolean.class,
      mpMessageBundle.getString("CHECK_MASS_BALANCE_DESC"), Boolean.TRUE);
  /**
   * 
   */
  @SuppressWarnings("unchecked")
  public static final Option<Double[]> FLUX_COEFFICIENTS = new Option<Double[]>(
      "FLUX_COEFFICIENTS", (Class<Double[]>) (new Double[0]).getClass(),
      mpMessageBundle.getString("FLUX_COEFF_DESC"), new Double[0]);
  /**
   * 
   */
  @SuppressWarnings("unchecked")
  public static final Option<String[]> FLUX_OBJECTIVES = new Option<String[]>(
      "FLUX_OBJECTIVES", (Class<String[]>) (new String[0]).getClass(),
      mpMessageBundle.getString("FLUX_OBJ_DESC"), new String[0]);
  /**
   * Decides whether or not the output file should directly be compressed and if
   * so, which archive type should be used.
   */
  @SuppressWarnings("unchecked")
  public static final Option<Compression> COMPRESSION_TYPE =
  new Option<Compression>("COMPRESSION_TYPE", Compression.class,
      mpMessageBundle.getString("COMPR_DESC"), Compression.NONE);
  /**
   * This option allows you to define the title of the SBML document's
   * description and hence the head line when the file is displayed in a web
   * browser.
   */
  @SuppressWarnings("unchecked")
  public static final Option<String> DOCUMENT_TITLE_PATTERN =
  new Option<String>("DOCUMENT_TITLE_PATTERN", String.class,
      mpMessageBundle.getString("DOC_TITLE_PATTERN_DESC"),
      "[biggId] - [organism]");
  /**
   * This XHTML file defines alternative model notes and makes them
   * exchangeable.
   */
  public static final Option<File> MODEL_NOTES_FILE =
      new Option<File>("MODEL_NOTES_FILE", File.class,
          mpMessageBundle.getString("MODEL_NOTES_DESC"));
  /**
   * This XHTML file defines alternative document notes and makes them
   * exchangeable.
   */
  public static final Option<File> DOCUMENT_NOTES_FILE =
      new Option<File>("DOCUMENT_NOTES_FILE", File.class,
          mpMessageBundle.getString("DOC_NOTES_DESC"));
  /**
   * Set this option to true if generic top-level annotations, such as 'process'
   * should not be applied. Not using those terms will reduce the size of the
   * resulting output file.
   */
  @SuppressWarnings("unchecked")
  public static final Option<Boolean> OMIT_GENERIC_TERMS =
  new Option<Boolean>("OMIT_GENERIC_TERMS", Boolean.class,
      mpMessageBundle.getString("OMIT_GENERIC_TERMS_DESC"), Boolean.FALSE);
  /**
   * If true, the created SBML file will be validated through the online
   * validator service at {@link "http://sbml.org"}. This option is only used
   * if the output is GZIP compressed.
   */
  @SuppressWarnings("unchecked")
  public static final Option<Boolean> SBML_VALIDATION =
  new Option<Boolean>("SBML_VALIDATION", Boolean.class,
      mpMessageBundle.getString("SBML_VAL_DESC"), Boolean.FALSE,
      new ValuePairUncomparable<Option<Compression>, Range<Compression>>(
          COMPRESSION_TYPE,
          new Range<Compression>(Compression.class, Compression.GZIP)),
      new ValuePairUncomparable<Option<Compression>, Range<Compression>>(
          COMPRESSION_TYPE,
          new Range<Compression>(Compression.class, Compression.ZIP)));
}
