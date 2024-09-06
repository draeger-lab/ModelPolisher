package de.uni_halle.informatik.biodata.mp.annotation.bigg;

import de.uni_halle.informatik.biodata.mp.annotation.IAnnotateSBases;
import de.uni_halle.informatik.biodata.mp.db.bigg.BiGGDB;
import de.uni_halle.informatik.biodata.mp.db.bigg.Publication;
import de.uni_halle.informatik.biodata.mp.parameters.BiGGAnnotationParameters;
import de.uni_halle.informatik.biodata.mp.resolver.Registry;
import de.uni_halle.informatik.biodata.mp.resolver.identifiersorg.IdentifiersOrgURI;
import de.uni_halle.informatik.biodata.mp.reporting.ProgressObserver;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Model;

import java.sql.SQLException;
import java.util.List;

public class BiGGPublicationsAnnotator extends AbstractBiGGAnnotator implements IAnnotateSBases<Model> {

    public BiGGPublicationsAnnotator(BiGGDB bigg, BiGGAnnotationParameters parameters, Registry registry, List<ProgressObserver> observers) {
        super(bigg, parameters, registry, observers);

    }

    @Override
    public void annotate(Model model) throws SQLException {
        List<Publication> publications = bigg.getPublications(model.getId());
        statusReport("Annotating Publications (1/5)  ", model);

        String[] resources = publications.stream()
                .map(publication -> new IdentifiersOrgURI(publication.referenceType(), publication.referenceId()))
                .map(IdentifiersOrgURI::getURI)
                .toArray(String[]::new);

        model.addCVTerm(new CVTerm(CVTerm.Qualifier.BQM_IS_DESCRIBED_BY, resources));
    }

}
