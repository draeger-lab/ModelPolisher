package edu.ucsd.sbrg.polishing;

import edu.ucsd.sbrg.parameters.PolishingParameters;
import edu.ucsd.sbrg.parameters.SBOParameters;
import edu.ucsd.sbrg.resolver.identifiersorg.IdentifiersOrg;
import org.sbml.jsbml.SBMLDocument;

public class PolisherFactory {

        public static IPolishSBases<SBMLDocument> sbmlPolisher(SBMLDocument doc,
                                                               PolishingParameters polishingParameters,
                                                               SBOParameters sboParameters) {
            if (doc.getLevel() == 3 && doc.getVersion() == 1) {
                return new SBMLPolisher(polishingParameters, sboParameters, new IdentifiersOrg());
            }
            else {
                throw new UnsupportedOperationException(
                        "Version " + doc.getVersion() + " and Level " + doc.getLevel()
                        + " is not supported at the moment.");
            }
        }
}
