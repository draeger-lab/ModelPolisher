/**
 * 
 */
package edu.ucsd.sbrg;

import de.zbit.io.ZIPUtils;
import de.zbit.io.filefilter.SBFileFilter;
import de.zbit.util.ResourceManager;
import de.zbit.util.Utils;
import de.zbit.util.logging.LogUtil;
import edu.ucsd.sbrg.fixing.ext.fbc.ObjectiveFixer;
import edu.ucsd.sbrg.fixing.ext.groups.GroupsFixer;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.TidySBMLWriter;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * This is a stand-alone bug-fix program. It recursively traverses a directory
 * of SBML files and applies fixes to each model found. The result is saved to
 * a target directory, which can be in-place, i.e., identical to the input
 * directory. Otherwise, an identical directory structure will be created within
 * the target directory.
 * <p>
 * This program became necessary as a temporary solution for invalid SBML models
 * in BiGG database before a new version of {@link ModelPolisherCLILauncher} could be
 * released.
 * <p>
 * The methods in this class can also be used in other parts of ModelPolisher,
 * and are used in fact. This class can become a collection of repair functions
 * for invalid SBML models.
 * <p>
 * Date: 2016-02-19
 * @author Andreas Dr&auml;ger
 */
public class SBMLFixerCLILauncher {

  private static final Logger logger = LoggerFactory.getLogger(SBMLFixerCLILauncher.class);
  private static final ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");

  private static void batchProcess(File input, File output) {
    if (!output.exists() && !output.isFile() && !(input.isFile() && input.getName().equals(output.getName()))) {
      logger.info(MessageFormat.format(MESSAGES.getString("DIRECTORY_CREATED"), output.getAbsolutePath()));
      output.mkdir();
    }
    if (input.isFile()) {
      if (SBFileFilter.isSBMLFile(input)) {
        if (output.isDirectory()) {
          String fName = input.getName();
          output = new File(Utils.ensureSlash(output.getAbsolutePath()) + fName);
        }
        try {
          fixSBML(input, output);
        } catch (XMLStreamException | IOException exc) {
          logger.error(exc.getMessage());
        }
      }
    } else {
      if (!output.isDirectory()) {
        logger.error(MessageFormat.format(MESSAGES.getString("WRITE_TO_FILE_ERROR"), output.getAbsolutePath()));
      }
      for (File file : input.listFiles()) {
        File target = new File(Utils.ensureSlash(output.getAbsolutePath()) + file.getName());
        batchProcess(file, target);
      }
    }
  }


  private static void fixSBML(File in, File out) throws XMLStreamException, IOException {
    long time = System.currentTimeMillis();
    logger.info(MessageFormat.format(MESSAGES.getString("READ_FILE_INFO"), in.getAbsolutePath()));
    SBMLDocument doc = SBMLReader.read(in);
    Model model = doc.getModel();
    GroupsFixer.fixGroups(model);
    FBCModelPlugin fbcPlug = (FBCModelPlugin) model.getExtension(FBCConstants.shortLabel);
    if ((fbcPlug != null) && fbcPlug.isSetListOfObjectives()) {
      ListOf<Reaction> listOfReactions = model.isSetListOfReactions() ? model.getListOfReactions() : null;
      ObjectiveFixer.fixObjective(in.getAbsolutePath(), listOfReactions, fbcPlug, null, null);
    }
    logger.info(MessageFormat.format(MESSAGES.getString("WRITE_FILE_INFO"), out.getAbsolutePath()));
    TidySBMLWriter.write(doc, out, ModelPolisherCLILauncher.class.getName(), "1.1", ' ', (short) 2);
    String archive = out.getAbsolutePath() + ".gz";
    logger.info(MessageFormat.format("ARCHIVE {0}", archive));
    ZIPUtils.GZip(out.getAbsolutePath(), archive);
    logger.info(MessageFormat.format("Done. Time elapsed: {0,number,integer} ms", System.currentTimeMillis() - time));
  }


  public static void main(String[] args) {
    LogUtil.initializeLogging("de.zbit", "edu.ucsd.sbrg");
    batchProcess(new File(args[0]), new File(args[1]));
  }
}
