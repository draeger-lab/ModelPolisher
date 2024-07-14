package edu.ucsd.sbrg.polishing;

import edu.ucsd.sbrg.Parameters;
import edu.ucsd.sbrg.identifiersorg.IdentifiersOrg;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import org.sbml.jsbml.Annotation;
import org.sbml.jsbml.CVTerm;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class AnnotationPolisher extends AbstractPolisher<Annotation> {

    public AnnotationPolisher(Parameters parameters) {
        super(parameters);
    }

    public AnnotationPolisher(Parameters parameters, List<ProgressObserver> observers) {
        super(parameters, observers);
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
        for (CVTerm term : annotation.getListOfCVTerms()) {
            Set<String> resources = new HashSet<>();
            for (String resource : term.getResources()) {
                Optional<String> checkedResource = IdentifiersOrg.checkResourceUrl(resource);
                if (checkedResource.isEmpty()) {
                    // The resource URL could not be verified, so it is retained as is.
                    resources.add(resource);
                } else {
                    String newResource = checkedResource.get();
                    if (newResource.equals(resource)) {
                        // The resource URL is correct and requires no changes.
                        resources.add(resource);
                    } else if (newResource.contains("identifiers.org") && !resource.contains("identifiers.org")) {
                        // A new identifiers.org URL has been obtained, add both the original and new URL.
                        resources.add(resource);
                        resources.add(newResource);
                    } else {
                        // Corrections were made to the resource URL.
                        resources.add(newResource);
                    }
                }
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
