package edu.ucsd.sbrg.bigg.polishing;

import de.zbit.util.progressbar.ProgressBar;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Unit;
import org.sbml.jsbml.UnitDefinition;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class UnitPolishingTest {

    @Test
    public void modelWithNoUnitDefinitions() {
        var m = new Model(3, 2);
        var polisher = new UnitPolishing(m, new ProgressBar(0));
        polisher.polishListOfUnitDefinitions();

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


    /**
     * If a growth unit exists that has units defined, the default units are not created.
     * Also, substance units and extent units are created if non existed, regardless.
     */
    @Test
    public void existingGrowthDefinitionIsUnchanged() {
        var m = new Model(3, 2);
        var uds = new ListOf<UnitDefinition>(3, 2);
        m.setListOfUnitDefinitions(uds);
        UnitDefinition growth = new UnitDefinition(3, 2);
        growth.setId("mmol_per_gDW_per_hr");
        uds.add(growth);
        var someUnit = new Unit(3, 2);
        someUnit.setId("some");
        growth.addUnit(someUnit);
        var polisher = new UnitPolishing(m, new ProgressBar(0));
        polisher.polishListOfUnitDefinitions();

        assertEquals(Set.of("mmol_per_gDW_per_hr", "substance"),
                m.getListOfUnitDefinitions()
                        .stream()
                        .map(UnitDefinition::getId)
                        .collect(Collectors.toSet()));
        assertNotNull(m.getSubstanceUnitsInstance());
        assertNull(m.getTimeUnitsInstance());
        assertNotNull(m.getExtentUnitsInstance());
        assertEquals(Set.of("some"),
                m.getUnitDefinition("mmol_per_gDW_per_hr")
                        .getListOfUnits()
                        .stream().map(Unit::getId)
                        .collect(Collectors.toSet()));
    }

    /**
     * If substance and time units already exists, they remains unchanged.
     */
    @Test
    public void existingSubstanceAndTimeAreUnchanged() {
        var m = new Model(3, 2);
        var uds = new ListOf<UnitDefinition>(3, 2);
        m.setListOfUnitDefinitions(uds);
        var substance = new UnitDefinition(3, 2);
        substance.setId("test_substance");
        uds.add(substance);
        m.setSubstanceUnits("test_substance");
        var time = new UnitDefinition(3, 2);
        time.setId("test_time");
        uds.add(time);
        m.setTimeUnits("test_time");
        var polisher = new UnitPolishing(m, new ProgressBar(0));
        polisher.polishListOfUnitDefinitions();

        assertEquals(Set.of("mmol_per_gDW_per_hr", "test_time", "test_substance"),
                m.getListOfUnitDefinitions()
                        .stream()
                        .map(UnitDefinition::getId)
                        .collect(Collectors.toSet()));
        assertNotNull(m.getSubstanceUnitsInstance());
        assertNotNull(m.getTimeUnitsInstance());
        assertNotNull(m.getExtentUnitsInstance());
        assertEquals(m.getExtentUnitsInstance(), m.getSubstanceUnitsInstance());
        assertEquals("test_substance",
                m.getSubstanceUnitsInstance().getId());
        assertEquals(Set.of(),
                new HashSet<>(m.getSubstanceUnitsInstance()
                        .getListOfUnits()));
        assertEquals("test_time",
                m.getTimeUnitsInstance().getId());
        assertEquals(Set.of(),
                new HashSet<>(m.getTimeUnitsInstance()
                        .getListOfUnits()));
    }

}
