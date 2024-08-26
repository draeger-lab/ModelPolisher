package edu.ucsd.sbrg.annotation.bigg.ext.fbc;

import edu.ucsd.sbrg.annotation.AnnotationException;
import edu.ucsd.sbrg.annotation.IAnnotateSBases;
import edu.ucsd.sbrg.parameters.BiGGAnnotationParameters;
import edu.ucsd.sbrg.annotation.bigg.AbstractBiGGAnnotator;
import edu.ucsd.sbrg.db.bigg.BiGGDB;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import edu.ucsd.sbrg.resolver.Registry;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;

import java.sql.SQLException;
import java.util.List;

public class BiGGFBCAnnotator extends AbstractBiGGAnnotator implements IAnnotateSBases<Model> {

    public BiGGFBCAnnotator(BiGGDB bigg, BiGGAnnotationParameters parameters, Registry registry, List<ProgressObserver> observers) {
        super(bigg, parameters, registry, observers);
    }

    @Override
    public void annotate(Model model) throws SQLException, AnnotationException {
//        // Calculate the change in the number of gene products to update the progress bar accordingly
//        int changed = fbcModelPlugin.getNumGeneProducts() - initialGeneProducts;
//        if (changed > 0) {
//            long current = progress.getCallNumber();
//            // Adjust the total number of calls for the progress bar by subtracting the placeholder count
//            progress.setNumberOfTotalCalls(progress.getNumberOfTotalCalls() + changed - 50);
//            progress.setCallNr(current);
//        }
        FBCModelPlugin fbcModelPlugin = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
        new BiGGFBCSpeciesAnnotator(bigg, biGGAnnotationParameters, registry).annotate(model.getListOfSpecies());
        new BiGGGeneProductAnnotator(new BiGGGeneProductReferencesAnnotator(), bigg, biGGAnnotationParameters, registry, getObservers())
                .annotate(fbcModelPlugin.getListOfGeneProducts());
    }
}
