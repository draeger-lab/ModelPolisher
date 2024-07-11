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
import edu.ucsd.sbrg.polishing.fbc.FBCPolisher;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import org.sbml.jsbml.*;
import org.sbml.jsbml.ext.fbc.FBCConstants;

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
public class ModelPolisher extends AbstractPolisher<Model> {

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

  public ModelPolisher(Parameters parameters, List<ProgressObserver> observers) {
      super(observers);
      this.parameters = parameters;
  }

  /**
   * This method orchestrates the polishing of an SBML model by delegating tasks to specific polishing methods
   * for different components of the model. It initializes a progress bar to track and display the progress of
   * the polishing process.
   * 
   * @param model The SBML Model to be polished.
   */
  @Override
  public void polish(Model model) {
    // Log the start of processing the model.
    logger.info(format(MESSAGES.getString("PROCESSING_MODEL"), model.getId()));

    updateProgressObservers("Polishing Model (1/9)  ", model);

    // Delegate polishing tasks to specific methods.
    new UnitPolisher(model, getObservers()).polishListOfUnitDefinitions();

    new CompartmentPolisher(getObservers()).polish(model.getListOfCompartments());

    new SpeciesPolisher(getObservers()).polish(model.getListOfSpecies());

    // Polish the list of parameters in the model
    new ParametersPolisher(getObservers()).polish(model.getListOfParameters());

    // Process the annotations in the model
    new AnnotationPolisher().polish(model.getAnnotation());

    new ReactionsPolisher(parameters, getObservers()).polish(model.getListOfReactions());

    // Check if the FBC plugin is set and proceed with polishing specific FBC components
    if (model.isSetPlugin(FBCConstants.shortLabel)) {
      new FBCPolisher(parameters, getObservers()).polish(model);
    }

    // Set the metaId of the model if it is not set and there are CV terms
    if (!model.isSetMetaId() && (model.getCVTermCount() > 0)) {
      model.setMetaId(model.getId());
    }
  }
}
