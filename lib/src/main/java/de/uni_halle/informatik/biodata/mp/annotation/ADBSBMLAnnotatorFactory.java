package de.uni_halle.informatik.biodata.mp.annotation;

import de.uni_halle.informatik.biodata.mp.db.adb.AnnotateDB;
import de.uni_halle.informatik.biodata.mp.parameters.ADBAnnotationParameters;
import org.sbml.jsbml.SBMLDocument;

public class ADBSBMLAnnotatorFactory {


    public static IAnnotateSBases<SBMLDocument> createADBAnnotator(SBMLDocument doc,
                                                                   AnnotateDB annotateDB,
                                                                   ADBAnnotationParameters adbAnnotationParameters) {
        return switch (doc.getLevel()) {
            case 3 -> switch (doc.getVersion()) {
                case 1 -> new de.uni_halle.informatik.biodata.mp.annotation.v3_1.adb.ADBSBMLAnnotator(annotateDB,
                        adbAnnotationParameters);
                default -> throw new IllegalArgumentException("Version not supported.");
            };

            default -> throw new IllegalArgumentException("Version not supported.");
        };
    }

}
