package de.uni_halle.informatik.biodata.mp.fixing.fbc;

import de.uni_halle.informatik.biodata.mp.fixing.ext.fbc.FBCSpeciesFixer;
import de.uni_halle.informatik.biodata.mp.io.ModelReader;
import de.uni_halle.informatik.biodata.mp.io.ModelReaderException;
import de.uni_halle.informatik.biodata.mp.parameters.SBOParameters;
import de.uni_halle.informatik.biodata.mp.polishing.ReactionsPolisherTest;
import de.uni_halle.informatik.biodata.mp.resolver.identifiersorg.IdentifiersOrg;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.JSBML;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCSpeciesPlugin;

import javax.xml.stream.XMLStreamException;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FBCSpeciesFixerTest {

    private final SBOParameters sboParameters = new SBOParameters();

    private SBMLDocument model1507180049() throws ModelReaderException {
        return new ModelReader(sboParameters, new IdentifiersOrg()).read(
                new File(ReactionsPolisherTest.class.getClassLoader().getResource("de/uni_halle/informatik/biodata/mp/models/missing-formula.xml").getFile()));
    }

    @Test
    public void fixChemicalFormula() throws XMLStreamException {
        var m = new Model(3, 1);
        var s = m.createSpecies("stuff_c");
        var sFbcPlugin = (FBCSpeciesPlugin) s.getPlugin(FBCConstants.shortLabel);

        sFbcPlugin.putUserObject(JSBML.ALLOW_INVALID_SBML, true);
        sFbcPlugin.setChemicalFormula("C2970H5292N202O1896P4charge297");
        sFbcPlugin.putUserObject(JSBML.ALLOW_INVALID_SBML, false);

        new FBCSpeciesFixer().fix(s, 0);

        assertEquals("C2970H5292N202O1896P4", sFbcPlugin.getChemicalFormula());
//        assertEquals("<notes><p>Charge string extracted from chemical formula: charge297</p></notes>",
//                s.get Notes().getChild(0).toString());
    }

    @Test
    public void invalidChemicalFormulaIsRetained() throws ModelReaderException, XMLStreamException, IOException {
        var m = model1507180049();
        var s = m.getModel().getSpecies("some");
        var sFbcPlugin = (FBCSpeciesPlugin) s.getPlugin(FBCConstants.shortLabel);

        new FBCSpeciesFixer().fix(s, 0);

        assertEquals("C21H25N7O14P2*", sFbcPlugin.getChemicalFormula());
    }

}
