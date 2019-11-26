/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of the program NetBuilder.
 * Copyright (C) 2013 by the University of California, San Diego.
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package edu.ucsd.sbrg.bigg;

import static edu.ucsd.sbrg.bigg.ModelPolisher.mpMessageBundle;
import static java.text.MessageFormat.format;

import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.InitialAssignment;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBO;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.Unit;
import org.sbml.jsbml.UnitDefinition;
import org.sbml.jsbml.Variable;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.FBCReactionPlugin;
import org.sbml.jsbml.ext.fbc.FluxObjective;
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.sbml.jsbml.ext.fbc.Objective;
import org.sbml.jsbml.util.ModelBuilder;
import org.sbml.jsbml.util.ResourceManager;

import de.zbit.kegg.AtomBalanceCheck;
import de.zbit.kegg.AtomBalanceCheck.AtomCheckResult;
import de.zbit.util.progressbar.AbstractProgressBar;
import de.zbit.util.progressbar.ProgressBar;
import edu.ucsd.sbrg.util.SBMLFix;
import edu.ucsd.sbrg.util.SBMLUtils;

/**
 * @author Andreas Dr&auml;ger
 */
public class SBMLPolisher {

  /**
   *
   */
  public enum Patterns {
    ATP_MAINTENANCE(".*[Aa][Tt][Pp][Mm]"),
    BIOMASS_CASE_INSENSITIVE(".*[Bb][Ii][Oo][Mm][Aa][Ss][Ss].*"),
    BIOMASS_CASE_SENSITIVE(".*BIOMASS.*"),
    DEFAULT_FLUX_BOUND(".*_[Dd][Ee][Ff][Aa][Uu][Ll][Tt]_.*"),
    DEMAND_REACTION(".*_[Dd][Mm]_.*"),
    EXCHANGE_REACTION(".*_[Ee][Xx]_.*"),
    SINK_OLD_STYLE(".*_[Ss][Ii][Nn][Kk]_.*"),
    SINK_REACTION(".*_[Ss]([Ii][Nn])?[Kk]_.*");

    private Pattern pattern;


    Patterns(String regex) {
      if (pattern == null) {
        pattern = Pattern.compile(regex);
      }
    }


    public Pattern getPattern() {
      return pattern;
    }
  }

  /**
   * A {@link Logger} for this class.
   */
  public static final transient Logger logger = Logger.getLogger(SBMLPolisher.class.getName());
  /**
   *
   */
  private boolean checkMassBalance = true;
  /**
   *
   */
  private String documentTitlePattern = "[biggId] - [organism]";
  /**
   * Switch to decide if generic and obvious terms should be used.
   */
  public boolean omitGenericTerms;
  /**
   * Switch to decide if also references to data sources can be included into
   * {@link CVTerm}s whose URLs are not (yet) part of the MIRIAM registry.
   */
  public boolean includeAnyURI;
  /**
   *
   */
  private AbstractProgressBar progress;
  /**
   *
   */
  private double[] fluxCoefficients;
  /**
   *
   */
  private String[] fluxObjectives;


  /**
   *
   */
  public SBMLPolisher() {
    omitGenericTerms = false;
  }


  /**
   * @param catalog
   * @param id
   * @return
   */
  public static String createURI(String catalog, BiGGId id) {
    return createURI(catalog, id.getAbbreviation());
  }


  /**
   * @param catalog
   * @param id
   * @return
   */
  public static String createURI(String catalog, Object id) {
    return "http://identifiers.org/" + catalog + "/" + id.toString();
  }


  /**
   * @param id
   *        species identifier
   * @return
   * @see <a href=
   *      "https://github.com/SBRG/BIGG2/wiki/BIGG2-ID-Proposal-and-Specification">Structure
   *      of BiGG ids</a>
   */
  public static BiGGId extractBiGGId(String id) {
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
   * @param doc
   * @return
   */
  public SBMLDocument polish(SBMLDocument doc) {
    if (!doc.isSetModel()) {
      logger.info(mpMessageBundle.getString("NO_MODEL_FOUND"));
      return doc;
    }
    Model model = doc.getModel();
    polish(model);
    doc.setSBOTerm(624); // flux balance framework
    if (progress != null) {
      progress.finished();
    }
    return doc;
  }


  /**
   * @param model
   */
  public void polish(Model model) {
    logger.info(format(mpMessageBundle.getString("PROCESSING_MODEL"), model.getId()));
    // initialize ProgressBar
    int count = 1 // for model properties
      + model.getUnitDefinitionCount() + model.getCompartmentCount() + model.getParameterCount()
      + model.getReactionCount() + model.getSpeciesCount() + model.getInitialAssignmentCount();
    if (model.isSetPlugin(FBCConstants.shortLabel)) {
      FBCModelPlugin fbcModelPlug = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
      count += fbcModelPlug.getObjectiveCount() + fbcModelPlug.getGeneProductCount();
    }
    progress = new ProgressBar(count);
    progress.DisplayBar(); // "Processing model " + model.getId());
    // Do not do this because this messes up the HTML display of the file.
    // for (int i = 0; i < model.getUnitDefinitionCount(); i++) {
    // UnitDefinition ud = model.setBasicUnitDefinition(i);
    // //ud.appendNotes(HTMLFormula.toHTML(ud));
    // }
    /*
     * Model
     */
    if (!model.isSetMetaId() && (model.getCVTermCount() > 0)) {
      model.setMetaId(model.getId());
    }
    polishListOfUnitDefinitions(model);
    polishListOfCompartments(model);
    polishListOfSpecies(model);
    boolean strict = polishListOfReactions(model);
    if (strict && model.isSetListOfInitialAssignments()) {
      strict = polishListOfInitialAssignments(model, true);
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
   * @param model
   */
  public void polishListOfUnitDefinitions(Model model) {
    progress.DisplayBar(); // "Processing unit definitions");
    int udCount = model.getUnitDefinitionCount();
    ListOf<UnitDefinition> unitDefinitions = model.getListOfUnitDefinitions();
    UnitDefinition mmol_per_gDW_per_hr = setBasicUnitDefinition(model);
    UnitDefinition substanceUnits = setUnits(model, unitDefinitions);
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
          Unit timeUnit = safeClone(unit);
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
          substanceUnits.addUnit(safeClone(unit));
        }
        break;
      case MOLE:
        if (unit.getScale() == -3) {
          unit.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_IS, createURI("unit", "UO:0000040")));
        }
        if (!substanceExists) {
          substanceUnits.addUnit(safeClone(unit));
        }
        break;
      default:
        break;
      }
    }
    while (progress.getCallNumber() < udCount) {
      progress.DisplayBar();
    }
  }


  /**
   * @param model
   * @return
   */
  private UnitDefinition setBasicUnitDefinition(Model model) {
    UnitDefinition mmol_per_gDW_per_hr = model.getUnitDefinition("mmol_per_gDW_per_hr");
    if (mmol_per_gDW_per_hr == null) {
      mmol_per_gDW_per_hr = model.createUnitDefinition("mmol_per_gDW_per_hr");
      logger.finest(mpMessageBundle.getString("ADDED_UNIT_DEF"));
    }
    if (mmol_per_gDW_per_hr.getUnitCount() < 1) {
      ModelBuilder.buildUnit(mmol_per_gDW_per_hr, 1d, -3, Unit.Kind.MOLE, 1d);
      ModelBuilder.buildUnit(mmol_per_gDW_per_hr, 1d, 0, Unit.Kind.GRAM, -1d);
      ModelBuilder.buildUnit(mmol_per_gDW_per_hr, 3600d, 0, Unit.Kind.SECOND, -1d);
    }
    if (!mmol_per_gDW_per_hr.isSetName()) {
      mmol_per_gDW_per_hr.setName("Millimoles per gram (dry weight) per hour");
    }
    if (!mmol_per_gDW_per_hr.isSetMetaId()) {
      mmol_per_gDW_per_hr.setMetaId(mmol_per_gDW_per_hr.getId());
    }
    mmol_per_gDW_per_hr.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_IS_DESCRIBED_BY, createURI("pubmed", 7986045)));
    return mmol_per_gDW_per_hr;
  }


  /**
   * @param model
   * @param unitDefinitions
   * @return
   */
  private UnitDefinition setUnits(Model model, ListOf<UnitDefinition> unitDefinitions) {
    UnitDefinition substanceUnits = model.getSubstanceUnitsInstance();
    if (substanceUnits == null && unitDefinitions.get("substance") != null) {
      model.setSubstanceUnits(UnitDefinition.SUBSTANCE);
      substanceUnits = model.getSubstanceUnitsInstance();
    }
    UnitDefinition volumeUnits = model.getVolumeUnitsInstance();
    if (volumeUnits == null && unitDefinitions.get("volume") != null) {
      model.setVolumeUnits(UnitDefinition.VOLUME);
    }
    UnitDefinition timeUnits = model.getTimeUnitsInstance();
    if (timeUnits == null && unitDefinitions.get("time") != null) {
      model.setTimeUnits(UnitDefinition.TIME);
    }
    return substanceUnits;
  }


  /**
   * @param model
   */
  public void polishListOfCompartments(Model model) {
    for (int i = 0; i < model.getCompartmentCount(); i++) {
      Compartment c = model.getCompartment(i);
      progress.DisplayBar(); // "Processing compartment " + c.getId());
      polish(c);
    }
  }


  /**
   * @param c
   */
  public void polish(Compartment c) {
    if (!c.isSetId()) {
      c.setId("d"); // default
    }
    c.setSBOTerm(410); // implicit compartment
    if (!c.isSetName()) {
      // TODO: make the name of a compartment a user setting
      c.setName("default");
    }
    if (!c.isSetMetaId() && (c.getCVTermCount() > 0)) {
      c.setMetaId(c.getId());
    }
    if (!c.isSetConstant()) {
      c.setConstant(true);
    }
    if (!c.isSetSpatialDimensions()) {
      // TODO: check with biGG id, not for surfaces etc.
      // c.setSpatialDimensions(3d);
    }
    if (!c.isSetUnits()) {
      Model model = c.getModel();
      // Let's take the model's default unless we don't have anything defined.
      if ((model == null) || !(model.isSetLengthUnits() || model.isSetAreaUnits() || model.isSetVolumeUnits())) {
        // TODO: set compartment units.
        /*
         * This is a temporary solution until we agree on something better.
         */
        c.setUnits(Unit.Kind.DIMENSIONLESS);
      }
    }
  }


  /**
   * @param model
   */
  public void polishListOfSpecies(Model model) {
    // Species, but do not create an overview table for all in the model:
    // String header[] = null;
    // String data[][] = null;
    // SortedMap<String, SortedMap<String, Species>> speciesList = new
    // TreeMap<String, SortedMap<String, Species>>();
    // if (model.getSpeciesCount() > 0) {
    // header = new String[3];
    // data = new String[model.getSpeciesCount()][header.length];
    // header[0] = "BiGG id";
    // header[1] = "Name";
    // header[2] = "Compartment";
    // }
    for (int i = 0; i < model.getSpeciesCount(); i++) {
      Species species = model.getSpecies(i);
      progress.DisplayBar(); // "Processing species " + species.getId());
      polish(species);
      // if (!speciesList.containsKey(species.getName())) {
      // speciesList.put(species.getName(), new TreeMap<String, Species>());
      // }
      // speciesList.get(species.getName()).put(species.getCompartment(),
      // species);
    }
    // int row = 0;
    // for (Map.Entry<String, SortedMap<String, Species>> entry :
    // speciesList.entrySet()) {
    // String htmlName = EscapeChars.forHTML(entry.getKey());
    // for (Map.Entry<String, Species> inner : entry.getValue().entrySet()) {
    // data[row][0] = EscapeChars.forHTML(inner.getValue().getId());
    // data[row][1] = htmlName;
    // data[row][2] =
    // EscapeChars.forHTML(model.getCompartment(inner.getKey()).getName());
    // row++;
    // }
    // }
    // Map<String, String> attributes = new HashMap<String, String>();
    // // TODO: add attributes!
    // attributes.put("width", "900px");
  }


  /**
   * @param species
   */
  public void polish(Species species) {
    String id = species.getId();
    if (species.getId().endsWith("_boundary")) {
      logger.warning(format(mpMessageBundle.getString("SPECIES_ID_INVALID"), id));
      id = id.substring(0, id.length() - 9);
      if (!species.isSetBoundaryCondition() || !species.isBoundaryCondition()) {
        logger.warning(format(mpMessageBundle.getString("BOUNDARY_FLAG_MISSING"), id));
        species.setBoundaryCondition(true);
      }
    } else if (!species.isSetBoundaryCondition()) {
      species.setBoundaryCondition(false);
    }
    BiGGId biggId = extractBiGGId(id);
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
      if (biggId.isSetAbbreviation()) {
        species.setName(polishName(species.getName()));
      }
      if ((species.getCVTermCount() > 0) && !species.isSetMetaId()) {
        species.setMetaId(species.getId());
      }
      if (biggId.isSetCompartmentCode() && species.isSetCompartment()
        && !biggId.getCompartmentCode().equals(species.getCompartment())) {
        logger.warning(format(mpMessageBundle.getString("CHANGE_COMPART_REFERENCE"), species.getId(),
          species.getCompartment(), biggId.getCompartmentCode()));
        species.setCompartment(biggId.getCompartmentCode());
      }
    }
    checkCompartment(species);
  }


  /**
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
      logger.warning(format(mpMessageBundle.getString("CREATE_MISSING_COMP"), cId, nsb.getId(), nsb.getElementName()));
      c = model.createCompartment(cId);
      polish(c);
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
      progress.DisplayBar(); // "Processing reaction " + r.getId());
    }
    return strict;
  }


  /**
   * @param r
   * @return {@code true} if the given reaction qualifies for strict FBC.
   */
  public boolean polish(Reaction r) {
    String id = r.getId();
    id = setSBOTermFromPattern(r, id);
    BiGGId biggId = extractBiGGId(id);
    if (biggId != null) {
      // TODO: what was the intention here, if compartmentId is not used?
      String compartmentId = r.isSetCompartment() ? r.getCompartment() : null;
      if (r.isSetListOfReactants()) {
        String cId = polish(r.getListOfReactants(), SBO.getReactant());
        compartmentId = checkCId(cId, compartmentId);
      }
      if (r.isSetListOfProducts()) {
        String cId = compartmentId = polish(r.getListOfProducts(), SBO.getProduct());
        compartmentId = checkCId(cId, compartmentId);
      }
      // TODO: it was decided not to do this:
      // if ((compartmentId != null) && !r.isSetCompartment()) {
      // r.setCompartment(compartmentId);
      // checkCompartment(r);
      // }
      // if (biggId.isSetCompartmentCode() && !r.isSetCompartment()) {
      // logger.info(format("Applying compartment declaration as
      // specified by compartment code ''{1}'' to reaction ''{0}''.", r.getId(),
      // biggId.getCompartmentCode()));
      // r.setCompartment(biggId.getCompartmentCode());
      // checkCompartment(r);
      // }
      if (!r.isSetMetaId() && (r.getCVTermCount() > 0)) {
        r.setMetaId(id);
      }
      if (id.startsWith("R_")) {
        id = id.substring(2);
      }
      r.setName(polishName(r.getName()));
      SBMLUtils.setRequiredAttributes(r);
    }
    // This is a check if we are producing invalid SBML.
    if ((r.getReactantCount() == 0) && (r.getProductCount() == 0)) {
      ResourceBundle bundle = ResourceManager.getBundle("org.sbml.jsbml.resources.cfg.Messages");
      logger.severe(format(bundle.getString("SBMLCoreParser.reactionWithoutParticipantsError"), r.getId()));
    } else {
      checkBalance(r);
    }
    boolean strict = checkBounds(r);
    strict = checkReactantsProducts(r, strict);
    return strict;
  }


  /**
   * @param r
   * @param id
   * @return
   */
  private String setSBOTermFromPattern(Reaction r, String id) {
    if (Patterns.BIOMASS_CASE_INSENSITIVE.getPattern().matcher(id).matches()) {
      r.setSBOTerm(629); // biomass production
      if (!Patterns.BIOMASS_CASE_SENSITIVE.getPattern().matcher(id).matches()) {
        // in response to https://github.com/SBRG/bigg_models/issues/175
        id = id.replaceAll("[Bb][Ii][Oo][Mm][Aa][Ss][Ss]", "BIOMASS");
        r.setId(id);
      }
    } else if (Patterns.DEMAND_REACTION.getPattern().matcher(id).matches()) {
      r.setSBOTerm(628); // demand reaction
    } else if (Patterns.EXCHANGE_REACTION.getPattern().matcher(id).matches()) {
      r.setSBOTerm(627); // exchange reaction
    } else if (Patterns.ATP_MAINTENANCE.getPattern().matcher(id).matches()) {
      r.setSBOTerm(630); // ATP maintenance
    } else if (Patterns.SINK_REACTION.getPattern().matcher(id).matches()) {
      r.setSBOTerm(632);
      if (Patterns.SINK_OLD_STYLE.getPattern().matcher(id).matches()) {
        id = id.replaceAll("_[Ss][Ii][Nn][Kk]_", "_SK_");
        r.setId(id);
      }
    }
    return id;
  }


  /**
   * @param cId
   * @param compartmentId
   * @return
   */
  private String checkCId(String cId, String compartmentId) {
    if (cId == null) {
      compartmentId = null;
    } else {
      if (compartmentId == null) {
        compartmentId = cId;
      } else if (!compartmentId.equals(cId)) {
        compartmentId = null;
      }
    }
    return compartmentId;
  }


  /**
   * @param r
   */
  private void checkBalance(Reaction r) {
    if (!r.isSetSBOTerm()) {
      // The reaction has not been recognized as demand or exchange reaction
      if (r.getReactantCount() == 0) {
        if (r.isReversible()) {
          // TODO: sink reaction
        } else {
          logger.warning(format(mpMessageBundle.getString("REACTION_DM_NOT_IN_ID"), r.getId()));
          r.setSBOTerm(628); // demand reaction
        }
      } else if (r.getProductCount() == 0) {
        if (r.isReversible()) {
          // TODO: source reaction
        } else {
          logger.warning(format(mpMessageBundle.getString("REACTION_DM_NOT_IN_ID"), r.getId()));
          r.setSBOTerm(628); // demand reaction
        }
      }
    }
    if (isCheckMassBalance() && ((r.getSBOTerm() < 627) || (630 < r.getSBOTerm()))) {
      // check atom balance only if the reaction is not identified as biomass
      // production, demand, exchange or ATP maintenance.
      AtomCheckResult<Reaction> defects = AtomBalanceCheck.checkAtomBalance(r, 1);
      if ((defects != null) && (defects.hasDefects())) {
        logger.warning(format(mpMessageBundle.getString("ATOMS_MISSING"), r.getId(), defects.getDefects().toString()));
      } else if (defects == null) {
        logger.fine(format(mpMessageBundle.getString("CHECK_ATOM_BALANCE_FAILED"), r.getId()));
      } else {
        logger.fine(format(mpMessageBundle.getString("ATOMS_OK"), r.getId()));
      }
    }
  }


  private boolean checkBounds(Reaction r) {
    FBCReactionPlugin rPlug = (FBCReactionPlugin) r.getPlugin(FBCConstants.shortLabel);
    Parameter lb = rPlug.getLowerFluxBoundInstance();
    Parameter ub = rPlug.getUpperFluxBoundInstance();
    boolean strict = polishFluxBound(lb) && polishFluxBound(ub);
    if (strict) {
      strict = checkBound(lb);
      strict &= lb.isSetValue() && (lb.getValue() < Double.POSITIVE_INFINITY);
      strict &= checkBound(ub);
      strict &= ub.isSetValue() && (ub.getValue() > Double.NEGATIVE_INFINITY);
      strict &= lb.isSetValue() && ub.isSetValue() && (lb.getValue() <= ub.getValue());
      if (!strict) {
        logger.warning(format(mpMessageBundle.getString("FLUX_BOUND_ERROR"), r.getId()));
      }
    } else {
      logger.warning(format(mpMessageBundle.getString("FLUX_BOUNDS_MISSING"), r.getId()));
    }
    return strict;
  }


  /**
   * @param bound
   * @return {@code true} if this method successfully updated the bound
   *         parameter.
   */
  public boolean polishFluxBound(Parameter bound) {
    if (bound == null) {
      return false;
    }
    if (Patterns.DEFAULT_FLUX_BOUND.getPattern().matcher(bound.getId()).matches()) {
      bound.setSBOTerm(626); // default flux bound
    } else {
      bound.setSBOTerm(625); // flux bound
    }
    return true;
  }


  /**
   * Checks if a given bound parameter satisfies the required properties of a
   * strict flux bound parameter:
   * <li>not null
   * <li>constant
   * <li>defined value
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
   * @param r
   * @param strict
   * @return
   */
  private boolean checkReactantsProducts(Reaction r, boolean strict) {
    if (strict && r.isSetListOfReactants()) {
      strict = checkSpeciesReferences(r.getListOfReactants());
      if (!strict) {
        logger.warning(format(mpMessageBundle.getString("ILLEGAL_STOICH_PROD"), r.getId()));
      }
    }
    if (strict && r.isSetListOfProducts()) {
      strict = checkSpeciesReferences(r.getListOfProducts());
      if (!strict) {
        logger.warning(format(mpMessageBundle.getString("ILLEGAL_STOICH_REACT"), r.getId()));
      }
    }
    return strict;
  }


  /**
   * @param listOfSpeciesReference
   * @return
   */
  @SuppressWarnings("deprecation")
  public boolean checkSpeciesReferences(ListOf<SpeciesReference> listOfSpeciesReference) {
    boolean strict = true;
    for (SpeciesReference sr : listOfSpeciesReference) {
      strict &= sr.isConstant() && sr.isSetStoichiometry() && !sr.isSetStoichiometryMath()
        && !Double.isNaN(sr.getValue()) && Double.isFinite(sr.getValue());
    }
    return strict;
  }


  /**
   * @param model
   * @param strict
   * @return
   */
  public boolean polishListOfInitialAssignments(Model model, boolean strict) {
    for (InitialAssignment ia : model.getListOfInitialAssignments()) {
      Variable variable = ia.getVariableInstance();
      progress.DisplayBar(); // "Processing initial assignment for " + variable.getId());
      if (variable != null) {
        if (variable instanceof Parameter) {
          if (variable.isSetSBOTerm() && SBO.isChildOf(variable.getSBOTerm(), 625)) {
            strict = false;
            logger.warning(format(mpMessageBundle.getString("FLUX_BOUND_STRICT_CHANGE"), variable.getId()));
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
      logger.warning(format(mpMessageBundle.getString("OBJ_MISSING"), modelPlug.getParent().getId()));
    } else {
      for (Objective objective : modelPlug.getListOfObjectives()) {
        progress.DisplayBar(); // "Processing objective " + objective.getId());
        if (!objective.isSetListOfFluxObjectives()) {
          Model model = modelPlug.getParent();
          strict &= SBMLFix.fixObjective(model.getId(), model.getListOfReactions(), modelPlug, fluxCoefficients,
            fluxObjectives);
        }
        if (objective.isSetListOfFluxObjectives()) {
          strict &= polishListOfFluxObjectives(strict, objective);
        }
      }
    }
    return strict;
  }


  /**
   * @param strict
   * @param objective
   * @return
   */
  public boolean polishListOfFluxObjectives(boolean strict, Objective objective) {
    if (objective.getFluxObjectiveCount() == 0) {
      // Note: the strict attribute does not require the presence of any flux objectives.
      logger.warning(format(mpMessageBundle.getString("OBJ_FLUX_OBJ_MISSING"), objective.getId()));
    } else {
      if (objective.getFluxObjectiveCount() > 1) {
        logger.warning(format(mpMessageBundle.getString("TOO_MUCH_OBJ_TARGETS"), objective.getId()));
      }
      for (FluxObjective fluxObjective : objective.getListOfFluxObjectives()) {
        if (!fluxObjective.isSetCoefficient() || Double.isNaN(fluxObjective.getCoefficient())
          || !Double.isFinite(fluxObjective.getCoefficient())) {
          logger.warning(format(mpMessageBundle.getString("FLUX_OBJ_COEFF_INVALID"), fluxObjective.getReaction()));
        }
      }
    }
    return strict;
  }


  /**
   * @param listOf
   * @param defaultSBOterm
   * @return
   */
  private String polish(ListOf<SpeciesReference> listOf, int defaultSBOterm) {
    String compartmentId = "";
    Model model = listOf.getModel();
    for (SpeciesReference sr : listOf) {
      // Not sure if we need this check here because SBML validator can do it:
      /*
       * if (sr.isSetId() && !SyntaxChecker.isValidId(sr.getId(), sr.getLevel(),
       * sr.getVersion())) {
       * logger.severe(MessageFormat.
       * format("Found a speciesReference with invalid identifier ''{0}''.",
       * sr.getId()));
       * }
       */
      if (!sr.isSetSBOTerm() && !omitGenericTerms) {
        sr.setSBOTerm(defaultSBOterm);
      }
      Species species = model.getSpecies(sr.getSpecies());
      if (species != null) {
        if (!species.isSetCompartment() || (compartmentId == null)
          || (!compartmentId.isEmpty() && !compartmentId.equals(species.getCompartment()))) {
          compartmentId = null;
        } else {
          compartmentId = species.getCompartment();
        }
      } else {
        logger.warning(format(mpMessageBundle.getString("SPECIES_REFERENCE_INVALID"), sr.getSpecies()));
      }
    }
    if ((compartmentId == null) || compartmentId.isEmpty()) {
      return null;
    }
    return compartmentId;
  }


  /**
   * @param fbcModelPlug
   */
  public void polishListOfGeneProducts(FBCModelPlugin fbcModelPlug) {
    for (GeneProduct geneProduct : fbcModelPlug.getListOfGeneProducts()) {
      progress.DisplayBar(); // "Processing gene product " +
      // geneProduct.getId());
      polish(geneProduct);
    }
  }


  /**
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
    if (geneProduct.getCVTermCount() > 0) {
      geneProduct.setMetaId(id);
    }
    if (!geneProduct.isSetName() || geneProduct.getName().equalsIgnoreCase("None")) {
      geneProduct.setName(label);
    }
  }


  /**
   * @param model
   */
  public void polishListOfParameters(Model model) {
    for (int i = 0; i < model.getParameterCount(); i++) {
      Parameter parameter = model.getParameter(i);
      progress.DisplayBar(); // "Processing parameter " + parameter.getId());
      polish(parameter);
    }
  }


  /**
   * @param p
   */
  public void polish(Parameter p) {
    if (p.isSetId() && !p.isSetName()) {
      p.setName(polishName(p.getId()));
    }
  }


  /**
   * @param sbase
   * @return
   */
  private <T extends SBase> T safeClone(T sbase) {
    @SuppressWarnings("unchecked")
    T sb = (T) sbase.clone();
    if (sb.isSetMetaId()) {
      sb.unsetMetaId();
    }
    return sb;
  }


  /**
   * @param name
   * @return
   */
  public static String polishName(String name) {
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
      newName =
        newName.substring(0, newName.lastIndexOf('_')) + " - " + newName.substring(newName.lastIndexOf('_') + 1);
    }
    newName = newName.replace("_", " ");
    if (!newName.equals(name)) {
      logger.fine(format(mpMessageBundle.getString("CHANGED_NAME"), name, newName));
    }
    return newName;
  }


  /**
   * @return the includeAnyURI
   */
  public boolean getIncludeAnyURI() {
    return includeAnyURI;
  }


  /**
   * @param includeAnyURI
   *        define if only identifiers.org URIs are included in MIRIAM
   *        annotation.
   */
  public void setIncludeAnyURI(boolean includeAnyURI) {
    this.includeAnyURI = includeAnyURI;
  }


  /**
   * @param checkMassBalance
   */
  public void setCheckMassBalance(boolean checkMassBalance) {
    this.checkMassBalance = checkMassBalance;
  }


  /**
   * @param modelNamePattern
   *        the modelNamePattern to set
   */
  public void setDocumentTitlePattern(String modelNamePattern) {
    documentTitlePattern = modelNamePattern;
    Parameters parameters = Parameters.get();
    parameters.documentTitlePattern = documentTitlePattern;
  }


  /**
   * @param omitGenericTerms
   *        the omitGenericTerms to set
   */
  public void setOmitGenericTerms(boolean omitGenericTerms) {
    this.omitGenericTerms = omitGenericTerms;
  }


  /**
   * @param fluxCoefficients
   */
  public void setFluxCoefficients(double[] fluxCoefficients) {
    this.fluxCoefficients = fluxCoefficients;
  }


  /**
   * @param fluxObjectives
   */
  public void setFluxObjectives(String[] fluxObjectives) {
    this.fluxObjectives = fluxObjectives;
  }
}
