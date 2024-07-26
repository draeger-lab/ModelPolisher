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
    private final File output;
    protected boolean sbmlValidation = ModelPolisherOptions.SBML_VALIDATION.getDefaultValue();
    protected ModelPolisherOptions.OutputType outputType = ModelPolisherOptions.OUTPUT_TYPE.getDefaultValue();

    private final SBOParameters sboParameters;
    private final PolishingParameters polishingParameters;
    private final AnnotationParameters annotationParameters;


    public CommandLineParameters(SBProperties args) throws IllegalArgumentException {
        String inPath = args.getProperty(IOOptions.INPUT);
        if (inPath == null) {
            throw new IllegalArgumentException(MESSAGES.getString("PARAM_INPUT_MISSING"));
        }

        String outPath = args.getProperty(IOOptions.OUTPUT);
        if (outPath == null) {
            throw new IllegalArgumentException(MESSAGES.getString("PARAM_OUTPUT_MISSING"));
        }
        output = new File(outPath);
        input = new File(inPath);
        sbmlValidation = args.getBooleanProperty(ModelPolisherOptions.SBML_VALIDATION);
        outputType = ModelPolisherOptions.OutputType.valueOf(args.getProperty(ModelPolisherOptions.OUTPUT_TYPE));
        annotationParameters = new AnnotationParameters(args);
        sboParameters = new SBOParameters(args);
        polishingParameters = new PolishingParameters(args);
    }

    public File input() {
        return input;
    }

    public File output() {
        return output;
    }

    public boolean SBMLValidation() {
        return sbmlValidation;
    }

    public ModelPolisherOptions.OutputType outputType() {
        return outputType;
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
