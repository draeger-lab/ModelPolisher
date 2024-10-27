package de.uni_halle.informatik.biodata.mp.annotation;

import de.uni_halle.informatik.biodata.mp.db.bigg.BiGGDB;
import de.uni_halle.informatik.biodata.mp.parameters.BiGGAnnotationParameters;
import de.uni_halle.informatik.biodata.mp.parameters.SBOParameters;
import de.uni_halle.informatik.biodata.mp.resolver.Registry;
import de.uni_halle.informatik.biodata.mp.resolver.identifiersorg.IdentifiersOrg;
import org.sbml.jsbml.SBMLDocument;

public class BiGGSBMLAnnotatorFactory {

    public static IAnnotateSBases<SBMLDocument> createBiGGAnnotator(SBMLDocument doc,
                                                                    BiGGDB biGGDB,
                                                                    BiGGAnnotationParameters biGGAnnotationParameters,
                                                                    SBOParameters sboParameters,
                                                                    Registry registry) {
        return switch (doc.getLevel()) {
            case 3 -> switch (doc.getVersion()) {
                case 1 -> new de.uni_halle.informatik.biodata.mp.annotation.v3_1.bigg.BiGGSBMLAnnotator(biGGDB,
                        biGGAnnotationParameters,
                        sboParameters,
                        registry);
                default -> throw new IllegalArgumentException("Version not supported.");
            };

            default -> throw new IllegalArgumentException("Version not supported.");
        };
    }
}
