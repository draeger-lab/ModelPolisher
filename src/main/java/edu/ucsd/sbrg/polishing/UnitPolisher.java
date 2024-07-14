package edu.ucsd.sbrg.polishing;

import edu.ucsd.sbrg.Parameters;
import edu.ucsd.sbrg.identifiersorg.IdentifiersOrg;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import org.sbml.jsbml.*;
import org.sbml.jsbml.util.ModelBuilder;

import java.util.List;
import java.util.Optional;

/**
 * This class is responsible for ensuring that all necessary {@link UnitDefinition}s and {@link Unit}s are correctly
 * defined and present in the SBML model. It handles the creation and verification of units used in the model,
 * particularly focusing on the units related to growth, substance, time, and volume.
 *
 * Additionally, this class is responsible for accurately assigning these defined units to specific components 
 * such as reactions, species, and parameters within the model as needed. 
 * This ensures that all these components adhere uniformly to the correct unit specifications, 
 * maintaining consistency and accuracy throughout the model's unit definitions.
 */
public class UnitPolisher extends AbstractPolisher<Model>{

    public static final CVTerm CV_TERM_DESCRIBED_BY_PUBMED_GROWTH_UNIT = new CVTerm(CVTerm.Qualifier.BQB_IS_DESCRIBED_BY, IdentifiersOrg.createURI("pubmed", 7986045));
    public static final String GROWTH_UNIT_ID = "mmol_per_gDW_per_hr";
    public static final String GROWTH_UNIT_NAME = "Millimoles per gram (dry weight) per hour";
    public static final CVTerm CV_TERM_IS_SUBSTANCE_UNIT = new CVTerm(CVTerm.Qualifier.BQB_IS, IdentifiersOrg.createURI("unit", "UO:0000006"));
    public static final CVTerm CV_TERM_IS_TIME_UNIT = new CVTerm(CVTerm.Qualifier.BQB_IS, IdentifiersOrg.createURI("unit", "UO:0000003"));
    public static final CVTerm CV_TERM_IS_VOLUME_UNIT = new CVTerm(CVTerm.Qualifier.BQB_IS, IdentifiersOrg.createURI("unit", "UO:0000095"));
    public static final CVTerm CV_TERM_IS_UO_SECOND = new CVTerm(CVTerm.Qualifier.BQB_IS, IdentifiersOrg.createURI("unit", "UO:0000010"));
    public static final CVTerm CV_TERM_IS_UO_HOUR = new CVTerm(CVTerm.Qualifier.BQB_IS, IdentifiersOrg.createURI("unit", "UO:0000032"));
    public static final CVTerm CV_TERM_IS_UO_MMOL = new CVTerm(CVTerm.Qualifier.BQB_IS, IdentifiersOrg.createURI("unit", "UO:0000040"));
    public static final CVTerm CV_TERM_IS_UO_GRAM = new CVTerm(CVTerm.Qualifier.BQB_IS, IdentifiersOrg.createURI("unit", "UO:0000021"));
    public static final CVTerm CV_TERM_IS_VERSION_OF_UO_SECOND = new CVTerm(CVTerm.Qualifier.BQB_IS_VERSION_OF, IdentifiersOrg.createURI("unit", "UO:0000010"));
    public static final CVTerm CV_TERM_IS_VERSION_OF_UO_MOLE = new CVTerm(CVTerm.Qualifier.BQB_IS_VERSION_OF, IdentifiersOrg.createURI("unit", "UO:0000013"));

    public UnitPolisher(Parameters parameters) {
        super(parameters);
    }
    public UnitPolisher(Parameters parameters, List<ProgressObserver> observers) {
        super(parameters, observers);
    }

    /**
     * Ensures that all necessary {@link UnitDefinition}s and {@link Unit}s are present in the model.
     * If any are missing, they are created and added to the model. This method also sets the model's
     * extent and substance units if they are not already set.
     */
    public void polish(Model model) {
        // Update progress bar to indicate the current process stage
        updateProgressObservers("Polishing Unit Definitions (2/9)   ", null);

//        int udCount = model.getUnitDefinitionCount();

        // Fetch all unit definitions from the model
        var unitDefinitions = model.getListOfUnitDefinitions();

        // Create or retrieve a growth unit definition
        var growth = createGrowthUnitDefinition(model);

        // Assign the growth unit definition to the model
        setModelUnits(model, growth, unitDefinitions);

        // Get the instance of substance units from the model
        UnitDefinition substanceUnits = model.getSubstanceUnitsInstance();
        // Set the extent units of the model if not already set
        if (!model.isSetExtentUnits())
            model.setExtentUnits(substanceUnits.getId());

        // Set the substance units of the model if not already set
        if (!model.isSetSubstanceUnits())
            model.setSubstanceUnits(substanceUnits.getId());

        // TODO: ich bin mir ziemlich sicher, dass dieser Code hier nonsense ist
        // Continue updating the progress bar until all unit definitions are processed
//        while (progress.getCallNumber() < udCount) {
//            progress.DisplayBar("Polishing Unit Definitions (2/9)   ");
//        }
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
    private UnitDefinition defaultGrowthUnitDefinition(Model model) {
        var growth = new UnitDefinition(model.getLevel(), model.getVersion());
        growth.setId(GROWTH_UNIT_ID);
        growth.setName(GROWTH_UNIT_NAME);
        ModelBuilder.buildUnit(growth, 1d, -3, Unit.Kind.MOLE, 1d);
        ModelBuilder.buildUnit(growth, 1d, 0, Unit.Kind.GRAM, -1d);
        ModelBuilder.buildUnit(growth, 3600d, 0, Unit.Kind.SECOND, -1d);
        return growth;
    }

    /**
     * Creates a growth unit definition for the model. If an equivalent growth unit already exists,
     * it modifies the ID of the existing unit to indicate it was preexisting. This method ensures
     * that the growth unit is properly annotated and added to the model.
     *
     * @return The newly created or modified growth unit definition.
     */
    private UnitDefinition createGrowthUnitDefinition(Model model) {
        // Create a default growth unit definition.
        var growth = defaultGrowthUnitDefinition(model);
        // Check if an equivalent growth unit already exists in the model.
        var otherGrowth = findGrowthUnit(model.getListOfUnitDefinitions(), growth).orElse(growth);
        // Set the meta ID of the growth unit if it is not already set.
        if (!growth.isSetMetaId())
            growth.setMetaId(growth.getId());
        // Annotate the growth unit definition with relevant metadata.
        annotateGrowthUnitDefinition(growth);
        // If the found growth unit is the same as the newly created one and it already exists in the model,
        // change its ID to indicate that it was preexisting.
        if (otherGrowth.equals(growth) && null != model.getUnitDefinition(GROWTH_UNIT_ID)) {
            model.getUnitDefinition(GROWTH_UNIT_ID).setId(GROWTH_UNIT_ID + "__preexisting");
        }
        // Add the growth unit definition to the model.
        model.addUnitDefinition(growth);
        return growth;
    }

    private CVTerm genericUnitAnnotation(Unit u) {
        return new CVTerm(CVTerm.Qualifier.BQB_IS_VERSION_OF,
                u.getKind().getUnitOntologyIdentifier());
    }

    /**
     * Annotates a growth unit definition with controlled vocabulary (CV) terms.
     * This method adds specific annotations based on the unit kind and its properties.
     * 
     * @param growth The UnitDefinition instance representing the growth unit to be annotated.
     */
    private void annotateGrowthUnitDefinition(UnitDefinition growth) {
        // Annotate the growth unit with a general descriptor from PubMed.
        growth.addCVTerm(CV_TERM_DESCRIBED_BY_PUBMED_GROWTH_UNIT);
        
        // Annotate the 'mole' unit based on its scale.
        getUnitByKind(growth, Unit.Kind.MOLE).ifPresent(u -> {
            if (u.getScale() == -3) {// If the scale is -3, it's millimoles.
                u.addCVTerm(CV_TERM_IS_UO_MMOL);
            } else {// For other scales, use a generic annotation.
                u.addCVTerm(this.genericUnitAnnotation(u));
            }
        });

        // Annotate the 'gram' unit generically.
        getUnitByKind(growth, Unit.Kind.GRAM).ifPresent(this::genericUnitAnnotation);

        // Annotate the 'second' unit based on its multiplier.
        getUnitByKind(growth, Unit.Kind.SECOND).ifPresent(u -> {
            switch (Double.valueOf(u.getMultiplier()).intValue()) {
                case 1: 
                    // If the multiplier is 1, it's seconds.
                    u.addCVTerm(CV_TERM_IS_UO_SECOND); 
                    break;
                case 3600: 
                    // If the multiplier is 3600, it's hours.
                    u.addCVTerm(CV_TERM_IS_UO_HOUR); 
                    break;
                default:
                    // For other multipliers, use a generic annotation.
                    u.addCVTerm(this.genericUnitAnnotation(u));
            }
        });
    }

    /**
     * This method sets the substance, volume, and time units for the model based on the provided unit definitions
     * or based on the growth unit if the specific units are not predefined in the model.
     * 
     * @param growth The UnitDefinition instance representing the growth unit.
     * @param unitDefinitions A ListOf<UnitDefinition> containing predefined unit definitions for substance, time, and volume.
     */
    private void setModelUnits(Model model, UnitDefinition growth, ListOf<UnitDefinition> unitDefinitions) {
        // Handle setting of substance units
        var substanceUnits = model.getSubstanceUnitsInstance();
        if (substanceUnits == null) {
            if (unitDefinitions.get(UnitDefinition.SUBSTANCE) != null) {
                model.setSubstanceUnits(UnitDefinition.SUBSTANCE);
            } else {
                model.setSubstanceUnits(createSubstanceUnit(model, growth));
            }
        }
        model.getSubstanceUnitsInstance().addCVTerm(CV_TERM_IS_SUBSTANCE_UNIT);

        // Handle setting of time units
        var timeUnits = model.getTimeUnitsInstance();
        if (timeUnits == null) {
            if (unitDefinitions.get(UnitDefinition.TIME) != null) {
                model.setTimeUnits(UnitDefinition.TIME);
            } else {
                model.setTimeUnits(createTimeUnit(model, growth));
            }
        }
        model.getTimeUnitsInstance().addCVTerm(CV_TERM_IS_TIME_UNIT);

        // Handle setting of volume units
        var volumeUnits = model.getVolumeUnitsInstance();
        if (volumeUnits == null && unitDefinitions.get(UnitDefinition.VOLUME) != null) {
            model.setVolumeUnits(UnitDefinition.VOLUME);
        }
        if (volumeUnits != null) {
            model.getVolumeUnitsInstance().addCVTerm(CV_TERM_IS_VOLUME_UNIT);
        }
    }

    /**
     * Creates a new UnitDefinition for substance units based on the growth UnitDefinition.
     * If specific units for GRAM or MOLE are present in the growth definition, those are cloned.
     * Otherwise, default units are created and added to the new UnitDefinition.
     *
     * @param growth The UnitDefinition instance representing the growth unit.
     * @return The newly created UnitDefinition with appropriate substance units.
     */
    private UnitDefinition createSubstanceUnit(Model model, UnitDefinition growth) {
        final var substanceUnits = model.createUnitDefinition(UnitDefinition.SUBSTANCE);
        
        // Handle GRAM units: clone if present, otherwise create default GRAM unit
        getUnitByKind(growth, Unit.Kind.GRAM).ifPresentOrElse(
                unit -> substanceUnits.addUnit(safeClone(unit)),
                () -> {
                    var u = substanceUnits.createUnit(Unit.Kind.GRAM);
                    u.setMultiplier(1);
                    u.setExponent(-1d);
                    u.setScale(0);
                    u.addCVTerm(CV_TERM_IS_UO_GRAM);
                });
        
        // Handle MOLE units: clone if present, otherwise create default MOLE unit
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

    /**
     * Creates a new UnitDefinition for time units based on the growth UnitDefinition.
     * If a unit of kind SECOND is present in the growth definition, it is cloned and adjusted if necessary.
     * If no such unit is present, a default unit representing an hour is created.
     *
     * @param growth The UnitDefinition instance representing the growth unit.
     * @return The newly created UnitDefinition with appropriate time units.
     */
    private UnitDefinition createTimeUnit(Model model, UnitDefinition growth) {
        final var timeUnitDefinition = model.createUnitDefinition(UnitDefinition.TIME);
        getUnitByKind(growth, Unit.Kind.SECOND).ifPresentOrElse(
                unit -> {
                    var timeUnit = safeClone(unit);
                    // Ensure the exponent is positive
                    if(timeUnit.getExponent() < 0)
                        timeUnit.setExponent(timeUnit.getExponent() * -1);
                    timeUnitDefinition.addUnit(timeUnit);
                },
                () -> {
                    var timeUnit = timeUnitDefinition.createUnit(Unit.Kind.SECOND);
                    // Set properties for the default hour unit
                    timeUnit.setMultiplier(3600);
                    timeUnit.setScale(0);
                    timeUnit.setExponent(1d);
                    timeUnit.addCVTerm(CV_TERM_IS_UO_HOUR);
                    timeUnit.addCVTerm(CV_TERM_IS_VERSION_OF_UO_SECOND);
                    timeUnitDefinition.setName("Hour");
                });
        return timeUnitDefinition;
    }

    /**
     * Retrieves the first unit of a specified kind from a given UnitDefinition.
     * 
     * @param ud   the UnitDefinition from which to retrieve the unit
     * @param kind the kind of unit to retrieve
     * @return an Optional containing the unit if found, or an empty Optional if no such unit exists
     */
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
