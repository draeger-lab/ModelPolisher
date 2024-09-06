package de.uni_halle.informatik.biodata.mp.fixing.fbc;

import de.uni_halle.informatik.biodata.mp.fixing.ext.fbc.FBCSpeciesFixer;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.JSBML;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCSpeciesPlugin;

import javax.xml.stream.XMLStreamException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FBCSpeciesFixerTest {

    @Test
    public void fixChemicalFormula() throws XMLStreamException {
        var m = new Model(3, 1);
        var s = m.createSpecies("stuff_c");
        var sFbcPlugin = (FBCSpeciesPlugin) s.getPlugin(FBCConstants.shortLabel);

        sFbcPlugin.putUserObject(JSBML.ALLOW_INVALID_SBML, true);
        sFbcPlugin.setChemicalFormula("C2970H5292N202O1896P4charge297");

        new FBCSpeciesFixer().fix(s, 0);

        assertEquals("C2970H5292N202O1896P4", sFbcPlugin.getChemicalFormula());
//        assertEquals("<notes><p>Charge string extracted from chemical formula: charge297</p></notes>",
//                s.get Notes().getChild(0).toString());
    }

}
