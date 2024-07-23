package edu.ucsd.sbrg.parameters;

import de.zbit.util.ResourceManager;
import de.zbit.util.prefs.SBProperties;
import edu.ucsd.sbrg.ModelPolisherOptions;
import edu.ucsd.sbrg.io.IOOptions;

import java.io.File;
import java.util.*;

public class CommandLineParameters {

    private static final ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");

    private final File input;
    protected boolean sbmlValidation = ModelPolisherOptions.SBML_VALIDATION.getDefaultValue();

    private final OutputParameters outputParameters;
    private final SBOParameters sboParameters;
    private final PolishingParameters polishingParameters;
    private final AnnotationParameters annotationParameters;


    public CommandLineParameters(SBProperties args) throws IllegalArgumentException {
        String inPath = args.getProperty(IOOptions.INPUT);
        if (inPath == null) {
            throw new IllegalArgumentException(MESSAGES.getString("PARAM_INPUT_MISSING"));
        }
        input = new File(inPath);
        sbmlValidation = args.getBooleanProperty(ModelPolisherOptions.SBML_VALIDATION);
        outputParameters = new OutputParameters(args);
        annotationParameters = new AnnotationParameters(args);
        sboParameters = new SBOParameters(args);
        polishingParameters = new PolishingParameters(args);
    }

    public File input() {
        return input;
    }

    public boolean SBMLValidation() {
        return sbmlValidation;
    }

    public OutputParameters outputParameters() {
        return outputParameters;
    }

    public AnnotationParameters annotationParameters() {
        return annotationParameters;
    }

    public SBOParameters sboParameters() {
        return sboParameters;
    }

    public PolishingParameters polishingParameters() {
        return polishingParameters;
    }
}
