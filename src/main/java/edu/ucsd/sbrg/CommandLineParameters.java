package edu.ucsd.sbrg;

import de.zbit.util.ResourceManager;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.SBProperties;
import edu.ucsd.sbrg.io.IOOptions;

import java.io.File;
import java.util.ResourceBundle;

public class CommandLineParameters extends Parameters{

    /**
     * Bundle for ModelPolisher logger messages
     */
    private static final ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");

    /**
     * @see IOOptions#INPUT
     */
    private File input = null;
    /**
     * @see IOOptions#OUTPUT
     */
    private File output = null;


    public CommandLineParameters(SBProperties args) throws IllegalArgumentException {
        super(args);
        initParameters(args);
    }

    private void initParameters(SBProperties args) {
        String inPath = args.getProperty(IOOptions.INPUT);
        if (inPath == null) {
            throw new IllegalArgumentException(MESSAGES.getString("PARAM_INPUT_MISSING"));
        }
        input = new File(inPath);
        String outPath = args.getProperty(IOOptions.OUTPUT);
        if (outPath == null) {
            throw new IllegalArgumentException(MESSAGES.getString("PARAM_OUTPUT_MISSING"));
        }
        output = new File(outPath);
        documentNotesFile = parseFileOption(args, ModelPolisherOptions.DOCUMENT_NOTES_FILE);
        modelNotesFile = parseFileOption(args, ModelPolisherOptions.MODEL_NOTES_FILE);
        documentTitlePattern = args.getProperty(ModelPolisherOptions.DOCUMENT_TITLE_PATTERN);
        if (args.containsKey(ModelPolisherOptions.FLUX_COEFFICIENTS)) {
            String c = args.getProperty(ModelPolisherOptions.FLUX_COEFFICIENTS);
            String[] coeff = c.substring(1, c.length() - 1).split(",");
            fluxCoefficients = new double[coeff.length];
            for (int i = 0; i < coeff.length; i++) {
                fluxCoefficients[i] = Double.parseDouble(coeff[i].trim());
            }
        }
        if (args.containsKey(ModelPolisherOptions.FLUX_OBJECTIVES)) {
            String fObjectives = args.getProperty(ModelPolisherOptions.FLUX_OBJECTIVES);
            fluxObjectives = fObjectives.substring(1, fObjectives.length() - 1).split(":");
        }
        annotateWithBiGG = args.getBooleanProperty(ModelPolisherOptions.ANNOTATE_WITH_BIGG);
        outputCOMBINE = args.getBooleanProperty(ModelPolisherOptions.OUTPUT_COMBINE);
        addADBAnnotations = args.getBooleanProperty(ModelPolisherOptions.ADD_ADB_ANNOTATIONS);
        checkMassBalance = args.getBooleanProperty(ModelPolisherOptions.CHECK_MASS_BALANCE);
        noModelNotes = args.getBooleanProperty(ModelPolisherOptions.NO_MODEL_NOTES);
        compression = ModelPolisherOptions.Compression.valueOf(args.getProperty(ModelPolisherOptions.COMPRESSION_TYPE));
        includeAnyURI = args.getBooleanProperty(ModelPolisherOptions.INCLUDE_ANY_URI);
        omitGenericTerms = args.getBooleanProperty(ModelPolisherOptions.OMIT_GENERIC_TERMS);
        sbmlValidation = args.getBooleanProperty(ModelPolisherOptions.SBML_VALIDATION);
        writeJSON = args.getBooleanProperty(ModelPolisherOptions.WRITE_JSON);
    }


    /**
     * Scans the given command-line options for a specific file option and
     * returns the corresponding file if it exists, {@code null} otherwise.
     *
     * @param args
     *        command-line options.
     * @param option
     *        a specific file option to look for.
     * @return a {@link File} object that corresponds to a desired command-line
     *         option, or {@code null} if it does not exist.
     */
    private File parseFileOption(SBProperties args, Option<File> option) {
        if (args.containsKey(option)) {
            File notesFile = new File(args.getProperty(option));
            if (notesFile.exists() && notesFile.canRead()) {
                return notesFile;
            }
        }
        return null;
    }


    public File input() {
        return input;
    }


    public File output() {
        return output;
    }

}
