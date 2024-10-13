package de.uni_halle.informatik.biodata.mp.annotation.bigg;

import java.sql.SQLException;
import java.util.List;

import de.uni_halle.informatik.biodata.mp.annotation.AnnotationException;
import de.uni_halle.informatik.biodata.mp.annotation.IAnnotateSBases;
import de.uni_halle.informatik.biodata.mp.parameters.BiGGAnnotationParameters;
import de.uni_halle.informatik.biodata.mp.parameters.SBOParameters;
import de.uni_halle.informatik.biodata.mp.annotation.AnnotationsSorter;
import de.uni_halle.informatik.biodata.mp.annotation.bigg.ext.fbc.BiGGFBCAnnotator;
import de.uni_halle.informatik.biodata.mp.reporting.ProgressObserver;
import de.uni_halle.informatik.biodata.mp.resolver.Registry;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;

import de.uni_halle.informatik.biodata.mp.db.bigg.BiGGDB;


/**
 * This class is responsible for annotating SBML models using data from the BiGG database.
 * It handles the addition of annotations related to compartments, species, reactions, and gene products.
 * 
 * @author Thomas Zajac
 *         This code runs only, if ANNOTATE_WITH_BIGG is true
 */
public class BiGGSBMLAnnotator extends AbstractBiGGAnnotator implements IAnnotateSBases<SBMLDocument> {

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
    Model model = doc.getModel();

    new BiGGModelAnnotator(bigg, biGGAnnotationParameters, registry, getObservers()).annotate(model);

    // Annotate various components of the model
    new BiGGPublicationsAnnotator(bigg, biGGAnnotationParameters, registry, getObservers()).annotate(model);

    new BiGGCompartmentsAnnotator(bigg, biGGAnnotationParameters, registry, getObservers()).annotate(model.getListOfCompartments());

    new BiGGSpeciesAnnotator(bigg, biGGAnnotationParameters, sboParameters, registry, getObservers()).annotate(model.getListOfSpecies());

    new BiGGReactionsAnnotator(bigg, biGGAnnotationParameters, sboParameters, registry).annotate(model.getListOfReactions());

    new BiGGFBCAnnotator(bigg, biGGAnnotationParameters, registry, getObservers()).annotate(model);

    new BiGGDocumentNotesProcessor(bigg, biGGAnnotationParameters).processNotes(doc);

    new AnnotationsSorter().groupAndSortAnnotations(doc);
  }

}
