package de.uni_halle.informatik.biodata.mp.polishing;

import de.zbit.util.ResourceManager;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.uni_halle.informatik.biodata.mp.logging.BundleNames;

import java.util.ResourceBundle;

public interface PolishingOptions extends KeyProvider {

    ResourceBundle MESSAGES = ResourceManager.getBundle(BundleNames.POLISHING_MESSAGES);

//    /**
//     * When set to true, the mass balance of each reaction will be checked where
//     * possible. Reactions that are recognized as peudoreactions are excluded from
//     * this check, also are reactions that lack information about elementary
//     * composition of their participants.
//     */
//    @SuppressWarnings("unchecked")
//    Option<Boolean> CHECK_MASS_BALANCE =
//            new Option<>("CHECK_MASS_BALANCE", Boolean.class, MESSAGES.getString("CHECK_MASS_BALANCE_DESC"),
//                    Boolean.TRUE);

    @SuppressWarnings("unchecked")
    Option<Boolean> POLISH_EVEN_IF_MODEL_INVALID =
            new Option<>("POLISH_EVEN_IF_MODEL_INVALID",
                    Boolean.class,
                    MESSAGES.getString("POLISH_EVEN_IF_MODEL_INVALID"),
                    Boolean.TRUE);

}
