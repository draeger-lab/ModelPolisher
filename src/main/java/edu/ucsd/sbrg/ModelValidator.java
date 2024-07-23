package edu.ucsd.sbrg;

import de.zbit.io.ZIPUtils;
import de.zbit.util.ResourceManager;
import edu.ucsd.sbrg.parameters.OutputParameters;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLError;
import org.sbml.jsbml.SBMLErrorLog;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.validator.SBMLValidator;
import org.sbml.jsbml.validator.offline.LoggingValidationContext;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

public class ModelValidator {
    /**
     * Bundle for ModelPolisher logger messages
     */
    private static final ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");
    /**
     * A {@link Logger} for this class.
     */
    private static final Logger logger = Logger.getLogger(ModelValidator.class.getName());
    private final OutputParameters parameters;

    public ModelValidator(OutputParameters parameters) {
        this.parameters = parameters;
    }

    /**
     * Validates an SBML file either online or offline based on the provided parameters.
     * Online validation refers to checking the file against a remote service or database, using specific parameters for the validation process.
     * Offline validation involves reading the file locally, handling different compression formats if necessary, and validating the SBML document against local constraints.
     * Errors encountered during the validation process are logged for further analysis.
     *
     * @param outputFile The file object of the SBML file to be validated.
     * @param online   A boolean flag indicating whether to perform online (true) or offline (false) validation.
     */
    public void validate(File outputFile, boolean online) {
        String fileExtension = parameters.compression().getFileExtension();
        String filename = outputFile.getAbsolutePath() + "." + fileExtension;

        if (online) {
            logger.info(format(MESSAGES.getString("VAL_ONLINE"), filename));
            String output = "xml";
            String offcheck = "p,u";
            Map<String, String> parameters = new HashMap<>();
            parameters.put("output", output);
            parameters.put("offcheck", offcheck);
            logger.info("Validating " + filename + "\n");
            SBMLErrorLog sbmlErrorLog = SBMLValidator.checkConsistency(filename, parameters);
            handleErrorLog(sbmlErrorLog, filename);
        } else {
            logger.info(format(MESSAGES.getString("VAL_OFFLINE"), filename));
            SBMLDocument doc = null;
            try {
                InputStream istream;
                if (filename.endsWith(".gz")) {
                    istream = ZIPUtils.GUnzipStream(filename);
                } else if (filename.endsWith(".zip")) {
                    istream = ZIPUtils.ZIPunCompressStream(filename);
                } else {
                    istream = new FileInputStream(filename);
                }
                doc = SBMLReader.read(istream);
            } catch (XMLStreamException | IOException e) {
                e.printStackTrace();
            }
            if (doc != null) {
                LoggingValidationContext context = new LoggingValidationContext(doc.getLevel(), doc.getVersion());
                context.loadConstraints(SBMLDocument.class);
                context.validate(doc);
                SBMLErrorLog sbmlErrorLog = context.getErrorLog();
                handleErrorLog(sbmlErrorLog, filename);
            } else {
                logger.severe(format(MESSAGES.getString("VAL_OFFLINE_FAIL"), filename));
            }
        }
    }


    private void handleErrorLog(SBMLErrorLog sbmlErrorLog, String filename) {
        if (sbmlErrorLog != null) {
            logger.info(format(MESSAGES.getString("VAL_ERR_COUNT"), sbmlErrorLog.getErrorCount(), filename));
            // printErrors
            for (int j = 0; j < sbmlErrorLog.getErrorCount(); j++) {
                SBMLError error = sbmlErrorLog.getError(j);
                logger.warning(error.toString());
            }
        } else {
            logger.info(MESSAGES.getString("VAL_ERROR"));
        }
    }
}
