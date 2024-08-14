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
import edu.ucsd.sbrg.parameters.PolishingParameters;
import edu.ucsd.sbrg.parameters.SBOParameters;
import edu.ucsd.sbrg.polishing.fbc.FBCPolisher;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import edu.ucsd.sbrg.resolver.Registry;
import org.sbml.jsbml.*;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ResourceBundle;

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
public class ModelPolisher extends AbstractPolisher implements IPolishSBases<Model> {

  private static final Logger logger = LoggerFactory.getLogger(ModelPolisher.class);
  private static final ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");
  private final SBOParameters sboParameters;


  public ModelPolisher(PolishingParameters polishingParameters, SBOParameters sboParameters, Registry registry) {
      super(polishingParameters, registry);
      this.sboParameters = sboParameters;
  }

  public ModelPolisher(PolishingParameters polishingParameters, SBOParameters sboParameters, Registry registry, List<ProgressObserver> observers) {
      super(polishingParameters, registry, observers);
      this.sboParameters = sboParameters;
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
    logger.debug(format(MESSAGES.getString("PROCESSING_MODEL"), model.toString()));
    statusReport("Polishing Model (1/9)  ", model);

    new AnnotationPolisher(polishingParameters, registry, getObservers()).polish(model.getAnnotation());

    // Set the metaId of the model if it is not set and there are CV terms
    if (!model.isSetMetaId() && (model.getCVTermCount() > 0)) {
      model.setMetaId(model.getId());
    }

    // Delegate polishing tasks
    new UnitPolisher(polishingParameters, registry, getObservers()).polish(model);

    new CompartmentPolisher(polishingParameters, registry, getObservers()).polish(model.getListOfCompartments());

    new SpeciesPolisher(polishingParameters, registry, getObservers()).polish(model.getListOfSpecies());

    new ParametersPolisher(polishingParameters, registry, getObservers()).polish(model.getListOfParameters());

    new ReactionsPolisher(polishingParameters, sboParameters, registry, getObservers()).polish(model.getListOfReactions());

    if (model.isSetPlugin(FBCConstants.shortLabel)) {
      new FBCPolisher(polishingParameters, sboParameters, registry, getObservers()).polish(model);
    }

  }

  @Override
  public String toString() {
    return "ModelPolisher{" +
            "parameters=" + polishingParameters +
            '}';
  }

}
