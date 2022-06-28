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
package edu.ucsd.sbrg.bigg.polishing;

import de.zbit.util.ResourceManager;
import de.zbit.util.progressbar.AbstractProgressBar;
import de.zbit.util.progressbar.ProgressBar;
import edu.ucsd.sbrg.miriam.Registry;
import org.sbml.jsbml.*;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

/**
 * @author Andreas Dr&auml;ger
 */
public class SBMLPolisher {

  /**
   * A {@link Logger} for this class.
   */
  private static final transient Logger logger = Logger.getLogger(SBMLPolisher.class.getName());
  /**
   * Bundle for ModelPolisher logger messages
   */
  private static final transient ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");
  /**
   *
   */
  protected AbstractProgressBar progress;

  /**
   *
   */
  public SBMLPolisher() {
  }


  /**
   * Entry point from #ModelPolisher class
   * 
   * @param doc:
   *        SBMLDocument containing the model to polish
   * @return SBMLDocument containing polished model
   */
  public SBMLDocument polish(SBMLDocument doc) {
    if (!doc.isSetModel()) {
      logger.severe(MESSAGES.getString("NO_MODEL_FOUND"));
      return doc;
    }
    Model model = doc.getModel();
    polish(model);
    doc.setSBOTerm(624); // flux balance framework
    if (progress != null) {
      progress.finished();
    }
    Registry.processResources(doc.getAnnotation());
    return doc;
  }


  /**
   * Main method delegating all polishing tasks
   * 
   * @param model:
   *        SBML Model to polish
   */
  public void polish(Model model) {
    logger.info(format(MESSAGES.getString("PROCESSING_MODEL"), model.getId()));
    // initialize ProgressBar
    int count = 1 // for model properties
      + model.getUnitDefinitionCount() + model.getCompartmentCount() + model.getParameterCount()
      + model.getReactionCount() + model.getSpeciesCount() + model.getInitialAssignmentCount();
    if (model.isSetPlugin(FBCConstants.shortLabel)) {
      FBCModelPlugin fbcModelPlug = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
      count += fbcModelPlug.getObjectiveCount() + fbcModelPlug.getGeneProductCount();
    }
    progress = new ProgressBar(count);
    progress.DisplayBar("Polishing Model (1/9)  ");
    new UnitPolishing(model, progress).polishListOfUnitDefinitions();
    polishListOfCompartments(model);
    polishListOfSpecies(model);
    boolean strict = polishListOfReactions(model);
    ModelPolishing modelPolishing = new ModelPolishing(model, strict, progress);
    modelPolishing.polish();
  }
  

  /**
   * @param model
   */
  public void polishListOfCompartments(Model model) {
    for (int i = 0; i < model.getCompartmentCount(); i++) {
      progress.DisplayBar("Polishing Compartments (3/9)  ");
      CompartmentPolishing compartmentPolishing = new CompartmentPolishing(model.getCompartment(i));
      compartmentPolishing.polish();
    }
  }


  /**
   * @param model
   */
  public void polishListOfSpecies(Model model) {
    List<Species> speciesToRemove = new ArrayList<>();
    for (Species species : model.getListOfSpecies()) {
      progress.DisplayBar("Polishing Species (4/9)  "); // "Processing species " + species.getId());
      SpeciesPolishing speciesPolishing = new SpeciesPolishing(species);
      speciesPolishing.polish().ifPresent(speciesToRemove::add);
    }
    for (Species species : speciesToRemove) {
      model.removeSpecies(species);
    }
  }


  /**
   * @param model
   * @return
   */
  public boolean polishListOfReactions(Model model) {
    boolean strict = true;
    for (int i = 0; i < model.getReactionCount(); i++) {
      progress.DisplayBar("Polishing Reactions (5/9)  ");
      ReactionPolishing reactionPolishing = new ReactionPolishing(model.getReaction(i));
      strict &= reactionPolishing.polish();
    }
    return strict;
  }


  /**
   * @param name
   * @return
   */
  public static String polishName(String name) {
    // can this be replaced by BiGGId creation?
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
      logger.fine(format(MESSAGES.getString("CHANGED_NAME"), name, newName));
    }
    return newName;
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

  public AbstractProgressBar getProgress() {
    return progress;
  }

  public void setProgress(AbstractProgressBar progress) {
    this.progress = progress;
  }
}
