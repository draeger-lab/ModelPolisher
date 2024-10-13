package de.uni_halle.informatik.biodata.mp.polishing;

import de.uni_halle.informatik.biodata.mp.parameters.PolishingParameters;
import de.uni_halle.informatik.biodata.mp.resolver.Registry;
import de.uni_halle.informatik.biodata.mp.resolver.RegistryURI;
import de.uni_halle.informatik.biodata.mp.reporting.ProgressObserver;
import org.sbml.jsbml.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.text.MessageFormat.format;


public class AnnotationPolisher extends AbstractPolisher implements IPolishAnnotations {
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
    @Override
    public void polish(Annotation annotation) {
        logger.trace(format("Polish Annotation: {0}", annotation.toString()));

        for (var term : annotation.getListOfCVTerms()) {
            Set<String> resources = new HashSet<>();
            for (String resource : term.getResources()) {
                var registryUri = registry.resolveBackwards(resource).map(RegistryURI::getURI);
                if (registryUri.isPresent()) {
                    resources.add(registryUri.get());
                } else {
                    resources.add(resource);
                }
            }

            // Remove all existing resources from the CV Term.
            for (int i = term.getResourceCount() -1; i >= 0 ; i--) {
                term.removeResource(i);
            }
            // Add the updated set of resources, sorted alphabetically, back to the CV Term.
            term.addResources(resources.stream().sorted().toArray(String[]::new));
        }
    }



}
