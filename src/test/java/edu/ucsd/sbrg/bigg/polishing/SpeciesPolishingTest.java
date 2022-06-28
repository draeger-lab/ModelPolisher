package edu.ucsd.sbrg.bigg.polishing;

import org.junit.jupiter.api.Test;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Species;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SpeciesPolishingTest {

    /**
     * Default values according to the SBML spec are set and the model is aware of it.
     */
    @Test
    public void defaultsAreSet() {
        var m = new Model(3, 2);

        var ls = new ListOf<Species>(3, 2);
        ls.setParent(m);
        var s = new Species(3, 2);
        s.setId("stuff_c");
        ls.add(s);

        assertEquals(0, m.getListOfCompartments().getChildCount());
        assertEquals(false, s.isSetConstant());
        assertEquals(false, s.isSetBoundaryCondition());
        assertEquals(false, s.isSetHasOnlySubstanceUnits());

        var polishing = new SpeciesPolishing(s);
        polishing.polish();

        assertEquals("c", s.getCompartment());
        assertEquals("stuff_c", s.getId());
        assertEquals("default", m.getListOfCompartments().get(0).getName());
        assertEquals("c", m.getListOfCompartments().get(0).getId());
        assertEquals(true, s.isSetConstant());
        assertEquals(false, s.getConstant());
        assertEquals(true, s.isSetBoundaryCondition());
        assertEquals(false, s.isBoundaryCondition());
        assertEquals(true, s.isSetHasOnlySubstanceUnits());
        assertEquals(true, s.hasOnlySubstanceUnits());
    }

    /**
     * If the compartment suffix on the species ID conflicts with the compartment attribute,
     * the attribute is changed to comply with the ID.
     * Also, if the compartment does not exist in the model, it is created.
     */
    @Test
    public void compartmentIsSetFromId() {
        var m = new Model(3, 2);

        var ls = new ListOf<Species>(3, 2);
        ls.setParent(m);
        var s = new Species(3, 2);
        s.setCompartment("e");
        s.setId("stuff_c");
        ls.add(s);

        assertEquals(0, m.getListOfCompartments().getChildCount());

        var polishing = new SpeciesPolishing(s);
        polishing.polish();

        assertEquals("c", s.getCompartment());
        assertEquals("stuff_c", s.getId());
        assertEquals("default", m.getListOfCompartments().get(0).getName());
        assertEquals("c", m.getListOfCompartments().get(0).getId());
    }

    /**
     * If a species has its compartment attribute set, but no compartment postfix on the ID,
     * the compartment is created in the model if it does not exist, but the
     * species ID is not changed,
     */
    @Test
    public void nonCompartmentIdIsNotSetToCompartmentAttribute() {
        var m = new Model(3, 2);

        var ls = new ListOf<Species>(3, 2);
        ls.setParent(m);
        var s = new Species(3, 2);
        s.setCompartment("e");
        s.setId("stuff");
        ls.add(s);

        assertEquals(0, m.getListOfCompartments().getChildCount());

        var polishing = new SpeciesPolishing(s);
        polishing.polish();

        assertEquals("e", s.getCompartment());
        assertEquals("stuff", s.getId());
        assertEquals("default", m.getListOfCompartments().get(0).getName());
        assertEquals("e", m.getListOfCompartments().get(0).getId());
    }

}
