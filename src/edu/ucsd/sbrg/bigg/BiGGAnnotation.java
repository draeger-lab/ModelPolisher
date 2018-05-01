package edu.ucsd.sbrg.bigg;

import static edu.ucsd.sbrg.bigg.ModelPolisher.mpMessageBundle;
import static java.text.MessageFormat.format;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.swing.tree.TreeNode;
import javax.xml.stream.XMLStreamException;

import org.identifiers.registry.RegistryLocalProvider;
import org.identifiers.registry.RegistryUtilities;
import org.identifiers.registry.data.DataType;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.CVTerm.Qualifier;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBO;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.FBCSpeciesPlugin;
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.sbml.jsbml.ext.groups.Group;
import org.sbml.jsbml.ext.groups.GroupsConstants;
import org.sbml.jsbml.ext.groups.GroupsModelPlugin;
import org.sbml.jsbml.util.Pair;

import de.zbit.util.ResourceManager;
import de.zbit.util.Utils;
import edu.ucsd.sbrg.util.SBMLUtils;

/**
 * @author Thomas Zajac
 *         This code runs only, if ANNOTATE_WITH_BIGG is true
 */
public class BiGGAnnotation {

  /**
   * BiGGBD instance, contains methods to run specific queries against the actual db
   */
  private BiGGDB bigg;
  /**
   * SBMLPolisher instance, containing methods and booleans used by BiGGAnnotation
   */
  private SBMLPolisher polisher;
  /**
   * A {@link Logger} for this class.
   */
  static final transient Logger logger = Logger.getLogger(BiGGAnnotation.class.getName());
  /**
   * Localization support.
   */
  private static final transient ResourceBundle baseBundle =
    ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");
  /**
   * Default model notes.
   */
  private String modelNotes = "ModelNotes.html";
  /**
   * Mapping for generic placeholder entries in notes files to actual values
   */
  protected Map<String, String> replacements;
  /**
   * Default document notes
   */
  private String documentNotesFile = "SBMLDocumentNotes.html";


  /**
   * @param bigg
   * @param polisher
   */
  public BiGGAnnotation(BiGGDB bigg, SBMLPolisher polisher) {
    this.bigg = bigg;
    this.polisher = polisher;
  }


  /**
   * @param doc
   * @return
   */
  SBMLDocument annotate(SBMLDocument doc) {
    if (!doc.isSetModel()) {
      logger.info(baseBundle.getString("NO_MODEL_FOUND"));
      return doc;
    }
    Model model = doc.getModel();
    replacements = new HashMap<>();
    annotate(model);
    try {
      appendNotes(doc);
    } catch (IOException | XMLStreamException exc) {
      logger.warning(baseBundle.getString("FAILED_WRITE_NOTES"));
    }
    // Recursively sort and group all annotations in the SBMLDocument.
    mergeMIRIAMannotations(doc);
    return doc;
  }


  /**
   * @param model
   */
  private void annotate(Model model) {
    String organism = bigg.getOrganism(model.getId());
    Integer taxonId = bigg.getTaxonId(model.getId());
    if (taxonId != null) {
      model.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_HAS_TAXON, polisher.createURI("taxonomy", taxonId)));
    }
    // Note: date is probably not accurate.
    // Date date = bigg.getModelCreationDate(model.getId());
    // if (date != null) {
    // History history = model.createHistory();
    // history.setCreatedDate(date);
    // }
    String name = polisher.getDocumentTitlePattern();
    name = name.replace("[biggId]", model.getId());
    name = name.replace("[organism]", organism);
    replacements.put("${title}", name);
    replacements.put("${organism}", organism);
    replacements.put("${bigg_id}", model.getId());
    replacements.put("${year}", Integer.toString(Calendar.getInstance().get(Calendar.YEAR)));
    replacements.put("${bigg.timestamp}", format("{0,date}", bigg.getBiGGVersion()));
    replacements.put("${species_table}", ""); // XHTMLBuilder.table(header, data, "Species", attributes));
    if (!model.isSetName()) {
      model.setName(organism);
    }
    if (bigg.isModel(model.getId())) {
      model.addCVTerm(new CVTerm(CVTerm.Qualifier.BQM_IS, polisher.createURI("bigg.model", model.getId())));
    }
    if (!model.isSetMetaId() && (model.getCVTermCount() > 0)) {
      model.setMetaId(model.getId());
    }
    annotatePublications(model);
    annotateListOfCompartments(model);
    annotateListOfSpecies(model);
    annotateListOfReactions(model);
    annotateListOfGeneProducts(model);
  }


  /**
   * Replaces generic placeholders in notes files and appends both note types
   *
   * @param doc
   * @throws IOException
   * @throws XMLStreamException
   */
  private void appendNotes(SBMLDocument doc) throws IOException, XMLStreamException {
    if (replacements.containsKey("${title}") && (documentNotesFile != null)) {
      doc.appendNotes(parseNotes(documentNotesFile, replacements));
    }
    if (modelNotes != null) {
      doc.getModel().appendNotes(parseNotes(modelNotes, replacements));
    }
  }


  /**
   * Recursively goes through all annotations in the given {@link SBase} and
   * alphabetically sort annotations after grouping them by {@link org.sbml.jsbml.CVTerm.Qualifier}.
   *
   * @param sbase
   */
  private void mergeMIRIAMannotations(SBase sbase) {
    if (sbase.isSetAnnotation()) {
      SortedMap<Qualifier, SortedSet<String>> miriam = new TreeMap<>();
      boolean doMerge = hashMIRIAMuris(sbase, miriam);
      if (doMerge) {
        sbase.getAnnotation().unsetCVTerms();
        for (Entry<Qualifier, SortedSet<String>> entry : miriam.entrySet()) {
          logger.info(format(baseBundle.getString("MERGING_MIRIAM_RESOURCES"), entry.getKey(),
            sbase.getClass().getSimpleName(), sbase.getId()));
          sbase.addCVTerm(new CVTerm(entry.getKey(), entry.getValue().toArray(new String[0])));
        }
      }
    }
    for (int i = 0; i < sbase.getChildCount(); i++) {
      TreeNode node = sbase.getChildAt(i);
      if (node instanceof SBase) {
        mergeMIRIAMannotations((SBase) node);
      }
    }
  }


  /**
   * @param sbase
   * @param miriam
   * @return
   */
  private boolean hashMIRIAMuris(SBase sbase, SortedMap<Qualifier, SortedSet<String>> miriam) {
    boolean doMerge = false;
    for (int i = 0; i < sbase.getCVTermCount(); i++) {
      CVTerm term = sbase.getCVTerm(i);
      Qualifier qualifier = term.getQualifier();
      if (miriam.containsKey(qualifier)) {
        doMerge = true;
      } else {
        if (sbase instanceof Model) {
          if (!qualifier.isModelQualifier()) {
            logger.info(format(baseBundle.getString("CORRECTING_INVALID_QUALIFIERS"),
              qualifier.getElementNameEquivalent(), sbase.getId()));
            qualifier = Qualifier.getModelQualifierFor(qualifier.getElementNameEquivalent());
          }
        } else if (!qualifier.isBiologicalQualifier()) {
          logger.info(format(baseBundle.getString("CORRECTING_INVALID_MODEL_QUALIFIER"),
            qualifier.getElementNameEquivalent(), sbase.getClass().getSimpleName(), sbase.getId()));
          qualifier = Qualifier.getBiologicalQualifierFor(qualifier.getElementNameEquivalent());
        }
        miriam.put(qualifier, new TreeSet<>());
      }
      miriam.get(qualifier).addAll(term.getResources());
    }
    return doMerge;
  }


  /**
   * @param model
   */
  private void annotatePublications(Model model) {
    List<Pair<String, String>> publications = null;
    try {
      publications = bigg.getPublications(model.getId());
    } catch (SQLException exc) {
      logger.severe(format("{0}: {1}", exc.getClass().getName(), Utils.getMessage(exc)));
    }
    int numPublications;
    if (publications != null && (numPublications = publications.size()) > 0) {
      String resources[] = new String[numPublications];
      int i = 0;
      for (Pair<String, String> publication : publications) {
        resources[i++] = polisher.createURI(publication.getKey(), publication.getValue());
      }
      model.addCVTerm(new CVTerm(CVTerm.Qualifier.BQM_IS_DESCRIBED_BY, resources));
    }
  }


  /**
   * @param model
   */
  private void annotateListOfCompartments(Model model) {
    for (int i = 0; i < model.getCompartmentCount(); i++) {
      annotateCompartment(model.getCompartment(i));
    }
  }


  /**
   * @param compartment
   */
  private void annotateCompartment(Compartment compartment) {
    BiGGId biggId = new BiGGId(compartment.getId());
    if (bigg.isCompartment(biggId.getAbbreviation())) {
      compartment.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_IS, polisher.createURI("bigg.compartment", biggId)));
      compartment.setSBOTerm(SBO.getCompartment()); // physical compartment
      if (!compartment.isSetName() || compartment.getName().equals("default")) {
        String name = bigg.getCompartmentName(biggId);
        if ((name != null) && !name.isEmpty()) {
          compartment.setName(name);
        }
      }
    }
  }


  /**
   * @param model
   */
  private void annotateListOfSpecies(Model model) {
    for (int i = 0; i < model.getSpeciesCount(); i++) {
      annotateSpecies(model.getSpecies(i));
    }
  }


  /**
   * @param species
   */
  private void annotateSpecies(Species species) {
    BiGGId biggId = polisher.extractBiGGId(species.getId());
    if (biggId == null) {
      return;
    }
    setSpeciesName(species, biggId);
    setSBOTermFromComponentType(species, biggId);
    setCVTermResources(species, biggId);
    FBCSetFormulaCharge(species, biggId);
  }


  /**
   * @param species
   * @param biggId
   */
  private void setSpeciesName(Species species, BiGGId biggId) {
    if (!species.isSetName()
      || species.getName().equals(format("{0}_{1}", biggId.getAbbreviation(), biggId.getCompartmentCode()))) {
      try {
        species.setName(polisher.polishName(bigg.getComponentName(biggId)));
      } catch (SQLException exc) {
        logger.severe(format("{0}: {1}", exc.getClass().getName(), Utils.getMessage(exc)));
      }
    }
  }


  /**
   * @param species
   * @param biggId
   */
  private void setSBOTermFromComponentType(Species species, BiGGId biggId) {
    String type = bigg.getComponentType(biggId);
    if (type == null) {
      return;
    }
    switch (type) {
    case "metabolite":
      species.setSBOTerm(SBO.getSimpleMolecule());
      break;
    case "protein":
      species.setSBOTerm(SBO.getProtein());
      break;
    default:
      if (polisher.omitGenericTerms) {
        species.setSBOTerm(SBO.getMaterialEntity());
      }
      break;
    }
  }


  /**
   * @param species
   * @param biggId
   */
  private void setCVTermResources(Species species, BiGGId biggId) {
    CVTerm cvTerm = new CVTerm(Qualifier.BQB_IS);
    if (bigg.isMetabolite(biggId.getAbbreviation())) {
      cvTerm.addResource(polisher.createURI("bigg.metabolite", biggId));
    }
    try {
      TreeSet<String> linkOut = bigg.getResources(biggId, polisher.includeAnyURI, false);
      // convert to set to remove possible duplicates; TreeSet respects order
      for (String resource : linkOut) {
        cvTerm.addResource(resource);
      }
    } catch (SQLException exc) {
      logger.severe(format("{0}: {1}", exc.getClass().getName(), Utils.getMessage(exc)));
    }
    if (cvTerm.getResourceCount() > 0) {
      species.addCVTerm(cvTerm);
    }
    if ((species.getCVTermCount() > 0) && !species.isSetMetaId()) {
      species.setMetaId(species.getId());
    }
  }


  /**
   * @param species
   * @param biggId
   */
  @SuppressWarnings("deprecation")
  private void FBCSetFormulaCharge(Species species, BiGGId biggId) {
    String modelId = species.getModel().getId();
    String compartmentCode = biggId.getCompartmentCode();
    FBCSpeciesPlugin fbcSpecPlug = (FBCSpeciesPlugin) species.getPlugin(FBCConstants.shortLabel);
    if (!fbcSpecPlug.isSetChemicalFormula()) {
      String chemicalFormula = null;
      if (bigg.isModel(modelId)) {
        chemicalFormula = bigg.getChemicalFormula(biggId.getAbbreviation(), species.getModel().getId());
      } else if (compartmentCode != null && !compartmentCode.equals("")) {
        chemicalFormula = bigg.getChemicalFormulaByCompartment(biggId.getAbbreviation(), compartmentCode);
      }
      try {
        fbcSpecPlug.setChemicalFormula(chemicalFormula);
      } catch (IllegalArgumentException exc) {
        logger.severe(format(mpMessageBundle.getString("CHEM_FORMULA_INVALID"), Utils.getMessage(exc)));
      }
    }
    Integer charge = null;
    if (bigg.isModel(modelId)) {
      charge = bigg.getCharge(biggId.getAbbreviation(), species.getModel().getId());
    } else if (compartmentCode != null && !compartmentCode.equals("")) {
      charge = bigg.getChargeByCompartment(biggId.getAbbreviation(), biggId.getCompartmentCode());
    }
    if (species.isSetCharge()) {
      if ((charge != null) && (charge != species.getCharge())) {
        logger.warning(
          format(mpMessageBundle.getString("CHARGE_CONTRADICTION"), charge, species.getCharge(), species.getId()));
      }
      species.unsetCharge();
    }
    if ((charge != null) && (charge != 0)) {
      // If charge is set and charge = 0 -> this can mean it is only a default!
      fbcSpecPlug.setCharge(charge);
    }
  }


  /**
   * @param model
   */
  private void annotateListOfReactions(Model model) {
    for (int i = 0; i < model.getReactionCount(); i++) {
      annotateReaction(model.getReaction(i));
    }
  }


  /**
   * @param reaction
   */
  private void annotateReaction(Reaction reaction) {
    String id = reaction.getId();
    if (!reaction.isSetSBOTerm()) {
      if (bigg.isPseudoreaction(id)) {
        reaction.setSBOTerm(631);
      } else if (!polisher.omitGenericTerms) {
        reaction.setSBOTerm(375); // generic process
      }
    }
    if ((reaction.getCVTermCount() > 0) && !reaction.isSetMetaId()) {
      reaction.setMetaId(id);
    }
    BiGGId biggId = polisher.extractBiGGId(id);
    if (id.startsWith("R_")) {
      id = id.substring(2);
    }
    String name = bigg.getReactionName(id);
    if ((name != null) && !name.equals(reaction.getName())) {
      reaction.setName(polisher.polishName(name));
    }
    SBMLUtils.parseGPR(reaction, bigg.getGeneReactionRule(id, reaction.getModel().getId()), polisher.omitGenericTerms);
    parseSubsystems(reaction, biggId);
    setCVTermResources(reaction, biggId);
  }


  /**
   * @param reaction
   * @param biggId
   */
  private void parseSubsystems(Reaction reaction, BiGGId biggId) {
    Model model = reaction.getModel();
    List<String> subsystems = bigg.getSubsystems(model.getId(), biggId.getAbbreviation());
    if (subsystems.size() < 1) {
      return;
    }
    String groupKey = "GROUP_FOR_NAME";
    if (model.getUserObject(groupKey) == null) {
      model.putUserObject(groupKey, new HashMap<String, Group>());
    }
    @SuppressWarnings("unchecked")
    Map<String, Group> groupForName = (Map<String, Group>) model.getUserObject(groupKey);
    for (String subsystem : subsystems) {
      Group group;
      if (groupForName.containsKey(subsystem)) {
        group = groupForName.get(subsystem);
      } else {
        GroupsModelPlugin groupsModelPlugin = (GroupsModelPlugin) model.getPlugin(GroupsConstants.shortLabel);
        group = groupsModelPlugin.createGroup("g" + (groupsModelPlugin.getGroupCount() + 1));
        group.setName(subsystem);
        group.setKind(Group.Kind.partonomy);
        group.setSBOTerm(633); // subsystem
        groupForName.put(subsystem, group);
      }
      SBMLUtils.createSubsystemLink(reaction, group.createMember());
    }
  }


  /**
   * @param reaction
   * @param biggId
   */
  private void setCVTermResources(Reaction reaction, BiGGId biggId) {
    CVTerm cvTerm = new CVTerm(Qualifier.BQB_IS);
    if (bigg.isReaction(reaction.getId())) {
      cvTerm.addResource(polisher.createURI("bigg.reaction", biggId));
    }
    try {
      TreeSet<String> linkOut = bigg.getResources(biggId, polisher.includeAnyURI, true);
      for (String resource : linkOut) {
        cvTerm.addResource(resource);
      }
    } catch (SQLException exc) {
      logger.severe(format("{0}: {1}", exc.getClass().getName(), Utils.getMessage(exc)));
    }
    if (cvTerm.getResourceCount() > 0) {
      reaction.addCVTerm(cvTerm);
    }
    if ((reaction.getCVTermCount() > 0) && !reaction.isSetMetaId()) {
      reaction.setMetaId(reaction.getId());
    }
  }


  /**
   * @param model
   */
  private void annotateListOfGeneProducts(Model model) {
    if (model.isSetPlugin(FBCConstants.shortLabel)) {
      FBCModelPlugin fbcModelPlug = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
      for (GeneProduct geneProduct : fbcModelPlug.getListOfGeneProducts()) {
        annotateGeneProduct(geneProduct);
      }
    }
  }


  /**
   * @param geneProduct
   */
  private void annotateGeneProduct(GeneProduct geneProduct) {
    String label = null;
    String id = geneProduct.getId();
    if (geneProduct.isSetLabel() && !geneProduct.getLabel().equalsIgnoreCase("None")) {
      label = geneProduct.getLabel();
    } else if (geneProduct.isSetId()) {
      label = id;
    }
    if (label == null) {
      return;
    }
    // label is stored without "G_" prefix in BiGG
    if (label.startsWith("G_")) {
      label = label.substring(2);
    }
    setCVTermResources(geneProduct, label);
    if (geneProduct.getCVTermCount() > 0) {
      geneProduct.setMetaId(id);
    }
    setGPLabelName(geneProduct, label);
  }


  /**
   * @param geneProduct
   * @param label
   */
  private void setCVTermResources(GeneProduct geneProduct, String label) {
    CVTerm termIs = new CVTerm(Qualifier.BQB_IS);
    CVTerm termEncodedBy = new CVTerm(Qualifier.BQB_IS_ENCODED_BY);
    for (String resource : bigg.getGeneIds(label)) {
      // get Collection part from uri without url prefix - all uris should
      // begin with http://identifiers.org, else this may fail
      String collection = RegistryUtilities.getDataCollectionPartFromURI(resource);
      if (collection == null) {
        continue;
      } else if (!collection.contains("identifiers.org")) {
        logger.warning(format(mpMessageBundle.getString("PATTERN_MISMATCH_DROP"), collection));
        continue;
      }
      collection = collection.substring(collection.indexOf("org/") + 4, collection.length() - 1);
      switch (collection) {
      case "interpro":
      case "pdb":
      case "uniprot":
        termIs.addResource(resource);
        break;
      default:
        termEncodedBy.addResource(resource);
      }
    }
    if (termIs.getResourceCount() > 0) {
      geneProduct.addCVTerm(termIs);
    }
    if (termEncodedBy.getResourceCount() > 0) {
      geneProduct.addCVTerm(termEncodedBy);
    }
  }


  /**
   * @param geneProduct
   * @param label
   */
  private void setGPLabelName(GeneProduct geneProduct, String label) {
    // we successfully found information by using the id, so this needs to be the label
    if (geneProduct.getLabel().equalsIgnoreCase("None")) {
      geneProduct.setLabel(label);
    }
    String geneName = bigg.getGeneName(label);
    if (geneName != null) {
      if (geneName.isEmpty()) {
        logger.fine(format(mpMessageBundle.getString("NO_GENE_FOR_LABEL"), geneProduct.getName()));
      } else if (geneProduct.isSetName() && !geneProduct.getName().equals(geneName)) {
        logger.warning(format(mpMessageBundle.getString("UPDATE_GP_NAME"), geneProduct.getName(), geneName));
      }
      geneProduct.setName(geneName);
    }
  }


  /**
   * Checks resource URIs and logs those not matching the specified pattern
   * Used to check URIs obtained from BiGGDB
   * A resource URI that does not match the pattern will be logged and not added
   * to the model
   * For a collection not in the registry correctness is assumed
   *
   * @param resource:
   *        resource URI to be added as annotation
   * @return corrected resource URI
   */
  static String checkResourceUrl(String resource) {
    String collection = RegistryUtilities.getDataCollectionPartFromURI(resource);
    if (!isCheckable(resource, collection)) {
      return resource;
    }
    DataType type = RegistryUtilities.getDataType(collection);
    RegistryLocalProvider registry = new RegistryLocalProvider();
    String identifier = RegistryUtilities.getIdentifierFromURI(resource);
    String regexp;
    if (type != null) {
      regexp = type.getRegexp();
    } else {
      logger.severe(format(mpMessageBundle.getString("UNCAUGHT_URI"), resource));
      return resource;
    }
    Boolean correct = registry.checkRegExp(identifier, collection);
    if (!correct) {
      logger.info(format(mpMessageBundle.getString("PATTERN_MISMATCH_INFO"), identifier, regexp, collection));
      // We can correct the kegg collection
      if (resource.contains("kegg")) {
        if (identifier.startsWith("D")) {
          logger.info(mpMessageBundle.getString("CHANGE_KEGG_DRUG"));
          resource = RegistryUtilities.replace(resource, "kegg.compound", "kegg.drug");
        } else if (identifier.startsWith("G")) {
          logger.info(mpMessageBundle.getString("CHANGE_KEGG_GLYCAN"));
          resource = RegistryUtilities.replace(resource, "kegg.compound", "kegg.glycan");
        }
        // add possibly missing "gi:" prefix to identifier
      } else if (resource.contains("ncbigi")) {
        if (!identifier.toLowerCase().startsWith("gi:")) {
          logger.info(mpMessageBundle.getString("ADD_PREFIX_GI"));
          resource = RegistryUtilities.replace(resource, identifier, "GI:" + identifier);
        }
      } else if (resource.contains("go") && !resource.contains("goa")) {
        if (!identifier.toLowerCase().startsWith("go:")) {
          logger.info(mpMessageBundle.getString("ADD_PREFIX_GO"));
          resource = RegistryUtilities.replace(resource, identifier, "GO:" + identifier);
        }
      } else if (resource.contains("ec-code")) {
        int missingDots = identifier.length() - identifier.replace(".", "").length();
        if (missingDots < 1) {
          logger.warning(format(mpMessageBundle.getString("EC_CHANGE_FAILED"), identifier));
          return null;
        }
        String replacement = identifier;
        for (int count = missingDots; count < 3; count++) {
          replacement += ".-";
        }
        resource = RegistryUtilities.replace(resource, identifier, replacement);
      } else {
        logger.warning(format(mpMessageBundle.getString("CORRECTION_FAILED_DROP"), resource, collection));
        return null;
      }
    }
    logger.fine(format("Added resource ", resource));
    return resource;
  }


  /**
   * @param resource
   * @param collection
   */
  private static boolean isCheckable(String resource, String collection) {
    // not present in provided registry, cannot be checked this way, might add a function checking the online version
    boolean checkable = true;
    if (collection.contains("metanetx") || collection.contains("unipathway.reaction")
      || collection.contains("reactome.org") || collection.contains("molecular-networks.com")) {
      logger.fine(format(mpMessageBundle.getString("NO_URI_CHECK_WARN"), resource));
      checkable = false;
    }
    return checkable;
  }


  /**
   * @param location
   *        relative path to the resource from this class.
   * @param replacements
   * @return Constants.URL_PREFIX + " like '%%identifiers.org%%'"
   * @throws IOException
   */
  private String parseNotes(String location, Map<String, String> replacements) throws IOException {
    StringBuilder sb = new StringBuilder();
    try (InputStream is = getClass().getResourceAsStream(location);
        InputStreamReader isReader = new InputStreamReader((is != null) ? is : new FileInputStream(new File(location)));
        BufferedReader br = new BufferedReader(isReader)) {
      String line;
      boolean start = false;
      while (br.ready() && ((line = br.readLine()) != null)) {
        if (line.matches("\\s*<body.*")) {
          start = true;
        }
        if (!start) {
          continue;
        }
        if (line.matches(".*\\$\\{.*\\}.*")) {
          for (String key : replacements.keySet()) {
            line = line.replace(key, replacements.get(key));
          }
        }
        sb.append(line);
        sb.append('\n');
        if (line.matches("\\s*</body.*")) {
          break;
        }
      }
    }
    return sb.toString();
  }


  /**
   * @return the modelNotes
   */
  File getModelNotesFile() {
    return new File(modelNotes);
  }


  /**
   * @param modelNotes
   *        the modelNotes to set
   */
  void setModelNotesFile(File modelNotes) {
    this.modelNotes = modelNotes != null ? modelNotes.getAbsolutePath() : null;
  }


  /**
   * @param documentNotesFile
   */
  void setDocumentNotesFile(File documentNotesFile) {
    this.documentNotesFile = documentNotesFile != null ? documentNotesFile.getAbsolutePath() : null;
  }
}
