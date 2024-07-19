package edu.ucsd.sbrg.annotation.fbc;

import edu.ucsd.sbrg.Parameters;
import edu.ucsd.sbrg.annotation.AbstractAnnotator;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import edu.ucsd.sbrg.resolver.Registry;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;

import java.util.List;

public class FBCAnnotator extends AbstractAnnotator<Model> {

    public FBCAnnotator(Parameters parameters, Registry registry, List<ProgressObserver> observers) {
        super(parameters, registry, observers);
    }

    @Override
    public void annotate(Model model) {
//        // Calculate the change in the number of gene products to update the progress bar accordingly
//        int changed = fbcModelPlugin.getNumGeneProducts() - initialGeneProducts;
//        if (changed > 0) {
//            long current = progress.getCallNumber();
//            // Adjust the total number of calls for the progress bar by subtracting the placeholder count
//            progress.setNumberOfTotalCalls(progress.getNumberOfTotalCalls() + changed - 50);
//            progress.setCallNr(current);
//        }
        FBCModelPlugin fbcModelPlugin = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
        new GeneProductAnnotator(new GeneProductReferencesAnnotator(), parameters, registry, getObservers())
                .annotate(fbcModelPlugin.getListOfGeneProducts());
    }
}
