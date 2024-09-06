package de.uni_halle.informatik.biodata.mp.reporting;

import org.sbml.jsbml.SBase;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.util.TreeNodeChangeListener;
import org.sbml.jsbml.util.TreeNodeRemovedEvent;

import javax.swing.tree.TreeNode;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;

public class DiffListener implements TreeNodeChangeListener {


    private final List<String> removedIds;

    public DiffListener() {
        removedIds = new ArrayList<>();
    }
{}
    @Override
    public void nodeAdded(TreeNode node) {

    }

    @Override
    public void nodeRemoved(TreeNodeRemovedEvent event) {
        if(event.getSource() instanceof SBase) {
            removedIds.add(((SBase) event.getSource()).getId());
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
        if (propertyChangeEvent.getSource().getClass().equals(Species.class)) {

        } else if (propertyChangeEvent.getSource().getClass().equals(Species.class)) {

        }   else if (propertyChangeEvent.getSource().getClass().equals(Species.class)) {

        }
    }
}
