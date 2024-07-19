package edu.ucsd.sbrg.annotation;

import java.util.List;

import edu.ucsd.sbrg.Parameters;
import edu.ucsd.sbrg.annotation.fbc.FBCAnnotator;
import edu.ucsd.sbrg.db.MemorizedQuery;
import edu.ucsd.sbrg.db.bigg.Publication;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import edu.ucsd.sbrg.resolver.Registry;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;

import edu.ucsd.sbrg.db.bigg.BiGGDB;


/**
 * This class is responsible for annotating SBML models using data from the BiGG database.
 * It handles the addition of annotations related to compartments, species, reactions, and gene products.
 * 
 * @author Thomas Zajac
 *         This code runs only, if ANNOTATE_WITH_BIGG is true
 */
public class BiGGAnnotator extends AbstractAnnotator<SBMLDocument> {

  public BiGGAnnotator(Parameters parameters, Registry registry) {
    super(parameters, registry);
  }

  public BiGGAnnotator(Parameters parameters, Registry registry, List<ProgressObserver> observers) {
      super(parameters, registry, observers);
  }
  

  /**
   * Annotates an SBMLDocument using data from the BiGG Knowledgebase. This method processes various components of the
   * SBML model such as compartments, species, reactions, and gene products by adding relevant annotations from BiGG.
   * It also handles the addition of publications and notes related to the model.
   *
   * @param doc The SBMLDocument that contains the model to be annotated.
   */
  @Override
  public void annotate(SBMLDocument doc) {
    // TODO: these sanity checks need to be improved
    Model model = doc.getModel();
    String modelId = model.getId();
    if (!MemorizedQuery.isModel(modelId)) {
      return;
    }
    // model.isSetMetaId()

    new ModelAnnotator(parameters, registry, getObservers()).annotate(model);

    // Annotate various components of the model
    List<Publication> publications = BiGGDB.getPublications(modelId);
    new PublicationsAnnotator(model, parameters, registry, getObservers()).annotatePublications(publications);

    new CompartmentsAnnotator(parameters, registry, getObservers()).annotate(model.getListOfCompartments());

    new SpeciesAnnotator(parameters, registry, getObservers()).annotate(model.getListOfSpecies());

    new ReactionsAnnotator(parameters, registry, getObservers()).annotate(model.getListOfReactions());

    new FBCAnnotator(parameters, registry, getObservers()).annotate(model);

    new DocumentNotesProcessor(parameters).processNotes(doc);

    new AnnotationsSorter().groupAndSortAnnotations(doc);
  }

}
