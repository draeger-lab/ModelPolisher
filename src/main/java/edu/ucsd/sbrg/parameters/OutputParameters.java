package edu.ucsd.sbrg.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.zbit.util.ResourceManager;
import de.zbit.util.prefs.SBProperties;
import edu.ucsd.sbrg.ModelPolisherOptions;
import edu.ucsd.sbrg.io.IOOptions;

import java.io.File;
import java.util.ResourceBundle;


public class OutputParameters {
    private static final ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");

    @JsonProperty("output-file")
    private File outputFile = IOOptions.OUTPUT.getDefaultValue();
    @JsonProperty("compression")
    protected ModelPolisherOptions.Compression compression = ModelPolisherOptions.COMPRESSION_TYPE.getDefaultValue();
    @JsonProperty("output-combine")
    protected boolean outputCOMBINE = ModelPolisherOptions.OUTPUT_COMBINE.getDefaultValue();
    @JsonProperty("write-json")
    protected boolean writeJSON = ModelPolisherOptions.WRITE_JSON.getDefaultValue();

    public OutputParameters() {
    }

    public OutputParameters(SBProperties args) {
        String outPath = args.getProperty(IOOptions.OUTPUT);
        if (outPath == null) {
            throw new IllegalArgumentException(MESSAGES.getString("PARAM_OUTPUT_MISSING"));
        }
        outputFile = new File(outPath);
        outputCOMBINE = args.getBooleanProperty(ModelPolisherOptions.OUTPUT_COMBINE);
        compression = ModelPolisherOptions.Compression.valueOf(args.getProperty(ModelPolisherOptions.COMPRESSION_TYPE));
        writeJSON = args.getBooleanProperty(ModelPolisherOptions.WRITE_JSON);
    }

    public boolean outputCOMBINE() {
        return outputCOMBINE;
    }

    public ModelPolisherOptions.Compression compression() {
        return compression;
    }

    public boolean writeJSON() {
        return writeJSON;
    }

    public File outputFile() {
        return outputFile;
    }

}
