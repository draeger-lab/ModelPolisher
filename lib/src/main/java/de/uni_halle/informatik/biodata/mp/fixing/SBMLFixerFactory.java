package de.uni_halle.informatik.biodata.mp.fixing;

import de.uni_halle.informatik.biodata.mp.parameters.FixingParameters;
import org.sbml.jsbml.SBMLDocument;

public class SBMLFixerFactory {

    public static IFixSBases<SBMLDocument> createSBMLFixer(SBMLDocument doc, FixingParameters fixingParameters) {
        return switch (doc.getLevel()) {
            case 3 -> switch (doc.getVersion()) {
                case 1 -> new de.uni_halle.informatik.biodata.mp.fixing.v3_1.SBMLFixer(fixingParameters);
                default -> throw new IllegalArgumentException("Version not supported.");
            };

            default -> throw new IllegalArgumentException("Version not supported.");
        };
    }
}
