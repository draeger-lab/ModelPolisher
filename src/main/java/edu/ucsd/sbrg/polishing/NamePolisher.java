package edu.ucsd.sbrg.polishing;

import de.zbit.util.ResourceManager;
import org.sbml.jsbml.SBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;

import static java.text.MessageFormat.format;

public class NamePolisher implements IPolishSBaseAttributes {

    private static final Logger logger = LoggerFactory.getLogger(NamePolisher.class);

    private static final ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");

    @Override
    public void polish(SBase sbase) {
        var name = sbase.getName();
        sbase.setName(polish(name));
    }

    public String polish(String name) {
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
            logger.debug(format(MESSAGES.getString("CHANGED_NAME"), name, newName));
        }
        return newName;
    }
}
