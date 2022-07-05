package edu.ucsd.sbrg.bigg.polishing;

import org.junit.jupiter.api.Test;
import org.sbml.jsbml.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpeciesPolishingTest {

    @Test
    public void noIdNoChanges() {
        var m = new Model(3, 2);
        var s = m.createSpecies();

        assertEquals(0, m.getCompartmentCount());
        assertEquals(false, s.isSetConstant());
        assertEquals(false, s.isSetBoundaryCondition());
        assertEquals(false, s.isSetHasOnlySubstanceUnits());
        assertEquals(1,  m.getSpeciesCount());

        var polishing = new SpeciesPolishing(s);
        var deleteTask = polishing.polish();

        assertTrue(deleteTask.isPresent());
        assertEquals(0, m.getCompartmentCount());
        assertEquals(false, s.isSetMetaId());
        assertEquals(false, s.isSetId());
        assertEquals(false, s.isSetConstant());
        assertEquals(false, s.isSetBoundaryCondition());
        assertEquals(false, s.isSetHasOnlySubstanceUnits());
        assertEquals(1,  m.getSpeciesCount());
    }

    /**
     * Default values according to the SBML spec are set and the model is aware of it.
     */
    @Test
    public void defaultsAreSet() {
        var m = new Model(3, 2);
        var s = m.createSpecies("stuff_c");

        assertEquals(0, m.getCompartmentCount());
        assertEquals(false, s.isSetConstant());
        assertEquals(false, s.isSetBoundaryCondition());
        assertEquals(false, s.isSetHasOnlySubstanceUnits());
        assertEquals(1,  m.getSpeciesCount());

        var polishing = new SpeciesPolishing(s);
        var deleteTask = polishing.polish();

        assertTrue(deleteTask.isEmpty());
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

        var polishing = new SpeciesPolishing(s);
        var deleteTask = polishing.polish();

        assertTrue(deleteTask.isEmpty());
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

        var polishing = new SpeciesPolishing(s);
        var deleteTask = polishing.polish();

        assertTrue(deleteTask.isEmpty());
        assertEquals("e", s.getCompartment());
        assertEquals("stuff", s.getId());
        assertEquals("default", m.getListOfCompartments().get(0).getName());
        assertEquals("e", m.getListOfCompartments().get(0).getId());
    }

}
