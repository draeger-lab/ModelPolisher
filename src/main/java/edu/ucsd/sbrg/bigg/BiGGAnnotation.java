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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.tree.TreeNode;
import javax.xml.stream.XMLStreamException;

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
import de.zbit.util.progressbar.AbstractProgressBar;
import de.zbit.util.progressbar.ProgressBar;
import edu.ucsd.sbrg.db.AnnotateDB;
import edu.ucsd.sbrg.db.BiGGDB;
import edu.ucsd.sbrg.db.QueryOnce;
import edu.ucsd.sbrg.miriam.Registry;
import edu.ucsd.sbrg.util.SBMLUtils;

/**
 * @author Thomas Zajac
 *         This code runs only, if ANNOTATE_WITH_BIGG is true
 */
public class BiGGAnnotation {

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
   *
   */
  private AbstractProgressBar progress;
  /**
   * 
   */
  private int initialGeneProducts;

  /**
   */
  public BiGGAnnotation() {
  }


  /**
   * @param doc
   * @return
   */
  public SBMLDocument annotate(SBMLDocument doc) {
    if (!doc.isSetModel()) {
      logger.info(baseBundle.getString("NO_MODEL_FOUND"));
      return doc;
    }
    Model model = doc.getModel();
    replacements = new HashMap<>();
    int count = model.getCompartmentCount() + model.getSpeciesCount() + model.getReactionCount();
    if (model.isSetPlugin(FBCConstants.shortLabel)) {
      FBCModelPlugin fbcModelPlug = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
      initialGeneProducts = fbcModelPlug.getGeneProductCount();
      count += initialGeneProducts;
    }
    progress = new ProgressBar(count);
    annotate(model);
    try {
      appendNotes(doc);
    } catch (IOException | XMLStreamException exc) {
      logger.warning(baseBundle.getString("FAILED_WRITE_NOTES"));
    }
    // Recursively sort and group all annotations in the SBMLDocument.
    mergeMIRIAMannotations(doc);
    if (progress != null) {
      progress.finished();
    }
    return doc;
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
  private void annotate(Model model) {
    String organism = BiGGDB.getOrganism(model.getId());
    Integer taxonId = BiGGDB.getTaxonId(model.getId());
    if (taxonId != null) {
      model.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_HAS_TAXON, Registry.createURI("taxonomy", taxonId)));
    }
    processReplacements(model, organism);
    if (!model.isSetName()) {
      model.setName(organism);
    }
    if (QueryOnce.isModel(model.getId())) {
      model.addCVTerm(new CVTerm(CVTerm.Qualifier.BQM_IS, Registry.createURI("bigg.model", model.getId())));
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
   * @param model
   * @param organism
   */
  private void processReplacements(Model model, String organism) {
    Parameters parameters = Parameters.get();
    String name = parameters.documentTitlePattern;
    name = name.replace("[biggId]", model.getId());
    name = name.replace("[organism]", organism);
    replacements.put("${title}", name);
    replacements.put("${organism}", organism);
    replacements.put("${bigg_id}", model.getId());
    replacements.put("${year}", Integer.toString(Calendar.getInstance().get(Calendar.YEAR)));
    replacements.put("${bigg.timestamp}", format("{0,date}", BiGGDB.getBiGGVersion()));
    replacements.put("${species_table}", ""); // XHTMLBuilder.table(header, data, "Species", attributes));
  }


  /**
   * @param model
   */
  private void annotatePublications(Model model) {
    progress.DisplayBar("Annotating Publications  ");
    List<Pair<String, String>> publications = null;
    try {
      publications = BiGGDB.getPublications(model.getId());
    } catch (SQLException exc) {
      logger.severe(format("{0}: {1}", exc.getClass().getName(), Utils.getMessage(exc)));
    }
    int numPublications;
    if (publications != null && (numPublications = publications.size()) > 0) {
      String[] resources = new String[numPublications];
      int i = 0;
      for (Pair<String, String> publication : publications) {
        resources[i++] = Registry.createURI(publication.getKey(), publication.getValue());
      }
      model.addCVTerm(new CVTerm(CVTerm.Qualifier.BQM_IS_DESCRIBED_BY, resources));
    }
  }


  /**
   * @param model
   */
  private void annotateListOfCompartments(Model model) {
    for (int i = 0; i < model.getCompartmentCount(); i++) {
      progress.DisplayBar("Annotating Compartments  ");
      annotateCompartment(model.getCompartment(i));
    }
  }


  /**
   * @param compartment
   */
  private void annotateCompartment(Compartment compartment) {
    BiGGId biggId = new BiGGId(compartment.getId());
    if (QueryOnce.isCompartment(biggId.getAbbreviation())) {
      compartment.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_IS, Registry.createURI("bigg.compartment", biggId)));
      compartment.setSBOTerm(SBO.getCompartment()); // physical compartment
      if (!compartment.isSetName() || compartment.getName().equals("default")) {
        String name = BiGGDB.getCompartmentName(biggId);
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
      progress.DisplayBar("Annotating Species  ");
      annotateSpecies(model.getSpecies(i));
    }
  }


  /**
   * @param species
   */
  private void annotateSpecies(Species species) {
    String id = checkId(species);
    // This biggId corresponds to BiGGId calculated from getSpeciesBiGGIdFromUriList method, if not present as
    // species.id
    BiGGId biggId = BiGGId.createMetaboliteId(id);
    setSpeciesName(species, biggId);
    setSBOTermFromComponentType(species, biggId);
    setCVTermResources(species, biggId);
    FBCSetFormulaCharge(species, biggId);
  }


  /**
   * @param species
   * @return
   */
  private String checkId(Species species) {
    String id = species.getId();
    // extracting BiGGId if not present for species
    boolean isBiGGid = id.matches("^(M_)?([a-zA-Z][a-zA-Z0-9_]+)(?:_([a-z][a-z0-9]?))?(?:_([A-Z][A-Z0-9]?))?$");
    if (!isBiGGid) {
      // Flatten all resources for all CVTerms into a list
      List<String> resources = species.getAnnotation().getListOfCVTerms().stream()
                                      .flatMap(term -> term.getResources().stream()).collect(Collectors.toList());
      if (!resources.isEmpty()) {
        String tmp = getBiGGIdFromResources(resources, BiGGDB.TYPE_SPECIES);
        // update id if we found something
        id = tmp.isEmpty() ? id : tmp;
      }
    }
    if (!id.startsWith("M_")) {
      id = "M_" + id;
    }
    return id;
  }


  /**
   * @param resources
   */
  private String getBiGGIdFromResources(List<String> resources, String type) {
    for (String resource : resources) {
      // Get identifiers.org URI for resource and fix ids
      resource = Registry.checkResourceUrl(resource);
      if (resource == null) {
        continue;
      }
      // Get data source and id from identifiers.or URI
      List<String> parts = Registry.getPartsFromCanonicalURI(resource);
      if (parts.isEmpty()) {
        continue;
      }
      String dataSource = parts.get(0);
      String synonymId = parts.get(1);
      if (QueryOnce.isDataSource(dataSource)) {
        String id = BiGGDB.getBiggIdFromSynonym(dataSource, synonymId, type);
        if (id != null) {
          return id;
        }
      }
    }
    return "";
  }


  /**
   * @param species
   * @param biggId
   */
  private void setSpeciesName(Species species, BiGGId biggId) {
    if (!species.isSetName()
      || species.getName().equals(format("{0}_{1}", biggId.getAbbreviation(), biggId.getCompartmentCode()))) {
      try {
        species.setName(SBMLPolisher.polishName(BiGGDB.getComponentName(biggId)));
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
    String type = BiGGDB.getComponentType(biggId);
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
      Parameters parameters = Parameters.get();
      if (parameters.omitGenericTerms) {
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
    // Set of annotations calculated from BiGGDB and AnnotateDB
    Set<String> annotations = new HashSet<>();
    CVTerm cvTerm = new CVTerm(Qualifier.BQB_IS);
    boolean isBiGGMetabolite = QueryOnce.isMetabolite(biggId.getAbbreviation());
    // using BiGG Database
    if (isBiGGMetabolite) {
      annotations.add(Registry.createURI("bigg.metabolite", biggId));
    }
    Parameters parameters = Parameters.get();
    try {
      TreeSet<String> linkOut = BiGGDB.getResources(biggId, parameters.includeAnyURI, false);
      // convert to set to remove possible duplicates; TreeSet respects order
      annotations.addAll(linkOut);
    } catch (SQLException exc) {
      logger.severe(format("{0}: {1}", exc.getClass().getName(), Utils.getMessage(exc)));
    }
    // using AnnotateDB
    if (parameters.addADBAnnotations && AnnotateDB.inUse() && isBiGGMetabolite) {
      // TODO: check if this works at all
      TreeSet<String> adb_annotations = AnnotateDB.getAnnotations(AnnotateDB.BIGG_METABOLITE, biggId.toBiGGId());
      annotations.addAll(adb_annotations);
    }
    // adding annotations to cvTerm
    for (String annotation : annotations) {
      cvTerm.addResource(annotation);
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
    boolean isBiGGModel = QueryOnce.isModel(modelId);
    boolean compartmentNonEmpty = compartmentCode != null && !compartmentCode.equals("");
    if (!fbcSpecPlug.isSetChemicalFormula()) {
      String chemicalFormula = null;
      if (isBiGGModel) {
        chemicalFormula = BiGGDB.getChemicalFormula(biggId.getAbbreviation(), species.getModel().getId());
      } else if (compartmentNonEmpty) {
        chemicalFormula = BiGGDB.getChemicalFormulaByCompartment(biggId.getAbbreviation(), compartmentCode);
      }
      try {
        fbcSpecPlug.setChemicalFormula(chemicalFormula);
      } catch (IllegalArgumentException exc) {
        logger.severe(format(mpMessageBundle.getString("CHEM_FORMULA_INVALID"), Utils.getMessage(exc)));
      }
    }
    Integer charge = null;
    if (isBiGGModel) {
      charge = BiGGDB.getCharge(biggId.getAbbreviation(), species.getModel().getId());
    } else if (compartmentNonEmpty) {
      charge = BiGGDB.getChargeByCompartment(biggId.getAbbreviation(), biggId.getCompartmentCode());
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
      progress.DisplayBar("Annotating Reactions  ");
      annotateReaction(model.getReaction(i));
    }
  }


  /**
   * @param reaction
   */
  private void annotateReaction(Reaction reaction) {
    String id = checkId(reaction);
    Parameters parameters = Parameters.get();
    if (!reaction.isSetSBOTerm()) {
      if (BiGGDB.isPseudoreaction(id)) {
        reaction.setSBOTerm(631);
      } else if (!parameters.omitGenericTerms) {
        reaction.setSBOTerm(375); // generic process
      }
    }
    if ((reaction.getCVTermCount() > 0) && !reaction.isSetMetaId()) {
      reaction.setMetaId(id);
    }
    // This biggId corresponds to BiGGId calculated from getSpeciesBiGGIdFromUriList method, if not present as
    // reaction.id
    BiGGId biggId = BiGGId.createReactionId(id);
    String abbreviation = biggId.getAbbreviation();
    String name = BiGGDB.getReactionName(abbreviation);
    if ((name != null) && !name.equals(reaction.getName())) {
      reaction.setName(SBMLPolisher.polishName(name));
    }
    List<String> geneReactionRules = BiGGDB.getGeneReactionRule(abbreviation, reaction.getModel().getId());
    for (String geneRactionRule : geneReactionRules) {
      SBMLUtils.parseGPR(reaction, geneRactionRule, parameters.omitGenericTerms);
    }
    parseSubsystems(reaction, biggId);
    setCVTermResources(reaction, biggId);
  }


  /**
   * @param reaction
   * @return
   */
  private String checkId(Reaction reaction) {
    String id = reaction.getId();
    // extracting BiGGId if not present for species
    boolean isBiGGid = id.matches("^(R_)?([a-zA-Z][a-zA-Z0-9_]+)(?:_([a-z][a-z0-9]?))?(?:_([A-Z][A-Z0-9]?))?$");
    if (!isBiGGid) {
      // Flatten all resources for all CVTerms into a list
      List<String> resources = reaction.getAnnotation().getListOfCVTerms().stream()
                                       .flatMap(term -> term.getResources().stream()).collect(Collectors.toList());
      if (!resources.isEmpty()) {
        String tmp = getBiGGIdFromResources(resources, BiGGDB.TYPE_REACTION);
        // update id if we found something
        id = tmp.isEmpty() ? id : tmp;
      }
    }
    if (!id.startsWith("R_")) {
      id = "R_" + id;
    }
    return id;
  }


  /**
   * @param reaction
   * @param biggId
   */
  private void parseSubsystems(Reaction reaction, BiGGId biggId) {
    Model model = reaction.getModel();
    List<String> subsystems = BiGGDB.getSubsystems(model.getId(), biggId.getAbbreviation());
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
        // TODO: group id seems to be off, check if this is indeed correct
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
    // Set of annotations calculated from BiGGDB and AnnotateDB
    Set<String> annotations = new HashSet<>();
    CVTerm cvTerm = new CVTerm(Qualifier.BQB_IS);
    boolean isBiGGReaction = QueryOnce.isReaction(biggId.getAbbreviation());
    // using BiGG Database
    if (isBiGGReaction) {
      annotations.add(Registry.createURI("bigg.reaction", biggId));
    }
    Parameters parameters = Parameters.get();
    try {
      TreeSet<String> linkOut = BiGGDB.getResources(biggId, parameters.includeAnyURI, true);
      annotations.addAll(linkOut);
    } catch (SQLException exc) {
      logger.severe(format("{0}: {1}", exc.getClass().getName(), Utils.getMessage(exc)));
    }
    // using AnnotateDB
    if (parameters.addADBAnnotations && AnnotateDB.inUse() && isBiGGReaction) {
      // TODO: check if this works at all
      TreeSet<String> adb_annotations = AnnotateDB.getAnnotations(AnnotateDB.BIGG_REACTION, biggId.toBiGGId());
      annotations.addAll(adb_annotations);
    }
    // adding annotations to cvTerm
    for (String annotation : annotations) {
      cvTerm.addResource(annotation);
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
      FBCModelPlugin fbcModelPlugin = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
      // update progress bar for added geneProducts
      int changed = fbcModelPlugin.getNumGeneProducts() - initialGeneProducts;
      if (changed > 0) {
        long current = progress.getCallNumber();
        progress.setNumberOfTotalCalls(progress.getNumberOfTotalCalls() + changed);
        progress.setCallNr(current);
      }
      for (GeneProduct geneProduct : fbcModelPlugin.getListOfGeneProducts()) {
        progress.DisplayBar("Annotating Gene Products  ");
        annotateGeneProduct(geneProduct);
      }
    }
  }


  /**
   * @param geneProduct
   */
  private void annotateGeneProduct(GeneProduct geneProduct) {
    String label = null;
    String id = checkId(geneProduct);
    if (geneProduct.isSetLabel() && !geneProduct.getLabel().equalsIgnoreCase("None")) {
      label = geneProduct.getLabel();
    } else if (geneProduct.isSetId()) {
      label = id;
    }
    if (label == null) {
      return;
    }
    // fix not updated geneProductReference in Association
    SBMLUtils.updateGeneProductReference(geneProduct);
    BiGGId biGGId = BiGGId.createGeneId(id);
    setCVTermResources(geneProduct, biGGId);
    if (geneProduct.getCVTermCount() > 0) {
      geneProduct.setMetaId(id);
    }
    setGPLabelName(geneProduct, label);
  }


  /**
   * @param geneProduct
   * @return
   */
  private String checkId(GeneProduct geneProduct) {
    String id = geneProduct.getId();
    // extracting BiGGId if not present for species
    boolean isBiGGid = id.matches("^(G_)?([a-zA-Z][a-zA-Z0-9_]+)(?:_([a-z][a-z0-9]?))?(?:_([A-Z][A-Z0-9]?))?$");
    if (!isBiGGid) {
      // Flatten all resources for all CVTerms into a list
      List<String> resources = geneProduct.getAnnotation().getListOfCVTerms().stream()
                                          .flatMap(term -> term.getResources().stream()).collect(Collectors.toList());
      if (!resources.isEmpty()) {
        String tmp = getBiGGIdFromResources(resources, BiGGDB.TYPE_GENE_PRODUCT);
        // update id if we found something
        id = tmp.isEmpty() ? id : tmp;
      }
    }
    if (!id.startsWith("G_")) {
      id = "G_" + id;
    }
    return id;
  }


  /**
   * @param geneProduct
   * @param biggId
   */
  private void setCVTermResources(GeneProduct geneProduct, BiGGId biggId) {
    CVTerm termIs = new CVTerm(Qualifier.BQB_IS);
    CVTerm termEncodedBy = new CVTerm(Qualifier.BQB_IS_ENCODED_BY);
    // label is stored without "G_" prefix in BiGG
    for (String resource : BiGGDB.getGeneIds(biggId.getAbbreviation())) {
      resource = Registry.checkResourceUrl(resource);
      if (resource == null) {
        continue;
      }
      List<String> parts = Registry.getPartsFromCanonicalURI(resource);
      String collection = parts.get(0);
      if (collection == null) {
        continue;
      } else if (!resource.contains("identifiers.org")) {
        logger.severe(format(mpMessageBundle.getString("PATTERN_MISMATCH_DROP"), collection));
        continue;
      }
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
    String geneName = BiGGDB.getGeneName(label);
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
   * @param modelNotes
   *        the modelNotes to set
   */
  public void setModelNotesFile(File modelNotes) {
    this.modelNotes = modelNotes != null ? modelNotes.getAbsolutePath() : null;
  }


  /**
   * @param documentNotesFile
   */
  public void setDocumentNotesFile(File documentNotesFile) {
    this.documentNotesFile = documentNotesFile != null ? documentNotesFile.getAbsolutePath() : null;
  }
}
