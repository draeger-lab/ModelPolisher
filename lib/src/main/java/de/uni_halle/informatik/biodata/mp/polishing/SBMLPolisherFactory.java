package de.uni_halle.informatik.biodata.mp.polishing;

import de.uni_halle.informatik.biodata.mp.parameters.PolishingParameters;
import de.uni_halle.informatik.biodata.mp.parameters.SBOParameters;
import de.uni_halle.informatik.biodata.mp.polishing.v3_1.SBMLPolisher;
import de.uni_halle.informatik.biodata.mp.resolver.Registry;
import de.uni_halle.informatik.biodata.mp.resolver.identifiersorg.IdentifiersOrg;
import org.sbml.jsbml.SBMLDocument;

public class SBMLPolisherFactory {

    public static IPolishSBases<SBMLDocument> createSBMLPolisher(SBMLDocument doc,
                                                                 PolishingParameters polishingParameters,
                                                                 SBOParameters sboParameters,
                                                                 Registry registry) {
        return switch (doc.getLevel()) {
            case 3 -> switch (doc.getVersion()) {
                case 1 -> new de.uni_halle.informatik.biodata.mp.polishing.v3_1.SBMLPolisher(polishingParameters,
                        sboParameters,
                        registry);
                default -> throw new IllegalArgumentException("Version not supported.");
            };

            default -> throw new IllegalArgumentException("Version not supported.");
        };
    }
}
