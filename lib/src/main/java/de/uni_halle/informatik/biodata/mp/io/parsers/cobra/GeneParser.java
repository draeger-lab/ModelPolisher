package de.uni_halle.informatik.biodata.mp.io.parsers.cobra;

import de.uni_halle.informatik.biodata.mp.db.bigg.BiGGId;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.GeneProduct;
import us.hebi.matlab.mat.types.Cell;

import java.util.Optional;

public class GeneParser {

  private final FBCModelPlugin plugin;
  private final int index;

  public GeneParser(FBCModelPlugin plugin, int index) {
    this.plugin = plugin;
    this.index = index;
  }

  public void parse() {
    Optional<Cell> geneCell = MatlabFields.getInstance().getCell(ModelField.genes.name());
    geneCell.map(genes -> COBRAUtils.asString(genes.get(index), ModelField.genes.name(), index + 1))
            .ifPresent(id -> {
              var biggId = BiGGId.createGeneId(id);
              GeneProduct gp = plugin.createGeneProduct(biggId.toBiGGId());
              gp.setLabel(id);
              gp.setName(id);
            });
  }
}
