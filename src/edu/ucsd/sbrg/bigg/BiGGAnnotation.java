package edu.ucsd.sbrg.bigg;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBO;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.FBCSpeciesPlugin;
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.sbml.jsbml.ext.groups.Group;
import org.sbml.jsbml.ext.groups.GroupsConstants;
import org.sbml.jsbml.ext.groups.GroupsModelPlugin;
import org.sbml.jsbml.util.Pair;

import de.zbit.util.Utils;
import edu.ucsd.sbrg.util.SBMLUtils;

public class BiGGAnnotation {

  /*
   * 
   */
  private BiGGDB                       bigg;
  /*
   * 
   */
  private SBMLPolisher                 polisher;
  /**
   * A {@link Logger} for this class.
   */
  public static final transient Logger logger            =
    Logger.getLogger(BiGGAnnotation.class.getName());
  /**
   * Default model notes.
   */
  private String                       modelNotes        = "ModelNotes.html";
  /**
   * 
   */
  protected Map<String, String>        replacements;
  /**
   * 
   */
  private String                       documentNotesFile =
    "SBMLDocumentNotes.html";


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
   * @throws IOException
   * @throws XMLStreamException
   */
  public SBMLDocument annotate(SBMLDocument doc)
    throws XMLStreamException, IOException {
    Model model = doc.getModel();
    replacements = new HashMap<>();
    if (!doc.isSetModel()) {
      logger.info(
        "This SBML document does not contain a model. Nothing to do.");
      return doc;
    }
    annotate(model);
    if (replacements.containsKey("${title}")) {
      doc.appendNotes(parseNotes(documentNotesFile, replacements));
    }
    model.appendNotes(parseNotes(modelNotes, replacements));
    return doc;
  }


  /**
   * @param model
   */
  public void annotatePublications(Model model) {
    try {
      List<Pair<String, String>> publications =
        bigg.getPublications(model.getId());
      if (publications.size() > 0) {
        String resources[] = new String[publications.size()];
        int i = 0;
        for (Pair<String, String> publication : publications) {
          resources[i++] =
            polisher.createURI(publication.getKey(), publication.getValue());
        }
        model.addCVTerm(
          new CVTerm(CVTerm.Qualifier.BQM_IS_DESCRIBED_BY, resources));
      }
    } catch (SQLException exc) {
      logger.severe(MessageFormat.format("{0}: {1}", exc.getClass().getName(),
        Utils.getMessage(exc)));
    }
  }


  /**
   * @param model
   */
  public void annotateCompartments(Model model) {
    for (int i = 0; i < model.getCompartmentCount(); i++) {
      Compartment c = model.getCompartment(i);
      BiGGId biggId = new BiGGId(c.getId());
      if (bigg.isCompartment(biggId.getAbbreviation())) {
        c.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_IS,
          polisher.createURI("bigg.compartment", biggId)));
        c.setSBOTerm(SBO.getCompartment()); // physical compartment
        if (!c.isSetName()) {
          c.setName(bigg.getCompartmentName(biggId));
        }
      }
    }
  }


  /**
   * @param model
   */
  @SuppressWarnings("deprecation")
  public void annotateSpecies(Model model) {
    for (int i = 0; i < model.getSpeciesCount(); i++) {
      Species species = model.getSpecies(i);
      BiGGId biggId = polisher.extractBiGGId(species.getId());
      if (biggId != null) {
        CVTerm cvTerm = new CVTerm(CVTerm.Qualifier.BQB_IS);
        if (!species.isSetName() || species.getName().equals(
          biggId.getAbbreviation() + "_" + biggId.getCompartmentCode())) {
          try {
            species.setName(polisher.polishName(bigg.getComponentName(biggId)));
          } catch (SQLException exc) {
            logger.severe(MessageFormat.format("{0}: {1}",
              exc.getClass().getName(), Utils.getMessage(exc)));
          }
        }
        if (bigg.isMetabolite(biggId.getAbbreviation())) {
          cvTerm.addResource(polisher.createURI("bigg.metabolite", biggId));
        }
        String type = bigg.getComponentType(biggId);
        if (type != null) {
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
        try {
          List<String> linkOut =
            bigg.getComponentResources(biggId, polisher.includeAnyURI);
          for (String resource : linkOut) {
            cvTerm.addResource(resource);
          }
        } catch (SQLException exc) {
          logger.severe(MessageFormat.format("{0}: {1}",
            exc.getClass().getName(), Utils.getMessage(exc)));
        }
        if (cvTerm.getResourceCount() > 0) {
          species.addCVTerm(cvTerm);
        }
        if ((species.getCVTermCount() > 0) && !species.isSetMetaId()) {
          species.setMetaId(species.getId());
        }
        FBCSpeciesPlugin fbcSpecPlug =
          (FBCSpeciesPlugin) species.getPlugin(FBCConstants.shortLabel);
        if (!fbcSpecPlug.isSetChemicalFormula()) {
          try {
            fbcSpecPlug.setChemicalFormula(
              bigg.getChemicalFormula(biggId, model.getId()));
          } catch (IllegalArgumentException exc) {
            logger.severe(MessageFormat.format("Invalid chemical formula: {0}",
              Utils.getMessage(exc)));
          }
        }
        Integer charge =
          bigg.getCharge(biggId.getAbbreviation(), model.getId());
        if (species.isSetCharge()) {
          if ((charge != null) && (charge != species.getCharge())) {
            logger.warning(MessageFormat.format(
              "Charge {0,number,integer} in BiGG Models contradicts attribute value {1,number,integer} on species ''{2}''.",
              charge, species.getCharge(), species.getId()));
          }
          species.unsetCharge();
        }
        if ((charge != null) && (charge != 0)) {
          // If charge is set and charge = 0 -> this can mean it is
          // only a
          // default!
          fbcSpecPlug.setCharge(charge);
        }
      }
    }
  }


  /**
   * @param model
   */
  public void annotateReactions(Model model) {
    for (int i = 0; i < model.getReactionCount(); i++) {
      Reaction r = model.getReaction(i);
      String id = r.getId();
      if (!r.isSetSBOTerm()) {
        if (bigg.isPseudoreaction(id)) {
          r.setSBOTerm(631);
        } else if (!polisher.omitGenericTerms) {
          r.setSBOTerm(375); // generic process
        }
      }
      BiGGId biggId = polisher.extractBiGGId(id);
      if (biggId != null) {
        if (bigg.isReaction(r.getId())) {
          r.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_IS,
            polisher.createURI("bigg.reaction", biggId)));
        }
      }
      if (!r.isSetMetaId() && (r.getCVTermCount() > 0)) {
        r.setMetaId(id);
      }
      if (id.startsWith("R_")) {
        id = id.substring(2);
      }
      String name = bigg.getReactionName(id);
      if ((name != null) && !name.equals(r.getName())) {
        r.setName(polisher.polishName(name));
      }
      SBMLUtils.parseGPR(r, bigg.getGeneReactionRule(id, r.getModel().getId()),
        polisher.omitGenericTerms);
      List<String> subsystems =
        bigg.getSubsystems(model.getId(), biggId.getAbbreviation());
      if (subsystems.size() > 0) {
        String groupKey = "GROUP_FOR_NAME";
        if (model.getUserObject(groupKey) == null) {
          model.putUserObject(groupKey, new HashMap<String, Group>());
        }
        @SuppressWarnings("unchecked")
        Map<String, Group> groupForName =
          (Map<String, Group>) model.getUserObject(groupKey);
        for (String subsystem : subsystems) {
          Group group;
          if (groupForName.containsKey(subsystem)) {
            group = groupForName.get(subsystem);
          } else {
            GroupsModelPlugin groupsModelPlugin =
              (GroupsModelPlugin) model.getPlugin(GroupsConstants.shortLabel);
            group = groupsModelPlugin.createGroup(
              "g" + (groupsModelPlugin.getGroupCount() + 1));
            group.setName(subsystem);
            group.setKind(Group.Kind.partonomy);
            group.setSBOTerm(633); // subsystem
            groupForName.put(subsystem, group);
          }
          SBMLUtils.createSubsystemLink(r, group.createMember());
        }
      }
    }
  }


  /**
   * @param model
   */
  public void annotateGeneProducts(Model model) {
    if (model.isSetPlugin(FBCConstants.shortLabel)) {
      FBCModelPlugin fbcModelPlug =
        (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
      for (GeneProduct geneProduct : fbcModelPlug.getListOfGeneProducts()) {
        String label = null;
        String id = geneProduct.getId();
        if (geneProduct.isSetLabel()
          && !geneProduct.getLabel().equalsIgnoreCase("None")) {
          label = geneProduct.getLabel();
        } else if (geneProduct.isSetId()) {
          label = id;
        }
        if (label == null) {
          return;
        }
        CVTerm termIs = new CVTerm(CVTerm.Qualifier.BQB_IS);
        CVTerm termEncodedBy = new CVTerm(CVTerm.Qualifier.BQB_IS_ENCODED_BY);
        for (Pair<String, String> pair : bigg.getGeneIds(label)) {
          MIRIAM miriam = MIRIAM.toMIRIAM(pair.getKey());
          if (miriam == null) {
            continue;
          }
          String resource = MIRIAM.toResourceURL(miriam, pair.getValue());
          if (resource == null) {
            continue;
          }
          switch (miriam) {
          case interpro:
          case PDB:
          case UniProtKB_Swiss_Prot:
          case uniprot:
            termIs.addResource(resource);
            break;
          default:
            termEncodedBy.addResource(resource);
            break;
          }
        }
        if (termIs.getResourceCount() > 0) {
          geneProduct.addCVTerm(termIs);
        }
        if (termEncodedBy.getResourceCount() > 0) {
          geneProduct.addCVTerm(termEncodedBy);
        }
        if (geneProduct.getCVTermCount() > 0) {
          geneProduct.setMetaId(id);
        }
        // we successfully found information by using the id, so this needs to
        // be
        // the label
        if (geneProduct.getLabel().equalsIgnoreCase("None")) {
          geneProduct.setLabel(label);
        }
        String geneName = bigg.getGeneName(label);
        if (geneName != null) {
          if (geneName.isEmpty()) {
            logger.fine(MessageFormat.format(
              "No gene name found in BiGG for label ''{0}''.",
              geneProduct.getName()));
          } else if (geneProduct.isSetName()
            && !geneProduct.getName().equals(geneName)) {
            logger.warning(MessageFormat.format(
              "Updating gene product name from ''{0}'' to ''{1}''.",
              geneProduct.getName(), geneName));
          }
          geneProduct.setName(geneName);
        }
      }
    }
  }


  /**
   * @param model
   */
  public void annotate(Model model) {
    String organism = bigg.getOrganism(model.getId());
    Integer taxonId = bigg.getTaxonId(model.getId());
    if (taxonId != null) {
      model.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_HAS_TAXON,
        polisher.createURI("taxonomy", taxonId)));
    }
    // Note: date is probably not accurate.
    // Date date = bigg.getModelCreationDate(model.getId());
    // if (date != null) {
    // History history = model.createHistory();
    // history.setCreatedDate(date);
    // }
    String name = polisher.getDocumentTitlePattern();
    name = name.replace("[biggId]", model.getId());
    replacements.put("${title}", name);
    replacements.put("${bigg_id}", model.getId());
    replacements.put("${year}",
      Integer.toString(Calendar.getInstance().get(Calendar.YEAR)));
    replacements.put("${species_table}", ""); // XHTMLBuilder.table(header,
                                              // data, "Species", attributes));
    name = name.replace("[organism]", organism);
    replacements.put("${organism}", organism);
    replacements.put("${bigg.timestamp}",
      MessageFormat.format("{0,date}", bigg.getBiGGVersion()));
    if (!model.isSetName()) {
      model.setName(organism);
    }
    if (bigg.isModel(model.getId())) {
      model.addCVTerm(new CVTerm(CVTerm.Qualifier.BQM_IS,
        polisher.createURI("bigg.model", model.getId())));
    }
    if (!model.isSetMetaId() && (model.getCVTermCount() > 0)) {
      model.setMetaId(model.getId());
    }
    annotatePublications(model);
    annotateCompartments(model);
    annotateSpecies(model);
    annotateReactions(model);
    annotateGeneProducts(model);
  }


  /**
   * @param location
   *        relative path to the resource from this class.
   * @param replacements
   * @return
   * @throws IOException
   */
  private String parseNotes(String location, Map<String, String> replacements)
    throws IOException {
    StringBuilder sb = new StringBuilder();
    try (InputStream is = getClass().getResourceAsStream(location);
        InputStreamReader isReader = new InputStreamReader(
          (is != null) ? is : new FileInputStream(new File(location)));
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
    } catch (IOException exc) {
      throw exc;
    }
    return sb.toString();
  }


  /**
   * @return the modelNotes
   */
  public File getModelNotesFile() {
    return new File(modelNotes);
  }


  /**
   * @param modelNotes
   *        the modelNotes to set
   */
  public void setModelNotesFile(File modelNotes) {
    this.modelNotes = modelNotes.getAbsolutePath();
  }


  /**
   * @param documentNotesFile
   */
  public void setDocumentNotesFile(File documentNotesFile) {
    this.documentNotesFile = documentNotesFile.getAbsolutePath();
  }
}
