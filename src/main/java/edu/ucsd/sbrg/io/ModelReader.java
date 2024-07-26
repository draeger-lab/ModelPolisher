package edu.ucsd.sbrg.io;

import edu.ucsd.sbrg.parameters.SBOParameters;
import edu.ucsd.sbrg.io.parsers.cobra.MatlabParser;
import edu.ucsd.sbrg.io.parsers.json.JSONParser;
import edu.ucsd.sbrg.polishing.SBMLPolisher;
import edu.ucsd.sbrg.resolver.Registry;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.Arrays;
import java.util.stream.Collectors;

import static java.text.MessageFormat.format;

public class ModelReader {
    private static final Logger logger = LoggerFactory.getLogger(ModelReader.class);

    private final SBOParameters sboParameters;
    private final Registry registry;

    public ModelReader(SBOParameters sboParameters, Registry registry) {
        this.sboParameters = sboParameters;
        this.registry = registry;
    }


    public SBMLDocument read(File input) throws ModelReaderException {
        logger.debug(format("Read model file: {0}", input.toString()));
        try {
            var fileType = SBMLFileUtils.getFileType(input);

            return switch (fileType) {
                case MAT_FILE -> new MatlabParser(sboParameters, registry).parse(input);
                case JSON_FILE -> new JSONParser(registry).parse(input);
                case SBML_FILE -> SBMLReader.read(input, new UpdateListener());
                case UNKNOWN ->
                        throw new IllegalArgumentException("Could not identify file type. Supported file types are: "
                                + Arrays.stream(SBMLFileUtils.FileType.values())
                                .map(SBMLFileUtils.FileType::name)
                                .collect(Collectors.joining(", ")));
            };

        } catch (Exception e) {
            throw new ModelReaderException("Error while reading input document.", e, input);
        }
    }
}
