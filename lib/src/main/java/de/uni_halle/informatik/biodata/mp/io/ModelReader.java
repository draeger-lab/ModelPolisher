package de.uni_halle.informatik.biodata.mp.io;

import de.uni_halle.informatik.biodata.mp.parameters.SBOParameters;
import de.uni_halle.informatik.biodata.mp.io.parsers.cobra.MatlabParser;
import de.uni_halle.informatik.biodata.mp.io.parsers.json.JSONParser;
import de.uni_halle.informatik.biodata.mp.resolver.Registry;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.stream.Collectors;

import static java.text.MessageFormat.format;

public class ModelReader implements IReadModelsFromFile {
    private static final Logger logger = LoggerFactory.getLogger(ModelReader.class);

    private final SBOParameters sboParameters;
    private final Registry registry;

    public ModelReader(SBOParameters sboParameters, Registry registry) {
        this.sboParameters = sboParameters;
        this.registry = registry;
    }

    @Override
    public SBMLDocument read(File input) throws ModelReaderException {
        logger.debug(format("Read model file: {0}", input.toString()));
        try {
            var fileType = SBMLFileUtils.getFileType(input);

            SBMLDocument sbmlDocument = switch (fileType) {
                case MAT_FILE -> new MatlabParser(sboParameters, registry).parse(input);
                case JSON_FILE -> new JSONParser(registry).parse(input);
                case SBML_FILE -> SBMLReader.read(input, new UpdateListener());
                case UNKNOWN ->
                        throw new IllegalArgumentException("Could not identify file type. Supported file types are: "
                                + Arrays.stream(SBMLFileUtils.FileType.values())
                                .map(SBMLFileUtils.FileType::name)
                                .collect(Collectors.joining(", ")));
            };
            if (sbmlDocument == null) {
                throw new ModelReaderException("Error while reading input document.", input);
            }
            return sbmlDocument;

        } catch (Exception e) {
            throw new ModelReaderException("Error while reading input document.", e, input);
        }
    }
}
