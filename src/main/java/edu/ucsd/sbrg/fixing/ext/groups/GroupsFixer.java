package edu.ucsd.sbrg.fixing.ext.groups;

import de.zbit.util.ResourceManager;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ext.groups.Group;
import org.sbml.jsbml.ext.groups.GroupsConstants;
import org.sbml.jsbml.ext.groups.GroupsModelPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public class GroupsFixer {

    private static final Logger logger = LoggerFactory.getLogger(GroupsFixer.class);
    private static final ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");

    /**
     * Set group kind where required
     *
     */
    public static void fixGroups(Model model) {
        GroupsModelPlugin gPlug = (GroupsModelPlugin) model.getExtension(GroupsConstants.shortLabel);
        if ((gPlug != null) && gPlug.isSetListOfGroups()) {
            for (Group group : gPlug.getListOfGroups()) {
                if (!group.isSetKind()) {
                    logger.info(MessageFormat.format(MESSAGES.getString("ADD_KIND_TO_GROUP"),
                            group.isSetName() ? group.getName() : group.getId()));
                    group.setKind(Group.Kind.partonomy);
                }
            }
        }
    }
}
