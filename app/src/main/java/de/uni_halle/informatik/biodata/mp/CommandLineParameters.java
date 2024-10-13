package de.uni_halle.informatik.biodata.mp;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.zbit.util.ResourceManager;
import de.zbit.util.prefs.SBProperties;
import de.uni_halle.informatik.biodata.mp.CommandLineIOOptions;
import de.uni_halle.informatik.biodata.mp.logging.BundleNames;
import de.uni_halle.informatik.biodata.mp.parameters.Parameters;

import java.io.File;
import java.util.*;

public class CommandLineParameters extends Parameters {

    private static final ResourceBundle MESSAGES = ResourceManager.getBundle(BundleNames.CLI_MESSAGES);

    @JsonProperty("input")
    private File input;
    @JsonProperty("output")
    private File output;

    public CommandLineParameters() {}

    public CommandLineParameters(SBProperties args) throws IllegalArgumentException {
        super(args);
        String inPath = args.getProperty(CommandLineIOOptions.INPUT);
        if (inPath == null) {
            throw new IllegalArgumentException(MESSAGES.getString("PARAM_INPUT_MISSING"));
        }

        String outPath = args.getProperty(CommandLineIOOptions.OUTPUT);
        if (outPath == null) {
            throw new IllegalArgumentException(MESSAGES.getString("PARAM_OUTPUT_MISSING"));
        }
        output = new File(outPath);
        input = new File(inPath);
    }

    public File input() {
        return input;
    }

    public File output() {
        return output;
    }


}
