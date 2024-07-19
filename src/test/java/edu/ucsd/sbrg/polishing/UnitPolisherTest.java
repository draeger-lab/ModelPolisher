package edu.ucsd.sbrg.polishing;

import edu.ucsd.sbrg.Parameters;
import edu.ucsd.sbrg.resolver.identifiersorg.IdentifiersOrg;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Unit;
import org.sbml.jsbml.UnitDefinition;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static edu.ucsd.sbrg.TestUtils.assertCVTermsArePresent;
import static edu.ucsd.sbrg.TestUtils.initParameters;
import static org.junit.jupiter.api.Assertions.*;

public class UnitPolisherTest {

    private final Parameters parameters = initParameters();

    public static Set<String> setOfUnitDefinitions(Model m) {
        return m.getListOfUnitDefinitions()
                .stream()
                .map(UnitDefinition::getId)
                .collect(Collectors.toSet());
    }

    public static Set<Unit> setOfUnits(UnitDefinition ud) {
        return new HashSet<>(ud.getListOfUnits());
    }

    public static Unit newUnit(int multiplier, int scale, Unit.Kind kind, int exponent) {
        return new Unit(multiplier, scale, kind, exponent, 3, 2);
    }

    public static void assertUnits(Set<Unit> expected, Set<Unit> us) {
        assertEquals(expected.size(), us.size(), "Unit sets are of unequal size."
                + System.getProperty("line.separator")
                + "Expected: " + expected
                + System.getProperty("line.separator")
                + "Found: " + us);
        for (var u : us) {
            assertNotNull(u);
        }
        for (var e : expected) {
            boolean found = false;
            for (var u : us) {
                if (Unit.areEquivalent(u, e)) {
                    found = true;
                    break;
                }
            }
            if (!found)
                fail(e.toString() + " not found."
                        + System.getProperty("line.separator")
                        + "Instead: " + us);
        }
    }

    private Unit getUnitByKind(UnitDefinition ud, Unit.Kind kind) {
        return ud.getListOfUnits().stream()
                .filter(unit -> unit.getKind().equals(kind))
                .findFirst().get();
    }

    final static Unit mmol = newUnit(1, -3, Unit.Kind.MOLE, 1);
    final static Unit perGram = newUnit(1, 0, Unit.Kind.GRAM, -1);
    final static Unit hour = newUnit(3600, 0, Unit.Kind.SECOND, 1);
    final static Unit perHour = newUnit(3600, 0, Unit.Kind.SECOND, -1);
    @Test
    public void modelWithNoUnitDefinitions() {
        var m = new Model(3, 2);

        new UnitPolisher(parameters, new IdentifiersOrg()).polish(m);

        assertEquals(3, m.getListOfUnitDefinitions().getChildCount());
        assertEquals(Set.of("mmol_per_gDW_per_hr", "substance", "time"),
                setOfUnitDefinitions(m));
        var substance = m.getSubstanceUnitsInstance();
        assertNotNull(substance);
        assertUnits(Set.of(mmol, perGram), setOfUnits(substance));

        assertCVTermsArePresent(substance,
                CVTerm.Type.BIOLOGICAL_QUALIFIER,
                CVTerm.Qualifier.BQB_IS,
                UnitPolisher.CV_TERM_IS_SUBSTANCE_UNIT.getResources(),
                "Expected annotations are not present.");
        assertCVTermsArePresent(getUnitByKind(substance, Unit.Kind.MOLE),
                CVTerm.Type.BIOLOGICAL_QUALIFIER,
                CVTerm.Qualifier.BQB_IS,
                UnitPolisher.CV_TERM_IS_UO_MMOL.getResources(),
                "Expected annotations are not present.");

        var time = m.getTimeUnitsInstance();
        assertNotNull(time);
        assertUnits(Set.of(hour), setOfUnits(time));

        assertCVTermsArePresent(time,
                CVTerm.Type.BIOLOGICAL_QUALIFIER,
                CVTerm.Qualifier.BQB_IS,
                UnitPolisher.CV_TERM_IS_TIME_UNIT.getResources(),
                "Expected annotations are not present.");
        assertCVTermsArePresent(getUnitByKind(time, Unit.Kind.SECOND),
                CVTerm.Type.BIOLOGICAL_QUALIFIER,
                CVTerm.Qualifier.BQB_IS,
                UnitPolisher.CV_TERM_IS_UO_HOUR.getResources(),
                "Expected annotations are not present.");

        var extent = m.getExtentUnitsInstance();
        assertNotNull(extent);
        assertUnits(Set.of(mmol, perGram), setOfUnits(extent));
    }


    /**
     * If a growth unit exists that has units defined, it is not overriden.
     * However, if it is not a viable growth unit, it is renamed '__preexisting'
     * and a proper growth unit is created.
     * Also, substance, time and extent units are created if non existed.
     */
    @Test
    public void existingGrowthDefinitionIsPreservedButSuperseded() {
        var m = new Model(3, 2);

        var growth = new UnitDefinition(3, 2);
        growth.setId("mmol_per_gDW_per_hr");
        m.addUnitDefinition(growth);
        var someUnit = new Unit(3, 2);
        someUnit.setId("some");
        growth.addUnit(someUnit);

        new UnitPolisher(parameters, new IdentifiersOrg()).polish(m);

        assertEquals(Set.of("mmol_per_gDW_per_hr", "mmol_per_gDW_per_hr__preexisting",
                "substance", "time"),
                setOfUnitDefinitions(m));

        var substance = m.getSubstanceUnitsInstance();
        assertNotNull(substance);
        assertUnits(Set.of(mmol, perGram), setOfUnits(substance));

        var time = m.getTimeUnitsInstance();
        assertNotNull(time);
        assertUnits(Set.of(hour), setOfUnits(time));

        var extent = m.getExtentUnitsInstance();
        assertUnits(Set.of(mmol, perGram), setOfUnits(extent));

        assertUnits(Set.of(mmol, perGram, perHour),
                setOfUnits(m.getUnitDefinition("mmol_per_gDW_per_hr")));
    }

    /**
     * If substance and time units already exists, they remain unchanged,
     * but are annotated as far as possible.
     */
    @Test
    public void existingSubstanceAndTimeAreUnchanged() {
        var m = new Model(3, 2);

        var substance = new UnitDefinition(3, 2);
        substance.setId("test_substance");
        m.addUnitDefinition(substance);
        m.setSubstanceUnits("test_substance");

        var time = new UnitDefinition(3, 2);
        time.setId("test_time");
        m.addUnitDefinition(time);
        m.setTimeUnits("test_time");

        new UnitPolisher(parameters, new IdentifiersOrg()).polish(m);

        assertEquals(Set.of("mmol_per_gDW_per_hr", "test_time", "test_substance"),
                setOfUnitDefinitions(m));
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

        assertCVTermsArePresent(substance,
                CVTerm.Type.BIOLOGICAL_QUALIFIER,
                CVTerm.Qualifier.BQB_IS,
                UnitPolisher.CV_TERM_IS_SUBSTANCE_UNIT.getResources(),
                "Expected annotations are not present.");
        assertCVTermsArePresent(time,
                CVTerm.Type.BIOLOGICAL_QUALIFIER,
                CVTerm.Qualifier.BQB_IS,
                UnitPolisher.CV_TERM_IS_TIME_UNIT.getResources(),
                "Expected annotations are not present.");
    }

}
