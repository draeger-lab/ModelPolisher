package edu.ucsd.sbrg;

import de.zbit.io.ZIPUtils;
import de.zbit.util.ResourceManager;
import edu.ucsd.sbrg.parsers.json.JSONConverter;
import edu.ucsd.sbrg.util.CombineArchive;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.TidySBMLWriter;

import javax.xml.stream.XMLStreamException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

public class ModelWriter {

    /**
     * Bundle for ModelPolisher logger messages
     */
    private static final ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");
    /**
     * A {@link Logger} for this class.
     */
    private static final Logger logger = Logger.getLogger(ModelWriter.class.getName());


    public void write(SBMLDocument doc, File output, String modelPolisherVersion) throws IOException, XMLStreamException {
        // Retrieve global parameters for the polishing process
        Parameters parameters = Parameters.get();
        // Convert and write the document to JSON if specified
        if (parameters.writeJSON()) {
            String out = output.getAbsolutePath().replaceAll("\\.xml", ".json");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(out))) {
                writer.write(JSONConverter.getJSONDocument(doc));
            }
        }
        // writing polished model
        logger.info(format(MESSAGES.getString("WRITE_FILE_INFO"), output.getAbsolutePath()));
        TidySBMLWriter.write(doc, output, getClass().getSimpleName(), modelPolisherVersion, ' ', (short) 2);
        // Handle COMBINE archive creation if specified
        if (parameters.outputCOMBINE()) {
            CombineArchive combineArchive = new CombineArchive(doc, output);
            combineArchive.write();
        }
        // Handle file compression based on the specified method
        if (parameters.compression() != ModelPolisherOptions.Compression.NONE) {
            writeZIP(output, parameters);
        }
    }

    private void writeZIP(File output, Parameters parameters) throws IOException {
        String fileExtension = parameters.compression().getFileExtension();
        String archive = output.getAbsolutePath() + "." + fileExtension;
        logger.info(format(MESSAGES.getString("ARCHIVE"), archive));
        switch (parameters.compression()) {
            case ZIP:
                ZIPUtils.ZIPcompress(new String[] {output.getAbsolutePath()}, archive, "SBML Archive", true);
                break;
            case GZIP:
                ZIPUtils.GZip(output.getAbsolutePath(), archive);
                break;
            default:
                break;
        }
        // Delete the original output file if compression is successful
        if (!output.delete()) {
            logger.warning(format(MESSAGES.getString("REMOVE_ZIP_INPUT_FAIL"), output.getAbsolutePath()));
        }
    }

}
