package edu.ucsd.sbrg.bigg.polishing;

import de.zbit.util.ResourceManager;
import de.zbit.util.progressbar.AbstractProgressBar;
import edu.ucsd.sbrg.miriam.Registry;
import org.sbml.jsbml.*;
import org.sbml.jsbml.util.ModelBuilder;

import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class UnitPolishing {

    private static final transient Logger logger = Logger.getLogger(UnitPolishing.class.getName());

    private static final transient ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");

    public static final CVTerm CV_TERM_DESCRIBED_BY_PUBMED_GROWTH_UNIT = new CVTerm(CVTerm.Qualifier.BQB_IS_DESCRIBED_BY, Registry.createURI("pubmed", 7986045));
    public static final String GROWTH_UNIT_ID = "mmol_per_gDW_per_hr";
    public static final String GROWTH_UNIT_NAME = "Millimoles per gram (dry weight) per hour";
    public static final CVTerm CV_TERM_IS_SUBSTANCE_UNIT = new CVTerm(CVTerm.Qualifier.BQB_IS, Registry.createURI("unit", "UO:0000006"));
    public static final CVTerm CV_TERM_IS_TIME_UNIT = new CVTerm(CVTerm.Qualifier.BQB_IS, Registry.createURI("unit", "UO:0000003"));
    public static final CVTerm CV_TERM_IS_VOLUME_UNIT = new CVTerm(CVTerm.Qualifier.BQB_IS, Registry.createURI("unit", "UO:0000095"));
    public static final CVTerm CV_TERM_IS_UO_SECOND = new CVTerm(CVTerm.Qualifier.BQB_IS, Registry.createURI("unit", "UO:0000010"));
    public static final CVTerm CV_TERM_IS_UO_HOUR = new CVTerm(CVTerm.Qualifier.BQB_IS, Registry.createURI("unit", "UO:0000032"));
    public static final CVTerm CV_TERM_IS_UO_MMOL = new CVTerm(CVTerm.Qualifier.BQB_IS, Registry.createURI("unit", "UO:0000040"));
    public static final CVTerm CV_TERM_IS_UO_GRAM = new CVTerm(CVTerm.Qualifier.BQB_IS, Registry.createURI("unit", "UO:0000021"));
    public static final CVTerm CV_TERM_IS_VERSION_OF_UO_SECOND = new CVTerm(CVTerm.Qualifier.BQB_IS_VERSION_OF, Registry.createURI("unit", "UO:0000010"));
    public static final CVTerm CV_TERM_IS_VERSION_OF_UO_MOLE = new CVTerm(CVTerm.Qualifier.BQB_IS_VERSION_OF, Registry.createURI("unit", "UO:0000013"));

    AbstractProgressBar progress;
    Model model;

    public UnitPolishing(Model model, AbstractProgressBar progress) {
        this.model = model;
        this.progress = progress;
    }

    /**
     * Check that all basic {@link UnitDefinition}s and {@link Unit}s exist and creates them, if not
     */
    public void polishListOfUnitDefinitions() {
        progress.DisplayBar("Polishing Unit Definitions (2/9)  "); // "Processing unit definitions");
        int udCount = model.getUnitDefinitionCount();
        var unitDefinitions = model.getListOfUnitDefinitions();

        var growth = createGrowthUnitDefinition();

        setModelUnits(growth, unitDefinitions);

        UnitDefinition substanceUnits = model.getSubstanceUnitsInstance();
        if (!model.isSetExtentUnits())
            model.setExtentUnits(substanceUnits.getId());

        if (!model.isSetSubstanceUnits())
            model.setSubstanceUnits(substanceUnits.getId());

        while (progress.getCallNumber() < udCount) {
            progress.DisplayBar("Polishing Unit Definitions (2/9)  ");
        }
    }

    /**
     * Using a growth unit as the template for equivalence, find a potentially already existing
     * growth unit.
     */
    private Optional<UnitDefinition> findGrowthUnit(ListOf<UnitDefinition> uds, UnitDefinition growth) {
        return uds.stream().filter(u -> UnitDefinition.areEquivalent(u, growth)).findFirst();
    }

    /**
     * This unit will be created if no other growth unit can be extracted from the model.
     */
    private UnitDefinition defaultGrowthUnitDefinition() {
        var growth = new UnitDefinition(model.getLevel(), model.getVersion());
        growth.setId(GROWTH_UNIT_ID);
        growth.setName(GROWTH_UNIT_NAME);
        ModelBuilder.buildUnit(growth, 1d, -3, Unit.Kind.MOLE, 1d);
        ModelBuilder.buildUnit(growth, 1d, 0, Unit.Kind.GRAM, -1d);
        ModelBuilder.buildUnit(growth, 3600d, 0, Unit.Kind.SECOND, -1d);
        return growth;
    }

    private UnitDefinition createGrowthUnitDefinition() {
        var growth = defaultGrowthUnitDefinition();
        var otherGrowth = findGrowthUnit(model.getListOfUnitDefinitions(), growth).orElse(growth);
        if (!growth.isSetMetaId())
            growth.setMetaId(growth.getId());
        annotateGrowthUnitDefinition(growth);
        if (otherGrowth.equals(growth) && null != model.getUnitDefinition(GROWTH_UNIT_ID)) {
            model.getUnitDefinition(GROWTH_UNIT_ID).setId(GROWTH_UNIT_ID + "__preexisting");
        }
        model.addUnitDefinition(growth);
        return growth;
    }

    private CVTerm genericUnitAnnotation(Unit u) {
        return new CVTerm(CVTerm.Qualifier.BQB_IS_VERSION_OF,
                u.getKind().getUnitOntologyIdentifier());
    }

    private void annotateGrowthUnitDefinition(UnitDefinition growth) {
        growth.addCVTerm(CV_TERM_DESCRIBED_BY_PUBMED_GROWTH_UNIT);
        getUnitByKind(growth, Unit.Kind.MOLE).ifPresent(
                u -> {
                    switch (u.getScale()) {
                        case -3: u.addCVTerm(CV_TERM_IS_UO_MMOL); break;
                        default:
                            u.addCVTerm(this.genericUnitAnnotation(u));
                    }
                }
        );
        getUnitByKind(growth, Unit.Kind.GRAM).ifPresent(this::genericUnitAnnotation);
        getUnitByKind(growth, Unit.Kind.SECOND).ifPresent(
                u -> {
                    switch (Double.valueOf(u.getMultiplier()).intValue()) {
                        case 1: u.addCVTerm(CV_TERM_IS_UO_SECOND); break;
                        case 3600: u.addCVTerm(CV_TERM_IS_UO_HOUR); break;
                        default:
                            u.addCVTerm(this.genericUnitAnnotation(u));
                    }
                });
    }

    /**
     * Sets substance, volume and time units for model from the models unit definitions,
     * or the growth unit,
     * if not set.
     */
    private void setModelUnits(UnitDefinition growth, ListOf<UnitDefinition> unitDefinitions) {
        var substanceUnits = model.getSubstanceUnitsInstance();
        if (substanceUnits == null)
            if (unitDefinitions.get(UnitDefinition.SUBSTANCE) != null)
                model.setSubstanceUnits(UnitDefinition.SUBSTANCE);
            else
                model.setSubstanceUnits(createSubstanceUnit(growth));
        model.getSubstanceUnitsInstance().addCVTerm(CV_TERM_IS_SUBSTANCE_UNIT);
        var timeUnits = model.getTimeUnitsInstance();
        if (timeUnits == null)
            if (unitDefinitions.get(UnitDefinition.TIME) != null)
                model.setTimeUnits(UnitDefinition.TIME);
            else
                model.setTimeUnits(createTimeUnit(growth));
        model.getTimeUnitsInstance().addCVTerm(CV_TERM_IS_TIME_UNIT);
        var volumeUnits = model.getVolumeUnitsInstance();
        if (volumeUnits == null && unitDefinitions.get(UnitDefinition.VOLUME) != null)
            model.setVolumeUnits(UnitDefinition.VOLUME);
        if (null != volumeUnits)
            model.getVolumeUnitsInstance().addCVTerm(CV_TERM_IS_VOLUME_UNIT);
    }

    private UnitDefinition createSubstanceUnit(UnitDefinition growth) {
            final var substanceUnits = model.createUnitDefinition(UnitDefinition.SUBSTANCE);
            getUnitByKind(growth, Unit.Kind.GRAM).ifPresentOrElse(
                    unit -> substanceUnits.addUnit(safeClone(unit)),
                    () -> {
                        var u = substanceUnits.createUnit(Unit.Kind.GRAM);
                        u.setMultiplier(1);
                        u.setExponent(-1d);
                        u.setScale(0);
                        u.addCVTerm(CV_TERM_IS_UO_GRAM);
                    });
            getUnitByKind(growth, Unit.Kind.MOLE).ifPresentOrElse(
                    unit -> substanceUnits.addUnit(safeClone(unit)),
                    () -> {
                        var u = substanceUnits.createUnit(Unit.Kind.MOLE);
                        u.setMultiplier(1);
                        u.setExponent(1d);
                        u.setScale(-3);
                        u.addCVTerm(CV_TERM_IS_UO_MMOL);
                        u.addCVTerm(CV_TERM_IS_VERSION_OF_UO_MOLE);
                    });
            return substanceUnits;
    }

    private UnitDefinition createTimeUnit(UnitDefinition growth) {
        final var timeUnitDefinition = model.createUnitDefinition(UnitDefinition.TIME);
        getUnitByKind(growth, Unit.Kind.SECOND).ifPresentOrElse(
                unit -> {
                    var timeUnit = safeClone(unit);
                    if(timeUnit.getExponent() < 0)
                        timeUnit.setExponent(timeUnit.getExponent() * -1);
                    timeUnitDefinition.addUnit(timeUnit);
                },
                () -> {
                    var timeUnit = timeUnitDefinition.createUnit(Unit.Kind.SECOND);
                    timeUnit.setMultiplier(3600);
                    timeUnit.setScale(0);
                    timeUnit.setExponent(1d);
                    timeUnit.addCVTerm(CV_TERM_IS_UO_HOUR);
                    timeUnit.addCVTerm(CV_TERM_IS_VERSION_OF_UO_SECOND);
                    timeUnitDefinition.setName("Hour");
                });
        return timeUnitDefinition;
    }

    private Optional<Unit> getUnitByKind(UnitDefinition ud, Unit.Kind kind) {
        return ud.getListOfUnits().stream()
                .filter(unit -> unit.getKind().equals(kind))
                .findFirst();
    }

    private <T extends SBase> T safeClone(T sbase) {
        @SuppressWarnings("unchecked")
        T sb = (T) sbase.clone();
        if (sb.isSetMetaId()) {
            sb.unsetMetaId();
        }
        return sb;
    }

}
