/**
 * 
 */
package edu.ucsd.sbrg.cobra;

import static org.sbml.jsbml.util.Pair.pairOf;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.TidySBMLWriter;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.FBCReactionPlugin;
import org.sbml.jsbml.ext.fbc.FBCSpeciesPlugin;
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.sbml.jsbml.util.ModelBuilder;

import com.jmatio.io.MatFileReader;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLCell;
import com.jmatio.types.MLChar;
import com.jmatio.types.MLDouble;
import com.jmatio.types.MLInt64;
import com.jmatio.types.MLNumericArray;
import com.jmatio.types.MLSparse;
import com.jmatio.types.MLStructure;

import de.zbit.sbml.util.SBMLtools;
import de.zbit.util.Utils;
import edu.ucsd.sbrg.bigg.BiGGId;

/**
 * @author Andreas Dr&auml;ger
 *
 */
public class COBRAparser {

  /**
   * A {@link Logger} for this class.
   */
  private static final transient Logger logger = Logger.getLogger(COBRAparser.class.getName());

  /**
   * @param args path to a file to be parsed.
   */
  public static void main(String[] args) throws IOException, SBMLException, XMLStreamException {
    SBMLDocument doc = COBRAparser.read(new File(args[0]));
    //TidySBMLWriter.write(doc, System.out, ' ', (short) 2);
    TidySBMLWriter.write(doc, File.createTempFile("tmp", ".xml", new File(System.getProperty("user.home"))), ' ', (short) 2);
  }

  /**
   * 
   * @param matFile
   * @return
   * @throws IOException
   */
  public static SBMLDocument read(File matFile) throws IOException {
    COBRAparser parser = new COBRAparser();
    return parser.parse(matFile);
  }

  /**
   * 
   * @param matFile
   * @return
   * @throws IOException
   */
  private SBMLDocument parse(File matFile) throws IOException {
    MatFileReader mfr = new MatFileReader(matFile);
    ModelBuilder builder = new ModelBuilder(3, 1);
    Map<String,MLArray> content = mfr.getContent();
    if (content.keySet().size() > 1) {
      logger.warning(MessageFormat.format(
        "Parsing only one randomly selected model of {0} available models the gien file {1}.",
        content.keySet().size(), matFile.getAbsolutePath()));
    }
    for (String key : content.keySet()) {
      MLArray array = content.get(key);
      builder.buildModel(SBMLtools.toSId(array.getName()), null);
      parseModel(builder, array);
      break;
    }
    return builder.getSBMLDocument();
  }

  /**
   * 
   * @param builder
   * @param genes
   */
  private void parseGenes(ModelBuilder builder, MLCell genes) {
    if (genes == null) {
      return;
    }
    Model model = builder.getModel();
    FBCModelPlugin modelPlug = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
    for (int i = 0; i < genes.getSize(); i++) {
      String id = toString(genes.get(i));
      if ((id != null) && (id.length() > 0)) {
        try {
          BiGGId biggId = new BiGGId(id);
          if (!biggId.isSetPrefix()) {
            biggId.setPrefix("G");
          }
          GeneProduct gp = modelPlug.createGeneProduct(biggId.toBiGGId());
          gp.setLabel(id);
          gp.setName(id);
        } catch (IllegalArgumentException exc) {
          logger.warning(MessageFormat.format("{0}: {1}", exc.getClass().getSimpleName(), Utils.getMessage(exc)));
        }
      }
    }
  }

  /**
   * 
   * @param builder
   * @param mets
   * @param metNames
   * @param metFormulas
   */
  private void parseMetabolites(ModelBuilder builder, MLCell mets,
    MLCell metNames, MLCell metFormulas) {
    Model model = builder.getModel();
    for (int i = 0; i < mets.getSize(); i++) {
      String id = toString(mets.get(i));
      if ((id != null) && (id.length() > 0)) {
        try {
          BiGGId biggId = new BiGGId(id);
          if (!biggId.isSetPrefix()) {
            biggId.setPrefix("M");
          }
          Species species = model.createSpecies(biggId.toBiGGId());
          species.setName(toString(metNames.get(i)));
          if (metFormulas != null) {
            FBCSpeciesPlugin specPlug = (FBCSpeciesPlugin) species.getPlugin(FBCConstants.shortLabel);
            specPlug.setChemicalFormula(toString(metFormulas.get(i)));
          }
        } catch (IllegalArgumentException exc) {
          logger.warning(MessageFormat.format("{0}: {1}", exc.getClass().getSimpleName(), Utils.getMessage(exc)));
        }
      }
    }
  }

  /**
   * 
   * @param builder
   * @param array
   * @return
   */
  private Model parseModel(ModelBuilder builder, MLArray array) {
    // TODO: always check for null!
    if ((array.getSize() != 1) && !array.isStruct()) {
      throw new IllegalArgumentException(MessageFormat.format(
        "Expected data struct of size 1, but found array ''{1}'' of size {0,number,integer}.",
        array.getSize(), array.getName()));
    }

    MLStructure struct = (MLStructure) array;
    // Check that the given data structure only contains allowable entries:
    for (MLArray field : struct.getAllFields()) {
      try {
        ModelFields.valueOf(field.getName());
      } catch (IllegalArgumentException t) {
        logger.warning(MessageFormat.format("{0}: {1}", t.getClass().getSimpleName(), Utils.getMessage(t)));
      }
    }
    // parse model
    Model model = builder.getModel();
    MLArray descriptionField = struct.getField(ModelFields.description.name());
    if (descriptionField.isChar()) {
      MLChar description = (MLChar) descriptionField;
      if (description.getDimensions()[0] == 1) {
        model.setName(description.getString(0));
      } else {
        logger.warning(MessageFormat.format("Found more than one identifier in cell {0}", descriptionField.contentToString()));
      }
    }

    // parse metabolites
    MLCell mets = toMLCell(struct.getField(ModelFields.mets.name()));
    MLCell metNames = toMLCell(struct.getField(ModelFields.metNames.name()));
    MLCell metFormulas = toMLCell(struct.getField(ModelFields.metFormulas.name()));
    parseMetabolites(builder, mets, metNames, metFormulas);

    // parse genes
    MLCell genes = toMLCell(struct.getField(ModelFields.mets.name()));
    parseGenes(builder, genes);

    // parse reactions
    MLCell rxns = toMLCell(struct.getField(ModelFields.rxns.name()));
    MLCell rxnNames = toMLCell(struct.getField(ModelFields.rxnNames.name()));
    MLNumericArray<?> rev = toMLNumericArray(struct.getField(ModelFields.rev.name()));
    MLDouble lb = toMLDouble(struct.getField(ModelFields.lb.name()));
    MLDouble ub = toMLDouble(struct.getField(ModelFields.ub.name()));
    MLSparse S = toMLSparse(struct.getField(ModelFields.S.name()));
    parseRxns(builder, rxns, rxnNames, rev, S, mets, lb, ub);

    return model;
  }

  /**
   * 
   * @param builder
   * @param rxns
   * @param rxnNames
   * @param rev
   * @param S
   * @param mets
   * @param lb
   * @param ub
   */
  private <T extends Number> void parseRxns(ModelBuilder builder, MLCell rxns, MLCell rxnNames, MLNumericArray<T> rev, MLSparse S, MLCell mets, MLDouble lb, MLDouble ub) {
    Model model = builder.getModel();
    for (int j = 0; j < rxns.getSize(); j++) {
      String rId = toString(rxns.get(j));
      if ((rId != null) && (rId.length() > 0)) {
        try {
          BiGGId biggId = new BiGGId(rId);
          if (!biggId.isSetPrefix()) {
            biggId.setPrefix("R");
          }
          Reaction r = model.createReaction(biggId.toBiGGId());
          r.setName(toString(rxnNames.get(j)));
          r.setReversible(rev.get(j).doubleValue() != 0d);
          FBCReactionPlugin rPlug = (FBCReactionPlugin) r.getPlugin(FBCConstants.shortLabel);
          rPlug.setLowerFluxBound(builder.buildParameter(r.getId() + "_lb", null, lb.get(j), true, (String) null));
          rPlug.setUpperFluxBound(builder.buildParameter(r.getId() + "_ub", null, ub.get(j), true, (String) null));
          // Take the current culumn of S and look for all non-zero coefficients
          for (int i = 0; i < S.getM(); i++) {
            double coeff = S.get(i, j).doubleValue();
            if (coeff != 0d) {
              try {
                BiGGId metId = new BiGGId(toString(mets.get(i)));
                metId.setPrefix("M");
                Species species = model.getSpecies(metId.toBiGGId());
                if (coeff < 0d) { // Reactant
                  ModelBuilder.buildReactants(r, pairOf(-coeff, species));
                } else if (coeff > 0d) { // Product
                  ModelBuilder.buildProducts(r, pairOf(coeff, species));
                }
              } catch (IllegalArgumentException exc) {
                logger.warning(MessageFormat.format("Could not add reaction participant because of invalid id: {0}", Utils.getMessage(exc)));
              }
            }
          }
        } catch (IllegalArgumentException exc) {
          logger.warning(MessageFormat.format("{0}: {1}", exc.getClass().getSimpleName(), Utils.getMessage(exc)));
        }
      }
    }
  }

  /**
   * 
   * @param array
   * @return
   */
  private MLInt64 toInt66(MLArray array) {
    if (array.isInt64()) {
      MLInt64 integer = (MLInt64) array;
      return integer;
    }
    throw new IllegalArgumentException(MessageFormat.format("Expected data structure ''{1}'' to be of type int64, but received type {0}.", MLArray.typeToString(array.getType()), array.getName()));
  }

  /**
   * 
   * @param array
   * @return
   */
  private MLCell toMLCell(MLArray array) {
    if ((array != null) && array.isCell()) {
      MLCell cell = (MLCell) array;
      return cell;
    }
    logger.warning(MessageFormat.format("Expected data structure ''{1}'' to be of type cell, but received type {0}.", MLArray.typeToString(array.getType()), array.getName()));
    return null;
  }
  /**
   * 
   * @param array
   * @return
   */
  private MLDouble toMLDouble(MLArray array) {
    if (array.isDouble()) {
      MLDouble real = (MLDouble) array;
      return real;
    }
    throw new IllegalArgumentException(MessageFormat.format("Expected data structure ''{1}'' to be of type double, but received type {0}.", MLArray.typeToString(array.getType()), array.getName()));
  }

  /**
   * 
   * @param field
   * @return
   */
  private MLNumericArray<?> toMLNumericArray(MLArray array) {
    if (array instanceof MLNumericArray<?>) {
      MLNumericArray<?> mlna = (MLNumericArray<?>) array;
      return mlna;
    }
    return null;
  }

  /**
   * 
   * @param array
   * @return
   */
  private MLSparse toMLSparse(MLArray array) {
    if (array.isSparse()) {
      MLSparse sparse = (MLSparse) array;
      return sparse;
    }
    logger.warning(MessageFormat.format("Expected data structure ''{1}'' to be of type sparse array, but received type {0}.", MLArray.typeToString(array.getType()), array.getName()));
    return null;
  }

  /**
   * 
   * @param array
   * @return
   */
  private String toString(MLArray array) {
    StringBuilder sb = new StringBuilder();
    if (array.isChar()) {
      MLChar string = (MLChar) array;
      if (string.getDimensions()[0] > 1) {
        logger.fine(MessageFormat.format("Found more than one string in cell {0}", string.contentToString()));
      }
      for (int i = 0; i < string.getDimensions()[0]; i++) {
        if (i > 0) {
          sb.append('\n');
        }
        sb.append(string.getString(i));
      }
    } else {
      logger.warning(MessageFormat.format("Expected character string in data field ''{1}'', but received data type {0}.", MLArray.typeToString(array.getType()), array.getName()));
    }
    return sb.toString();
  }

}
