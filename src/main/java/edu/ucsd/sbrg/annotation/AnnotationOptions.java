package edu.ucsd.sbrg.annotation;

import de.zbit.util.ResourceManager;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import edu.ucsd.sbrg.logging.BundleNames;
import edu.ucsd.sbrg.parameters.GeneralOptions;

import java.io.File;
import java.util.ResourceBundle;

public interface AnnotationOptions extends KeyProvider {

    ResourceBundle MESSAGES = ResourceManager.getBundle(BundleNames.ANNOTATION_MESSAGES);

    /**
     * If set to true, annotations will be added to species and reactions from AnnotateDB also.
     */
    @SuppressWarnings("unchecked")
    Option<Boolean> ADD_ADB_ANNOTATIONS =
      new Option<>("ADD_ADB_ANNOTATIONS", Boolean.class, MESSAGES.getString("ADD_ADB_ANNOTATIONS_DESC"),
              Boolean.FALSE);
    /**
     * If set to true, the model will be annotated with data from BiGG Models
     * database. If set to false, the resulting model will not receive annotation
     * or correction from BiGG Models database
     */
    @SuppressWarnings("unchecked")
    Option<Boolean> ANNOTATE_WITH_BIGG =
      new Option<>("ANNOTATE_WITH_BIGG", Boolean.class, MESSAGES.getString("ANNOTATE_WITH_BIGG_DESC"),
              Boolean.FALSE);

    /**
     * This XHTML file defines alternative model notes and makes them
     * exchangeable.
     */
    Option<File> MODEL_NOTES_FILE = new Option<>("MODEL_NOTES_FILE", File.class,
            MESSAGES.getString("MODEL_NOTES_DESC"));

    /**
     * If set to true, no web content will be inserted in the SBML container nor
     * into the model within the SBML file.
     */
    @SuppressWarnings("unchecked")
    Option<Boolean> NO_MODEL_NOTES =
      new Option<>("NO_MODEL_NOTES", Boolean.class, MESSAGES.getString("NO_MODEL_NOTES_DESC"),
              Boolean.FALSE);

    /**
     * This switch allows users to specify if also those database cross-links
     * should be extracted from BiGG Models database for which currently no entry
     * in the MIRIAM exists. If set to true, ModelPolisher also includes URIs that
     * do not contain the pattern identifiers.org.
     */
    @SuppressWarnings("unchecked")
    Option<Boolean> INCLUDE_ANY_URI =
      new Option<>("INCLUDE_ANY_URI", Boolean.class, MESSAGES.getString("INCLUDE_ANY_URI_DESC"),
              Boolean.FALSE);

    /**
     * This XHTML file defines alternative document notes and makes them
     * exchangeable.
     */
    Option<File> DOCUMENT_NOTES_FILE =
      new Option<>("DOCUMENT_NOTES_FILE", File.class, MESSAGES.getString("DOC_NOTES_DESC"));

    /**
     * This option allows you to define the title of the SBML document's
     * description and hence the headline when the file is displayed in a web
     * browser.
     */
    @SuppressWarnings("unchecked")
    Option<String> DOCUMENT_TITLE_PATTERN = new Option<>("DOCUMENT_TITLE_PATTERN", String.class,
      MESSAGES.getString("DOC_TITLE_PATTERN_DESC"), "[biggId] - [organism]");
}
