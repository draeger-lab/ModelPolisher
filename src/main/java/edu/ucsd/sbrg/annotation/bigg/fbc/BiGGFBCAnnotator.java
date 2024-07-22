package edu.ucsd.sbrg.annotation.bigg.fbc;

import edu.ucsd.sbrg.Parameters;
import edu.ucsd.sbrg.annotation.bigg.AbstractBiGGAnnotator;
import edu.ucsd.sbrg.db.bigg.BiGGDB;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import edu.ucsd.sbrg.resolver.Registry;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;

import java.util.List;

public class BiGGFBCAnnotator extends AbstractBiGGAnnotator<Model> {

    public BiGGFBCAnnotator(BiGGDB bigg, Parameters parameters, Registry registry, List<ProgressObserver> observers) {
        super(bigg, parameters, registry, observers);
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
        new BiGGGeneProductAnnotator(new BiGGGeneProductReferencesAnnotator(), bigg, parameters, registry, getObservers())
                .annotate(fbcModelPlugin.getListOfGeneProducts());
    }
}
