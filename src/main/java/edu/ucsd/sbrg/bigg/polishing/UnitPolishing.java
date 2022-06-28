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
    public static final CVTerm CV_TERM_IS_UO_MOLE = new CVTerm(CVTerm.Qualifier.BQB_IS, Registry.createURI("unit", "UO:0000040"));
    public static final CVTerm CV_TERM_IS_UO_SECOND = new CVTerm(CVTerm.Qualifier.BQB_IS, Registry.createURI("unit", "UO:0000032"));
    public static final CVTerm CV_TERM_IS_VERSION_OF_UO_SECOND = new CVTerm(CVTerm.Qualifier.BQB_IS_VERSION_OF, Registry.createURI("unit", "UO:0000032"));

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
        annotateGrowthUnitDefinition(growth);

        setModelUnits(unitDefinitions);
        setSubstanceUnit(unitDefinitions, growth);
        setTimeUnit(growth);

        UnitDefinition substanceUnits = model.getSubstanceUnitsInstance();
        if (!model.isSetExtentUnits()) {
            model.setExtentUnits(substanceUnits.getId());
        }
        if (!model.isSetSubstanceUnits()) {
            model.setSubstanceUnits(substanceUnits.getId());
        }
        while (progress.getCallNumber() < udCount) {
            progress.DisplayBar("Polishing Unit Definitions (2/9)  ");
        }
    }

    /**
     * Adds basic unit definitions to model, if not present
     *
     * @return Millimoles per gram (dry weight) per hour {@link UnitDefinition}
     */
    private UnitDefinition createGrowthUnitDefinition() {
        var growth = model.getUnitDefinition(GROWTH_UNIT_ID);
        if (growth == null) {
            growth = model.createUnitDefinition(GROWTH_UNIT_ID);
            logger.finest(MESSAGES.getString("ADDED_UNIT_DEF"));
        }
        if (growth.getUnitCount() < 1) {
            ModelBuilder.buildUnit(growth, 1d, -3, Unit.Kind.MOLE, 1d);
            ModelBuilder.buildUnit(growth, 1d, 0, Unit.Kind.GRAM, -1d);
            ModelBuilder.buildUnit(growth, 3600d, 0, Unit.Kind.SECOND, -1d);
        }
        if (!growth.isSetName()) {
            growth.setName(GROWTH_UNIT_NAME);
        }
        if (!growth.isSetMetaId()) {
            growth.setMetaId(growth.getId());
        }
        return growth;
    }

    private void annotateGrowthUnitDefinition(UnitDefinition growth) {
        growth.addCVTerm(CV_TERM_DESCRIBED_BY_PUBMED_GROWTH_UNIT);
        getUnitByKind(growth, Unit.Kind.MOLE)
                .filter(unit -> unit.getScale() == -3)
                .ifPresent(unit -> unit.addCVTerm(CV_TERM_IS_UO_MOLE));
        getUnitByKind(growth, Unit.Kind.GRAM)
                .ifPresent(unit -> unit.addCVTerm(new CVTerm(
                        CVTerm.Qualifier.BQB_IS_VERSION_OF,
                        unit.getKind().getUnitOntologyIdentifier())));
    }


    /**
     * Sets substance, volume and time units for model from the models unit definitions, if not set
     */
    private void setModelUnits(ListOf<UnitDefinition> unitDefinitions) {
        var substanceUnits = model.getSubstanceUnitsInstance();
        if (substanceUnits == null && unitDefinitions.get(UnitDefinition.SUBSTANCE) != null) {
            model.setSubstanceUnits(UnitDefinition.SUBSTANCE);
        }
        var volumeUnits = model.getVolumeUnitsInstance();
        if (volumeUnits == null && unitDefinitions.get(UnitDefinition.VOLUME) != null) {
            model.setVolumeUnits(UnitDefinition.VOLUME);
        }
        var timeUnits = model.getTimeUnitsInstance();
        if (timeUnits == null && unitDefinitions.get(UnitDefinition.TIME) != null) {
            model.setTimeUnits(UnitDefinition.TIME);
        }
    }

    private void setSubstanceUnit(ListOf<UnitDefinition> unitDefinitions, UnitDefinition growth) {
        if (null == model.getSubstanceUnitsInstance()) {
            if (unitDefinitions.get(UnitDefinition.SUBSTANCE) != null) {
                model.setSubstanceUnits(UnitDefinition.SUBSTANCE);
            } else {
                final var substanceUnits = model.createUnitDefinition(UnitDefinition.SUBSTANCE);
                substanceUnits.setName("Millimoles per gram (dry weight)");
                getUnitByKind(growth, Unit.Kind.GRAM).ifPresent(unit -> substanceUnits.addUnit(safeClone(unit)));
                getUnitByKind(growth, Unit.Kind.MOLE).ifPresent(unit -> substanceUnits.addUnit(safeClone(unit)));
                model.setSubstanceUnits(substanceUnits);
            }
        }
    }

    private void setTimeUnit(UnitDefinition growth) {
        if (null == model.getTimeUnitsInstance() ) {
            getUnitByKind(growth, Unit.Kind.SECOND)
                    .ifPresent(unit -> {
                        var timeUnitDefinition = model.createUnitDefinition(UnitDefinition.TIME);
                        timeUnitDefinition.setName("Hour");
                        model.setTimeUnits(timeUnitDefinition.getId());
                        var timeUnit = safeClone(unit);
                        timeUnit.setExponent(1d);
                        timeUnitDefinition.addUnit(timeUnit);
                        timeUnit.addCVTerm(CV_TERM_IS_UO_SECOND);
                        timeUnit.addCVTerm(CV_TERM_IS_VERSION_OF_UO_SECOND);
                    });
        }
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
