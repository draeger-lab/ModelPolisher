package de.uni_halle.informatik.biodata.mp;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class CommandLineParametersParser {


    public CommandLineParameters parseCLIParameters(File parametersJson) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return mapper.readValue(parametersJson, CommandLineParameters.class);
    }


}
