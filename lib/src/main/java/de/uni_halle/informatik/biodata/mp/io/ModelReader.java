package de.uni_halle.informatik.biodata.mp.io;

import de.uni_halle.informatik.biodata.mp.parameters.SBOParameters;
import de.uni_halle.informatik.biodata.mp.io.parsers.cobra.MatlabParser;
import de.uni_halle.informatik.biodata.mp.io.parsers.json.JSONParser;
import de.uni_halle.informatik.biodata.mp.resolver.Registry;
import org.sbml.jsbml.JSBML;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCSpeciesPlugin;
import org.sbml.jsbml.xml.XMLNode;
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
                throw new ModelReaderException("Error while reading input document: returned SBMLDocument is null.",
                        input);
            }

            fixJSBMLBehaviour(sbmlDocument);
            return sbmlDocument;

        } catch (Exception e) {
            throw new ModelReaderException("Error while reading input document.", e, input);
        }
    }

    private void fixJSBMLBehaviour(SBMLDocument sbmlDocument) {
        // unfortunately, it seems impossible to make the JSBML reader read invalid SBML
        // this leads to chemicalFormulas vanishing from models, which some users were unhappy about
        // therefore, in case there is an invalid formula in the userObject (put there by the reader)
        // we just put it back onto the model
        if (sbmlDocument.getModel().isSetPlugin(FBCConstants.shortLabel)) {
            for (var s : sbmlDocument.getModel().getListOfSpecies()) {
                FBCSpeciesPlugin plugin = (FBCSpeciesPlugin) s.getPlugin(FBCConstants.shortLabel);
                plugin.putUserObject(JSBML.ALLOW_INVALID_SBML, true);
                var invalidXML = plugin.getUserObject("jsbml.invalid.xml");
                if (invalidXML instanceof XMLNode) {
                    XMLNode userObject = (XMLNode) plugin.getUserObject("jsbml.invalid.xml");
                    for (var i = 0; i < userObject.getAttributesLength(); i++) {
                        var invalidAttributeName = userObject.getAttributes().getName(i);
                        if (invalidAttributeName != null && invalidAttributeName.contains("chemicalFormula")) {
                            var invalidFormula = userObject.getAttributes().getValue(i);
                            plugin.setChemicalFormula(invalidFormula);
                        }
                    }
                }
            }
        }
    }
}
