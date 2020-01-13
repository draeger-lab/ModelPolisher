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
import java.util.ArrayList;
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

import javax.swing.tree.TreeNode;
import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.Annotation;
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
import edu.ucsd.sbrg.db.AnnotateDB;
import edu.ucsd.sbrg.db.BiGGDB;
import edu.ucsd.sbrg.miriam.Registry;
import edu.ucsd.sbrg.util.SBMLUtils;

/**
 * @author Thomas Zajac
 *         This code runs only, if ANNOTATE_WITH_BIGG is true
 */
public class BiGGAnnotation {

  /**
   * BiGGBD instance, contains methods to run specific queries against the BiGG db
   */
  private BiGGDB bigg;
  /**
   * AnnotateDB instance, contains methods to run specific queries against the AnnotateDB
   */
  private AnnotateDB adb;
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
   * @param adb
   */
  public BiGGAnnotation(BiGGDB bigg, AnnotateDB adb) {
    this.bigg = bigg;
    this.adb = adb;
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
    String organism = bigg.getOrganism(model.getId());
    Integer taxonId = bigg.getTaxonId(model.getId());
    if (taxonId != null) {
      model.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_HAS_TAXON, Registry.createURI("taxonomy", taxonId)));
    }
    dealWithReplacements(model, organism);
    if (!model.isSetName()) {
      model.setName(organism);
    }
    if (bigg.isModel(model.getId())) {
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
  private void dealWithReplacements(Model model, String organism) {
    Parameters parameters = Parameters.get();
    String name = parameters.documentTitlePattern;
    name = name.replace("[biggId]", model.getId());
    name = name.replace("[organism]", organism);
    replacements.put("${title}", name);
    replacements.put("${organism}", organism);
    replacements.put("${bigg_id}", model.getId());
    replacements.put("${year}", Integer.toString(Calendar.getInstance().get(Calendar.YEAR)));
    replacements.put("${bigg.timestamp}", format("{0,date}", bigg.getBiGGVersion()));
    replacements.put("${species_table}", ""); // XHTMLBuilder.table(header, data, "Species", attributes));
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
      annotateCompartment(model.getCompartment(i));
    }
  }


  /**
   * @param compartment
   */
  private void annotateCompartment(Compartment compartment) {
    BiGGId biggId = new BiGGId(compartment.getId());
    if (bigg.isCompartment(biggId.getAbbreviation())) {
      compartment.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_IS, Registry.createURI("bigg.compartment", biggId)));
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
    String id = species.getId();
    // extracting BiGGId if not present for species
    // TODO: adapt to new BiGGId implementation
    boolean isBiGGid = id.matches("^([RMG])_([a-zA-Z][a-zA-Z0-9_]+)(?:_([a-z][a-z0-9]?))?(?:_([A-Z][A-Z0-9]?))?$");
    if (!isBiGGid) {
      Annotation annotation = species.getAnnotation();
      ArrayList<String> list_Uri = new ArrayList<>();
      for (CVTerm cvTerm : annotation.getListOfCVTerms()) {
        list_Uri.addAll(cvTerm.getResources());
      }
      if (!list_Uri.isEmpty()) {
        String temp;
        temp = getSpeciesBiGGIdFromUriList(list_Uri);
        if (temp != null) {
          // update the id in species
          id = temp;
        }
      }
    }
    // This biggId corresponds to BiGGId calculated from getSpeciesBiGGIdFromUriList method, if not present as
    // species.id
    BiGGId biggId = BiGGId.createMetaboliteId(id);
    setSpeciesName(species, biggId);
    setSBOTermFromComponentType(species, biggId);
    setCVTermResources(species, biggId);
    FBCSetFormulaCharge(species, biggId);
  }


  /**
   * @param list_Uri
   */
  private String getSpeciesBiGGIdFromUriList(List<String> list_Uri) {
    String biggId = null;
    for (String uri : list_Uri) {
      String dataSource, synonym_id, currentBiGGId; // currentBiGGId is id calculated in current iteration
      synonym_id = uri.substring(uri.lastIndexOf('/') + 1);
      // crop uri to remove synonym identifier from end
      uri = uri.substring(0, uri.lastIndexOf('/'));
      dataSource = uri.substring(uri.lastIndexOf('/') + 1);
      // updating the dataSource and synonym_id to match bigg database
      switch (dataSource) {
      // bigg.metabolite data_source identifier will directly give biggId
      case "bigg.metabolite":
        return "M_" + synonym_id;
      case "metanetx.chemical":
        dataSource = "mnx.chemical";
        break;
      case "chebi":
      case "kegg.compound":
      case "hmdb":
      case "lipidmaps":
      case "kegg.drug":
      case "seed.compound":
      case "biocyc":
        break;
      case "sgd":
      case "uniprot":
        return null; // it maps to a gene not a component
      default:
        return null; // the dataSource must belong one of above
      }
      currentBiGGId = bigg.getBiggIdFromSynonym(dataSource, synonym_id, BiGGDB.TYPE_SPECIES);
      if (currentBiGGId != null) {
        if (biggId == null) {
          biggId = currentBiGGId;
        } else {
          // we must get same biggId from each synonym
          if (!currentBiGGId.equals(biggId))
            return null;
        }
      }
    }
    return biggId == null ? null : "M_" + biggId;
  }


  /**
   * @param species
   * @param biggId
   */
  private void setSpeciesName(Species species, BiGGId biggId) {
    if (!species.isSetName()
      || species.getName().equals(format("{0}_{1}", biggId.getAbbreviation(), biggId.getCompartmentCode()))) {
      try {
        species.setName(SBMLPolisher.polishName(bigg.getComponentName(biggId)));
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
    Set<String> annotations_set = new HashSet<>();
    CVTerm cvTerm = new CVTerm(Qualifier.BQB_IS);
    boolean isBiGGMetabolite = bigg.isMetabolite(biggId.getAbbreviation());
    // using BiGG Database
    if (isBiGGMetabolite) {
      annotations_set.add(Registry.createURI("bigg.metabolite", biggId));
    }
    Parameters parameters = Parameters.get();
    try {
      TreeSet<String> linkOut = bigg.getResources(biggId, parameters.includeAnyURI, false);
      // convert to set to remove possible duplicates; TreeSet respects order
      annotations_set.addAll(linkOut);
    } catch (SQLException exc) {
      logger.severe(format("{0}: {1}", exc.getClass().getName(), Utils.getMessage(exc)));
    }
    // using AnnotateDB
    if (parameters.addADBAnnotations && adb != null && isBiGGMetabolite) {
      TreeSet<String> adb_annotations = adb.getAnnotations(AnnotateDB.BIGG_METABOLITE, biggId.toBiGGId());
      annotations_set.addAll(adb_annotations);
    }
    // adding annotations to cvTerm
    for (String annotation : annotations_set) {
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
    // extract biggId if not present for reaction
    //TODO: adapt to new BiGGId implementation
    boolean isBiGGid = id.matches("^([RMG])_([a-zA-Z][a-zA-Z0-9_]+)(?:_([a-z][a-z0-9]?))?(?:_([A-Z][A-Z0-9]?))?$");
    if (!isBiGGid) {
      Annotation annotation = reaction.getAnnotation();
      ArrayList<String> list_Uri = new ArrayList<>();
      for (CVTerm cvTerm : annotation.getListOfCVTerms()) {
        list_Uri.addAll(cvTerm.getResources());
      }
      if (!list_Uri.isEmpty()) {
        String temp;
        temp = getReactionBiGGIdFromUriList(list_Uri);
        if (temp != null) {
          id = temp;
        }
      }
    }
    Parameters parameters = Parameters.get();
    if (!reaction.isSetSBOTerm()) {
      if (bigg.isPseudoreaction(id)) {
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
    if (id.startsWith("R_")) {
      id = id.substring(2);
    }
    String name = bigg.getReactionName(id);
    if ((name != null) && !name.equals(reaction.getName())) {
      reaction.setName(SBMLPolisher.polishName(name));
    }
    List<String> geneReactionRules = bigg.getGeneReactionRule(id, reaction.getModel().getId());
    for (String geneRactionRule : geneReactionRules) {
      SBMLUtils.parseGPR(reaction, geneRactionRule, parameters.omitGenericTerms);
    }
    parseSubsystems(reaction, biggId);
    setCVTermResources(reaction, biggId);
  }


  /**
   * @param list_Uri
   */
  private String getReactionBiGGIdFromUriList(List<String> list_Uri) {
    String biggId = null;
    for (String uri : list_Uri) {
      String dataSource, synonym_id, currentBiGGId; // currentBiGGId is id calculated in current iteration
      synonym_id = uri.substring(uri.lastIndexOf('/') + 1);
      uri = uri.substring(0, uri.lastIndexOf('/'));
      dataSource = uri.substring(uri.lastIndexOf('/') + 1);
      // updating the dataSource and synonym_id to match bigg database
      switch (dataSource) {
      // bigg.metabolite data_source identifier will directly give biggId
      case "bigg.reaction":
        return "R_" + synonym_id;
      case "metanetx.reaction":
        dataSource = "mnx.equation";
        break;
      case "ec-code":
        dataSource = "ec";
        break;
      case "kegg.reaction":
      case "rhea":
        break;
      default:
        return null; // the dataSource must belong one of above
      }
      currentBiGGId = bigg.getBiggIdFromSynonym(dataSource, synonym_id, BiGGDB.TYPE_REACTION);
      if (biggId == null) {
        biggId = currentBiGGId;
      } else {
        // we must get same biggId from each synonym
        if (!currentBiGGId.equals(biggId))
          return null;
      }
    }
    return biggId == null ? null : "R_" + biggId;
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
        //TODO: group id seems to be off, check if this is indeed correct
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
    Set<String> annotations_set = new HashSet<>();
    CVTerm cvTerm = new CVTerm(Qualifier.BQB_IS);
    boolean isBiGGReaction = bigg.isReaction(reaction.getId());
    // using BiGG Database
    if (isBiGGReaction) {
      annotations_set.add(Registry.createURI("bigg.reaction", biggId));
    }
    Parameters parameters = Parameters.get();
    try {
      TreeSet<String> linkOut = bigg.getResources(biggId, parameters.includeAnyURI, true);
      annotations_set.addAll(linkOut);
    } catch (SQLException exc) {
      logger.severe(format("{0}: {1}", exc.getClass().getName(), Utils.getMessage(exc)));
    }
    // using AnnotateDB
    if (parameters.addADBAnnotations && adb != null && isBiGGReaction) {
      TreeSet<String> adb_annotations = adb.getAnnotations(AnnotateDB.BIGG_REACTION, biggId.toBiGGId());
      annotations_set.addAll(adb_annotations);
    }
    // adding annotations to cvTerm
    for (String annotation : annotations_set) {
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
      for (GeneProduct geneProduct : fbcModelPlugin.getListOfGeneProducts()) {
        annotateGeneProduct(geneProduct, fbcModelPlugin);
      }
    }
  }


  /**
   * @param geneProduct
   */
  private void annotateGeneProduct(GeneProduct geneProduct, FBCModelPlugin fbcModelPlugin) {
    String label = null;
    String id = geneProduct.getId();
    String calculatedBiGGId = id;
    //TODO: adapt to new BiGGId implementation
    boolean isBiGGid = id.matches("^([RMG])_([a-zA-Z][a-zA-Z0-9_]+)(?:_([a-z][a-z0-9]?))?(?:_([A-Z][A-Z0-9]?))?$");
    if (!isBiGGid || !bigg.isGenePresentInBigg(geneProduct)) {
      Annotation annotation = geneProduct.getAnnotation();
      ArrayList<String> list_Uri = new ArrayList<>();
      for (CVTerm cvTerm : annotation.getListOfCVTerms()) {
        list_Uri.addAll(cvTerm.getResources());
      }
      if (!list_Uri.isEmpty()) {
        String temp;
        temp = getGeneProductBiGGIdFromUriList(list_Uri);
        if (temp != null) {
          // update the id in geneProduct
          calculatedBiGGId = temp;
        }
      }
    }
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
    setCVTermResources(geneProduct, calculatedBiGGId);
    if (geneProduct.getCVTermCount() > 0) {
      geneProduct.setMetaId(id);
    }
    setGPLabelName(geneProduct, label);
  }


  private String getGeneProductBiGGIdFromUriList(List<String> list_Uri) {
    String biggId = null;
    for (String uri : list_Uri) {
      String dataSource, synonym_id, currentBiGGId; // currentBiGGId is id calculated in current iteration
      synonym_id = uri.substring(uri.lastIndexOf('/') + 1);
      uri = uri.substring(0, uri.lastIndexOf('/'));
      dataSource = uri.substring(uri.lastIndexOf('/') + 1);
      // updating the dataSource and synonym_id to match bigg database
      switch (dataSource) {
      case "ncbigi":
        synonym_id = synonym_id.substring(3);
        break;
      case "ncbigene":
      case "goa":
      case "interpro":
      case "asap":
      case "ecogene":
      case "uniprot":
        break;
      default:
        return null; // the dataSource must belong one of above
      }
      currentBiGGId = bigg.getBiggIdFromSynonym(dataSource, synonym_id, BiGGDB.TYPE_GENE_PRODUCT);
      if (biggId == null) {
        biggId = currentBiGGId;
      } else {
        // we must get same biggId from each synonym
        if (!currentBiGGId.equals(biggId))
          return null;
      }
    }
    return biggId == null ? null : "G_" + biggId;
  }


  /**
   * @param geneProduct
   * @param id
   */
  private void setCVTermResources(GeneProduct geneProduct, String id) {
    CVTerm termIs = new CVTerm(Qualifier.BQB_IS);
    CVTerm termEncodedBy = new CVTerm(Qualifier.BQB_IS_ENCODED_BY);
    // label is stored without "G_" prefix in BiGG
    if (id.startsWith("G_")) {
      id = id.substring(2);
    }
    for (String resource : bigg.getGeneIds(id)) {
      // get Collection part from uri without url prefix - all uris should
      // begin with http://identifiers.org, else this may fail
      String collection = Registry.getDataCollectionPartFromURI(resource);
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
