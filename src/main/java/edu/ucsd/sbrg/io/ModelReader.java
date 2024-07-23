package edu.ucsd.sbrg.io;

import edu.ucsd.sbrg.parameters.SBOParameters;
import edu.ucsd.sbrg.io.parsers.cobra.MatlabParser;
import edu.ucsd.sbrg.io.parsers.json.JSONParser;
import edu.ucsd.sbrg.resolver.Registry;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ModelReader {

    private final SBOParameters sboParameters;
    private final Registry registry;

    public ModelReader(SBOParameters sboParameters, Registry registry) {
        this.sboParameters = sboParameters;
        this.registry = registry;
    }


    public SBMLDocument read(File input) throws IOException, XMLStreamException {
        var fileType = SBMLFileUtils.getFileType(input);

        return switch (fileType) {
            case MAT_FILE -> new MatlabParser(sboParameters, registry).parse(input);
            case JSON_FILE -> new JSONParser(registry).parse(input);
            case SBML_FILE -> SBMLReader.read(input, new UpdateListener());
            case UNKNOWN -> throw new IllegalArgumentException("Could not identify file type. Supported file types are: "
                    + Arrays.stream(SBMLFileUtils.FileType.values())
                    .map(SBMLFileUtils.FileType::name)
                    .collect(Collectors.joining(", ")));
        };
    }

}
