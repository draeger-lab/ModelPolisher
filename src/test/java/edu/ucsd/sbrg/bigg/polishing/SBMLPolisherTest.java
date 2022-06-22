package edu.ucsd.sbrg.bigg.polishing;

import de.zbit.util.progressbar.ProgressBar;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.UnitDefinition;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SBMLPolisherTest {

    @Test
    public void modelWithNoUnitDefinitions() {
        var m = new Model();
        m.setLevel(3);
        m.setVersion(2);
        var polisher = new SBMLPolisher();
        polisher.setProgress(new ProgressBar(0));
        polisher.polish(m);
        assertEquals(3, m.getListOfUnitDefinitions().getChildCount());
        assertEquals(List.of("mmol_per_gDW_per_hr", "substance", "time"),
                m.getListOfUnitDefinitions()
                        .stream()
                        .map(UnitDefinition::getId)
                        .collect(Collectors.toList()));
        assertNotNull(m.getSubstanceUnitsInstance());
        assertNotNull(m.getTimeUnitsInstance());
        assertNotNull(m.getExtentUnitsInstance());
    }

}
