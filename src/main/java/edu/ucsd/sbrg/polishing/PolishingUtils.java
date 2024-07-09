package edu.ucsd.sbrg.polishing;

import de.zbit.util.ResourceManager;

import java.util.ResourceBundle;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

public class PolishingUtils {

    private static final Logger logger = Logger.getLogger(ModelPolisher.class.getName());

    private static final ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");

    /**
     * Processes and polishes a given identifier name by applying a series of string transformations
     * to make it more readable or compliant with certain standards.
     *
     * @param name The original identifier name to be polished.
     * @return The polished version of the identifier name.
     */
    public static String polishName(String name) {
        String newName = name;
        // Remove leading "?_" if present
        if (name.startsWith("?_")) {
            newName = name.substring(2);
        }
        // Replace patterns enclosed by double underscores with "(.*)"
        if (newName.matches("__.*__")) {
            newName = newName.replaceAll("__.*__", "(.*)");
        } else if (newName.contains("__")) { // Replace standalone double underscores with a hyphen
            newName = newName.replace("__", "-");
        }
        // Replace last underscore with " - " if it's followed by a number
        if (newName.matches(".*_C?\\d*.*\\d*")) {
            newName = newName.substring(0, newName.lastIndexOf('_')) + " - " + newName.substring(newName.lastIndexOf('_') + 1);
        }
        // Replace all remaining underscores with spaces
        newName = newName.replace("_", " ");
        // Log the change if the name was altered
        if (!newName.equals(name)) {
            logger.fine(format(MESSAGES.getString("CHANGED_NAME"), name, newName));
        }
        return newName;
    }
}
