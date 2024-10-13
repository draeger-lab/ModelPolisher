package de.uni_halle.informatik.biodata.mp.fixing;

import de.zbit.util.ResourceManager;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.uni_halle.informatik.biodata.mp.logging.BundleNames;

import java.util.ResourceBundle;

public interface FixingOptions extends KeyProvider {

    ResourceBundle MESSAGES = ResourceManager.getBundle(BundleNames.FIXING_MESSAGES);

    @SuppressWarnings("unchecked")
    Option<Boolean> DONT_FIX =
            new Option<>("DONT_FIX", Boolean.class, "Don't run the fixer module on the input.",
                    Boolean.FALSE);

    @SuppressWarnings("unchecked")
    Option<Double[]> FLUX_COEFFICIENTS =
            new Option<>("FLUX_COEFFICIENTS", Double[].class, MESSAGES.getString("FLUX_COEFF_DESC"), new Double[0]);

    @SuppressWarnings("unchecked")
    Option<String[]> FLUX_OBJECTIVES =
            new Option<>("FLUX_OBJECTIVES", String[].class, MESSAGES.getString("FLUX_OBJ_DESC"), new String[0]);

}
