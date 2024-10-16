package de.uni_halle.informatik.biodata.mp.fixing.ext.groups;

import de.zbit.util.ResourceManager;
import de.uni_halle.informatik.biodata.mp.fixing.AbstractFixer;
import de.uni_halle.informatik.biodata.mp.fixing.IFixSBases;
import de.uni_halle.informatik.biodata.mp.reporting.ProgressObserver;
import org.sbml.jsbml.ext.groups.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;

public class GroupsFixer extends AbstractFixer implements IFixSBases<Group> {

    private static final Logger logger = LoggerFactory.getLogger(GroupsFixer.class);
    private static final ResourceBundle MESSAGES = ResourceManager.getBundle("de.uni_halle.informatik.biodata.mp.fixing.Messages");

    public GroupsFixer(List<ProgressObserver> observers) {
        super(observers);
    }


    @Override
    public void fix(List<Group> rs) {
        statusReport("Fixing Groups (5/6)  ", rs);
        IFixSBases.super.fix(rs);
    }


    @Override
    public void fix(Group group, int index) {
        if (!group.isSetKind()) {
            logger.debug(MessageFormat.format(MESSAGES.getString("ADD_KIND_TO_GROUP"),
                    group.isSetName() ? group.getName() : group.getId()));
            group.setKind(Group.Kind.partonomy);
        }
    }
}
