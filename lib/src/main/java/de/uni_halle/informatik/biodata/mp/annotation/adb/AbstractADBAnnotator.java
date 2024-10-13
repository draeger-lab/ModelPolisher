package de.uni_halle.informatik.biodata.mp.annotation.adb;

import de.uni_halle.informatik.biodata.mp.parameters.ADBAnnotationParameters;
import de.uni_halle.informatik.biodata.mp.db.adb.AnnotateDB;
import de.uni_halle.informatik.biodata.mp.db.bigg.BiGGId;
import org.sbml.jsbml.Annotation;
import org.sbml.jsbml.CVTerm;

import java.sql.SQLException;
import java.util.*;

public abstract class AbstractADBAnnotator {

    protected final AnnotateDB adb;
    protected final ADBAnnotationParameters parameters;

    public AbstractADBAnnotator(AnnotateDB adb, ADBAnnotationParameters parameters) {
        super();
        this.adb = adb;
        this.parameters = parameters;
    }

    protected void addBQB_IS_AnnotationsFromADB(Annotation annotation, String type, BiGGId biggId) throws SQLException {
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
