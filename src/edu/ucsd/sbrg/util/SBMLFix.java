/**
 * 
 */
package edu.ucsd.sbrg.util;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.TidySBMLWriter;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.FluxObjective;
import org.sbml.jsbml.ext.fbc.Objective;
import org.sbml.jsbml.ext.groups.Group;
import org.sbml.jsbml.ext.groups.GroupsConstants;
import org.sbml.jsbml.ext.groups.GroupsModelPlugin;
import org.sbml.jsbml.util.filters.NameFilter;

import de.zbit.io.ZIPUtils;
import de.zbit.io.filefilter.SBFileFilter;
import de.zbit.util.Utils;
import de.zbit.util.logging.LogUtil;
import edu.ucsd.sbrg.bigg.ModelPolisher;
import edu.ucsd.sbrg.bigg.SBMLPolisher;

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
 * <p>
 * The methods in this class can also be used in other parts of ModelPolisher,
 * and are used in fact. This class can become a collection of repair functions
 * for invalid SBML models.
 * 
 * @author Andreas Dr&auml;ger
 * @date 2016-02-19
 */
public class SBMLFix {

  /**
   * A {@link Logger} for this class.
   */
  private static final transient Logger logger =
    Logger.getLogger(SBMLFix.class.getName());
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
          output =
            new File(Utils.ensureSlash(output.getAbsolutePath()) + fName);
        }
        try {
          fixSBML(input, output);
        } catch (XMLStreamException | IOException exc) {
          logger.severe(exc.getMessage());
        }
      }
    } else {
      if (!output.isDirectory()) {
        logger.severe(MessageFormat.format("Cannot write to file {0}.",
          output.getAbsolutePath()));
      }
      for (File file : input.listFiles()) {
        File target = new File(
          Utils.ensureSlash(output.getAbsolutePath()) + file.getName());
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
    GroupsModelPlugin gPlug =
      (GroupsModelPlugin) model.getExtension(GroupsConstants.shortLabel);
    if ((gPlug != null) && gPlug.isSetListOfGroups()) {
      for (Group group : gPlug.getListOfGroups()) {
        if (!group.isSetKind()) {
          logger.info(
            MessageFormat.format("Adding missing kind attribute to group {0}.",
              group.isSetName() ? group.getName() : group.getId()));
          group.setKind(Group.Kind.partonomy);
        }
      }
    }
  }


  /**
   * Check for missing objective function.
   * 
   * @param modelDescriptor
   *        this can be the path to the model file or some name that describes
   *        this model.
   * @param model
   */
  public static void fixObjective(String modelDescriptor, Model model) {
    FBCModelPlugin fbcPlug =
      (FBCModelPlugin) model.getExtension(FBCConstants.shortLabel);
    if ((fbcPlug != null) && fbcPlug.isSetListOfObjectives()) {
      fixObjective(modelDescriptor,
        model.isSetListOfReactions() ? model.getListOfReactions() : null,
        fbcPlug);
    }
  }


  /**
   * @param modelDescriptor
   *        some descriptive String for the model, e.g., its id, the path to the
   *        file, or any other meaningful information for users.
   * @param listOfReactions
   * @param fbcPlug
   * @return {@code true} if this operation was successful and {@code false} if
   *         the problem could not be fixed.
   */
  public static boolean fixObjective(String modelDescriptor,
    ListOf<Reaction> listOfReactions, FBCModelPlugin fbcPlug) {
    return fixObjective(modelDescriptor, listOfReactions, fbcPlug, null, null);
  }


  /**
   * @param modelDescriptor
   * @param listOfReactions
   * @param fbcPlug
   * @param fluxCoefficients
   * @param fluxObjectives
   * @return
   */
  public static boolean fixObjective(String modelDescriptor,
    ListOf<Reaction> listOfReactions, FBCModelPlugin fbcPlug,
    double[] fluxCoefficients, String[] fluxObjectives) {
    Objective activeObjective = null;
    if (!fbcPlug.isSetActiveObjective()) {
      logger.severe(MessageFormat.format(
        "No active objective defined in model {0}.", modelDescriptor));
      if (fbcPlug.getObjectiveCount() == 1) {
        activeObjective = fbcPlug.getObjective(0);
        fbcPlug.setActiveObjective(activeObjective);
        logger.info(MessageFormat.format(
          "The problem could be successfully solved by declaring ''{0}'' the active objective.",
          activeObjective.getId()));
      }
    } else {
      activeObjective = fbcPlug.getListOfObjectives().firstHit(
        new NameFilter(fbcPlug.getListOfObjectives().getActiveObjective()));
    }
    if (activeObjective != null) {
      Objective o = activeObjective;
      if (!o.isSetListOfFluxObjectives()) {
        logger.severe(MessageFormat.format(
          "Trying to identify missing flux objective from model in model {0}.",
          modelDescriptor));
        if (listOfReactions != null) {
          if (fluxObjectives != null) {
            /*
             * An array of target reactions is provided. We want to use this as
             * flux objectives.
             */
            boolean strict = false;
            for (int i = 0; i < fluxObjectives.length; i++) {
              final String id = fluxObjectives[i];
              Reaction r = listOfReactions.firstHit((obj) -> {
                return (obj instanceof Reaction)
                  && id.equals(((Reaction) obj).getId());
              });
              if (r != null) {
                createFluxObjective(modelDescriptor, r, fluxCoefficients, o, i);
                // if at least one flux objective exists, the model qualifies as
                // strict model.
                strict = true;
              } else {
                logger.severe(MessageFormat.format(
                  "Operation failed! Could not identify reaction ''{0}'' in model {1}.",
                  id, modelDescriptor));
              }
            }
            return strict;
          } else {
            /*
             * Search for biomass reaction in the model and use this as
             * objective.
             */
            final Pattern pattern =
              SBMLPolisher.PATTERN_BIOMASS_CASE_INSENSITIVE;
            Reaction rBiomass = listOfReactions.firstHit((obj) -> {
              return (obj instanceof Reaction)
                && pattern.matcher(((Reaction) obj).getId()).matches();
            });
            if (rBiomass != null) {
              createFluxObjective(modelDescriptor, rBiomass, fluxCoefficients,
                o, 0);
              return true;
            } else {
              logger.severe(
                "Operation failed! Could not identify biomass reaction.");
            }
          }
        } else {
          logger.severe(MessageFormat.format(
            "Operation failed! Missing list of reactions in model {0}.",
            modelDescriptor));
        }
      }
    }
    return false;
  }


  /**
   * @param modelDescriptor
   * @param r
   * @param fluxCoefficients
   * @param o
   * @param i
   */
  private static void createFluxObjective(String modelDescriptor, Reaction r,
    double[] fluxCoefficients, Objective o, int i) {
    double coeff = DEFAULT_COEFFICIENT;
    if ((fluxCoefficients != null) && (fluxCoefficients.length > i)) {
      coeff = fluxCoefficients[i];
    }
    logger.info(MessageFormat.format(
      "Added flux objective for reaction ''{0}'' with coefficient {1,number} to model {2}.",
      r.getId(), coeff, modelDescriptor));
    String rId = null;
    // o.createFluxObjective(null, null, coeff, r);
    rId = r != null ? r.getId() : null;
    FluxObjective fluxObjective =
      new FluxObjective(null, null, o.getLevel(), o.getVersion());
    if (!Double.isNaN(coeff)) {
      fluxObjective.setCoefficient(coeff);
    }
    if (rId != null) {
      fluxObjective.setReaction(rId);
    }
    o.getListOfFluxObjectives().add(fluxObjective);
  }


  /**
   * @param in
   * @param out
   * @throws XMLStreamException
   * @throws IOException
   */
  public static void fixSBML(File in, File out)
    throws XMLStreamException, IOException {
    long time = System.currentTimeMillis();
    logger.info(
      MessageFormat.format("Reading input file {0}", in.getAbsolutePath()));
    SBMLDocument doc = SBMLReader.read(in);
    Model model = doc.getModel();
    fixGroups(model);
    fixObjective(in.getAbsolutePath(), model);
    logger.info(
      MessageFormat.format("Writing output file {0}.", out.getAbsolutePath()));
    TidySBMLWriter.write(doc, out, ModelPolisher.class.getName(), "1.1", ' ',
      (short) 2);
    String archive = out.getAbsolutePath() + ".gz";
    logger.info(MessageFormat.format("Packing archive file {0}.", archive));
    ZIPUtils.GZip(out.getAbsolutePath(), archive);
    logger.info(
      MessageFormat.format("Done. Time elapsed: {0,number,integer} ms",
        System.currentTimeMillis() - time));
  }


  /**
   * @param args
   */
  public static void main(String[] args) {
    LogUtil.initializeLogging("de.zbit", "edu.ucsd.sbrg");
    batchProcess(new File(args[0]), new File(args[1]));
  }
}
