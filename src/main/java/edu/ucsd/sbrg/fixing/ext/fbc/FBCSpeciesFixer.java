package edu.ucsd.sbrg.fixing.ext.fbc;

import edu.ucsd.sbrg.fixing.AbstractFixer;
import edu.ucsd.sbrg.fixing.IFixSBases;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCSpeciesPlugin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FBCSpeciesFixer extends AbstractFixer implements IFixSBases<Species> {

    @Override
    public void fix(Species species, int index) {
        FBCSpeciesPlugin plugin = (FBCSpeciesPlugin) species.getPlugin(FBCConstants.shortLabel);

        if (plugin.isSetChemicalFormula()) {
            /*
            As of 2024, the only model on BiGG or Biomodels this occurs in is iYS1720.
            Proper handling should move this information to the Species Notes. However, I do not know
            how to do proper XHTML handling right now, and since it is just one model, I cannot really be bothered
            to deal with it.
             */
            String pattern = "charge[0-9]*";
            Matcher matcher = Pattern.compile(pattern).matcher(plugin.getChemicalFormula());
            if (matcher.find()) {
                var chargeGroup = matcher.group();
//                if (!species.isSetNotes()) {
//                    species.setNotes(new XMLNode("<p>Charge string extracted from chemical formula: " + chargeGroup + "</p>"));
//                } else {
//                    species.getNotes().addChild(new XMLNode("<p>Charge string extracted from chemical formula: "
//                            + chargeGroup + "</p>"));
//                }
                plugin.setChemicalFormula(matcher.replaceAll(""));

            }
        }
    }
}
