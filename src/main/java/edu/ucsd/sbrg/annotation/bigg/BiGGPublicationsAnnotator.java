package edu.ucsd.sbrg.annotation.bigg;

import edu.ucsd.sbrg.Parameters;
import edu.ucsd.sbrg.db.bigg.BiGGDB;
import edu.ucsd.sbrg.db.bigg.Publication;
import edu.ucsd.sbrg.resolver.Registry;
import edu.ucsd.sbrg.resolver.identifiersorg.IdentifiersOrgURI;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Model;

import java.util.List;

public class BiGGPublicationsAnnotator extends AbstractBiGGAnnotator<Model> {

    public BiGGPublicationsAnnotator(BiGGDB bigg, Parameters parameters, Registry registry, List<ProgressObserver> observers) {
        super(bigg, parameters, registry, observers);

    }

    @Override
    public void annotate(Model model) {
        List<Publication> publications = bigg.getPublications(model.getId());
        statusReport("Annotating Publications (1/5)  ", model);

        String[] resources = publications.stream()
                .map(publication -> new IdentifiersOrgURI(publication.referenceType(), publication.referenceId()))
                .map(IdentifiersOrgURI::getURI)
                .toArray(String[]::new);

        model.addCVTerm(new CVTerm(CVTerm.Qualifier.BQM_IS_DESCRIBED_BY, resources));
    }

}
