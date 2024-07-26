package edu.ucsd.sbrg.polishing;

import edu.ucsd.sbrg.parameters.PolishingParameters;
import edu.ucsd.sbrg.resolver.Registry;
import edu.ucsd.sbrg.resolver.RegistryURI;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import org.sbml.jsbml.Annotation;
import org.sbml.jsbml.CVTerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.text.MessageFormat.format;


public class AnnotationPolisher extends AbstractPolisher<Annotation> {
    private static final Logger logger = LoggerFactory.getLogger(AnnotationPolisher.class);

    public AnnotationPolisher(PolishingParameters polishingParameters, Registry registry) {
        super(polishingParameters, registry);
    }

    public AnnotationPolisher(PolishingParameters polishingParameters, Registry registry, List<ProgressObserver> observers) {
        super(polishingParameters, registry, observers);
    }

    /**
     * Processes the annotations of an SBML entity to potentially correct identifiers
     * and/or retrieve additional identifiers.org URLs.
     * This method iterates over all Controlled Vocabulary (CV) Terms in the provided Annotation object.
     * For each resource URL in a CV Term,
     * it checks and possibly corrects the URL or adds new URLs from identifiers.org.
     * It then updates the CV Term with the corrected and/or additional URLs.
     *
     * @param annotation The {@link Annotation} object associated with an SBML entity that contains CV Terms to be processed.
     */
    public void polish(Annotation annotation) {
        logger.trace(format("Polish Annotation: {0}", annotation.toString()));
        for (CVTerm term : annotation.getListOfCVTerms()) {
            Set<String> resources = new HashSet<>();
            for (String resource : term.getResources()) {
                registry.findRegistryUrlForOtherUrl(resource)
                        .map(RegistryURI::getURI)
                        .map(resources::add);

                resource = resource.replaceAll("http://identifiers.org", "https://identifiers.org");

                resources.add(resource);
            }
            // Remove all existing resources from the CV Term.
            for (int i = 0; i < term.getResourceCount(); i++) {
                term.removeResource(i);
            }
            // Add the updated set of resources, sorted alphabetically, back to the CV Term.
            term.addResources(resources.stream().sorted().toArray(String[]::new));
        }
    }



}
