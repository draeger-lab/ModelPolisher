package edu.ucsd.sbrg.polishing;

import edu.ucsd.sbrg.Parameters;
import edu.ucsd.sbrg.resolver.identifiersorg.IdentifiersOrg;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.Model;

import static edu.ucsd.sbrg.TestUtils.initParameters;
import static org.junit.jupiter.api.Assertions.*;

public class SpeciesPolisherTest {

    private final Parameters parameters = initParameters();

    @Test
    public void speciesWithoutIdIsDeleted() {
        var m = new Model(3, 2);
        var s = m.createSpecies();

        assertEquals(0, m.getCompartmentCount());
        assertFalse(s.isSetConstant());
        assertFalse(s.isSetBoundaryCondition());
        assertFalse(s.isSetHasOnlySubstanceUnits());
        assertEquals(1,  m.getSpeciesCount());

        new SpeciesPolisher(parameters, new IdentifiersOrg()).polish(m.getListOfSpecies());

        assertEquals(0, m.getCompartmentCount());
        assertFalse(s.isSetMetaId());
        assertFalse(s.isSetId());
        assertFalse(s.isSetConstant());
        assertFalse(s.isSetBoundaryCondition());
        assertFalse(s.isSetHasOnlySubstanceUnits());
        assertEquals(0,  m.getSpeciesCount());
    }

    /**
     * Default values according to the SBML spec are set and the model is aware of it.
     */
    @Test
    public void defaultsAreSet() {
        var m = new Model(3, 2);
        var s = m.createSpecies("stuff_c");

        assertEquals(0, m.getCompartmentCount());
        assertFalse(s.isSetConstant());
        assertFalse(s.isSetBoundaryCondition());
        assertFalse(s.isSetHasOnlySubstanceUnits());
        assertEquals(1,  m.getSpeciesCount());

        new SpeciesPolisher(parameters, new IdentifiersOrg()).polish(s);

        assertEquals("c", s.getCompartment());
        assertEquals("stuff_c", s.getId());
        assertEquals("default", m.getListOfCompartments().get(0).getName());
        assertEquals("c", m.getListOfCompartments().get(0).getId());
        assertTrue(s.isSetConstant());
        assertFalse(s.getConstant());
        assertTrue(s.isSetBoundaryCondition());
        assertFalse(s.isBoundaryCondition());
        assertTrue(s.isSetHasOnlySubstanceUnits());
        assertTrue(s.hasOnlySubstanceUnits());
        assertEquals(1,  m.getSpeciesCount());
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

        new SpeciesPolisher(parameters, new IdentifiersOrg()).polish(s);

        assertEquals("c", s.getCompartment());
        assertEquals("stuff_c", s.getId());
        assertEquals("default", m.getListOfCompartments().get(0).getName());
        assertEquals("c", m.getListOfCompartments().get(0).getId());
        assertEquals(1,  m.getSpeciesCount());
    }

    /**
     * If a species has its compartment attribute set, but no compartment postfix on the ID,
     * the compartment is created in the model if it does not exist, but the
     * species ID is not changed,
     */
    @Test
    public void nonCompartmentIdIsNotSetToCompartmentAttribute() {
        var m = new Model(3, 2);
        var s = m.createSpecies("stuff");
        s.setCompartment("e");

        assertEquals(0, m.getCompartmentCount());

        new SpeciesPolisher(parameters, new IdentifiersOrg()).polish(s);

        assertEquals("e", s.getCompartment());
        assertEquals("stuff", s.getId());
        assertEquals("default", m.getListOfCompartments().get(0).getName());
        assertEquals("e", m.getListOfCompartments().get(0).getId());
    }

}
