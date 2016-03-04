/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of the program NetBuilder.
 *
 * Copyright (C) 2013 by the University of California, San Diego.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
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
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.InitialAssignment;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBO;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.Unit;
import org.sbml.jsbml.UnitDefinition;
import org.sbml.jsbml.Variable;
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
import org.sbml.jsbml.ext.groups.Member;
import org.sbml.jsbml.util.Pair;
import org.sbml.jsbml.util.ResourceManager;

import de.zbit.kegg.AtomBalanceCheck;
import de.zbit.kegg.AtomBalanceCheck.AtomCheckResult;
import de.zbit.util.Utils;
import de.zbit.util.progressbar.AbstractProgressBar;
import de.zbit.util.progressbar.ProgressBar;
import edu.ucsd.sbrg.util.SBMLFix;
import edu.ucsd.sbrg.util.SBMLUtils;

/**
 * @author Andreas Dr&auml;ger
 *
 */
public class SBMLPolisher {

  /**
   * 
   */
  public static final transient Pattern PATTERN_DEFAULT_FLUX_BOUND = Pattern.compile(".*_[Dd][Ee][Ff][Aa][Uu][Ll][Tt]_.*");
  /**
   * 
   */
  public static final transient Pattern PATTERN_ATP_MAINTENANCE = Pattern.compile(".*[Aa][Tt][Pp][Mm]");
  /**
   * 
   */
  public static final transient Pattern PATTERN_BIOMASS_CASE_INSENSITIVE = Pattern.compile(".*[Bb][Ii][Oo][Mm][Aa][Ss][Ss].*");
  /**
   * 
   */
  public static final transient Pattern PATTERN_BIOMASS_CASE_SENSITIVE = Pattern.compile(".*BIOMASS.*");
  /**
   * 
   */
  public static final transient Pattern PATTERN_DEMAND_REACTION = Pattern.compile(".*_[Dd][Mm]_.*");
  /**
   * 
   */
  public static final transient Pattern PATTERN_EXCHANGE_REACTION = Pattern.compile(".*_[Ee][Xx]_.*");
  /**
   * A {@link Logger} for this class.
   */
  public static final transient Logger logger = Logger.getLogger(SBMLPolisher.class.getName());
  /**
   * 
   */
  public static final transient Pattern PATTERN_SINK_OLD_STYLE = Pattern.compile(".*_[Ss][Ii][Nn][Kk]_.*");
  /**
   * 
   */
  public static final transient Pattern PATTERN_SINK_REACTION = Pattern.compile(".*_[Ss]([Ii][Nn])?[Kk]_.*");

  /**
   * 
   */
  private BiGGDB bigg;

  /**
   * 
   */
  private boolean checkMassBalance = true;

  /**
   * 
   */
  private String documentTitlePattern = "[biggId] - [organism]";

  /**
   * Default model notes.
   */
  private String modelNotes = "ModelNotes.html";

  /**
   * Switch to decide if generic and obvious terms should be used.
   */
  private boolean omitGenericTerms;

  /**
   * 
   */
  private AbstractProgressBar progress;


  /**
   * 
   */
  private Map<String, String> replacements;


  /**
   * 
   */
  private String documentNotesFile = "SBMLDocumentNotes.html";
  /**
   * 
   */
  private double[] fluxCoefficients;
  /**
   * 
   */
  private String[] fluxObjectives;

  /**
   * @param bigg
   */
  public SBMLPolisher(BiGGDB bigg) {
    this.bigg = bigg;
    omitGenericTerms = false;
  }

  /**
   * Checks if a given bound parameter satisfies the required properties of a
   * strict flux bound parameter: <li>not null <li>constant <li>defined value
   * other than {@link Double#NaN}
   * 
   * @param bound
   * @return {@code true} if the given parameter can be used as a flux bound in
   *         strict FBC models, {@code false} otherwise.
   */
  public boolean checkBound(Parameter bound) {
    return (bound != null) && bound.isConstant() && bound.isSetValue() && !Double.isNaN(bound.getValue());
  }

  /**
   * 
   * @param nsb
   */
  public void checkCompartment(NamedSBase nsb) {
    if ((nsb instanceof Species) && !((Species) nsb).isSetCompartment()) {
      BiGGId biggId = extractBiGGId(nsb.getId());
      if (biggId.isSetCompartmentCode()) {
        ((Species) nsb).setCompartment(biggId.getCompartmentCode());
      } else {
        return;
      }
    } else if ((nsb instanceof Reaction) && !((Reaction) nsb).isSetCompartment()) {
      return;
    }
    String cId = (nsb instanceof Species) ? ((Species) nsb).getCompartment() : ((Reaction) nsb).getCompartment();
    Model model = nsb.getModel();
    Compartment c = (Compartment) model.findUniqueNamedSBase(cId);
    if (c == null) {
      logger.warning(MessageFormat.format(
        "Creating compartment ''{0}'' because it is referenced by {2} ''{1}'' but does not yet exist in the model.",
        cId, nsb.getId(), nsb.getElementName()));
      c = model.createCompartment(cId);
      polish(c);
    }
  }

  /**
   * @param listOfSpeciesReference
   * @return
   */
  @SuppressWarnings("deprecation")
  public boolean checkSpeciesReferences(ListOf<SpeciesReference> listOfSpeciesReference) {
    boolean strict = true;
    for (SpeciesReference sr : listOfSpeciesReference) {
      strict &= sr.isConstant() && sr.isSetStoichiometry()
          && !sr.isSetStoichiometryMath() && !Double.isNaN(sr.getValue())
          && Double.isFinite(sr.getValue());
    }
    return strict;
  }

  /**
   * 
   * @param catalog
   * @param id
   * @return
   */
  public String createURI(String catalog, BiGGId id) {
    return createURI(catalog, id.getAbbreviation());
  }

  /**
   * 
   * @param catalog
   * @param id
   * @return
   */
  public String createURI(String catalog, Object id) {
    return "http://identifiers.org/" + catalog + "/" + id.toString();
  }

  /**
   * 
   * @param id species identifier
   * @return
   * @see <a href="https://github.com/SBRG/BIGG2/wiki/BIGG2-ID-Proposal-and-Specification">Structure of BiGG ids</a>
   */
  private BiGGId extractBiGGId(String id) {
    if (id.matches(".*_copy\\d*")) {
      return new BiGGId(id.substring(0, id.lastIndexOf('_')));
    }
    return new BiGGId(id);
  }

  /**
   * @return the modelNamePattern
   */
  public String getDocumentTitlePattern() {
    return documentTitlePattern;
  }

  /**
   * @return the modelNotes
   */
  public File getModelNotesFile() {
    return new File(modelNotes);
  }

  /**
   * @return the checkMassBalance
   */
  public boolean isCheckMassBalance() {
    return checkMassBalance;
  }

  /**
   * @return the omitGenericTerms
   */
  public boolean isOmitGenericTerms() {
    return omitGenericTerms;
  }
  /**
   * @param location relative path to the resource from this class.
   * @param replacements
   * @return
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
    } catch (IOException exc) {
      throw exc;
    }
    return sb.toString();
  }
  /**
   * @param c
   */
  public void polish(Compartment c) {
    if (!c.isSetId()) {
      c.setId("d"); // default
    }
    BiGGId biggId = new BiGGId(c.getId());
    if (bigg.isCompartment(biggId.getAbbreviation())) {
      c.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_IS, createURI("bigg.compartment", biggId)));
      c.setSBOTerm(SBO.getCompartment()); // physical compartment
      if (!c.isSetName()) {
        c.setName(bigg.getCompartmentName(biggId));
      }
    } else {
      c.setSBOTerm(410); // implicit compartment
      if (!c.isSetName()) {
        // TODO: make the name of a compartment a user setting
        c.setName("default");
      }
    }
    if (!c.isSetMetaId() && (c.getCVTermCount() > 0)) {
      c.setMetaId(c.getId());
    }
    if (!c.isSetConstant()) {
      c.setConstant(true);
    }
    if (!c.isSetSpatialDimensions()) {
      // TODO: check with biGG id, not for surfaces etc.
      //c.setSpatialDimensions(3d);
    }
    if (!c.isSetUnits()) {
      // TODO: set compartment units.
      /*
       * This is a temporary solution until we agree on something better.
       */
      c.setUnits(Unit.Kind.DIMENSIONLESS);
    }
  }
  /**
   * 
   * @param geneProduct
   */
  public void polish(GeneProduct geneProduct) {
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
    id = SBMLUtils.updateGeneId(id);
    if (!id.equals(geneProduct.getId())) {
      geneProduct.setId(id);
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
    // we successfully found information by using the id, so this needs to be the label
    if (geneProduct.getLabel().equalsIgnoreCase("None")) {
      geneProduct.setLabel(label);
    }
    String geneName = bigg.getGeneName(label);
    if (geneName != null) {
      if (geneName.isEmpty()) {
        logger.fine(MessageFormat.format("No gene name found in BiGG for label ''{0}''.", geneProduct.getName()));
      } else if (geneProduct.isSetName() && !geneProduct.getName().equals(geneName)) {
        logger.warning(MessageFormat.format(
          "Updating gene product name from ''{0}'' to ''{1}''.",
          geneProduct.getName(), geneName));
      }
      geneProduct.setName(geneName);
    } else {
      if (!geneProduct.isSetName() || geneProduct.getName().equalsIgnoreCase("None")) {
        geneProduct.setName(label);
      }
    }
  }
  /**
   * 
   * @param listOf
   * @param defaultSBOterm
   * @return
   */
  private String polish(ListOf<SpeciesReference> listOf, int defaultSBOterm) {
    String compartmentId = "";
    Model model = listOf.getModel();
    for (SpeciesReference sr : listOf) {
      // Not sure if we need this check here because SBML validator can do it:
      /*if (sr.isSetId() && !SyntaxChecker.isValidId(sr.getId(), sr.getLevel(), sr.getVersion())) {
        logger.severe(MessageFormat.format("Found a speciesReference with invalid identifier ''{0}''.", sr.getId()));
      }*/
      if (!sr.isSetSBOTerm() && !omitGenericTerms) {
        sr.setSBOTerm(defaultSBOterm);
      }
      Species species = model.getSpecies(sr.getSpecies());
      if (species != null) {
        if (!species.isSetCompartment()
            || (compartmentId == null)
            || (!compartmentId.isEmpty() && !compartmentId.equals(species.getCompartment()))) {
          compartmentId = null;
        } else {
          compartmentId = species.getCompartment();
        }
      } else {
        logger.warning(MessageFormat.format("Invalid reference to a species ''{0}'' that doesn''t exist in the model.", sr.getSpecies()));
      }
    }
    if ((compartmentId == null) || compartmentId.isEmpty()) {
      return null;
    }
    return compartmentId;
  }
  /**
   * 
   * @param model
   * @throws IOException
   * @throws XMLStreamException
   */
  public void polish(Model model) throws XMLStreamException, IOException {

    logger.info(MessageFormat.format("Processing model {0}.", model.getId()));

    // initialize ProgressBar
    int count = 1 // for model properties
        + model.getUnitDefinitionCount()
        + model.getCompartmentCount()
        + model.getParameterCount()
        + model.getReactionCount()
        + model.getSpeciesCount()
        + model.getInitialAssignmentCount();
    if (model.isSetPlugin(FBCConstants.shortLabel)) {
      FBCModelPlugin fbcModelPlug = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
      count += fbcModelPlug.getObjectiveCount() + fbcModelPlug.getGeneProductCount();
    }

    progress = new ProgressBar(count);
    progress.DisplayBar(); //"Processing model " + model.getId());
    // Do not do this because this messes up the HTML display of the file.
    //    for (int i = 0; i < model.getUnitDefinitionCount(); i++) {
    //      UnitDefinition ud = model.getUnitDefinition(i);
    //      //ud.appendNotes(HTMLFormula.toHTML(ud));
    //    }

    /*
     * Model
     */
    if (bigg.isModel(model.getId())) {
      model.addCVTerm(new CVTerm(CVTerm.Qualifier.BQM_IS, createURI("bigg.model", model.getId())));
    }
    if (!model.isSetMetaId() && (model.getCVTermCount() > 0)) {
      model.setMetaId(model.getId());
    }

    try {
      List<Pair<String, String>> publications = bigg.getPublications(model.getId());
      if (publications.size() > 0) {
        String resources[] = new String[publications.size()];
        int i = 0;
        for (Pair<String, String> publication : publications) {
          resources[i++] = createURI(publication.getKey(), publication.getValue());
        }
        model.addCVTerm(new CVTerm(CVTerm.Qualifier.BQM_IS_DESCRIBED_BY, resources));
      }
    } catch (SQLException exc) {
      logger.severe(MessageFormat.format("{0}: {1}", exc.getClass().getName(), Utils.getMessage(exc)));
    }

    // Note: date is probably not accurate.
    //    Date date = bigg.getModelCreationDate(model.getId());
    //    if (date != null) {
    //      History history = model.createHistory();
    //      history.setCreatedDate(date);
    //    }

    String organism = bigg.getOrganism(model.getId());
    Integer taxonId = bigg.getTaxonId(model.getId());
    if (taxonId != null) {
      model.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_HAS_TAXON, createURI("taxonomy", taxonId)));
    }

    if (!model.isSetName()) {
      model.setName(organism);
    }

    String name = getDocumentTitlePattern();
    name = name.replace("[biggId]", model.getId());
    name = name.replace("[organism]", organism);
    replacements.put("${title}", name);
    replacements.put("${organism}", organism);
    replacements.put("${bigg_id}", model.getId());
    replacements.put("${year}", Integer.toString(Calendar.getInstance().get(Calendar.YEAR)));
    replacements.put("${bigg.timestamp}", MessageFormat.format("{0,date}", bigg.getBiGGVersion()));


    try {
    } catch (Throwable exc) {
      logger.severe(MessageFormat.format("{0}: {1}", exc.getClass().getName(), Utils.getMessage(exc)));
    }
    polishListOfUnitDefinitions(model);
    polishListOfCompartments(model);
    polishListOfSpecies(model);
    replacements.put("${species_table}", ""); // XHTMLBuilder.table(header, data, "Species", attributes));
    boolean strict = polishListOfReactions(model);

    if (strict && model.isSetListOfInitialAssignments()) {
      strict &= polishListOfInitialAssignments(model, strict);
    }

    FBCModelPlugin modelPlug = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);

    if (modelPlug.isSetListOfObjectives()) {
      strict &= polishListOfObjectives(strict, modelPlug);
    }

    if (model.isSetPlugin(FBCConstants.shortLabel)) {
      FBCModelPlugin fbcModelPlug = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
      if (fbcModelPlug.isSetListOfGeneProducts()) {
        polishListOfGeneProducts(fbcModelPlug);
      }
    }

    polishListOfParameters(model);

    modelPlug.setStrict(strict);
  }
  /**
   * 
   * @param p
   */
  public void polish(Parameter p) {
    if (p.isSetId() && !p.isSetName()) {
      p.setName(polishName(p.getId()));
    }
  }

  /**
   * 
   * @param r
   * @return {@code true} if the given reaction qualifies for strict FBC.
   */
  public boolean polish(Reaction r) {
    String id = r.getId();
    if (PATTERN_BIOMASS_CASE_INSENSITIVE.matcher(id).matches()) {
      r.setSBOTerm(629); // biomass production
      if (!PATTERN_BIOMASS_CASE_SENSITIVE.matcher(id).matches()) {
        // in response to https://github.com/SBRG/bigg_models/issues/175
        id = id.replaceAll("[Bb][Ii][Oo][Mm][Aa][Ss][Ss]", "BIOMASS");
        r.setId(id);
      }
    } else if (PATTERN_DEMAND_REACTION.matcher(id).matches()) {
      r.setSBOTerm(628); // demand reaction
    } else if (PATTERN_EXCHANGE_REACTION.matcher(id).matches()) {
      r.setSBOTerm(627); // exchange reaction
    } else if (PATTERN_ATP_MAINTENANCE.matcher(id).matches()) {
      r.setSBOTerm(630); // ATP maintenance
    } else if (PATTERN_SINK_REACTION.matcher(id).matches()) {
      r.setSBOTerm(632);
      if (PATTERN_SINK_OLD_STYLE.matcher(id).matches()) {
        id = id.replaceAll("_[Ss][Ii][Nn][Kk]_", "_SK_");
        r.setId(id);
      }
    } else if (bigg.isPseudoreaction(id)) {
      r.setSBOTerm(631);
    } else if (!omitGenericTerms) {
      r.setSBOTerm(375); // generic process
    }
    BiGGId biggId = extractBiGGId(id);
    if (biggId != null) {
      String compartmentId = r.isSetCompartment() ? r.getCompartment() : null;
      if (r.isSetListOfReactants()) {
        String cId = polish(r.getListOfReactants(), SBO.getReactant());
        if (cId == null) {
          compartmentId = null;
        } else {
          if (compartmentId == null) {
            compartmentId = cId;
          } else if (!compartmentId.equals(cId)) {
            compartmentId = null;
          }
        }
      }
      if (r.isSetListOfProducts()) {
        String cId = compartmentId = polish(r.getListOfProducts(), SBO.getProduct());
        if (cId == null) {
          compartmentId = null;
        } else {
          if (compartmentId == null) {
            compartmentId = cId;
          } else if (!compartmentId.equals(cId)) {
            compartmentId = null;
          }
        }
      }
      // TODO: it was decided not to do this:
      //      if ((compartmentId != null) && !r.isSetCompartment()) {
      //        r.setCompartment(compartmentId);
      //        checkCompartment(r);
      //      }
      //      if (biggId.isSetCompartmentCode() && !r.isSetCompartment()) {
      //        logger.info(MessageFormat.format("Applying compartment declaration as specified by compartment code ''{1}'' to reaction ''{0}''.", r.getId(), biggId.getCompartmentCode()));
      //        r.setCompartment(biggId.getCompartmentCode());
      //        checkCompartment(r);
      //      }
      if (bigg.isReaction(r.getId())) {
        r.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_IS, createURI("bigg.reaction", biggId)));
      }
      if (!r.isSetMetaId() && (r.getCVTermCount() > 0)) {
        r.setMetaId(id);
      }

      if (id.startsWith("R_")) {
        id = id.substring(2);
      }
      String name = bigg.getReactionName(id);
      if ((name != null) && !name.equals(r.getName())) {
        r.setName(polishName(name));
      } else {
        r.setName(polishName(r.getName()));
      }

      SBMLUtils.parseGPR(r, bigg.getGeneReactionRule(id, r.getModel().getId()), omitGenericTerms);

      Model model = r.getModel();
      List<String> subsystems = bigg.getSubsystems(model.getId(), biggId.getAbbreviation());
      if (subsystems.size() > 0) {
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
            groupForName.put(subsystem, group);
          }
          Member member = group.createMember();
          member.setIdRef(r);
        }
      }
      SBMLUtils.setRequiredAttributes(r);
    }

    // This is a check if we are producing invalid SBML.
    if ((r.getReactantCount() == 0) && (r.getProductCount() == 0)) {
      ResourceBundle bundle = ResourceManager.getBundle("org.sbml.jsbml.resources.cfg.Messages");
      logger.severe(MessageFormat.format(
        bundle.getString("SBMLCoreParser.reactionWithoutParticipantsError"),
        r.getId()));
    } else {
      if (!r.isSetSBOTerm()) {
        // The reaction has not been recognized as demand or exchange reaction
        if (r.getReactantCount() == 0) {
          if (r.isReversible()) {
            // TODO: sink reaction
          } else {
            logger.warning(MessageFormat.format("Reaction ''{0}'' has been recognized as demand reaction, but this is not reflected in its BiGG id.", r.getId()));
            r.setSBOTerm(628); // demand reaction
          }
        } else if (r.getProductCount() == 0) {
          if (r.isReversible()) {
            // TODO: source reaction
          } else {
            logger.warning(MessageFormat.format("Reaction ''{0}'' has been recognized as demand reaction, but this is not reflected in its BiGG id.", r.getId()));
            r.setSBOTerm(628); // demand reaction
          }
        }
      }
      if (isCheckMassBalance() && ((r.getSBOTerm() < 627) || (630 < r.getSBOTerm()))) {
        // check atom balance only if the reaction is not identified as biomass production, demand, exchange or ATP maintenance.
        AtomCheckResult<Reaction> defects = AtomBalanceCheck.checkAtomBalance(r, 1);
        if ((defects != null) && (defects.hasDefects())) {
          logger.warning(MessageFormat.format(
            "There are missing atoms in reaction ''{0}''. Values lower than zero indicate missing atoms on the substrate side, whereas positive values indicate missing atoms on the product side: {1}",
            r.getId(), defects.getDefects().toString()));
        } else if (defects == null) {
          logger.fine(MessageFormat.format(
            "Could not check the atom balance of reaction ''{0}''.", r.getId()));
        } else {
          logger.fine(MessageFormat.format(
            "There are no missing atoms in reaction ''{0}''.", r.getId()));
        }

      }
    }

    FBCReactionPlugin rPlug = (FBCReactionPlugin) r.getPlugin(FBCConstants.shortLabel);
    Parameter lb = rPlug.getLowerFluxBoundInstance();
    Parameter ub = rPlug.getUpperFluxBoundInstance();
    boolean strict = polishFluxBound(lb) && polishFluxBound(ub);
    if (strict) {
      strict &= checkBound(lb);
      strict &= lb.isSetValue() && (lb.getValue() < Double.POSITIVE_INFINITY);
      strict &= checkBound(ub);
      strict &= ub.isSetValue() && (ub.getValue() > Double.NEGATIVE_INFINITY);
      strict &= lb.isSetValue() && ub.isSetValue() && (lb.getValue() <= ub.getValue());
      if (!strict) {
        logger.warning(MessageFormat.format("The flux bounds of reaction {0} can either not be resolved or they have illegal values.", r.getId()));
      }
    } else {
      logger.warning(MessageFormat.format("Reaction {0} does not define both required flux bounds.", r.getId()));
    }
    if (strict && r.isSetListOfReactants()) {
      strict &= checkSpeciesReferences(r.getListOfReactants());
      if (!strict) {
        logger.warning(MessageFormat.format("Some reactants in reaction {0} have an illegal stoichiometry", r.getId()));
      }
    }
    if (strict && r.isSetListOfProducts()) {
      strict &= checkSpeciesReferences(r.getListOfProducts());
      if (!strict) {
        logger.warning(MessageFormat.format("Some products in reaction {0} have an illegal stoichiometry", r.getId()));
      }
    }
    return strict;
  }

  /**
   * @param doc
   * @return
   * @throws XMLStreamException
   * @throws SBMLException
   * @throws IOException
   */
  public SBMLDocument polish(SBMLDocument doc) throws SBMLException, XMLStreamException, IOException {

    replacements = new HashMap<>();
    if (!doc.isSetModel()) {
      logger.info("This SBML document does not contain a model. Nothing to do.");
      return doc;
    }

    Model model = doc.getModel();
    polish(model);

    doc.setSBOTerm(624); // flux balance framework
    if (replacements.containsKey("${title}")) {
      doc.appendNotes(parseNotes(documentNotesFile, replacements));
    }

    model.appendNotes(parseNotes(modelNotes , replacements));

    if (progress != null) {
      progress.finished();
    }

    return doc;
  }

  /**
   * @param species
   */
  @SuppressWarnings("deprecation")
  public void polish(Species species) {
    String id = species.getId();
    if (species.getId().endsWith("_boundary")) {
      logger.warning(MessageFormat.format("Found a species with invalid BiGG id ''{0}''.", id));
      id = id.substring(0, id.length() - 9);
      if (!species.isSetBoundaryCondition() || !species.isBoundaryCondition()) {
        logger.warning(MessageFormat.format("Species ''{0}'' is supposed to be on the system''s boundary, but its boundary condition flag was not correctly set.", id));
        species.setBoundaryCondition(true);
      }
    } else if (!species.isSetBoundaryCondition()) {
      species.setBoundaryCondition(false);
    }
    checkCompartment(species);

    BiGGId biggId = extractBiGGId(id);
    FBCSpeciesPlugin fbcSpecPlug = (FBCSpeciesPlugin) species.getPlugin(FBCConstants.shortLabel);

    /*
     * Set mandatory attributes to default values
     * TODO: make those maybe user settings.
     */
    if (!species.isSetHasOnlySubstanceUnits()) {
      species.setHasOnlySubstanceUnits(true);
    }
    if (!species.isSetConstant()) {
      species.setConstant(false);
    }

    if (biggId != null) {
      Model model = species.getModel();

      CVTerm cvTerm = new CVTerm(CVTerm.Qualifier.BQB_IS);
      if (biggId.isSetAbbreviation()) {
        if (!species.isSetName() || species.getName().equals(biggId.getAbbreviation() + "_" + biggId.getCompartmentCode())) {
          try {
            species.setName(polishName(bigg.getComponentName(biggId)));
          } catch (SQLException exc) {
            logger.severe(MessageFormat.format("{0}: {1}", exc.getClass().getName(), Utils.getMessage(exc)));
          }
        } else {
          species.setName(polishName(species.getName()));
        }
        if (bigg.isMetabolite(biggId.getAbbreviation())) {
          cvTerm.addResource(createURI("bigg.metabolite", biggId));
        }
      }
      String type = bigg.getComponentType(biggId);
      if (type != null) {
        switch (type) {
        case "metabolite" :
          species.setSBOTerm(SBO.getSimpleMolecule());
          break;
        case "protein" :
          species.setSBOTerm(SBO.getProtein());
          break;
        default:
          if (!omitGenericTerms) {
            species.setSBOTerm(SBO.getMaterialEntity());
          }
          break;
        }
      }
      try {
        List<String> linkOut = bigg.getComponentResources(biggId);
        for (String resource : linkOut) {
          cvTerm.addResource(resource);
        }
      } catch (SQLException exc) {
        logger.severe(MessageFormat.format("{0}: {1}", exc.getClass().getName(), Utils.getMessage(exc)));
      }
      if (cvTerm.getResourceCount() > 0) {
        species.addCVTerm(cvTerm);
      }
      if ((species.getCVTermCount() > 0) && !species.isSetMetaId()) {
        species.setMetaId(species.getId());
      }

      if (biggId.isSetCompartmentCode() && species.isSetCompartment()
          && !biggId.getCompartmentCode().equals(species.getCompartment())) {
        logger.warning(MessageFormat.format(
          "Changing compartment reference in species ''{0}'' from ''{1}'' to ''{2}'' so that it matches the compartment code of its BiGG id ''{0}''.",
          species.getId(), species.getCompartment(), biggId.getCompartmentCode()));
        species.setCompartment(biggId.getCompartmentCode());
      }

      if (!fbcSpecPlug.isSetChemicalFormula()) {
        try {
          fbcSpecPlug.setChemicalFormula(bigg.getChemicalFormula(biggId, model.getId()));
        } catch (IllegalArgumentException exc) {
          logger.severe(MessageFormat.format("Invalid chemical formula: {0}", Utils.getMessage(exc)));
        }
      }

      Integer charge = bigg.getCharge(biggId.getAbbreviation(), model.getId());
      if (species.isSetCharge()) {
        if ((charge != null) && (charge != species.getCharge())) {
          logger.warning(MessageFormat.format(
            "Charge {0,number,integer} in BiGG Models contradicts attribute value {1,number,integer} on species ''{2}''.",
            charge, species.getCharge(), species.getId()));
        }
        species.unsetCharge();
      }
      if ((charge != null) && (charge != 0)) {
        // If charge is set and charge = 0 -> this can mean it is only a default!
        fbcSpecPlug.setCharge(charge);
      }
    }
  }

  /**
   * @param bound
   * @return {@code true} if this method successfully updated the bound parameter.
   */
  public boolean polishFluxBound(Parameter bound) {
    if (bound == null) {
      return false;
    }
    if (PATTERN_DEFAULT_FLUX_BOUND.matcher(bound.getId()).matches()) {
      bound.setSBOTerm(626); // default flux bound
    } else {
      bound.setSBOTerm(625); // flux bound
    }
    return true;
  }

  /**
   * @param model
   */
  public void polishListOfCompartments(Model model) {
    for (int i = 0; i < model.getCompartmentCount(); i++) {
      Compartment c = model.getCompartment(i);
      progress.DisplayBar(); //"Processing compartment " + c.getId());
      polish(c);
    }
  }

  /**
   * @param strict
   * @param objective
   * @return
   */
  public boolean polishListOfFluxObjectives(boolean strict, Objective objective) {
    if (objective.getFluxObjectiveCount() == 0) {
      // Note: the strict attribute does not require the presence of any flux objectives.
      logger.warning(MessageFormat.format("Objective {0} does not have any flux objectives", objective.getId()));
    } else {
      if (objective.getFluxObjectiveCount() > 1) {
        logger.warning(MessageFormat.format("Only one reaction should be the target of objective {0}.", objective.getId()));
      }
      for (FluxObjective fluxObjective : objective.getListOfFluxObjectives()) {
        if (fluxObjective.isSetCoefficient()
            && !Double.isNaN(fluxObjective.getCoefficient())
            && Double.isFinite(fluxObjective.getCoefficient())) {
          strict &= true;
        } else {
          logger.warning(MessageFormat.format("A flux objective for reaction {0} has an illegal coefficient value.", fluxObjective.getReaction()));
        }
      }
    }
    return strict;
  }


  /**
   * @param fbcModelPlug
   */
  public void polishListOfGeneProducts(FBCModelPlugin fbcModelPlug) {
    for (GeneProduct geneProduct : fbcModelPlug.getListOfGeneProducts()) {
      progress.DisplayBar(); //"Processing gene product " + geneProduct.getId());
      polish(geneProduct);
    }
  }


  /**
   * @param model
   * @param strict
   * @return
   */
  public boolean polishListOfInitialAssignments(Model model, boolean strict) {
    for (InitialAssignment ia : model.getListOfInitialAssignments()) {
      Variable variable = ia.getVariableInstance();
      progress.DisplayBar(); //"Processing initial assignment for " + variable.getId());
      if (variable != null) {
        if (variable instanceof Parameter) {
          if (!variable.isSetSBOTerm() || !SBO.isChildOf(variable.getSBOTerm(), 625)) { // flux bound
            strict &= true;
          } else {
            strict = false;
            logger.warning(MessageFormat.format("The parameter {0} is used as flux bound but an initial assignment changes its value.", variable.getId()));
          }
        } else if (variable instanceof SpeciesReference) {
          strict = false;
        }
      }
    }
    return strict;
  }

  /**
   * @param strict
   * @param modelPlug
   * @return
   */
  public boolean polishListOfObjectives(boolean strict, FBCModelPlugin modelPlug) {
    if (modelPlug.getObjectiveCount() == 0) {
      // Note: the strict attribute does not require the presence of any Objectives in the model.
      logger.warning(MessageFormat.format("No objectives defined for model {0}.", modelPlug.getParent().getId()));
    } else {
      for (Objective objective : modelPlug.getListOfObjectives()) {
        progress.DisplayBar(); //"Processing objective " + objective.getId());
        if (!objective.isSetListOfFluxObjectives()) {
          Model model = modelPlug.getParent();
          strict &= SBMLFix.fixObjective(model.getId(), model.getListOfReactions(), modelPlug, fluxCoefficients, fluxObjectives);
        }
        if (objective.isSetListOfFluxObjectives()) {
          strict &= polishListOfFluxObjectives(strict, objective);
        }
      }
    }
    return strict;
  }

  /**
   * @param model
   */
  public void polishListOfParameters(Model model) {
    for (int i = 0; i < model.getParameterCount(); i++) {
      Parameter parameter = model.getParameter(i);
      progress.DisplayBar(); //"Processing parameter " + parameter.getId());
      polish(parameter);
    }
  }

  /**
   * @param model
   * @return
   */
  public boolean polishListOfReactions(Model model) {
    boolean strict = true;
    for (int i = 0; i < model.getReactionCount(); i++) {
      Reaction r = model.getReaction(i);
      strict &= polish(r);
      progress.DisplayBar(); //"Processing reaction " + r.getId());
    }
    return strict;
  }

  /**
   * @param model
   */
  public void polishListOfSpecies(Model model) {
    // Species, but do not create an overview table for all in the model:
    //        String header[] = null;
    //        String data[][] = null;
    //    SortedMap<String, SortedMap<String, Species>> speciesList = new TreeMap<String, SortedMap<String, Species>>();
    //    if (model.getSpeciesCount() > 0) {
    //      header = new String[3];
    //      data = new String[model.getSpeciesCount()][header.length];
    //      header[0] = "BiGG id";
    //      header[1] = "Name";
    //      header[2] = "Compartment";
    //    }
    for (int i = 0; i < model.getSpeciesCount(); i++) {
      Species species = model.getSpecies(i);
      progress.DisplayBar(); //"Processing species " + species.getId());
      polish(species);
      //      if (!speciesList.containsKey(species.getName())) {
      //        speciesList.put(species.getName(), new TreeMap<String, Species>());
      //      }
      //      speciesList.get(species.getName()).put(species.getCompartment(), species);
    }
    //    int row = 0;
    //    for (Map.Entry<String, SortedMap<String, Species>> entry : speciesList.entrySet()) {
    //      String htmlName = EscapeChars.forHTML(entry.getKey());
    //      for (Map.Entry<String, Species> inner : entry.getValue().entrySet()) {
    //        data[row][0] = EscapeChars.forHTML(inner.getValue().getId());
    //        data[row][1] = htmlName;
    //        data[row][2] = EscapeChars.forHTML(model.getCompartment(inner.getKey()).getName());
    //        row++;
    //      }
    //    }
    //    Map<String, String> attributes = new HashMap<String, String>();
    //    // TODO: add attributes!
    //    attributes.put("width", "900px");
  }

  /**
   * @param model
   */
  public void polishListOfUnitDefinitions(Model model) {
    progress.DisplayBar(); //"Processing unit definitions");
    int udCount = model.getUnitDefinitionCount();
    UnitDefinition mmol_per_gDW_per_hr = model.getUnitDefinition("mmol_per_gDW_per_hr");
    if ((mmol_per_gDW_per_hr != null) && (mmol_per_gDW_per_hr.getUnitCount() > 0)) {
      if (!mmol_per_gDW_per_hr.isSetName()) {
        mmol_per_gDW_per_hr.setName("Millimoles per gram (dry weight) per hour");
      }
      if (!mmol_per_gDW_per_hr.isSetMetaId()) {
        mmol_per_gDW_per_hr.setMetaId(mmol_per_gDW_per_hr.getId());
      }
      mmol_per_gDW_per_hr.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_IS_DESCRIBED_BY, createURI("pubmed", 7986045)));
      UnitDefinition substanceUnits = model.getSubstanceUnitsInstance();
      boolean substanceExists = true;
      if (substanceUnits == null) {
        substanceUnits = model.createUnitDefinition(UnitDefinition.SUBSTANCE);
        substanceUnits.setName("Millimoles per gram (dry weight)");
        substanceExists = false;
      }
      if (!model.isSetExtentUnits()) {
        model.setExtentUnits(substanceUnits.getId());
      }
      if (!model.isSetSubstanceUnits()) {
        model.setSubstanceUnits(substanceUnits.getId());
      }
      for (Unit unit : mmol_per_gDW_per_hr.getListOfUnits()) {
        switch (unit.getKind()) {
        case SECOND:
          // Assumes it is per hour:
          UnitDefinition ud = model.getTimeUnitsInstance();
          if (ud == null) {
            ud = model.createUnitDefinition(UnitDefinition.TIME);
            model.setTimeUnits(ud.getId());
            Unit timeUnit = unit.clone();
            timeUnit.setExponent(1d);
            ud.setName("Hour");
            ud.addUnit(timeUnit);
            timeUnit.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_IS, createURI("unit", "UO:0000032")));
            unit.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_IS_VERSION_OF, createURI("unit", "UO:0000032")));
          }
          break;
        case GRAM:
          unit.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_IS_VERSION_OF, unit.getKind().getUnitOntologyIdentifier()));
          if (!substanceExists) {
            substanceUnits.addUnit(unit.clone());
          }
          break;
        case MOLE:
          if (unit.getScale() == -3) {
            unit.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_IS, createURI("unit", "UO:0000040")));
          }
          if (!substanceExists) {
            substanceUnits.addUnit(unit.clone());
          }
          break;
        default:
          break;
        }
      }
    }
    while (progress.getCallNumber() < udCount) {
      progress.DisplayBar();
    }
  }

  /**
   * 
   * @param name
   * @return
   */
  private String polishName(String name) {
    String newName = name;
    if (name.startsWith("?_")) {
      newName = name.substring(2);
    }
    if (newName.matches("__.*__")) {
      newName = newName.replaceAll("__.*__", "(.*)");
    } else if (newName.contains("__")) {
      newName = newName.replace("__", "-");
    }
    if (newName.matches(".*_C?\\d*.*\\d*")) {
      newName = newName.substring(0, newName.lastIndexOf('_')) + " - " + newName.substring(newName.lastIndexOf('_') + 1);
    }
    newName = newName.replace("_", " ");
    if (!newName.equals(name)) {
      logger.fine(MessageFormat.format("Changed name ''{0}'' to ''{1}''", name, newName));
    }
    return newName;
  }

  /**
   * 
   * @param checkMassBalance
   */
  public void setCheckMassBalance(boolean checkMassBalance) {
    this.checkMassBalance = checkMassBalance;
  }

  /**
   * @param modelNamePattern the modelNamePattern to set
   */
  public void setDocumentTitlePattern(String modelNamePattern) {
    documentTitlePattern = modelNamePattern;
  }

  /**
   * @param modelNotes the modelNotes to set
   */
  public void setModelNotesFile(File modelNotes) {
    this.modelNotes = modelNotes.getAbsolutePath();
  }

  /**
   * @param omitGenericTerms the omitGenericTerms to set
   */
  public void setOmitGenericTerms(boolean omitGenericTerms) {
    this.omitGenericTerms = omitGenericTerms;
  }

  /**
   * 
   * @param documentNotesFile
   */
  public void setDocumentNotesFile(File documentNotesFile) {
    this.documentNotesFile = documentNotesFile.getAbsolutePath();
  }

  /**
   * 
   * @param fluxCoefficients
   */
  public void setFluxCoefficients(double[] fluxCoefficients) {
    this.fluxCoefficients = fluxCoefficients;
  }

  /**
   * 
   * @param fluxObjectives
   */
  public void setFluxObjectives(String[] fluxObjectives) {
    this.fluxObjectives = fluxObjectives;
  }

}
