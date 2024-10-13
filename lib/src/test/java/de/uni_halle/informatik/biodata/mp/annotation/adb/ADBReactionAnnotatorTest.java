package de.uni_halle.informatik.biodata.mp.annotation.adb;

import de.uni_halle.informatik.biodata.mp.parameters.ADBAnnotationParameters;
import de.uni_halle.informatik.biodata.mp.parameters.SBOParameters;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCReactionPlugin;
import org.sbml.jsbml.ext.fbc.FBCSpeciesPlugin;

import java.sql.SQLException;

import static de.uni_halle.informatik.biodata.mp.TestUtils.assertCVTermIsPresent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ADBReactionAnnotatorTest extends ADBDBContainerTest{

    // http://identifiers.org/metanetx.reaction/MNXR142736

    private final ADBAnnotationParameters adbAnnotationParameters = new ADBAnnotationParameters();
    private final SBOParameters sboParameters = new SBOParameters();


    @Test
    public void basicAnnotationTest() throws SQLException {
        var m = new Model("iECSP_1301", 3, 1);
        var r = m.createReaction("R_DM_5drib_c");
        r.setMetaId("R_DM_5drib_c");
        r.setSBOTerm(628);
        r.setCompartment("c");
        r.setFast(false);
        r.setReversible(false);
        r.addCVTerm(new CVTerm(
                CVTerm.Type.BIOLOGICAL_QUALIFIER,
                CVTerm.Qualifier.BQB_IS,
                "https://identifiers.org/metanetx.reaction/MNXR142736"));
        var sFbcPlugin = (FBCReactionPlugin) r.getPlugin(FBCConstants.shortLabel);

        new ADBReactionsAnnotator(adb, adbAnnotationParameters).annotate(r);

        assertEquals(1, r.getCVTerm(0).getNumResources());
        }

}
