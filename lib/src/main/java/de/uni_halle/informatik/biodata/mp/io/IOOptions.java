package de.uni_halle.informatik.biodata.mp.io;

import com.fasterxml.jackson.annotation.JsonValue;
import de.zbit.util.ResourceManager;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.uni_halle.informatik.biodata.mp.logging.BundleNames;

import java.util.ResourceBundle;

public interface IOOptions extends KeyProvider {

    ResourceBundle MESSAGES = ResourceManager.getBundle(BundleNames.IO_MESSAGES);

    /**
     * Decides whether the output file should directly be compressed and if
     * so, which archive type should be used.
     */
    @SuppressWarnings("unchecked")
    Option<OutputType> OUTPUT_TYPE =
            new Option<>("OUTPUT_TYPE", OutputType.class, MESSAGES.getString("OUTPUT_TYPE_DESCRIPTION"),
                    OutputType.SBML);

//    @SuppressWarnings("unchecked")
//    Option<Boolean> WRITE_JSON = new Option<>("WRITE_JSON", Boolean.class, "TODO", Boolean.FALSE);
//    /**
//     * Produce output as a single COMBINE Archive.
//     */
//    @SuppressWarnings("unchecked")
//    Option<Boolean> OUTPUT_COMBINE =
//      new Option<>("OUTPUT_COMBINE", Boolean.class, MESSAGES.getString("OUTPUT_COMBINE"), Boolean.FALSE);

    /**
   * @author Andreas Dr&auml;ger
   */
  enum OutputType {

    SBML("xml"),
    JSON("json"),
    COMBINE("combine");

    @JsonValue
    private final String extension;

    OutputType() {
       this(null);
    }

    OutputType(String extension) {
      this.extension = extension;
    }

    public String getFileExtension() {
      return extension;
    }
  }
}
