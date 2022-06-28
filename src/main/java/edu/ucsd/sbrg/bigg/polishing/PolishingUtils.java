package edu.ucsd.sbrg.bigg.polishing;

import de.zbit.util.ResourceManager;

import java.util.ResourceBundle;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

public class PolishingUtils {

    private static final transient Logger logger = Logger.getLogger(SBMLPolisher.class.getName());

    private static final transient ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");

    public static String polishName(String name) {
        String newName = name;
        if (name.startsWith("?_")) {
            newName = name.substring(2);
        }
        if (newName.matches("__.*__")) {
            newName = newName.replaceAll("__.*__", "(.*)");
        } else if (newName.contains("__")) {
            newName = newName.replace("__", "-");
        }
        if (newName.matches(".*_C?\\d*.*\\d*")) {
            newName =
                    newName.substring(0, newName.lastIndexOf('_')) + " - " + newName.substring(newName.lastIndexOf('_') + 1);
        }
        newName = newName.replace("_", " ");
        if (!newName.equals(name)) {
            logger.fine(format(MESSAGES.getString("CHANGED_NAME"), name, newName));
        }
        return newName;
    }

}
