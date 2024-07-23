package edu.ucsd.sbrg.annotation.adb;

import edu.ucsd.sbrg.annotation.AbstractAnnotator;
import edu.ucsd.sbrg.parameters.ADBAnnotationParameters;
import edu.ucsd.sbrg.parameters.BiGGAnnotationParameters;
import edu.ucsd.sbrg.db.adb.AnnotateDB;
import edu.ucsd.sbrg.db.bigg.BiGGId;
import org.sbml.jsbml.Annotation;
import org.sbml.jsbml.CVTerm;

import java.util.*;

public abstract class AbstractADBAnnotator<SBMLElement> extends AbstractAnnotator<SBMLElement> {

    protected final AnnotateDB adb;
    protected final ADBAnnotationParameters parameters;

    public AbstractADBAnnotator(AnnotateDB adb, ADBAnnotationParameters parameters) {
        super();
        this.adb = adb;
        this.parameters = parameters;
    }

    public abstract void annotate(SBMLElement element);

    protected void addBQB_IS_AnnotationsFromADB(Annotation annotation, String type, BiGGId biggId) {
        CVTerm cvTerm = annotation.getListOfCVTerms().stream()
                .filter(term -> term.getQualifier() == CVTerm.Qualifier.BQB_IS)
                .findFirst()
                .orElse(new CVTerm(CVTerm.Qualifier.BQB_IS));

        Set<String> annotations = adb.getAnnotations(type, biggId.toBiGGId());

        annotations.removeAll(new HashSet<>(cvTerm.getResources()));
        List<String> sortedAnnotations = new ArrayList<>(annotations);
        Collections.sort(sortedAnnotations);
        for (String a : sortedAnnotations) {
            cvTerm.addResource(a);
        }

        if (cvTerm.getResourceCount() == 0) {
            annotation.removeCVTerm(cvTerm);
        }
    }
}
