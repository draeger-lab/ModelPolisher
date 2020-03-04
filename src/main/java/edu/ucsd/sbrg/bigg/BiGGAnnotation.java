package edu.ucsd.sbrg.bigg;

import static edu.ucsd.sbrg.bigg.ModelPolisher.mpMessageBundle;
import static java.text.MessageFormat.format;

import java.io.*;
import java.sql.Date;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.tree.TreeNode;
import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.*;
import org.sbml.jsbml.CVTerm.Qualifier;
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
import edu.ucsd.sbrg.util.GPRParser;
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
    // add fake count so it never reaches 100 before gene products are processed
    int count = model.getCompartmentCount() + model.getSpeciesCount() + model.getReactionCount() + 50;
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
    int taxonId = BiGGDB.getTaxonId(model.getId());
    if (taxonId > Integer.MIN_VALUE) {
      model.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_HAS_TAXON, Registry.createURI("taxonomy", taxonId)));
    }
    BiGGDB.getOrganism(model.getId()).ifPresent(organism -> processReplacements(model, organism));
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
    replacements.put("${bigg.timestamp}", format("{0,date}", BiGGDB.getBiGGVersion().map(Date::toString).orElse("")));
    replacements.put("${species_table}", "");
    if (!model.isSetName()) {
      model.setName(organism);
    }
  }


  /**
   * @param model
   */
  private void annotatePublications(Model model) {
    progress.DisplayBar("Annotating Publications (1/5)  ");
    List<Pair<String, String>> publications = BiGGDB.getPublications(model.getId());
    int numPublications;
    if ((numPublications = publications.size()) > 0) {
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
      progress.DisplayBar("Annotating Compartments (2/5)  ");
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
        BiGGDB.getCompartmentName(biggId).ifPresent(compartment::setName);
      }
    }
  }


  /**
   * @param model
   */
  private void annotateListOfSpecies(Model model) {
    for (int i = 0; i < model.getSpeciesCount(); i++) {
      progress.DisplayBar("Annotating Species (3/5)  ");
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
        // update id if we found something
        id = getBiGGIdFromResources(resources, BiGGDB.TYPE_SPECIES).orElse(id);
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
  private Optional<String> getBiGGIdFromResources(List<String> resources, String type) {
    for (String resource : resources) {
      Optional<String> id = Registry.checkResourceUrl(resource).map(Registry::getPartsFromCanonicalURI)
                                    .flatMap(parts -> getBiggIdFromParts(parts, type));
      if (id.isPresent()) {
        return id;
      }
    }
    return Optional.empty();
  }


  /**
   * @param parts
   * @param type
   * @return
   */
  private Optional<String> getBiggIdFromParts(List<String> parts, String type) {
    String dataSource = parts.get(0);
    String synonymId = parts.get(1);
    if (QueryOnce.isDataSource(dataSource)) {
      Optional<String> id = BiGGDB.getBiggIdFromSynonym(dataSource, synonymId, type);
      if (id.isPresent()) {
        return id;
      }
    }
    return Optional.empty();
  }


  /**
   * @param species
   * @param biggId
   */
  private void setSpeciesName(Species species, BiGGId biggId) {
    if (!species.isSetName()
      || species.getName().equals(format("{0}_{1}", biggId.getAbbreviation(), biggId.getCompartmentCode()))) {
      BiGGDB.getComponentName(biggId).map(SBMLPolisher::polishName).ifPresent(species::setName);
    }
  }


  /**
   * @param species
   * @param biggId
   */
  private void setSBOTermFromComponentType(Species species, BiGGId biggId) {
    BiGGDB.getComponentType(biggId).ifPresent(type -> {
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
    });
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
    Set<String> linkOut = BiGGDB.getResources(biggId, parameters.includeAnyURI, false);
    // convert to set to remove possible duplicates; TreeSet respects order
    annotations.addAll(linkOut);
    // using AnnotateDB
    if (parameters.addADBAnnotations && AnnotateDB.inUse() && isBiGGMetabolite) {
      // TODO: check if this works at all
      Set<String> adb_annotations = AnnotateDB.getAnnotations(AnnotateDB.BIGG_METABOLITE, biggId.toBiGGId());
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
      Optional<String> chemicalFormula = Optional.empty();
      if (isBiGGModel) {
        chemicalFormula = BiGGDB.getChemicalFormula(biggId.getAbbreviation(), species.getModel().getId());
      }
      if ((!isBiGGModel || chemicalFormula.isEmpty()) && compartmentNonEmpty) {
        chemicalFormula = BiGGDB.getChemicalFormulaByCompartment(biggId.getAbbreviation(), compartmentCode);
      }
      chemicalFormula.ifPresent(formula -> {
        try {
          fbcSpecPlug.setChemicalFormula(formula);
        } catch (IllegalArgumentException exc) {
          logger.severe(format(mpMessageBundle.getString("CHEM_FORMULA_INVALID"), Utils.getMessage(exc)));
        }
      });
    }
    Optional<Integer> chargeFromBiGG = Optional.empty();
    if (isBiGGModel) {
      chargeFromBiGG = BiGGDB.getCharge(biggId.getAbbreviation(), species.getModel().getId());
    } else if (compartmentNonEmpty) {
      chargeFromBiGG = BiGGDB.getChargeByCompartment(biggId.getAbbreviation(), biggId.getCompartmentCode());
    }
    if (species.isSetCharge()) {
      chargeFromBiGG.filter(charge -> charge != species.getCharge()).ifPresent(charge -> logger.warning(
        format(mpMessageBundle.getString("CHARGE_CONTRADICTION"), charge, species.getCharge(), species.getId())));
      species.unsetCharge();
    }
    chargeFromBiGG.filter(charge -> charge != 0).ifPresent(fbcSpecPlug::setCharge);
  }


  /**
   * @param model
   */
  private void annotateListOfReactions(Model model) {
    for (int i = 0; i < model.getReactionCount(); i++) {
      progress.DisplayBar("Annotating Reactions (4/5)  ");
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
    BiGGDB.getReactionName(abbreviation).filter(name -> !name.equals(reaction.getName())).map(SBMLPolisher::polishName)
          .ifPresent(reaction::setName);
    List<String> geneReactionRules = BiGGDB.getGeneReactionRule(abbreviation, reaction.getModel().getId());
    for (String geneRactionRule : geneReactionRules) {
      GPRParser.parseGPR(reaction, geneRactionRule, parameters.omitGenericTerms);
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
        // update id if we found something
        id = getBiGGIdFromResources(resources, BiGGDB.TYPE_REACTION).orElse(id);
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
    Set<String> linkOut = BiGGDB.getResources(biggId, parameters.includeAnyURI, true);
    annotations.addAll(linkOut);
    // using AnnotateDB
    if (parameters.addADBAnnotations && AnnotateDB.inUse() && isBiGGReaction) {
      // TODO: check if this works at all
      Set<String> adb_annotations = AnnotateDB.getAnnotations(AnnotateDB.BIGG_REACTION, biggId.toBiGGId());
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
        // substract fake count
        progress.setNumberOfTotalCalls(progress.getNumberOfTotalCalls() + changed - 50);
        progress.setCallNr(current);
      }
      for (GeneProduct geneProduct : fbcModelPlugin.getListOfGeneProducts()) {
        progress.DisplayBar("Annotating Gene Products (5/5)  ");
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
    // fix geneProductReference in Association not updated
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
        // update id if we found something
        id = getBiGGIdFromResources(resources, BiGGDB.TYPE_GENE_PRODUCT).orElse(id);
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
    BiGGDB.getGeneIds(biggId.getAbbreviation()).forEach(
      resource -> Registry.checkResourceUrl(resource).map(Registry::getPartsFromCanonicalURI)
                          .filter(parts -> parts.size() > 0).map(parts -> parts.get(0)).ifPresent(collection -> {
                            switch (collection) {
                            case "interpro":
                            case "pdb":
                            case "uniprot":
                              termIs.addResource(resource);
                              break;
                            default:
                              termEncodedBy.addResource(resource);
                            }
                          }));
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
    BiGGDB.getGeneName(label).ifPresent(geneName -> {
      if (geneName.isEmpty()) {
        logger.fine(format(mpMessageBundle.getString("NO_GENE_FOR_LABEL"), geneProduct.getName()));
      } else if (geneProduct.isSetName() && !geneProduct.getName().equals(geneName)) {
        logger.warning(format(mpMessageBundle.getString("UPDATE_GP_NAME"), geneProduct.getName(), geneName));
      }
      geneProduct.setName(geneName);
    });
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
