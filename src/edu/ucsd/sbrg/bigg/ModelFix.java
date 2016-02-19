/**
 * 
 */
package edu.ucsd.sbrg.bigg;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.TidySBMLWriter;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.Objective;
import org.sbml.jsbml.ext.groups.Group;
import org.sbml.jsbml.ext.groups.GroupsConstants;
import org.sbml.jsbml.ext.groups.GroupsModelPlugin;
import org.sbml.jsbml.util.filters.NameFilter;

import de.zbit.io.filefilter.SBFileFilter;
import de.zbit.util.Utils;
import de.zbit.util.logging.LogUtil;


/**
 * This is a stand-alone bug-fix program. It recursively traverses a directory
 * of SBML files and applies fixes to each model found. The result is saved to
 * a target directory, which can be in-place, i.e., identical to the input
 * directory. Otherwise, an identical directory structure will be created within
 * the target directory.
 * <p>
 * This program became necessary as a temporary solution for invalid SBML models
 * in BiGG database before a new version of {@link ModelPolisher} could be
 * released.
 * 
 * @author Andreas Dr&auml;ger
 * @date 2016-02-19
 */
public class ModelFix {

  /**
   * A {@link Logger} for this class.
   */
  private static final transient Logger logger = Logger.getLogger(ModelFix.class.getSimpleName());

  /**
   * 
   */
  private static final double DEFAULT_COEFFICIENT = 1d;

  /**
   * @param input
   * @param output
   * @throws XMLStreamException
   * @throws IOException
   */
  public static void batchProcess(File input, File output) {
    if (!output.exists() && !output.isFile()
        && !(input.isFile() && input.getName().equals(output.getName()))) {
      logger.info(MessageFormat.format("Creating directory {0}.",
        output.getAbsolutePath()));
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
          logger.severe(exc.getMessage());
        }
      }
    } else {
      if (!output.isDirectory()) {
        logger.severe(MessageFormat.format("Cannot write to file {0}.", output.getAbsolutePath()));
      }
      for (File file : input.listFiles()) {
        File target = new File(Utils.ensureSlash(output.getAbsolutePath()) + file.getName());
        batchProcess(file, target);
      }
    }
  }

  /**
   * Set group kind where required
   * 
   * @param model
   */
  public static void fixGroups(Model model) {
    GroupsModelPlugin gPlug = (GroupsModelPlugin) model.getExtension(GroupsConstants.shortLabel);
    if ((gPlug != null) && gPlug.isSetListOfGroups()) {
      for (Group group : gPlug.getListOfGroups()) {
        if (!group.isSetKind()) {
          logger.info(MessageFormat.format("Adding missing kind attribute to group {0}.", group.isSetName() ? group.getName() : group.getId()));
          group.setKind(Group.Kind.partonomy);
        }
      }
    }
  }

  /**
   * Check for missing objective function.
   * 
   * @param in
   * @param model
   */
  public static void fixObjective(File in, Model model) {
    FBCModelPlugin fbcPlug = (FBCModelPlugin) model.getExtension(FBCConstants.shortLabel);
    if ((fbcPlug != null) && fbcPlug.isSetListOfObjectives()) {
      if (!fbcPlug.isSetActiveObjective()) {
        logger.severe(MessageFormat.format("No active objective defined in model file {0}.", in.getAbsolutePath()));
      } else {
        Objective o = fbcPlug.getListOfObjectives().firstHit(new NameFilter(fbcPlug.getListOfObjectives().getActiveObjective()));
        if (!o.isSetListOfFluxObjectives()) {
          logger.severe(MessageFormat.format("Trying to identify missing flux objective from model in file {0}.", in.getAbsolutePath()));
          if (model.isSetListOfReactions()) {
            final Pattern pattern = SBMLPolisher.PATTERN_BIOMASS_CASE_INSENSITIVE;
            Reaction rBiomass = model.getListOfReactions().firstHit((obj) -> {
              return (obj instanceof Reaction) && pattern.matcher(((Reaction) obj).getId()).matches();
            });
            if (rBiomass != null) {
              logger.info(MessageFormat.format("Added biomass function {0} with coefficient {1,number} to model file {2}.", rBiomass.getId(), DEFAULT_COEFFICIENT, in.getAbsolutePath()));
              o.createFluxObjective(null, null, DEFAULT_COEFFICIENT, rBiomass);
            } else {
              logger.severe("Operation failed! Could not identify biomass reaction.");
            }
          } else {
            logger.severe(MessageFormat.format("Operation failed! Missing list of reactions in file {0}.", in.getAbsolutePath()));
          }
        }
      }
    }
  }

  /**
   * 
   * @param in
   * @param out
   * @throws XMLStreamException
   * @throws IOException
   */
  public static void fixSBML(File in, File out) throws XMLStreamException,
  IOException {
    long time = System.currentTimeMillis();
    logger.info(MessageFormat.format("Reading input file {0}", in.getAbsolutePath()));
    SBMLDocument doc = SBMLReader.read(in);
    Model model = doc.getModel();

    fixGroups(model);
    fixObjective(in, model);

    logger.info(MessageFormat.format("Writing output file {0}.", out.getAbsolutePath()));
    TidySBMLWriter.write(doc, out, ModelPolisher.class.getName(), "1.1", ' ', (short) 2);
    logger.info(MessageFormat.format("Done. Time elapsed: {0,number,integer} ms", System.currentTimeMillis() - time));
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    //Locale.setDefault(Locale.ENGLISH);
    LogUtil.initializeLogging("de.zbit", "edu.ucsd.sbrg");
    batchProcess(new File(args[0]), new File(args[1]));
  }

}
