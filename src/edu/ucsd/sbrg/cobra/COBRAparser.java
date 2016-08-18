/**
 * 
 */
package edu.ucsd.sbrg.cobra;

import static org.sbml.jsbml.util.Pair.pairOf;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.TidySBMLWriter;
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
import org.sbml.jsbml.util.ModelBuilder;

import com.jmatio.io.MatFileReader;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLCell;
import com.jmatio.types.MLChar;
import com.jmatio.types.MLDouble;
import com.jmatio.types.MLInt64;
import com.jmatio.types.MLNumericArray;
import com.jmatio.types.MLSparse;
import com.jmatio.types.MLStructure;

import de.zbit.sbml.util.SBMLtools;
import de.zbit.util.Utils;
import edu.ucsd.sbrg.bigg.BiGGId;
import edu.ucsd.sbrg.bigg.MIRIAM;
import edu.ucsd.sbrg.util.SBMLUtils;
import edu.ucsd.sbrg.util.UpdateListener;

/**
 * @author Andreas Dr&auml;ger
 */
public class COBRAparser {

  private static final String           DELIM               = " ,;\t\n\r\f";
  private static final String           GENE_PRODUCT_PREFIX = "G";
  private static final String           REACTION_PREFIX     = "R";
  private static final String           METABOLITE_PREFIX   = "M";
  /**
   * A {@link Logger} for this class.
   */
  private static final transient Logger logger              =
    Logger.getLogger(COBRAparser.class.getName());


  /**
   * @param args
   *        path to a file to be parsed and to an output SBML file.
   */
  public static void main(String[] args)
    throws IOException, SBMLException, XMLStreamException {
    SBMLDocument doc = COBRAparser.read(new File(args[0]));
    // TidySBMLWriter.write(doc, System.out, ' ', (short) 2);
    TidySBMLWriter.write(doc, new File(args[1]),
      COBRAparser.class.getSimpleName(), "1.0", ' ', (short) 2);
  }


  /**
   * @param matFile
   * @return
   * @throws IOException
   */
  public static SBMLDocument read(File matFile) throws IOException {
    return read(matFile, false);
  }


  /**
   * @param matFile
   * @param omitGenericTerms
   * @return
   * @throws IOException
   */
  public static SBMLDocument read(File matFile, boolean omitGenericTerms)
    throws IOException {
    COBRAparser parser = new COBRAparser();
    parser.setOmitGenericTerms(omitGenericTerms);
    return parser.parse(matFile);
  }


  /**
   * 
   */
  public COBRAparser() {
    super();
    setOmitGenericTerms(false);
  }

  /**
   * 
   */
  private boolean omitGenericTerms;


  /**
   * @return the omitGenericTerms
   */
  public boolean getOmitGenericTerms() {
    return omitGenericTerms;
  }


  /**
   * @param omitGenericTerms
   *        the omitGenericTerms to set
   */
  public void setOmitGenericTerms(boolean omitGenericTerms) {
    this.omitGenericTerms = omitGenericTerms;
  }


  /**
   * @param matFile
   * @return
   * @throws IOException
   */
  private SBMLDocument parse(File matFile) throws IOException {
    MatFileReader mfr = new MatFileReader(matFile);
    ModelBuilder builder = new ModelBuilder(3, 1);
    SBMLDocument doc = builder.getSBMLDocument();
    doc.addTreeNodeChangeListener(new UpdateListener());
    Map<String, MLArray> content = mfr.getContent();
    if (content.keySet().size() > 1) {
      logger.warning(MessageFormat.format(
        "Parsing only one randomly selected model of {0} available models in the given file {1}.",
        content.keySet().size(), matFile.getAbsolutePath()));
    }
    for (String key : content.keySet()) {
      MLArray array = content.get(key);
      builder.buildModel(SBMLtools.toSId(array.getName()), null);
      parseModel(builder, array);
      break;
    }
    return doc;
  }


  /**
   * @param builder
   * @param genes
   */
  private void parseGenes(ModelBuilder builder, MLCell genes) {
    if (genes == null) {
      return;
    }
    Model model = builder.getModel();
    FBCModelPlugin modelPlug =
      (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
    for (int i = 0; i < genes.getSize(); i++) {
      String id = toString(genes.get(i), genes.getName(), i + 1);
      if ((id != null) && (id.length() > 0)) {
        try {
          BiGGId biggId = new BiGGId(correctId(id));
          if (!biggId.isSetPrefix()) {
            biggId.setPrefix(GENE_PRODUCT_PREFIX);
          }
          GeneProduct gp = modelPlug.createGeneProduct(biggId.toBiGGId());
          gp.setLabel(id);
          gp.setName(id);
        } catch (IllegalArgumentException exc) {
          logException(exc);
        }
      }
    }
  }


  /**
   * @param builder
   * @param mets
   * @param metNames
   * @param metFormulas
   * @param metCharge
   * @param metCHEBIID
   * @param metHMDB
   * @param metInchiString
   * @param metKeggID
   * @param metPubChemID
   * @param metSmile
   */
  private void parseMetabolites(ModelBuilder builder, MLCell mets,
    MLCell metNames, MLCell metFormulas, MLDouble metCharge, MLCell metCHEBIID,
    MLCell metHMDB, MLCell metInchiString, MLCell metKeggID,
    MLCell metPubChemID, MLCell metSmile) {
    Model model = builder.getModel();
    for (int i = 0; i < mets.getSize(); i++) {
      String id = toString(mets.get(i), mets.getName(), i + 1);
      if ((id != null) && (id.length() > 0)) {
        try {
          BiGGId biggId = new BiGGId(correctId(id));
          if (!biggId.isSetPrefix()) {
            biggId.setPrefix(METABOLITE_PREFIX);
          }
          Species species = model.createSpecies(biggId.toBiGGId());
          species.setName(toString(metNames.get(i), metNames.getName(), i + 1));
          if ((metFormulas != null) || (metCharge != null)) {
            FBCSpeciesPlugin specPlug =
              (FBCSpeciesPlugin) species.getPlugin(FBCConstants.shortLabel);
            if (exists(metFormulas, i)) {
              specPlug.setChemicalFormula(
                toString(metFormulas.get(i), metFormulas.getName(), i + 1));
            }
            if ((metCharge != null) && (metCharge.get(i) != null)) {
              double charge = metCharge.get(i).doubleValue();
              specPlug.setCharge((int) charge);
              if (charge - ((int) charge) != 0d) {
                logger.warning(MessageFormat.format(
                  "Non-integer charge {0,number} was trunkated to {1}.", charge,
                  specPlug.getCharge()));
              }
            }
          }
          CVTerm term = new CVTerm();
          term.setQualifierType(CVTerm.Type.BIOLOGICAL_QUALIFIER);
          term.setBiologicalQualifierType(CVTerm.Qualifier.BQB_IS);
          addResource(metCHEBIID, i, term, MIRIAM.chebi);
          addResource(metHMDB, i, term, MIRIAM.hdmb);
          addResource(metInchiString, i, term, MIRIAM.INCHI);
          addResource(metKeggID, i, term, MIRIAM.KEGGID);
          addResource(metPubChemID, i, term, MIRIAM.PUBCHEMID);
          if (term.getResourceCount() > 0) {
            species.addCVTerm(term);
          }
          if ((metSmile != null) && (metSmile.get(i) != null)) {
            // For some reason, the mat files appear to store the creation date
            // in the field smile, rather than a smiles string.
            String smile = toString(metSmile.get(i), metSmile.getName(), i + 1);
            Date date = parseDate(smile);
            if (date != null) {
              species.createHistory().setCreatedDate(date);
            } else if (!isEmptyString(smile)) {
              try {
                species.appendNotes("<html:p>SMILES: " + smile + "</html:p>");
              } catch (XMLStreamException exc) {
                logException(exc);
              }
            }
          }
          if (species.isSetAnnotation()) {
            species.setMetaId(species.getId());
          }
        } catch (IllegalArgumentException exc) {
          logException(exc);
        }
      }
    }
  }


  /**
   * Attempts to parse the given String into a {@link Date} object. To this end,
   * it applies some date format patterns that have been found in some mat
   * files.
   * 
   * @param dateString
   *        the {@link String} to be parsed, i.e., what is assumed to
   *        be a representation of some date.
   * @return a {@link Date} object or {@code null} if parsing fails.
   */
  private Date parseDate(String dateString) {
    try {
      return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(dateString);
    } catch (ParseException exc) {
      try {
        return new SimpleDateFormat("yyyy/MM/dd").parse(dateString);
      } catch (ParseException exc1) {
      }
    }
    return null;
  }


  /**
   * @param cell
   * @param i
   *        the index within the cell.
   * @param term
   * @param catalog
   */
  private void addResource(MLCell cell, int i, CVTerm term, MIRIAM catalog) {
    if (exists(cell, i)) {
      String id = toMIRIAMid(cell.get(i));
      if ((id != null) && !id.isEmpty()) {
        String resource = MIRIAM.toResourceURL(catalog, id);
        if ((resource != null) && !resource.isEmpty()) {
          term.addResource(resource);
        }
      }
    }
  }


  /**
   * @param array
   * @return
   */
  private String toMIRIAMid(MLArray array) {
    return toMIRIAMid(toString(array));
  }


  /**
   * @param idCandidate
   * @return
   */
  private String toMIRIAMid(String idCandidate) {
    if ((idCandidate == null) || idCandidate.isEmpty()) {
      return null;
    }
    int start = 0;
    int end = idCandidate.length() - 1;
    if ((idCandidate.charAt(start) == '[')
      || (idCandidate.charAt(start) == '\'')) {
      start++;
    }
    if ((idCandidate.charAt(end) == ']') || (idCandidate.charAt(end) == '\'')) {
      end--;
    }
    return (start < end) ? idCandidate.substring(start, end) : null;
  }


  /**
   * @param cell
   * @param i
   * @return
   */
  private boolean exists(MLArray cell, int i) {
    if (cell != null) {
      if (cell instanceof MLCell) {
        return (((MLCell) cell).get(i) != null);
      }
      return true;
    }
    return false;
  }


  /**
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
      if (((c == ' ') || (c < 48) || ((57 < c) && (c < 65))
        || ((90 < c) && (c < 97)))) {
        if (i < id.length() - 1) {
          newId.append('_'); // Replace spaces and special characters with "_"
        }
      } else {
        newId.append(c);
      }
    }
    if (!newId.toString().equals(id)) {
      logger.warning(MessageFormat.format(
        "Changed metabolite id from ''{0}'' to ''{1}'' in order to match the BiGG id specification.",
        id, newId));
    }
    return newId.toString();
  }


  /**
   * @param builder
   * @param array
   * @return
   */
  private Model parseModel(ModelBuilder builder, MLArray array) {
    // TODO: always check for null!
    if ((array.getSize() != 1) && !array.isStruct()) {
      throw new IllegalArgumentException(MessageFormat.format(
        "Expected data struct of size 1, but found array ''{1}'' of size {0,number,integer}.",
        array.getSize(), array.getName()));
    }
    MLStructure struct = (MLStructure) array;
    // Check that the given data structure only contains allowable entries:
    for (MLArray field : struct.getAllFields()) {
      try {
        logger.finest(MessageFormat.format("Found model component {0}.",
          ModelField.valueOf(field.getName())));
      } catch (IllegalArgumentException exc) {
        logException(exc);
      }
    }
    // parse model
    Model model = builder.getModel();
    parseDescription(model, struct.getField(ModelField.description.name()));
    // Generate basic unit:
    UnitDefinition ud =
      builder.buildUnitDefinition("mmol_per_gDW_per_hr", null);
    ModelBuilder.buildUnit(ud, 1d, -3, Unit.Kind.MOLE, 1d);
    ModelBuilder.buildUnit(ud, 1d, 0, Unit.Kind.GRAM, -1d);
    ModelBuilder.buildUnit(ud, 3600d, 0, Unit.Kind.SECOND, -1d);
    // parse metabolites
    MLCell mets = toMLCell(struct, ModelField.mets);
    MLCell metNames = toMLCell(struct, ModelField.metNames);
    MLCell metFormulas = toMLCell(struct, ModelField.metFormulas);
    MLDouble metCharge = toMLDouble(struct, ModelField.metCharge);
    MLCell metCHEBIID = toMLCell(struct, ModelField.metCHEBIID);
    MLCell metHMDB = toMLCell(struct, ModelField.metHMDB);
    MLCell metInchiString = toMLCell(struct, ModelField.metInchiString);
    MLCell metKeggID = toMLCell(struct, ModelField.metKeggID);
    MLCell metPubChemID = toMLCell(struct, ModelField.metPubChemID);
    MLCell metSmile = toMLCell(struct, ModelField.metSmile);
    parseMetabolites(builder, mets, metNames, metFormulas, metCharge,
      metCHEBIID, metHMDB, metInchiString, metKeggID, metPubChemID, metSmile);
    // parse genes
    MLCell genes = toMLCell(struct.getField(ModelField.mets.name()));
    parseGenes(builder, genes);
    // parse reactions
    MLCell rxns = toMLCell(struct, ModelField.rxns);
    MLCell rxnNames = toMLCell(struct, ModelField.rxnNames);
    MLCell rxnKeggID = toMLCell(struct, ModelField.rxnKeggID);
    MLCell ecNumbers = toMLCell(struct, ModelField.ecNumbers);
    MLCell confidenceScores = toMLCell(struct, ModelField.confidenceScores);
    MLCell citations = toMLCell(struct, ModelField.citations);
    MLCell comments = toMLCell(struct, ModelField.comments);
    MLNumericArray<?> rev = toMLNumericArray(struct, ModelField.rev);
    MLDouble lb = toMLDouble(struct.getField(ModelField.lb.name()));
    MLDouble ub = toMLDouble(struct.getField(ModelField.ub.name()));
    MLSparse S = toMLSparse(struct.getField(ModelField.S.name()));
    parseRxns(builder, rxns, rxnNames, rev, S, mets, lb, ub, rxnKeggID,
      ecNumbers, confidenceScores, citations, comments);
    // parse gprs
    MLCell grRules = toMLCell(struct, ModelField.grRules);
    for (int i = 0; i < grRules.getSize(); i++) {
      String geneReactionRule =
        toString(grRules.get(i), grRules.getName(), i + 1);
      SBMLUtils.parseGPR(model.getReaction(i), geneReactionRule,
        omitGenericTerms);
    }
    // parse subsystems
    MLCell subSystems = toMLCell(struct, ModelField.subSystems);
    if ((subSystems != null) && (subSystems.getSize() > 0)) {
      parseSubsystems(model, subSystems);
    }
    FBCModelPlugin fbc =
      (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
    Objective obj = fbc.createObjective("obj");
    obj.setType(Objective.Type.MAXIMIZE);
    fbc.getListOfObjectives().setActiveObjective(obj.getId());
    MLChar csense = toMLChar(struct, ModelField.csense);
    if (csense != null) {
      for (int i = 0; i < csense.getSize(); i++) {
        char c = csense.getChar(0, i);
        // TODO: only 'E' (equality) is supported for now!
        if (c != 'E') {
          logger.severe(MessageFormat.format(
            "Unsupported nonequality relationship for metabolite with id ''{0}''.",
            model.getSpecies(i).getId()));
        }
      }
    }
    MLNumericArray<?> coefficients = toMLNumericArray(struct, ModelField.c);
    if (coefficients != null) {
      for (int i = 0; i < coefficients.getSize(); i++) {
        double coef = coefficients.get(i).doubleValue();
        if (coef != 0d) {
          Reaction r = model.getReaction(i);
          FluxObjective fo = obj.createFluxObjective("fo_" + r.getId());
          fo.setCoefficient(coef);
          fo.setReaction(r);
        }
      }
    }
    MLNumericArray<?> b = toMLNumericArray(struct, ModelField.b);
    if (b != null) {
      for (int i = 0; i < b.getSize(); i++) {
        double bVal = b.get(i).doubleValue();
        if (bVal != 0d) {
          // TODO: this should be incorporated into FBC version 3.
          logger.warning(MessageFormat.format(
            "Skipping unsupported non-zero b-value of {0,number,######.####} for metabolite {1}",
            bVal, model.getSpecies(i).getId()));
        }
      }
    }
    return model;
  }


  /**
   * @param model
   * @param descriptionField
   */
  private void parseDescription(Model model, MLArray descriptionField) {
    if (descriptionField != null) {
      if (descriptionField.isChar()) {
        MLChar description = (MLChar) descriptionField;
        if (description.getDimensions()[0] == 1) {
          model.setName(description.getString(0));
        } else {
          logger.warning(
            MessageFormat.format("Found more than one identifier in cell {0}",
              descriptionField.contentToString()));
        }
      } else if (descriptionField.isStruct()) {
        MLStructure descrStruct = (MLStructure) descriptionField;
        MLCell name = toMLCell(descrStruct, ModelField.name);
        MLCell organism = toMLCell(descrStruct, ModelField.organism);
        MLCell author = toMLCell(descrStruct, ModelField.author);
        MLCell geneindex = toMLCell(descrStruct, ModelField.geneindex);
        MLCell genedate = toMLCell(descrStruct, ModelField.genedate);
        MLCell genesource = toMLCell(descrStruct, ModelField.genesource);
        MLCell notes = toMLCell(descrStruct, ModelField.notes);
        if (name != null) {
          model.setName(toString(name));
        }
        if (organism != null) {
          // TODO
        }
        if (author != null) {
          // TODO
        }
        if (geneindex != null) {
          // TODO
        }
        if (genedate != null) {
          // TODO
        }
        if (genesource != null) {
          // TODO
        }
        if (notes != null) {
          try {
            model.appendNotes(SBMLtools.toNotesString(toString(notes)));
          } catch (XMLStreamException exc) {
            logException(exc);
          }
        }
      }
    } else {
      logger.warning(
        MessageFormat.format("Missing field: {0}", ModelField.description));
    }
  }


  /**
   * @param exc
   */
  private void logException(Exception exc) {
    logger.warning(MessageFormat.format("{0}: {1}",
      exc.getClass().getSimpleName(), Utils.getMessage(exc)));
  }


  /**
   * @param struct
   * @param field
   * @return
   */
  private MLNumericArray<?> toMLNumericArray(MLStructure struct,
    ModelField field) {
    return toMLNumericArray(struct.getField(field.name()));
  }


  /**
   * @param model
   * @param subSystems
   */
  private void parseSubsystems(Model model, MLCell subSystems) {
    Map<String, Group> nameToGroup = new HashMap<>(); // this is to avoid
                                                      // creating the identical
                                                      // group multiple times.
    GroupsModelPlugin groupsModelPlugin =
      (GroupsModelPlugin) model.getPlugin(GroupsConstants.shortLabel);
    for (int i = 0; i < subSystems.getSize(); i++) {
      String name = toString(subSystems.get(i), subSystems.getName(), i + 1);
      Group group = nameToGroup.get(name);
      if (group == null) {
        group = groupsModelPlugin.createGroup();
        group.setName(name);
        group.setKind(Group.Kind.partonomy);
        nameToGroup.put(name, group);
      }
      SBMLUtils.createSubsystemLink(model.getReaction(i), group.createMember());
    }
  }


  /**
   * @param struct
   * @param field
   * @return
   */
  private MLDouble toMLDouble(MLStructure struct, ModelField field) {
    return toMLDouble(struct.getField(field.name()));
  }


  /**
   * @param struct
   * @param field
   * @return an {@link MLCell} or {@code null} if no such entry exists in the
   *         given data structure.
   */
  private MLCell toMLCell(MLStructure struct, ModelField field) {
    return toMLCell(struct.getField(field.name()));
  }


  /**
   * @param struct
   * @param field
   * @return
   */
  private MLChar toMLChar(MLStructure struct, ModelField field) {
    return toMLChar(struct.getField(field.name()));
  }


  /**
   * @param builder
   * @param rxns
   * @param rxnNames
   * @param rev
   * @param S
   * @param mets
   * @param lb
   * @param ub
   * @param rxnKeggID
   * @param ecNumbers
   * @param confidenceScores
   * @param citations
   * @param comments
   */
  @SuppressWarnings("unchecked")
  private <T extends Number> void parseRxns(ModelBuilder builder, MLCell rxns,
    MLCell rxnNames, MLNumericArray<T> rev, MLSparse S, MLCell mets,
    MLDouble lb, MLDouble ub, MLCell rxnKeggID, MLCell ecNumbers,
    MLCell confidenceScores, MLCell citations, MLCell comments) {
    Model model = builder.getModel();
    for (int j = 0; j < rxns.getSize(); j++) {
      String rId = toString(rxns.get(j), rxns.getName(), j + 1);
      if ((rId != null) && (rId.length() > 0)) {
        try {
          BiGGId biggId = new BiGGId(correctId(rId));
          if (!biggId.isSetPrefix()) {
            biggId.setPrefix(REACTION_PREFIX);
          }
          Reaction r = model.createReaction(biggId.toBiGGId());
          r.setName(toString(rxnNames.get(j), rxnNames.getName(), j + 1));
          r.setReversible(rev.get(j).doubleValue() != 0d);
          FBCReactionPlugin rPlug =
            (FBCReactionPlugin) r.getPlugin(FBCConstants.shortLabel);
          rPlug.setLowerFluxBound(builder.buildParameter(r.getId() + "_lb",
            null, lb.get(j), true, (String) null));
          rPlug.setUpperFluxBound(builder.buildParameter(r.getId() + "_ub",
            null, ub.get(j), true, (String) null));
          // Take the current column of S and look for all non-zero coefficients
          for (int i = 0; i < S.getM(); i++) {
            double coeff = S.get(i, j).doubleValue();
            if (coeff != 0d) {
              try {
                BiGGId metId = new BiGGId(
                  correctId(toString(mets.get(i), mets.getName(), i + 1)));
                metId.setPrefix(METABOLITE_PREFIX);
                Species species = model.getSpecies(metId.toBiGGId());
                if (coeff < 0d) { // Reactant
                  ModelBuilder.buildReactants(r, pairOf(-coeff, species));
                } else if (coeff > 0d) { // Product
                  ModelBuilder.buildProducts(r, pairOf(coeff, species));
                }
              } catch (IllegalArgumentException exc) {
                logger.warning(MessageFormat.format(
                  "Could not add reaction participant because of invalid id: {0}",
                  Utils.getMessage(exc)));
              }
            }
          }
          if (exists(rxnKeggID, j)) {
            parseRxnKEGGids(
              toString(rxnKeggID.get(j), rxnKeggID.getName(), j + 1), r);
          }
          if (exists(ecNumbers, j)) {
            parseECcodes(toString(ecNumbers.get(j), ecNumbers.getName(), j + 1),
              r);
          }
          if (exists(comments, j)) {
            String comment =
              toString(comments.get(j), comments.getName(), j + 1);
            appendComment(comment, r);
          }
          if (exists(confidenceScores, j)) {
            MLArray cell = confidenceScores.get(j);
            if (cell instanceof MLDouble) {
              Double score = ((MLDouble) cell).get(0);
              logger.fine(MessageFormat.format(
                "Reaction {1} has confident score {0,number,##}.", score,
                r.getId()));
              builder.buildParameter(
                "P_confidenceScore_of_" + SBMLtools.toSId(rId), // id
                MessageFormat.format("Confidence score of reaction {0}",
                  r.isSetName() ? r.getName() : r.getId()), // name
                score, // value
                true, // constant
                Unit.Kind.DIMENSIONLESS // unit
              ).setSBOTerm(613); // TODO: there should be a specific term for
                                 // confidence scores. Use "613 - reaction
                                 // parameter" for now.
            } else {
              logger.warning(
                MessageFormat.format("Expected MLDouble, but received {0}.",
                  cell.getClass().getSimpleName()));
            }
          }
          if (exists(citations, j)) {
            parseCitation(
              toString(citations.get(j), citations.getName(), j + 1), r);
          }
          if (r.getCVTermCount() > 0) {
            r.setMetaId(r.getId());
          }
        } catch (IllegalArgumentException exc) {
          logException(exc);
        }
      }
    }
  }


  /**
   * @param citation
   * @param r
   */
  private void parseCitation(String citation, Reaction r) {
    StringBuilder otherCitation = new StringBuilder();
    if (!isEmptyString(citation)) {
      CVTerm term = new CVTerm(CVTerm.Type.BIOLOGICAL_QUALIFIER,
        CVTerm.Qualifier.BQB_IS_DESCRIBED_BY);
      StringTokenizer st = new StringTokenizer(citation, ",");
      while (st.hasMoreElements()) {
        String ref = st.nextElement().toString().trim();
        if (!addResource(ref, term, MIRIAM.PUBMED)) {
          if (!addResource(ref, term, MIRIAM.DOI)) {
            if (otherCitation.length() > 0) {
              otherCitation.append(", ");
            }
            otherCitation.append(ref);
          }
        }
      }
      if (otherCitation.length() > 0) {
        try {
          if (r.isSetNotes()) {
            r.appendNotes("\n\nReference: " + otherCitation);
          } else {
            r.appendNotes(SBMLtools.toNotesString(
              "Reference: " + otherCitation.toString()));
          }
        } catch (XMLStreamException exc) {
          logException(exc);
        }
      }
      if ((term.getResourceCount() > 0) && (term.getParent() == null)) {
        r.addCVTerm(term);
      }
    }
  }


  /**
   * @param comment
   * @param sbase
   */
  private void appendComment(String comment, SBase sbase) {
    try {
      if (!isEmptyString(comment)) {
        sbase.appendNotes(SBMLtools.toNotesString("<p>" + comment + "</p>"));
      }
    } catch (XMLStreamException exc) {
      logException(exc);
    }
  }


  /**
   * @param keggId
   * @param r
   */
  private void parseRxnKEGGids(String keggId, Reaction r) {
    if (!isEmptyString(keggId)) {
      MIRIAM kegg = MIRIAM.KEGGID;
      String catalog = "kegg.reaction";
      Pattern pattern = kegg.getPattern(catalog);
      CVTerm term = findOrCreateCVTerm(r, CVTerm.Qualifier.BQB_IS);
      StringTokenizer st = new StringTokenizer(keggId, DELIM);
      while (st.hasMoreElements()) {
        String kId = st.nextElement().toString().trim();
        if (!kId.isEmpty() && pattern.matcher(kId).matches()) {
          term.addResource(MIRIAM.toResourceURL(kegg, kId));
        }
      }
      if (term.getResourceCount() == 0) {
        // This is actually bad.. should only be KEGG ids, not EC-Codes
        parseECcodes(keggId, r);
      }
      if ((term.getResourceCount() > 0) && (term.getParent() == null)) {
        r.addCVTerm(term);
      }
    }
  }


  /**
   * @param ec
   * @param r
   */
  private void parseECcodes(String ec, Reaction r) {
    if (!isEmptyString(ec)) {
      CVTerm term = findOrCreateCVTerm(r, CVTerm.Qualifier.BQB_HAS_PROPERTY);
      MIRIAM miriam = MIRIAM.EC_CODE;
      for (String catalog : miriam.getCatalogs()) {
        StringTokenizer st = new StringTokenizer(ec, DELIM);
        boolean match = false;
        while (st.hasMoreElements()) {
          String ecCode = st.nextElement().toString().trim();
          // if (ecCode.startsWith("E") || ecCode.startsWith("T")) {
          // ecCode = ecCode.substring(1);
          // }
          if ((ecCode != null) && !ecCode.isEmpty()
            && miriam.getPattern(catalog).matcher(ecCode).matches()) {
            String resource = MIRIAM.toResourceURL(catalog, ecCode);
            if ((resource != null) && !term.getResources().contains(resource)) {
              match = term.addResource(resource);
            }
          }
        }
        if (!match) {
          logger.warning(MessageFormat.format(
            "Could not recognize any of the EC codes from {0}.", ec));
        }
      }
      if ((term.getResourceCount() > 0) && (term.getParent() == null)) {
        r.addCVTerm(term);
      }
    }
  }


  /**
   * Searches for the first {@link CVTerm} within the given {@link SBase} that
   * has the given {@link CVTerm.Qualifier}.
   * 
   * @param sbase
   * @param qualifier
   * @return the found {@link CVTerm} or a new {@link CVTerm} if non exists.
   *         To distinguish between both cases, test if the parent is
   *         {@code null}.
   */
  private CVTerm findOrCreateCVTerm(SBase sbase, CVTerm.Qualifier qualifier) {
    if (sbase.getCVTermCount() > 0) {
      for (CVTerm term : sbase.getCVTerms()) {
        if (term.getQualifier().equals(qualifier)) {
          return term;
        }
      }
    }
    return new CVTerm(CVTerm.Type.BIOLOGICAL_QUALIFIER, qualifier);
  }


  /**
   * Tries to update a resource according to pre-defined rules. If the resource
   * starts with the MIRIAM name followed by a colon, its value is added to the
   * given term. This method assumes that there is a colon between catalog id
   * and resource id. If this is not the case, {@code false} will be returned.
   * 
   * @param resource
   * @param term
   * @param miriam
   * @return {@code true} if successful, {@code false} otherwise.
   */
  private boolean addResource(String resource, CVTerm term, MIRIAM miriam) {
    for (String catalog : miriam.getCatalogs()) {
      StringTokenizer st = new StringTokenizer(resource, " ");
      while (st.hasMoreElements()) {
        String r = st.nextElement().toString().trim();
        if (r.contains(":")) {
          r = r.substring(r.indexOf(':') + 1).trim();
        } else {
          continue;
        }
        if (r.endsWith("'") || r.endsWith(".")) {
          r = r.substring(0, r.length() - 1);
        }
        if (miriam.getPattern(catalog).matcher(r).find()) {
          if (!resource.isEmpty()) {
            if (st.countTokens() > 1) {
              logger.warning(MessageFormat.format(
                "Skipping comment for resource: ''{0}'', only keeping reference ''{1}'' to {2}.",
                resource, r, catalog));
            }
            return term.addResource(MIRIAM.toResourceURL(miriam, r));
          }
        }
      }
    }
    return false;
  }


  /**
   * @param string
   *        the {@link String} to be tested.
   * @return {@code true} if the given {@link String} is either {@code null} or
   *         empty or equals empty square brackets.
   */
  private boolean isEmptyString(String string) {
    return (string == null) || string.isEmpty() || string.equals("[]");
  }


  /**
   * @param array
   * @return
   */
  private MLInt64 toInt64(MLArray array) {
    if (array.isInt64()) {
      MLInt64 integer = (MLInt64) array;
      return integer;
    }
    throw new IllegalArgumentException(MessageFormat.format(
      "Expected data structure ''{1}'' to be of type int64, but received type {0}.",
      MLArray.typeToString(array.getType()), array.getName()));
  }


  /**
   * @param array
   * @return
   */
  private MLCell toMLCell(MLArray array) {
    if (array != null) {
      if (array.isCell()) {
        return (MLCell) array;
      }
      logger.warning(MessageFormat.format(
        "Expected data structure ''{1}'' to be of type cell, but received type {0}.",
        MLArray.typeToString(array.getType()), array.getName()));
    }
    return null;
  }


  /**
   * @param field
   * @return
   */
  private MLChar toMLChar(MLArray array) {
    if (array != null) {
      if (array.isChar()) {
        return (MLChar) array;
      }
      logger.warning(MessageFormat.format(
        "Expected data structure ''{1}'' to be of type char, but received type {0}.",
        MLArray.typeToString(array.getType()), array.getName()));
    }
    return null;
  }


  /**
   * @param array
   * @return
   */
  private MLDouble toMLDouble(MLArray array) {
    if (array != null) {
      if (array.isDouble()) {
        MLDouble real = (MLDouble) array;
        return real;
      }
      logger.warning(MessageFormat.format(
        "Expected data structure ''{1}'' to be of type double, but received type {0}.",
        MLArray.typeToString(array.getType()), array.getName()));
    }
    return null;
  }


  /**
   * @param field
   * @return
   */
  private MLNumericArray<?> toMLNumericArray(MLArray array) {
    if (array instanceof MLNumericArray<?>) {
      MLNumericArray<?> mlna = (MLNumericArray<?>) array;
      return mlna;
    }
    return null;
  }


  /**
   * @param array
   * @return
   */
  private MLSparse toMLSparse(MLArray array) {
    if (array.isSparse()) {
      MLSparse sparse = (MLSparse) array;
      return sparse;
    }
    logger.warning(MessageFormat.format(
      "Expected data structure ''{1}'' to be of type sparse array, but received type {0}.",
      MLArray.typeToString(array.getType()), array.getName()));
    return null;
  }


  private String toString(MLArray array) {
    return toString(array, null, -1);
  }


  /**
   * @param array
   * @param parentName
   * @param parentIndex
   * @return
   */
  private String toString(MLArray array, String parentName, int parentIndex) {
    StringBuilder sb = new StringBuilder();
    if (array.isChar()) {
      MLChar string = (MLChar) array;
      if (string.getDimensions()[0] > 1) {
        logger.fine(MessageFormat.format(
          "Found more than one string in cell {0}", string.contentToString()));
      }
      for (int i = 0; i < string.getDimensions()[0]; i++) {
        if (i > 0) {
          sb.append('\n');
        }
        sb.append(string.getString(i));
      }
    } else if (!Arrays.equals(array.getDimensions(), new int[] {0, 0})) {
      String name = array.getName();
      String pos = "";
      if (name.equals("@") && (parentName != null)) {
        name = parentName;
        pos = "at position " + parentIndex;
      }
      logger.warning(MessageFormat.format(
        "Expected character string in data field ''{1}''{2}, but received data type {0}.",
        MLArray.typeToString(array.getType()), name, pos));
    }
    return sb.toString();
  }
}
