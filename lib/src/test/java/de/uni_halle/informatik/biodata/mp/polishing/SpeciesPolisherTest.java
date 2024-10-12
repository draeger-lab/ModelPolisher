package de.uni_halle.informatik.biodata.mp.polishing;

import de.uni_halle.informatik.biodata.mp.parameters.PolishingParameters;
import de.uni_halle.informatik.biodata.mp.parameters.SBOParameters;
import de.uni_halle.informatik.biodata.mp.resolver.identifiersorg.IdentifiersOrg;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.Model;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class SpeciesPolisherTest {

    private final SBOParameters sboParameters = new SBOParameters();
    private final PolishingParameters parameters = new PolishingParameters();

    /**
     * Default values according to the SBML spec are set and the model is aware of it.
     */
    @Test
    public void defaultsAreSet() {
        var m = new Model(3, 2);
        var s = m.createSpecies("stuff_c");

        assertEquals(0, m.getCompartmentCount());
        assertEquals(1,  m.getSpeciesCount());

        new SpeciesPolisher(parameters, sboParameters, new IdentifiersOrg(), new ArrayList<>()).polish(s);

        assertEquals("c", s.getCompartment());
        assertEquals("stuff_c", s.getId());
        assertEquals("", m.getListOfCompartments().get(0).getName());
        assertEquals("c", m.getListOfCompartments().get(0).getId());
        assertEquals(1,  m.getSpeciesCount());
        assertEquals(247, s.getSBOTerm());
    }

    /**
     * If the compartment suffix on the species ID conflicts with the compartment attribute,
     * the attribute is changed to comply with the ID.
     * Also, if the compartment does not exist in the model, it is created.
     */
    @Test
    public void compartmentIsSetFromId() {
        var m = new Model(3, 2);
        var s = m.createSpecies("stuff_c");
        s.setCompartment("e");

        assertEquals(0, m.getCompartmentCount());
        assertEquals(1,  m.getSpeciesCount());

        new SpeciesPolisher(parameters, sboParameters, new IdentifiersOrg(), new ArrayList<>()).polish(s);

        assertEquals("c", s.getCompartment());
        assertEquals("stuff_c", s.getId());
        assertEquals("", m.getListOfCompartments().get(0).getName());
        assertEquals("c", m.getListOfCompartments().get(0).getId());
        assertEquals(1,  m.getSpeciesCount());
        assertEquals(247, s.getSBOTerm());
    }
}
