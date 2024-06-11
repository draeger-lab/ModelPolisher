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
 * This class provides functionality to polish an SBML (Systems Biology Markup Language) document.
 * Polishing involves enhancing the document with additional annotations, setting appropriate SBO (Systems Biology Ontology) terms,
 * and ensuring the document adheres to certain standards and conventions useful for computational models in systems biology.
 * The class supports operations such as checking the document's structure, polishing individual model components,
 * and processing external resources linked within the document.
 * 
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
   * Progress bar to visually indicate the progress of the polishing process.
   */
  protected AbstractProgressBar progress;

  /**
   *
   */
  public SBMLPolisher() {
  }


  /**
   * This method serves as the entry point from the ModelPolisher class to polish an SBML document.
   * It ensures the document contains a model, performs a sanity check, polishes the model, sets the SBO term,
   * marks the progress as finished if applicable, and processes any linked resources.
   * 
   * @param doc The SBMLDocument containing the model to be polished.
   * @return The polished SBMLDocument.
   */
  public SBMLDocument polish(SBMLDocument doc) {
    // Check if the document has a model set, log severe error if not.
    if (!doc.isSetModel()) {
      logger.severe(MESSAGES.getString("NO_MODEL_FOUND"));
      return doc;
    }
    // Retrieve the model from the document.
    Model model = doc.getModel();

    // Polish the model.
    polish(model);
    // Set the SBO term for the document to indicate a flux balance framework.
    doc.setSBOTerm(624);
    // If a progress bar is set, mark the progress as finished.
    if (progress != null) {
      progress.finished();
    }
    // Process any external resources linked in the document's annotations.
    Registry.processResources(doc.getAnnotation());
    return doc;
  }


  /**
   * This method orchestrates the polishing of an SBML model by delegating tasks to specific polishing methods
   * for different components of the model. It initializes a progress bar to track and display the progress of
   * the polishing process.
   * 
   * @param model The SBML Model to be polished.
   */
  public void polish(Model model) {
    // Log the start of processing the model.
    logger.info(format(MESSAGES.getString("PROCESSING_MODEL"), model.getId()));
    
    // Calculate the total number of tasks to initialize the progress bar.
    int count = 1 // Account for model properties
      + model.getUnitDefinitionCount() 
      + model.getCompartmentCount() 
      + model.getParameterCount()
      + model.getReactionCount() 
      + model.getSpeciesCount() 
      + model.getInitialAssignmentCount();
    
    // Include tasks from FBC plugin if present.
    if (model.isSetPlugin(FBCConstants.shortLabel)) {
      FBCModelPlugin fbcModelPlug = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
      count += fbcModelPlug.getObjectiveCount() + fbcModelPlug.getGeneProductCount();
    }
    
    // Initialize the progress bar with the total count of tasks.
    progress = new ProgressBar(count);
    progress.DisplayBar("Polishing Model (1/9)  ");
    
    // Delegate polishing tasks to specific methods.
    new UnitPolishing(model, progress).polishListOfUnitDefinitions();
    polishListOfCompartments(model);
    polishListOfSpecies(model);
    boolean strict = polishListOfReactions(model);
    
    // Perform final polishing adjustments based on the strictness of the reactions.
    ModelPolishing modelPolishing = new ModelPolishing(model, strict, progress);
    modelPolishing.polish();
  }
  

  /**
   * Polishes all compartments in the given SBML model. This method iterates through each compartment
   * in the model, updates the progress display, and applies polishing operations defined in the
   * CompartmentPolishing class.
   * 
   * @param model The SBML Model containing the compartments to be polished.
   */
  public void polishListOfCompartments(Model model) {
    for (Compartment compartment : model.getListOfCompartments()) {
      progress.DisplayBar("Polishing Compartments (3/9)  ");
      CompartmentPolishing compartmentPolishing = new CompartmentPolishing(compartment);
      compartmentPolishing.polish();
    }
  }


  /**
   * Polishes the list of species in the given SBML model. This method iterates through each species,
   * applies polishing operations, and collects species that need to be removed based on the polishing results.
   * Removal is based on criteria defined in the SpeciesPolishing class.
   * 
   * @param model The SBML Model containing the species to be polished.
   */
  public void polishListOfSpecies(Model model) {
    List<Species> speciesToRemove = new ArrayList<>();
    for (Species species : model.getListOfSpecies()) {
      progress.DisplayBar("Polishing Species (4/9)  "); // Update progress display for each species
      SpeciesPolishing speciesPolishing = new SpeciesPolishing(species);
      // Polish each species and collect those that need to be removed
      speciesPolishing.polish().ifPresent(speciesToRemove::add);
    }
    // Remove the collected species from the model
    for (Species species : speciesToRemove) {
      model.removeSpecies(species);
    }
  }


  /**
   * Polishes all reactions in the given SBML model. This method iterates through each reaction,
   * updates the progress display, and applies polishing operations defined in the ReactionPolishing class.
   * It also aggregates a strictness flag that indicates if all reactions conform to strict FBC (Flux Balance Constraints) standards.
   * 
   * @param model The SBML Model containing the reactions to be polished.
   * @return true if all reactions are strictly defined according to FBC standards, false otherwise.
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

}
