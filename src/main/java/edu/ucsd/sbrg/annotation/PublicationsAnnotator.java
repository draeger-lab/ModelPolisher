package edu.ucsd.sbrg.annotation;

import edu.ucsd.sbrg.Parameters;
import edu.ucsd.sbrg.db.bigg.Publication;
import edu.ucsd.sbrg.reporting.ProgressUpdate;
import edu.ucsd.sbrg.reporting.ReportType;
import edu.ucsd.sbrg.resolver.Registry;
import edu.ucsd.sbrg.resolver.identifiersorg.IdentifiersOrgURI;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Model;

import java.util.List;

public class PublicationsAnnotator extends AbstractAnnotator<Publication>{

    private final Model model;

    public PublicationsAnnotator(Model model, Parameters parameters, Registry registry, List<ProgressObserver> observers) {
        super(parameters, registry, observers);
        this.model = model;
    }

    @Override
    public void annotate(Publication publication) {
        model.addCVTerm(
                new CVTerm(
                        CVTerm.Qualifier.BQM_IS_DESCRIBED_BY,
                        new IdentifiersOrgURI(publication.referenceId(), publication.referenceType()).getURI()));
    }


    /**
     * This method annotates a given {@link Model} with publication references retrieved from the BiGG database.
     * It is specifically designed to work with models that are part of the BiGG database. The method first checks
     * if the model exists in the BiGG database. If it does, it retrieves a list of publications associated with
     * the model's ID. Each publication is then converted into a URI and added to the model as a {@link CVTerm}
     * with the qualifier {@link CVTerm.Qualifier#BQM_IS_DESCRIBED_BY}.
     */
    public void annotatePublications(List<Publication> publications) {
        statusReport("Annotating Publications (1/5)  ", model);

        String[] resources = publications.stream()
                .map(publication -> new IdentifiersOrgURI(publication.referenceType(), publication.referenceId()))
                .map(IdentifiersOrgURI::getURI)
                .toArray(String[]::new);

        model.addCVTerm(new CVTerm(CVTerm.Qualifier.BQM_IS_DESCRIBED_BY, resources));
    }
}
