/**
 * 
 */
package edu.ucsd.sbrg.cobra;

import static edu.ucsd.sbrg.bigg.ModelPolisher.mpMessageBundle;
import static java.text.MessageFormat.format;
import static org.sbml.jsbml.util.Pair.pairOf;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.identifiers.registry.RegistryLocalProvider;
import org.identifiers.registry.RegistryUtilities;
import org.identifiers.registry.data.DataType;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBase;
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
import org.sbml.jsbml.util.ModelBuilder;

import com.jmatio.io.MatFileReader;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLCell;
import com.jmatio.types.MLChar;
import com.jmatio.types.MLDouble;
import com.jmatio.types.MLNumericArray;
import com.jmatio.types.MLSparse;
import com.jmatio.types.MLStructure;

import de.zbit.sbml.util.SBMLtools;
import de.zbit.util.Utils;
import edu.ucsd.sbrg.bigg.BiGGId;
import edu.ucsd.sbrg.util.SBMLUtils;
import edu.ucsd.sbrg.util.UpdateListener;

/**
 * @author Andreas Dr&auml;ger
 */
public class COBRAparser {

  private static final String DELIM = " ,;\t\n\r\f";
  // BiGGID prefixes
  private static final String GENE_PRODUCT_PREFIX = "G";
  private static final String REACTION_PREFIX = "R";
  private static final String METABOLITE_PREFIX = "M";
  /**
   *
   */
  private MatlabFields mlField;
  /**
   * A {@link Logger} for this class.
   */
  private static final transient Logger logger =
    Logger.getLogger(COBRAparser.class.getName());

  /**
   *
   */
  private class MatlabFields {

    MLArray description;
    MLCell author;
    MLCell citations;
    MLCell comments;
    MLCell confidenceScores;
    MLCell ecNumbers;
    MLCell genedate;
    MLCell geneindex;
    MLCell genesource;
    MLCell genes;
    MLCell grRules;
    MLCell metCHEBIID;
    MLCell metFormulas;
    MLCell metHMDB;
    MLCell metInchiString;
    MLCell metKeggID;
    MLCell metNames;
    MLCell metPubChemID;
    MLCell metSmile;
    MLCell mets;
    MLCell name;
    MLCell notes;
    MLCell organism;
    MLCell rxnKeggID;
    MLCell rxns;
    MLCell rxnNames;
    MLCell subSystems;
    MLChar csense;
    MLDouble lb;
    MLDouble metCharge;
    MLDouble ub;
    MLNumericArray<?> b;
    MLNumericArray<?> coefficients;
    MLNumericArray<?> rev;
    MLSparse S;
    MLStructure struct;


    /**
     * @param struct
     */
    MatlabFields(MLStructure struct) {
      this.struct = struct;
      initializeFields();
    }


    /**
     *
     */
    void initializeFields() {
      b = toMLNumericArray(getStructField(ModelField.b));
      citations = toMLCell(getStructField(ModelField.citations));
      coefficients = toMLNumericArray(getStructField(ModelField.c));
      comments = toMLCell(getStructField(ModelField.comments));
      confidenceScores = toMLCell(getStructField(ModelField.confidenceScores));
      description = toMLCell(getStructField(ModelField.description));
      csense = toMLChar(getStructField(ModelField.csense));
      ecNumbers = toMLCell(getStructField(ModelField.ecNumbers));
      genes = toMLCell(getStructField(ModelField.mets));
      grRules = toMLCell(getStructField(ModelField.grRules));
      lb = toMLDouble(getStructField(ModelField.lb));
      mets = toMLCell(getStructField(ModelField.mets));
      metCharge = toMLDouble(getStructField(ModelField.metCharge));
      metCHEBIID = toMLCell(getStructField(ModelField.metCHEBIID));
      metFormulas = toMLCell(getStructField(ModelField.metFormulas));
      metHMDB = toMLCell(getStructField(ModelField.metHMDB));
      metInchiString = toMLCell(getStructField(ModelField.metInchiString));
      metKeggID = toMLCell(getStructField(ModelField.metKEGGID));
      metNames = toMLCell(getStructField(ModelField.metNames));
      metPubChemID = toMLCell(getStructField(ModelField.metPubChemID));
      metSmile = toMLCell(getStructField(ModelField.metSmile));
      rev = toMLNumericArray(getStructField(ModelField.rev));
      rxns = toMLCell(getStructField(ModelField.rxns));
      rxnKeggID = toMLCell(getStructField(ModelField.rxnKeggID));
      rxnNames = toMLCell(getStructField(ModelField.rxnNames));
      S = toMLSparse(getStructField(ModelField.S));
      subSystems = toMLCell(getStructField(ModelField.subSystems));
      ub = toMLDouble(getStructField(ModelField.ub));
    }


    /**
     *
     */
    void setDescriptionFields() {
      MLStructure descrStruct = (MLStructure) description;
      name = toMLCell(getStructField(descrStruct, ModelField.name));
      organism = toMLCell(getStructField(descrStruct, ModelField.organism));
      author = toMLCell(getStructField(descrStruct, ModelField.author));
      geneindex = toMLCell(getStructField(descrStruct, ModelField.geneindex));
      genedate = toMLCell(getStructField(descrStruct, ModelField.genedate));
      genesource = toMLCell(getStructField(descrStruct, ModelField.genesource));
      notes = toMLCell(getStructField(descrStruct, ModelField.notes));
    }


    /**
     * @param field
     * @return
     */
    MLArray getStructField(ModelField field) {
      return struct.getField(field.name());
    }


    /**
     * @param struct
     * @param field
     * @return
     */
    MLArray getStructField(MLStructure struct, ModelField field) {
      return struct.getField(field.name());
    }
  }


  /**
   * @param matFile
   * @param omitGenericTerms
   * @return
   * @throws IOException
   */
  public static List<SBMLDocument> read(File matFile, boolean omitGenericTerms)
    throws IOException {
    COBRAparser parser = new COBRAparser();
    parser.setOmitGenericTerms(omitGenericTerms);
    return parser.parse(matFile);
  }


  /**
   * @param reader
   * @return
   */
  private List<MLArray> getModels(MatFileReader reader) {
    Map<String, MLArray> content = reader.getContent();
    Iterator<String> keyIter = content.keySet().iterator();
    List<MLArray> models = new ArrayList<>();
    while (keyIter.hasNext()) {
      String key = keyIter.next();
      MLArray array = content.get(key);
      if ((array.getSize() == 1) && array.isStruct()) {
        models.add(array);
      }
    }
    if (content.keySet().size() > 1) {
      logger.warning(format(mpMessageBundle.getString("MORE_MODELS_COBRA_FILE"),
        content.keySet().size(), models.size()));
    }
    return models;
  }


  /**
   * @param models
   * @return
   */
  private List<SBMLDocument> parseModels(List<MLArray> models) {
    List<SBMLDocument> docs = new ArrayList<>();
    for (MLArray model : models) {
      ModelBuilder builder = new ModelBuilder(3, 1);
      builder.buildModel(SBMLtools.toSId(model.getName()), null);
      SBMLDocument doc = builder.getSBMLDocument();
      doc.addTreeNodeChangeListener(new UpdateListener());
      parseModel(builder, model);
      docs.add(doc);
    }
    return docs;
  }


  /**
   *
   */
  private COBRAparser() {
    super();
    setOmitGenericTerms(false);
  }

  /**
   *
   */
  private boolean omitGenericTerms;
  /**
   *
   */
  private RegistryLocalProvider registry;


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
  private List<SBMLDocument> parse(File matFile) throws IOException {
    MatFileReader reader = new MatFileReader(matFile);
    return parseModels(getModels(reader));
  }


  /**
   * @param builder
   */
  private void parseGenes(ModelBuilder builder) {
    if (mlField.genes == null) {
      logger.info(mpMessageBundle.getString("GENES_MISSING"));
      return;
    }
    Model model = builder.getModel();
    FBCModelPlugin modelPlug =
      (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
    for (int i = 0; i < mlField.genes.getSize(); i++) {
      String id =
        toString(mlField.genes.get(i), mlField.genes.getName(), i + 1);
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
   */
  private void parseMetabolites(ModelBuilder builder) {
    Model model = builder.getModel();
    registry = new RegistryLocalProvider();
    for (int i = 0; (mlField.mets != null)
      && (i < mlField.mets.getSize()); i++) {
      String id = toString(mlField.mets.get(i), mlField.mets.getName(), i + 1);
      if ((id != null) && (id.length() > 0)) {
        try {
          BiGGId biggId = new BiGGId(correctId(id));
          if (!biggId.isSetPrefix()) {
            biggId.setPrefix(METABOLITE_PREFIX);
          }
          Species species = model.createSpecies(biggId.toBiGGId());
          if (mlField.metNames != null) {
            species.setName(toString(mlField.metNames.get(i),
              mlField.metNames.getName(), i + 1));
          }
          if ((mlField.metFormulas != null) || (mlField.metCharge != null)) {
            FBCSpeciesPlugin specPlug =
              (FBCSpeciesPlugin) species.getPlugin(FBCConstants.shortLabel);
            if (exists(mlField.metFormulas, i)) {
              specPlug.setChemicalFormula(toString(mlField.metFormulas.get(i),
                mlField.metFormulas.getName(), i + 1));
            }
            if ((mlField.metCharge != null) && (mlField.metCharge.getSize() > i)
              && (mlField.metCharge.get(i) != null)) {
              double charge = mlField.metCharge.get(i);
              specPlug.setCharge((int) charge);
              if (charge - ((int) charge) != 0d) {
                logger.warning(
                  format(mpMessageBundle.getString("CHARGE_TO_INT_COBRA"),
                    charge, specPlug.getCharge()));
              }
            }
          }
          CVTerm term = new CVTerm();
          term.setQualifierType(CVTerm.Type.BIOLOGICAL_QUALIFIER);
          term.setBiologicalQualifierType(CVTerm.Qualifier.BQB_IS);
          addResource(mlField.metCHEBIID, i, term, "ChEBI");
          addResource(mlField.metHMDB, i, term, "HMDB");
          addResource(mlField.metInchiString, i, term, "InChI");
          // all kegg resources
          do {
            if (addResource(mlField.metKeggID, i, term, "KEGG Compound"))
              break;
            if (addResource(mlField.metKeggID, i, term, "KEGG Drug"))
              break;
            if (addResource(mlField.metKeggID, i, term, "KEGG Genes"))
              break;
            if (addResource(mlField.metKeggID, i, term, "KEGG Glycan"))
              break;
            if (addResource(mlField.metKeggID, i, term, "KEGG Pathway"))
              break;
          } while (false);
          do {
            if (addResource(mlField.metPubChemID, i, term, "PubChem-compound"))
              break;
            if (addResource(mlField.metPubChemID, i, term, "PubChem-substance"))
              break;
          } while (false);
          if (term.getResourceCount() > 0) {
            species.addCVTerm(term);
          }
          if ((mlField.metSmile != null) && (mlField.metSmile.get(i) != null)) {
            // For some reason, the mat files appear to store the creation date
            // in the field smile, rather than a smiles string.
            String smile = toString(mlField.metSmile.get(i),
              mlField.metSmile.getName(), i + 1);
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
   * Adds resources to provided CVTerm if catalog and id from MLCell
   * provide a valid URI. Logs ids not matching collection patterns
   * and invalid collections. In both cases no resource is added.
   *
   * @param cell
   * @param i
   *        the index within the cell.
   * @param term
   * @param catalog
   */
  private boolean addResource(MLCell cell, int i, CVTerm term, String catalog) {
    boolean success = false;
    if (exists(cell, i)) {
      String id = toMIRIAMid(cell.get(i));
      if ((id != null) && !id.isEmpty()) {
        id = checkId(id);
        if (validId(catalog, id)) {
          String resource = registry.getURI(catalog, id);
          if ((resource != null) && !resource.isEmpty()) {
            term.addResource(resource);
            success = true;
            logger.finest(
              format(mpMessageBundle.getString("ADDED_URI_COBRA"), resource));
          } else {
            logger.severe(format(
              mpMessageBundle.getString("ADD_URI_FAILED_COBRA"), catalog, id));
          }
        }
      }
    }
    return success;
  }


  /**
   * Necessary to check for a special whitespace (code 160) at beginning of id
   * (iCHOv1.mat, possibly other models) and to remove trailing ';'
   *
   * @param id
   * @return: trimmed id without ';' at the end
   */
  private String checkId(String id) {
    if (id.startsWith(Character.toString((char) 160)) || id.startsWith("/")) {
      id = id.substring(1);
    }
    if (id.endsWith(";")) {
      id = id.substring(0, id.length() - 1);
    } else if (id.contains(";")) {
      logger.warning(mpMessageBundle.getString("TRUNCATED_ID") + id);
      id = id.substring(0, id.indexOf(";"));
    }
    return id;
  }


  /**
   * Checks if id belongs to a given collection by matching it with the
   * respective regexp
   *
   * @param catalog:
   *        Miriam collection
   * @param id:
   *        id to test for membership
   * @return {@code true}, if it matches, else {@code false}
   */
  private boolean validId(String catalog, String id) {
    if (id.isEmpty()) {
      return false;
    }
    DataType collection = RegistryUtilities.getDataType(catalog);
    boolean validId = false;
    if (collection != null) {
      validId = registry.checkRegExp(id, collection.getName());
      if (!validId) {
        logger.warning(format(mpMessageBundle.getString("PATTERN_MISMATCH"), id,
          collection.getRegexp()));
      }
    } else {
      logger.severe(
        format(mpMessageBundle.getString("COLLECTION_UNKNOWN"), catalog));
    }
    return validId;
  }


  /**
   * @param array:
   *        MLArray to be stringified
   * @return String representation of the given array
   */
  private String toMIRIAMid(MLArray array) {
    return toMIRIAMid(toString(array));
  }


  /**
   * @param idCandidate:
   *        id part for the uri
   * @return finalized id, without [, ], ' at the string boundaries
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
    // substring method already decrements second index
    return (start < end) ? idCandidate.substring(start, end + 1) : null;
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
      logger.info(
        format(mpMessageBundle.getString("CHANGED_METABOLITE_ID"), id, newId));
    }
    return newId.toString();
  }


  /**
   * @param builder
   * @param array
   * @return
   */
  private Model parseModel(ModelBuilder builder, MLArray array) {
    MLStructure struct = (MLStructure) array;
    // Check that the given data structure only contains allowable entries
    MLStructure correctedStruct =
      new MLStructure(struct.getName(), struct.getDimensions());
    for (MLArray field : struct.getAllFields()) {
      boolean invalidField = false;
      String fieldName = field.getName();
      try {
        logger.finest(format(mpMessageBundle.getString("FOUND_COMPO"),
          ModelField.valueOf(fieldName)));
      } catch (IllegalArgumentException exc) {
        logException(exc);
        invalidField = true;
      }
      finally {
        // TODO: refactor into a lean method if possible
        // For each non matched struct field, check if an enum variant exists in
        // a case insensitive way
        if (invalidField) {
          for (ModelField variant : ModelField.values()) {
            String variantLC = variant.name().toLowerCase();
            if (variantLC.equals(fieldName.toLowerCase())
              || variantLC.startsWith(fieldName.toLowerCase())) {
              if (correctedStruct.getField(variant.name()) != null) {
                logger.warning(
                  format(mpMessageBundle.getString("FIELD_ALREADY_PRESENT"),
                    variant.name(), fieldName));
                break;
              }
              correctedStruct.setField(variant.name(), field);
              logger.warning(
                format(mpMessageBundle.getString("CHANGED_TO_VARIANT"),
                  fieldName, variant.name()));
              invalidField = false;
              break;
            }
          }
          if (invalidField) {
            logger.warning(format(
              mpMessageBundle.getString("CORRECT_VARIANT_FAILED"), fieldName));
          }
        } else {
          correctedStruct.setField(fieldName, field);
        }
      }
    }
    // parse model
    Model model = builder.getModel();
    buildBasicUnits(builder);
    mlField = new MatlabFields(correctedStruct);
    parseDescription(model);
    parseMetabolites(builder);
    parseGenes(builder);
    parseRxns(builder);
    // parse gprs
    for (int i = 0; (mlField.grRules != null)
      && (i < mlField.grRules.getSize()); i++) {
      String geneReactionRule =
        toString(mlField.grRules.get(i), mlField.grRules.getName(), i + 1);
      if (model.getReaction(i) == null) {
        logger.severe(
          format(mpMessageBundle.getString("CREATE_GPR_FAILED"), i));
      } else {
        SBMLUtils.parseGPR(model.getReaction(i), geneReactionRule,
          omitGenericTerms);
      }
    }
    // parse subsystems
    if ((mlField.subSystems != null) && (mlField.subSystems.getSize() > 0)) {
      parseSubsystems(model);
    }
    FBCModelPlugin fbc =
      (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
    Objective obj = fbc.createObjective("obj");
    obj.setType(Objective.Type.MAXIMIZE);
    fbc.getListOfObjectives().setActiveObjective(obj.getId());
    for (int i = 0; (mlField.csense != null)
      && (i < mlField.csense.getSize()); i++) {
      char c = mlField.csense.getChar(i, 0);
      // TODO: only 'E' (equality) is supported for now!
      if (c != 'E' && model.getListOfSpecies().size() > i) {
        logger.severe(
          format(mpMessageBundle.getString("NEQ_RELATION_UNSUPPORTED"),
            model.getSpecies(i).getId()));
      }
    }
    for (int i = 0; (mlField.coefficients != null)
      && (i < mlField.coefficients.getSize()); i++) {
      double coef = mlField.coefficients.get(i).doubleValue();
      if (coef != 0d) {
        Reaction r = model.getReaction(i);
        if (r == null) {
          break;
        }
        FluxObjective fo = obj.createFluxObjective("fo_" + r.getId());
        fo.setCoefficient(coef);
        fo.setReaction(r);
      }
    }
    for (int i = 0; (mlField.b != null) && (i < mlField.b.getSize()); i++) {
      double bVal = mlField.b.get(i).doubleValue();
      if (bVal != 0d && model.getListOfSpecies().size() > i) {
        // TODO: this should be incorporated into FBC version 3.
        logger.warning(format(mpMessageBundle.getString("B_VALUE_UNSUPPORTED"),
          bVal, model.getSpecies(i).getId()));
      }
    }
    return model;
  }


  /**
   * @param builder
   */
  private void buildBasicUnits(ModelBuilder builder) {
    UnitDefinition ud =
      builder.buildUnitDefinition("mmol_per_gDW_per_hr", null);
    ModelBuilder.buildUnit(ud, 1d, -3, Unit.Kind.MOLE, 1d);
    ModelBuilder.buildUnit(ud, 1d, 0, Unit.Kind.GRAM, -1d);
    ModelBuilder.buildUnit(ud, 3600d, 0, Unit.Kind.SECOND, -1d);
  }


  /**
   * @param model
   */
  private void parseDescription(Model model) {
    if (mlField.description != null) {
      if (mlField.description.isChar()) {
        MLChar description = (MLChar) mlField.description;
        if (description.getDimensions()[0] == 1) {
          model.setName(description.getString(0));
        } else {
          logger.warning(format(mpMessageBundle.getString("MANY_IDS_IN_DESC"),
            mlField.description.contentToString()));
        }
      } else if (mlField.description.isStruct()) {
        mlField.setDescriptionFields();
        if (mlField.name != null) {
          model.setName(toString(mlField.name));
        }
        if (mlField.organism != null) {
          // TODO
        }
        if (mlField.author != null) {
          // TODO
        }
        if (mlField.geneindex != null) {
          // TODO
        }
        if (mlField.genedate != null) {
          // TODO
        }
        if (mlField.genesource != null) {
          // TODO
        }
        if (mlField.notes != null) {
          try {
            model.appendNotes(SBMLtools.toNotesString(toString(mlField.notes)));
          } catch (XMLStreamException exc) {
            logException(exc);
          }
        }
      }
    } else {
      logger.warning(format(mpMessageBundle.getString("FIELD_MISSING"),
        ModelField.description));
    }
  }


  /**
   * @param exc
   */
  private void logException(Exception exc) {
    logger.warning(format("{0}: {1}", exc.getClass().getSimpleName(),
      Utils.getMessage(exc)));
  }


  /**
   * @param model
   */
  private void parseSubsystems(Model model) {
    Map<String, Group> nameToGroup = new HashMap<>(); // this is to avoid
    // creating the identical
    // group multiple times.
    GroupsModelPlugin groupsModelPlugin =
      (GroupsModelPlugin) model.getPlugin(GroupsConstants.shortLabel);
    for (int i = 0; (mlField.subSystems != null)
      && (i < mlField.subSystems.getSize()); i++) {
      String name = toString(mlField.subSystems.get(i),
        mlField.subSystems.getName(), i + 1);
      Group group = nameToGroup.get(name);
      if (group == null) {
        group = groupsModelPlugin.createGroup();
        group.setName(name);
        group.setKind(Group.Kind.partonomy);
        nameToGroup.put(name, group);
      }
      if (model.getReaction(i) != null) {
        SBMLUtils.createSubsystemLink(model.getReaction(i),
          group.createMember());
      } else {
        logger.severe(
          format(mpMessageBundle.getString("SUBSYS_LINK_ERROR"), i));
      }
    }
  }


  /**
   * @param builder
   */
  @SuppressWarnings("unchecked")
  private <T extends Number> void parseRxns(ModelBuilder builder) {
    Model model = builder.getModel();
    for (int j = 0; (mlField.rxns != null)
      && (j < mlField.rxns.getSize()); j++) {
      String rId = toString(mlField.rxns.get(j), mlField.rxns.getName(), j + 1);
      if ((rId != null) && (rId.length() > 0)) {
        try {
          BiGGId biggId = new BiGGId(correctId(rId));
          if (!biggId.isSetPrefix()) {
            biggId.setPrefix(REACTION_PREFIX);
          }
          Reaction r = model.createReaction(biggId.toBiGGId());
          if (mlField.rxnNames != null) {
            r.setName(toString(mlField.rxnNames.get(j),
              mlField.rxnNames.getName(), j + 1));
          }
          if (mlField.rev != null) {
            r.setReversible(mlField.rev.get(j).doubleValue() != 0d);
          }
          FBCReactionPlugin rPlug =
            (FBCReactionPlugin) r.getPlugin(FBCConstants.shortLabel);
          if (mlField.lb != null) {
            rPlug.setLowerFluxBound(builder.buildParameter(r.getId() + "_lb",
              null, mlField.lb.get(j), true, (String) null));
          }
          if (mlField.ub != null) {
            rPlug.setUpperFluxBound(builder.buildParameter(r.getId() + "_ub",
              null, mlField.ub.get(j), true, (String) null));
          }
          // Take the current column of S and look for all non-zero coefficients
          for (int i = 0; (mlField.S != null) && (i < mlField.S.getM()); i++) {
            double coeff = mlField.S.get(i, j);
            if (coeff != 0d) {
              try {
                BiGGId metId =
                  new BiGGId(correctId(toString(mlField.mets.get(i),
                    mlField.mets.getName(), i + 1)));
                metId.setPrefix(METABOLITE_PREFIX);
                Species species = model.getSpecies(metId.toBiGGId());
                if (coeff < 0d) { // Reactant
                  ModelBuilder.buildReactants(r, pairOf(-coeff, species));
                } else if (coeff > 0d) { // Product
                  ModelBuilder.buildProducts(r, pairOf(coeff, species));
                }
              } catch (IllegalArgumentException exc) {
                logger.warning(
                  format(mpMessageBundle.getString("REACT_PARTIC_INVALID"),
                    Utils.getMessage(exc)));
              }
            }
          }
          if (mlField.rxnKeggID != null && exists(mlField.rxnKeggID, j)) {
            parseRxnKEGGids(toString(mlField.rxnKeggID.get(j),
              mlField.rxnKeggID.getName(), j + 1), r);
          }
          if (mlField.ecNumbers != null && exists(mlField.ecNumbers, j)) {
            parseECcodes(toString(mlField.ecNumbers.get(j),
              mlField.ecNumbers.getName(), j + 1), r);
          }
          if (mlField.comments != null && exists(mlField.comments, j)) {
            String comment = toString(mlField.comments.get(j),
              mlField.comments.getName(), j + 1);
            appendComment(comment, r);
          }
          if (mlField.confidenceScores != null
            && exists(mlField.confidenceScores, j)) {
            MLArray cell = mlField.confidenceScores.get(j);
            if (cell instanceof MLDouble) {
              if (cell.getSize() == 0) {
                logger.warning(
                  mpMessageBundle.getString("CONF_CELL_WRONG_DIMS"));
                break;
              }
              Double score = ((MLDouble) cell).get(0);
              logger.fine(
                format(mpMessageBundle.getString("DISPLAY_CONF_SCORE"), score,
                  r.getId()));
              builder.buildParameter(
                "P_confidenceScore_of_" + SBMLtools.toSId(rId), // id
                format("Confidence score of reaction {0}",
                  r.isSetName() ? r.getName() : r.getId()), // name
                score, // value
                true, // constant
                Unit.Kind.DIMENSIONLESS // unit
              ).setSBOTerm(613); // TODO: there should be a specific term for
              // confidence scores. Use "613 - reaction parameter" for now.
            } else {
              logger.warning(
                format(mpMessageBundle.getString("TYPE_MISMATCH_MLDOUBLE"),
                  cell.getClass().getSimpleName()));
            }
          }
          if (mlField.citations != null && exists(mlField.citations, j)) {
            parseCitation(toString(mlField.citations.get(j),
              mlField.citations.getName(), j + 1), r);
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
        if (!addResource(ref, term, "PubMed")) {
          if (!addResource(ref, term, "DOI")) {
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
      String catalog = "kegg.reaction";
      DataType collection = RegistryUtilities.getDataType(catalog);
      String regexp = "";
      if (collection != null) {
        regexp = collection.getRegexp();
      }
      CVTerm term = findOrCreateCVTerm(r, CVTerm.Qualifier.BQB_IS);
      StringTokenizer st = new StringTokenizer(keggId, DELIM);
      while (st.hasMoreElements()) {
        String kId = st.nextElement().toString().trim();
        if (!kId.isEmpty() && RegistryUtilities.checkRegexp(kId, regexp)) {
          term.addResource(registry.getURI(catalog, kId));
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
      StringTokenizer st = new StringTokenizer(ec, DELIM);
      boolean match = false;
      while (st.hasMoreElements()) {
        String ecCode = st.nextElement().toString().trim();
        // if (ecCode.startsWith("E") || ecCode.startsWith("T")) {
        // ecCode = ecCode.substring(1);
        // }
        if ((ecCode != null) && !ecCode.isEmpty()
          && validId("ec-code", ecCode)) {
          String resource = registry.getURI("ec-code", ecCode);
          if ((resource != null) && !term.getResources().contains(resource)) {
            match = term.addResource(resource);
          }
        }
      }
      if (!match) {
        logger.warning(
          format(mpMessageBundle.getString("EC_CODES_UNKNOWN"), ec));
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
   * @param catalog
   * @return {@code true} if successful, {@code false} otherwise.
   */
  private boolean addResource(String resource, CVTerm term, String catalog) {
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
      r = checkId(r);
      if (validId(catalog, r)) {
        if (!resource.isEmpty()) {
          if (st.countTokens() > 1) {
            logger.warning(format(mpMessageBundle.getString("SKIP_COMMENT"),
              resource, r, catalog));
          }
          resource = registry.getURI(catalog, r);
          logger.finest(
            format(mpMessageBundle.getString("ADDED_URI"), resource));
          return term.addResource(resource);
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
  private MLCell toMLCell(MLArray array) {
    if (array != null) {
      if (array.isCell()) {
        return (MLCell) array;
      }
      logger.warning(format(mpMessageBundle.getString("TYPE_MISMATCH_CELL"),
        MLArray.typeToString(array.getType()), array.getName()));
    }
    return null;
  }


  /**
   * @param array
   * @return
   */
  private MLChar toMLChar(MLArray array) {
    if (array != null) {
      if (array.isChar()) {
        return (MLChar) array;
      }
      logger.warning(format(mpMessageBundle.getString("TYPE_MISMATCH_CHAR"),
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
        return (MLDouble) array;
      }
      logger.warning(format(mpMessageBundle.getString("TYPE_MISMATCH_DOUBLE"),
        MLArray.typeToString(array.getType()), array.getName()));
    }
    return null;
  }


  /**
   * @param array
   * @return
   */
  private MLNumericArray<?> toMLNumericArray(MLArray array) {
    if (array instanceof MLNumericArray<?>) {
      return (MLNumericArray<?>) array;
    }
    return null;
  }


  /**
   * @param array
   * @return
   */
  private MLSparse toMLSparse(MLArray array) {
    if (array.isSparse()) {
      return (MLSparse) array;
    }
    logger.warning(format(mpMessageBundle.getString("TYPE_MISMATCH_S_ARRAY"),
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
        logger.fine(format(mpMessageBundle.getString("MANY_STRINGS_IN_CELL"),
          string.contentToString()));
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
      logger.warning(format(mpMessageBundle.getString("TYPE_MISMATCH_STRING"),
        MLArray.typeToString(array.getType()), name, pos));
    }
    return sb.toString();
  }
}
