package edu.ucsd.sbrg.validation;

import de.zbit.io.ZIPUtils;
import de.zbit.util.ResourceManager;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLError;
import org.sbml.jsbml.SBMLErrorLog;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.validator.offline.LoggingValidationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.ResourceBundle;

import static java.text.MessageFormat.format;

public class ModelValidator {
    private static final ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");
    private static final Logger logger = LoggerFactory.getLogger(ModelValidator.class);


    public SBMLErrorLog validate(SBMLDocument doc) {
        LoggingValidationContext context = new LoggingValidationContext(doc.getLevel(), doc.getVersion());
        context.setValidateRecursively(true);
        context.loadConstraints(SBMLDocument.class, doc.getLevel(), doc.getVersion());
        context.validate(doc);
        return context.getErrorLog();
    }


    public void validate(File outputFile) throws ModelValidatorException {
        try {
            String filename = outputFile.getAbsolutePath();

            logger.info(format(MESSAGES.getString("VAL_OFFLINE"), filename));
            SBMLDocument doc = null;
            InputStream istream;
            if (filename.endsWith(".gz")) {
                istream = ZIPUtils.GUnzipStream(filename);
            } else if (filename.endsWith(".zip")) {
                istream = ZIPUtils.ZIPunCompressStream(filename);
            } else {
                istream = new FileInputStream(filename);
            }
            doc = SBMLReader.read(istream);
            if (doc != null) {
                SBMLErrorLog sbmlErrorLog = validate(doc);
                logErrorLog(sbmlErrorLog, filename);
            } else {
                logger.info(format(MESSAGES.getString("VAL_OFFLINE_FAIL"), filename));
            }
        } catch (XMLStreamException | IOException e) {
            throw new ModelValidatorException(e, outputFile);
        }
    }


    private void logErrorLog(SBMLErrorLog sbmlErrorLog, String filename) {
        if (sbmlErrorLog != null) {
            logger.info(format(MESSAGES.getString("VAL_ERR_COUNT"), sbmlErrorLog.getErrorCount(), filename));
            // printErrors
            for (int j = 0; j < sbmlErrorLog.getErrorCount(); j++) {
                SBMLError error = sbmlErrorLog.getError(j);
                logger.info(error.toString());
            }
        } else {
            logger.info(MESSAGES.getString("VAL_ERROR"));
        }
    }
}
