package edu.ucsd.sbrg.annotation.bigg;

import java.sql.SQLException;
import java.util.List;

import edu.ucsd.sbrg.annotation.AnnotationException;
import edu.ucsd.sbrg.parameters.BiGGAnnotationParameters;
import edu.ucsd.sbrg.parameters.SBOParameters;
import edu.ucsd.sbrg.annotation.AnnotationsSorter;
import edu.ucsd.sbrg.annotation.bigg.fbc.BiGGFBCAnnotator;
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
public class BiGGSBMLAnnotator extends AbstractBiGGAnnotator<SBMLDocument> {

  private final SBOParameters sboParameters;

  public BiGGSBMLAnnotator(BiGGDB bigg, BiGGAnnotationParameters parameters, SBOParameters sboParameters, Registry registry) {
    super(bigg, parameters, registry);
    this.sboParameters = sboParameters;
  }

  public BiGGSBMLAnnotator(BiGGDB bigg, BiGGAnnotationParameters parameters, SBOParameters sboParameters, Registry registry, List<ProgressObserver> observers) {
    super(bigg, parameters, registry, observers);
    this.sboParameters = sboParameters;
  }
  

  /**
   * Annotates an SBMLDocument using data from the BiGG Knowledgebase. This method processes various components of the
   * SBML model such as compartments, species, reactions, and gene products by adding relevant annotations from BiGG.
   * It also handles the addition of publications and notes related to the model.
   *
   * @param doc The SBMLDocument that contains the model to be annotated.
   */
  @Override
  public void annotate(SBMLDocument doc) throws SQLException, AnnotationException {
    // TODO: these sanity checks need to be improved
    Model model = doc.getModel();
    String modelId = model.getId();
    if (!bigg.isModel(modelId)) {
      return;
    }
    // model.isSetMetaId()

    new BiGGModelAnnotator(bigg, biGGAnnotationParameters, registry, getObservers()).annotate(model);

    // Annotate various components of the model
    new BiGGPublicationsAnnotator(bigg, biGGAnnotationParameters, registry, getObservers()).annotate(model);

    new BiGGCompartmentsAnnotator(bigg, biGGAnnotationParameters, registry, getObservers()).annotate(model.getListOfCompartments());

    new BiGGSpeciesAnnotator(bigg, biGGAnnotationParameters, sboParameters, registry, getObservers()).annotate(model.getListOfSpecies());

    new BiGGReactionsAnnotator(bigg, biGGAnnotationParameters, sboParameters, registry, getObservers()).annotate(model.getListOfReactions());

    new BiGGFBCAnnotator(bigg, biGGAnnotationParameters, registry, getObservers()).annotate(model);

    new BiGGDocumentNotesProcessor(bigg, biGGAnnotationParameters).processNotes(doc);

    new AnnotationsSorter().groupAndSortAnnotations(doc);
  }

}
