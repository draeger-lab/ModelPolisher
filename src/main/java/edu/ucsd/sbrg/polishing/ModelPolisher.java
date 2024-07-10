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
package edu.ucsd.sbrg.polishing;

import de.zbit.util.ResourceManager;
import edu.ucsd.sbrg.Parameters;
import edu.ucsd.sbrg.util.GeneProductAssociationsPolisher;
import edu.ucsd.sbrg.util.ProgressInitialization;
import edu.ucsd.sbrg.util.ProgressObserver;
import edu.ucsd.sbrg.util.ProgressUpdate;
import edu.ucsd.sbrg.miriam.Registry;
import org.sbml.jsbml.*;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.converters.CobraToFbcV2Converter;
import org.sbml.jsbml.util.SBMLtools;
import org.sbml.jsbml.util.ValuePair;

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
public class ModelPolisher {

  /**
   * A {@link Logger} for this class.
   */
  private static final Logger logger = Logger.getLogger(ModelPolisher.class.getName());

  /**
   * Bundle for ModelPolisher logger messages
   */
  private static final ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");
  private final Parameters parameters;

  public ModelPolisher(Parameters parameters) {
      this.parameters = parameters;
  }

  private final List<ProgressObserver> observers = new ArrayList<>();


    /**
   * Ensures that the SBML document is set to Level 3 and Version 1, which are required for compatibility with necessary plugins.
   * If the document is not already at this level and version, it updates the document to meet these specifications.
   * After ensuring the document is at the correct level and version, it converts the document using the CobraToFbcV2Converter.
   *
   * @param doc The SBMLDocument to be checked and potentially converted.
   * @return The SBMLDocument after potentially updating its level and version and converting it.
   */
  private SBMLDocument checkLevelAndVersion(SBMLDocument doc) {
    if (!doc.isSetLevelAndVersion() || (doc.getLevelAndVersion().compareTo(ValuePair.of(3, 1)) < 0)) {
      logger.info(MESSAGES.getString("TRY_CONV_LVL3_V1"));
      SBMLtools.setLevelAndVersion(doc, 3, 1);
    }
    // Initialize the converter for Cobra to FBC version 2
    CobraToFbcV2Converter converter = new CobraToFbcV2Converter();
    // Convert the document and return the converted document
    return converter.convert(doc);
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
    if (doc.getModel() == null) {
      logger.severe(MESSAGES.getString("MODEL_MISSING"));
      // TODO: handle this better
      throw new RuntimeException();
    }
    // Ensure the document is at the correct SBML level and version
    doc = checkLevelAndVersion(doc);
    // Retrieve the model from the document.
    Model model = doc.getModel();

    int count = taskCount(model);
    for (var o : observers) {
      o.initialize(new ProgressInitialization(count));
    }

    // Polish the model.
    polish(model);
    // Set the SBO term for the document to indicate a flux balance framework.
    doc.setSBOTerm(624);

    for (var o : observers) {
      o.finish(null);
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

    updateProgressObservers("Polishing Model (1/9)  ", model);
    // Delegate polishing tasks to specific methods.
    new UnitPolishing(model, observers).polishListOfUnitDefinitions();
    polishListOfCompartments(model);
    polishListOfSpecies(model);
    // TODO: strictness sollte losgelöst von dem Polishing bestimmt werden
    boolean strict = polishListOfReactions(model);
    
    // Perform final polishing adjustments based on the strictness of the reactions.
    var modelPolishing = new MiscPolishing(model, strict, observers, parameters);
    modelPolishing.polish();
  }

  private void updateProgressObservers(String text, AbstractSBase obj) {
    for (var o : observers) {
      o.update(new ProgressUpdate(text, obj));
    }
  }

  private int taskCount(Model model) {
    // Calculate the total number of tasks to initialize the progress bar.
    int count = 1 // Account for model properties
      // + model.getUnitDefinitionCount()
      // TODO: see UnitPolishing TODO, why UnitDefinitionCount is replaced by 1
      + 1
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
    return count;
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
      updateProgressObservers("Polishing Compartments (3/9)  ", compartment);
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
      updateProgressObservers("Polishing Species (4/9)  ", species); // Update progress display for each species
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

    var gpaPolisher = new GeneProductAssociationsPolisher();

    var iterator = model.getListOfReactions().iterator();
    while (iterator.hasNext()) {
      var reaction = iterator.next();
      updateProgressObservers("Polishing Reactions (5/9)  ", reaction);

      // remove reaction if ID is missing
      String id = reaction.getId();
      if (id.isEmpty()) {
        if (reaction.isSetName()) {
          logger.severe(format(MESSAGES.getString("REACTION_MISSING_ID"), reaction.getName()));
        } else {
          logger.severe(MESSAGES.getString("REACTION_MISSING_ID_NAME"));
        }
        iterator.remove();
      } else {
        var reactionPolishing = new ReactionPolishing(reaction, gpaPolisher, parameters);
        reactionPolishing.polish();
        // TODO: das hier sollte nicht in
        strict &= reactionPolishing.checkReactionStrictness();
      }


    }
    return strict;
  }

  public void addObserver(ProgressObserver o) {
    observers.add(o);
  }
}
