package edu.ucsd.sbrg.annotation;

import edu.ucsd.sbrg.Parameters;
import edu.ucsd.sbrg.db.bigg.Publication;
import edu.ucsd.sbrg.identifiersorg.IdentifiersOrg;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Model;

import java.util.List;

public class PublicationsAnnotator extends AbstractAnnotator<Publication>{

    private final Model model;

    public PublicationsAnnotator(Model model, Parameters parameters, List<ProgressObserver> observers) {
        super(parameters, observers);
        this.model = model;
    }

    @Override
    public void annotate(Publication publication) {
        model.addCVTerm(
                new CVTerm(CVTerm.Qualifier.BQM_IS_DESCRIBED_BY,
                        IdentifiersOrg.createURI(publication.referenceId(), publication.referenceType())));
    }


    /**
     * This method annotates a given {@link Model} with publication references retrieved from the BiGG database.
     * It is specifically designed to work with models that are part of the BiGG database. The method first checks
     * if the model exists in the BiGG database. If it does, it retrieves a list of publications associated with
     * the model's ID. Each publication is then converted into a URI and added to the model as a {@link CVTerm}
     * with the qualifier {@link CVTerm.Qualifier#BQM_IS_DESCRIBED_BY}.
     */
    public void annotatePublications(List<Publication> publications) {
        updateProgressObservers("Annotating Publications (1/5)  ", model);

        String[] resources = publications.stream()
                .map(publication -> IdentifiersOrg.createURI(publication.referenceType(), publication.referenceId()))
                .toArray(String[]::new);

        model.addCVTerm(new CVTerm(CVTerm.Qualifier.BQM_IS_DESCRIBED_BY, resources));
    }
}
