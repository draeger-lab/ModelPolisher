package edu.ucsd.sbrg.json;

import static edu.ucsd.sbrg.bigg.ModelPolisher.mpMessageBundle;
import static java.text.MessageFormat.format;
import static org.sbml.jsbml.util.Pair.pairOf;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.AssignmentRule;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.JSBML;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.Unit;
import org.sbml.jsbml.UnitDefinition;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.FBCReactionPlugin;
import org.sbml.jsbml.ext.fbc.FBCSpeciesPlugin;
import org.sbml.jsbml.ext.fbc.FluxObjective;
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.sbml.jsbml.ext.fbc.Objective;
import org.sbml.jsbml.ext.groups.Group;
import org.sbml.jsbml.ext.groups.GroupsConstants;
import org.sbml.jsbml.ext.groups.GroupsModelPlugin;
import org.sbml.jsbml.text.parser.ParseException;
import org.sbml.jsbml.util.ModelBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.zbit.util.Utils;
import edu.ucsd.sbrg.bigg.BiGGId;
import edu.ucsd.sbrg.util.SBMLUtils;
import edu.ucsd.sbrg.util.UpdateListener;

/**
 * @author Thomas Jakob Zajac
 */
public class JSONparser {

  private static final String GENE_PRODUCT_PREFIX = "G";
  private static final String REACTION_PREFIX = "R";
  private static final String METABOLITE_PREFIX = "M";
  /**
   * Regex pattern to split JSON arrays into their respective values at ",",
   * keeping both quotation marks in the process
   */
  private static final Pattern METABOLITE_DELIMITER = Pattern.compile("((?<=\\w)|(?<=\")),(?=\")");
  /**
   * Regex pattern for biomass prefix exclusion
   */
  private static Pattern PATTERN_BIOMASS_CASE_INSENSITIVE = Pattern.compile("(.*)([Bb][Ii][Oo][Mm][Aa][Ss][Ss])(.*)");


  /**
   * 
   */
  public JSONparser() {
    super();
  }

  /**
   * A {@link Logger} for this class.
   */
  private static final transient Logger logger = Logger.getLogger(JSONparser.class.getName());


  /**
   * @param jsonFile,
   *        to be read and parsed
   * @return parsed {@link SBMLDocument}
   * @throws IOException
   */
  public static SBMLDocument read(File jsonFile) throws IOException {
    JSONparser parser = new JSONparser();
    return parser.parse(jsonFile);
  }


  /**
   * Creates the {@link ModelBuilder}, {@link SBMLDocument} and reads the
   * jsonFile as a tree
   * 
   * @param jsonFile
   * @return
   */
  private SBMLDocument parse(File jsonFile) {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = null;
    try {
      root = mapper.readTree(jsonFile);
    } catch (Exception e) {
      logException(e);
    }
    ModelBuilder builder = new ModelBuilder(3, 1);
    SBMLDocument doc = builder.getSBMLDocument();
    doc.addTreeNodeChangeListener(new UpdateListener());
    builder.buildModel(correctId(crop(root.path("id").toString())), crop(root.path("name").toString()));
    parseModel(builder, root);
    return doc;
  }


  /**
   * Sets all informational fields for the model (id, name, annotation, notes,
   * version), generates a basic unit definition (mmol_per_gDW_per_hr) and calls
   * the parse methods for the main fields (compartments, metabolites, genes,
   * reactions)
   * 
   * @param builder
   * @param root
   * @return
   */
  private void parseModel(ModelBuilder builder, JsonNode root) {
    if (root.isMissingNode()) {
      throw new IllegalArgumentException(mpMessageBundle.getString("ROOT_EMPTY_ERROR"));
    }
    // 4 is the minimum of required fields, 10 the maximum with optional
    // fields, non-conforming models might still get parsed, omitting
    // superfluous information
    if (root.size() < 4 || root.size() > 10) {
      logger.warning(format(mpMessageBundle.getString("NUM_CHILDREN_NOT_IN_RANGE"), root.size()));
    }
    logger.info(mpMessageBundle.getString("JSON_PARSER_STARTED"));
    // get Model and set all informational fields
    Model model = builder.getModel();
    JsonNode annotation = root.path("annotation");
    JsonNode id = root.path("id");
    JsonNode name = root.path("name");
    JsonNode notes = root.path("notes");
    JsonNode version = root.path("version");
    if (annotation.isMissingNode()) {
      logger.fine(mpMessageBundle.getString("ANNOTATION_MISSING"));
    } else {
      if (!annotation.toString().isEmpty()) {
        try {
          model.setAnnotation(checkAnnotation(crop(annotation.toString())));
        } catch (XMLStreamException e) {
          logException(e);
        }
      }
    }
    if (id.isMissingNode()) {
      throw new IllegalArgumentException(mpMessageBundle.getString("MODEL_NULL_ERROR"));
    } else {
      model.setId(correctId(crop(id.toString())));
    }
    if (name.isMissingNode()) {
      logger.fine(mpMessageBundle.getString("NAME_MISSING"));
    } else {
      model.setName(crop(name.toString()));
    }
    if (notes.isMissingNode()) {
      logger.fine(mpMessageBundle.getString("NOTES_MISSING"));
    } else {
      if (!notes.toString().isEmpty()) {
        try {
          model.setNotes(checkNotes(crop(notes.toString())));
        } catch (XMLStreamException e) {
          logException(e);
        }
      }
    }
    if (version.isMissingNode()) {
      logger.fine(mpMessageBundle.getString("VERSION_NR_MISSING"));
    } else {
      model.setVersion(version.asInt());
    }
    // Generate basic unit:
    UnitDefinition ud = builder.buildUnitDefinition("mmol_per_gDW_per_hr", null);
    ModelBuilder.buildUnit(ud, 1d, -3, Unit.Kind.MOLE, 1d);
    ModelBuilder.buildUnit(ud, 1d, 0, Unit.Kind.GRAM, -1d);
    ModelBuilder.buildUnit(ud, 3600d, 0, Unit.Kind.SECOND, -1d);
    // parse main fields
    parseCompartments(builder, root.path("compartments"));
    parseMetabolites(builder, root.path("metabolites"));
    parseGenes(builder, root.path("genes"));
    parseReactions(builder, root.path("reactions"));
  }


  /**
   * @param builder
   * @param compartments
   */
  private void parseCompartments(ModelBuilder builder, JsonNode compartments) {
    if (compartments.isMissingNode()) {
      logger.fine(mpMessageBundle.getString("COMPART_MISSING"));
    }
    int compSize = compartments.size();
    logger.info(format(mpMessageBundle.getString("NUM_COMPART"), compSize));
    if (compSize == 0) {
      return;
    }
    Model model = builder.getModel();
    Iterator<Entry<String, JsonNode>> compIter = compartments.fields();
    while (compIter.hasNext()) {
      String compartment = compIter.next().toString();
      String cId = compartment.substring(0, compartment.indexOf("="));
      String cName = crop(compartment.substring(compartment.indexOf("=") + 1));
      Compartment comp = model.createCompartment(cId);
      comp.setName(cName);
    }
  }


  /**
   * @param builder
   * @param metabolites
   */
  private void parseMetabolites(ModelBuilder builder, JsonNode metabolites) {
    if (metabolites.isMissingNode()) {
      throw new IllegalArgumentException(mpMessageBundle.getString("METABOLITES_MISSING"));
    }
    int metSize = metabolites.size();
    logger.info(format(mpMessageBundle.getString("NUM_METABOLITES"), metSize));
    if (metSize == 0) {
      return;
    }
    Model model = builder.getModel();
    for (int counter = 0; counter < metSize; counter++) {
      JsonNode current = metabolites.path(counter);
      String id = crop(current.path("id").toString());
      if ((id != null) && (!id.isEmpty())) {
        BiGGId biggId = new BiGGId(correctId(id));
        if (!biggId.isSetPrefix() && !PATTERN_BIOMASS_CASE_INSENSITIVE.matcher(biggId.toBiGGId()).find()) {
          biggId.setPrefix(METABOLITE_PREFIX);
        }
        Species species = model.createSpecies(biggId.toBiGGId());
        String name = crop(current.path("name").toString());
        if (name != null && !name.isEmpty()) {
          species.setName(name);
        }
        String charge = crop(current.path("charge").toString());
        String formula = crop(current.path("formula").toString());
        if ((formula != null) || (charge != null)) {
          FBCSpeciesPlugin specPlug = (FBCSpeciesPlugin) species.getPlugin(FBCConstants.shortLabel);
          if (!formula.isEmpty()) {
            specPlug.setChemicalFormula(formula);
          }
          if (!charge.isEmpty()) {
            int metCharge = Integer.parseInt(charge);
            specPlug.setCharge(metCharge);
          }
        }
        String csense = crop(current.path("_constraint_sense").toString());
        if (csense != null && !csense.isEmpty() && !csense.equals("E")) {
          logger.severe(format(mpMessageBundle.getString("NEQ_RELATION_UNSUPPORTED"), species.getId()));
        }
        String compartment = crop(current.path("compartment").toString());
        if (compartment != null && !compartment.isEmpty()) {
          species.setCompartment(compartment);
        }
        String annotation = crop(current.path("annotation").toString());
        if (annotation != null && !annotation.isEmpty()) {
          try {
            species.setAnnotation(checkAnnotation(annotation));
          } catch (XMLStreamException e) {
            logException(e);
          }
        }
        String notes = crop(current.path("notes").toString());
        if (notes != null && !notes.isEmpty()) {
          try {
            species.setNotes(checkNotes(notes));
          } catch (XMLStreamException e) {
            logException(e);
          }
        }
        if (species.isSetAnnotation()) {
          species.setMetaId(species.getId());
        }
      }
    }
  }


  /**
   * @param builder
   * @param genes
   */
  private void parseGenes(ModelBuilder builder, JsonNode genes) {
    if (genes.isMissingNode()) {
      throw new IllegalArgumentException(mpMessageBundle.getString("GENES_MISSING"));
    }
    int genSize = genes.size();
    logger.info(format(mpMessageBundle.getString("NUM_GENES"), genSize));
    if (genSize == 0) {
      return;
    }
    Model model = builder.getModel();
    FBCModelPlugin modelPlug = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
    // "name", "id", "notes", "annotations"
    for (int counter = 0; counter < genSize; counter++) {
      JsonNode current = genes.path(counter);
      String id = crop(current.path("id").toString());
      if (id != null && !id.isEmpty()) {
        BiGGId biggId = new BiGGId(correctId(id));
        if (!biggId.isSetPrefix()) {
          biggId.setPrefix(GENE_PRODUCT_PREFIX);
        }
        GeneProduct gp = modelPlug.createGeneProduct(biggId.toBiGGId());
        gp.setLabel(id);
        String name = crop(current.path("name").toString());
        String notes = crop(current.path("notes").toString());
        String annotation = crop(current.path("annotation").toString());
        if (name != null && name.length() > 0) {
          gp.setName(name);
        } else {
          throw new IllegalArgumentException(mpMessageBundle.getString("GP_NAME_MISSING") + gp.getId());
        }
        if (notes != null && !notes.isEmpty()) {
          try {
            gp.setNotes(checkNotes(notes));
          } catch (XMLStreamException e) {
            logException(e);
          }
        }
        if (annotation != null && !annotation.isEmpty()) {
          try {
            gp.setAnnotation(checkAnnotation(annotation));
          } catch (XMLStreamException e) {
            logException(e);
          }
        }
      }
    }
  }


  /**
   * @param builder
   * @param reactions
   */
  @SuppressWarnings("unchecked")
  private void parseReactions(ModelBuilder builder, JsonNode reactions) {
    if (reactions.isMissingNode()) {
      throw new IllegalArgumentException(mpMessageBundle.getString("REACTIONS_MISSING"));
    }
    int reactSize = reactions.size();
    logger.info(format(mpMessageBundle.getString("NUM_REACTIONS"), reactSize));
    if (reactSize == 0) {
      return;
    }
    Model model = builder.getModel();
    for (int counter = 0; counter < reactSize; counter++) {
      JsonNode current = reactions.path(counter);
      String id = crop(current.path("id").toString());
      if ((id != null) && (!id.isEmpty())) {
        BiGGId biggId = new BiGGId(correctId(id));
        if (!biggId.isSetPrefix()) {
          biggId.setPrefix(REACTION_PREFIX);
        }
        Reaction r = model.createReaction(biggId.toBiGGId());
        String name = crop(current.path("name").toString());
        if (name != null && !name.isEmpty()) {
          r.setName(name);
        }
        FBCReactionPlugin rPlug = (FBCReactionPlugin) r.getPlugin(FBCConstants.shortLabel);
        // used the definition of reversibility given by the cobrapy sbml module
        if (current.path("lower_bound").asDouble() < 0) {
          r.setReversible(true);
        }
        rPlug.setLowerFluxBound(
          builder.buildParameter(r.getId() + "_lb", null, current.path("lower_bound").asDouble(), true, (String) null));
        rPlug.setUpperFluxBound(
          builder.buildParameter(r.getId() + "_ub", null, current.path("upper_bound").asDouble(), true, (String) null));
        String[] metabolites = METABOLITE_DELIMITER.split(crop(current.path("metabolites").toString()));
        for (int i = 0; i < metabolites.length; i++) {
          String type = crop(metabolites[i].substring(0, metabolites[i].indexOf("\":") + 1));
          String value = metabolites[i].substring(metabolites[i].indexOf("\":") + 2, metabolites[i].length());
          // The JSON Strings for value are inconsistent, some use
          // quotation marks, some do not
          if (value.startsWith("\"")) {
            value = crop(value);
          }
          ASTNode ast = null;
          try {
            ast = JSBML.parseFormula(value);
          } catch (ParseException e) {
            logException(e);
          }
          BiGGId metId = new BiGGId(correctId(type));
          if (!PATTERN_BIOMASS_CASE_INSENSITIVE.matcher(metId.toBiGGId()).find()) {
            metId.setPrefix(METABOLITE_PREFIX);
          }
          if (ast.getChildCount() > 1) {
            String paramString = "mu";
            Parameter param = model.getParameter(paramString);
            if (param == null) {
              param = model.createParameter();
              param.setId(paramString);
              param.setConstant(false);
            }
            String mId = correctId(r.getId() + "_Reac_Prod_" + metId.toBiGGId());
            AssignmentRule rule = model.createAssignmentRule();
            rule.setMath(ast);
            rule.setMetaId(mId);
          } else {
            double coeff = 0d;
            if (ast.getType().equals(ASTNode.Type.MINUS)) {
              coeff = ast.getChild(0).getReal() * (-1);
            } else {
              coeff = ast.getReal();
            }
            if (coeff != 0d) {
              Species species = model.getSpecies(metId.toBiGGId());
              if (species == null) {
                species = model.createSpecies(metId.toBiGGId());
                logger.info(format(mpMessageBundle.getString("SPECIES_UNDEFINED"), metId, r.getId()));
              }
              if (coeff < 0d) {
                ModelBuilder.buildReactants(r, pairOf(-coeff, species));
              } else if (coeff > 0d) {
                ModelBuilder.buildProducts(r, pairOf(coeff, species));
              }
            }
          }
        }
        String geneReactionRule = crop(current.path("gene_reaction_rule").toString());
        if (geneReactionRule != null && !geneReactionRule.isEmpty()) {
          SBMLUtils.parseGPR(r, geneReactionRule, false);
        }
        String subsystem = crop(current.path("subsystem").toString());
        if (subsystem != null && !subsystem.isEmpty()) {
          GroupsModelPlugin groupsModelPlugin = (GroupsModelPlugin) model.getPlugin(GroupsConstants.shortLabel);
          Group group = null;
          for (Group existingGroup : groupsModelPlugin.getListOfGroups()) {
            if (name.equals(existingGroup.getName())) {
              group = existingGroup;
              break;
            }
          }
          if (group == null) {
            group = groupsModelPlugin.createGroup();
            group.setName(name);
            group.setKind(Group.Kind.partonomy);
          }
          SBMLUtils.createSubsystemLink(r, group.createMember());
        }
        FBCModelPlugin fbc = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
        Objective obj = fbc.getObjective(0);
        if (obj == null) {
          obj = fbc.createObjective("obj");
          obj.setType(Objective.Type.MAXIMIZE);
          fbc.getListOfObjectives().setActiveObjective(obj.getId());
        }
        Double coefficient = current.path("objective_coefficient").asDouble();
        if (coefficient != null && coefficient != 0d) {
          FluxObjective fo = obj.createFluxObjective("fo_" + r.getId());
          fo.setCoefficient(coefficient);
          fo.setReaction(r);
        }
        String notes = crop(current.path("notes").toString());
        if (notes != null && !notes.isEmpty()) {
          try {
            r.setNotes(checkNotes(notes));
          } catch (XMLStreamException e) {
            logException(e);
          }
        }
        String annotation = crop(current.path("annotation").toString());
        if (annotation != null && !annotation.isEmpty()) {
          try {
            r.setAnnotation(checkAnnotation(annotation));
          } catch (XMLStreamException e) {
            logException(e);
          }
        }
      }
    }
  }


  /**
   * @param annotation
   * @return
   */
  private String checkAnnotation(String annotation) {
    if (annotation.startsWith("<")) {
      return annotation;
    } else {
      return "<annotation>" + annotation + "</annotation>";
    }
  }


  /**
   * @param notes
   * @return
   */
  private String checkNotes(String notes) {
    if (notes.startsWith("<")) {
      return notes;
    } else {
      return "<notes>" + notes + "</notes>";
    }
  }


  /**
   * Copied from COBRAparser: Checks id strings for BiGGId conformity and
   * modifies them if needed
   * 
   * @param id
   * @return
   */
  private String correctId(String id) {
    StringBuilder newId = new StringBuilder(id.length() + 4);
    char c = id.charAt(0);
    // Must start with letter or '_'.
    if (!(((c >= 97) && (c <= 122)) || ((c >= 65) && (c <= 90)) || c == '_')) {
      newId.append("_");
    }
    // May contain letters, digits or '_'
    for (int i = 0; i < id.length(); i++) {
      c = id.charAt(i);
      if (((c == ' ') || (c < 48) || ((57 < c) && (c < 65)) || ((90 < c) && (c < 97)))) {
        if (i < id.length() - 1) {
          newId.append('_'); // Replace spaces and special characters
          // with "_"
        }
      } else {
        newId.append(c);
      }
    }
    if (!newId.toString().equals(id)) {
      logger.fine(format(mpMessageBundle.getString("CHANGED_METABOLITE_ID"), id, newId));
    }
    return newId.toString();
  }


  /**
   * @param str
   * @return str, without the first and last character, used to remove leading
   *         and trailing quotation marks
   */
  private String crop(String str) {
    if (str.length() > 1) {
      return str.substring(1, str.length() - 1);
    }
    return str;
  }


  /**
   * @param exc
   */
  private void logException(Exception exc) {
    logger.warning(format("{0}: {1}", exc.getClass().getSimpleName(), Utils.getMessage(exc)));
  }
}
