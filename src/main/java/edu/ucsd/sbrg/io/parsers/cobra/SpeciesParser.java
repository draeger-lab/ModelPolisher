package edu.ucsd.sbrg.io.parsers.cobra;

import de.zbit.util.ResourceManager;
import edu.ucsd.sbrg.db.bigg.BiGGId;
import edu.ucsd.sbrg.logging.BundleNames;
import edu.ucsd.sbrg.resolver.Registry;
import edu.ucsd.sbrg.resolver.identifiersorg.IdentifiersOrgURI;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCSpeciesPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.hebi.matlab.mat.types.Array;
import us.hebi.matlab.mat.types.Cell;

import javax.xml.stream.XMLStreamException;
import java.util.ResourceBundle;

import static java.text.MessageFormat.format;

public class SpeciesParser {

  private static final Logger logger = LoggerFactory.getLogger(SpeciesParser.class);
  public static final ResourceBundle MESSAGES = ResourceManager.getBundle(BundleNames.IO_MESSAGES);
  private final Model model;
  private final int index;
  private static MatlabFields matlabFields;

  private final Registry registry;

  public SpeciesParser(Model model, int index, Registry registry) {
    this.model = model;
    this.index = index;
    matlabFields = MatlabFields.getInstance();
    this.registry = registry;
  }


  public void parse() {
    matlabFields.getCell(ModelField.mets.name())
            .map(mets -> COBRAUtils.asString(mets.get(index), ModelField.mets.name(), index + 1))
            .map(BiGGId::createMetaboliteId)
            .ifPresent(biggId -> {
              Species species = model.createSpecies(biggId.toBiGGId());
              parseSpeciesFields(species);
              parseAnnotation(species);
            });
  }


  private void parseSpeciesFields(Species species) {
    matlabFields.getCell(ModelField.metNames.name()).ifPresent(
      metNames -> species.setName(COBRAUtils.asString(metNames.get(index), ModelField.metNames.name(), index + 1)));
    if (species.isSetPlugin(FBCConstants.shortLabel)) {
      FBCSpeciesPlugin specPlug = (FBCSpeciesPlugin) species.getPlugin(FBCConstants.shortLabel);
      matlabFields.getCell(ModelField.metFormulas.name()).ifPresent(metFormulas -> {
        if (metFormulas.get(index) != null) {
          specPlug.setChemicalFormula(
            COBRAUtils.asString(metFormulas.get(index), ModelField.metFormulas.name(), index + 1));
        }
      });
      matlabFields.getMatrix(ModelField.metFormulas.name()).ifPresent(metCharge -> {
        if (metCharge.getNumElements() > index) {
          double charge = metCharge.getDouble(index);
          specPlug.setCharge((int) charge);
          if (charge - ((int) charge) != 0d) {
            logger.debug(format(MESSAGES.getString("CHARGE_TO_INT_COBRA"), charge, specPlug.getCharge()));
          }
        }
      });
    }
    matlabFields.getCell(ModelField.metSmile.name()).ifPresent(metSmile -> {
      if (metSmile.get(index) != null) {
        String smile = COBRAUtils.asString(metSmile.get(index), ModelField.metSmile.name(), index + 1);
        if (!COBRAUtils.isEmptyString(smile)) {
          try {
            species.appendNotes("<html:p>SMILES: " + smile + "</html:p>");
          } catch (XMLStreamException exc) {
            throw new RuntimeException(exc);
          }
        }
      }
    });
    if (species.isSetAnnotation()) {
      species.setMetaId(species.getId());
    }
  }


  private void parseAnnotation(Species species) {
    CVTerm term = new CVTerm();
    term.setQualifierType(CVTerm.Type.BIOLOGICAL_QUALIFIER);
    term.setBiologicalQualifierType(CVTerm.Qualifier.BQB_IS);
    matlabFields.getCell(ModelField.metCHEBIID.name())
                .ifPresent(metCHEBIID -> addResource(metCHEBIID, index, term, "ChEBI"));
    matlabFields.getCell(ModelField.metHMDB.name()).ifPresent(metHMDB -> addResource(metHMDB, index, term, "HMDB"));
    matlabFields.getCell(ModelField.metInchiString.name())
                .ifPresent(metInchiString -> addResource(metInchiString, index, term, "InChI"));
    addKEGGResources(term, index);
    addPubChemResources(term, index);
    if (term.getResourceCount() > 0) {
      species.addCVTerm(term);
    }
  }


  /**
   * Adds resources to provided CVTerm if catalog and id from MLCell
   * provide a valid URI. Logs ids not matching namespaceName patterns
   * and invalid collections. In both cases no resource is added.
   *
   * @param i
   *        the index within the cell.
   */
  private boolean addResource(Cell cell, int i, CVTerm term, String namespaceName) {
    boolean success = false;
    if (cell.get(i) != null) {
      String id = toMIRIAMid((Array) cell.get(i));
      if (!COBRAUtils.isEmptyString(id)) {
        id = COBRAUtils.checkId(id);
        String prefix = registry.getPrefixByNamespaceName(namespaceName);
        if (!prefix.isEmpty()) {
          String resource;
          if (id.startsWith("http")) {
            resource = id;
          } else {
            resource = new IdentifiersOrgURI(prefix, id).getURI();
          }
          String finalId = id;
          success = registry.resolveBackwards(resource)
                  .map(res -> {
                    term.addResource(res.getURI());
                    logger.debug(format(MESSAGES.getString("ADDED_URI_COBRA"), res));
                    return true;
                  }).orElseGet(() -> {
                    logger.debug(format(MESSAGES.getString("ADD_URI_FAILED_COBRA"), namespaceName, finalId));
                    return false;
                  });
        }
      }
    }
    return success;
  }


  /**
   * @param array:
   *        MLArray to be stringified
   * @return String representation of the given array
   */
  private String toMIRIAMid(Array array) {
    return toMIRIAMid(COBRAUtils.asString(array));
  }


  /**
   * @param idCandidate:
   *        id part for the uri
   * @return finalized id, without [, ], ' at the string boundaries
   */
  private String toMIRIAMid(String idCandidate) {
    if ((idCandidate == null) || idCandidate.isEmpty()) {
      return null;
    }
    int start = 0;
    int end = idCandidate.length() - 1;
    if ((idCandidate.charAt(start) == '[') || (idCandidate.charAt(start) == '\'')) {
      start++;
    }
    if ((idCandidate.charAt(end) == ']') || (idCandidate.charAt(end) == '\'')) {
      end--;
    }
    // substring method already decrements second index
    return (start < end) ? idCandidate.substring(start, end + 1) : null;
  }


  private void addKEGGResources(CVTerm term, int i) {
    // use short circuit evaluation to only run addResource until one of them returns true
    // return type is needed for this to work
    matlabFields.getCell(ModelField.metKeggID.name())
                .map(metKeggID -> addResource(metKeggID, i, term, "KEGG Compound")
                  || addResource(metKeggID, i, term, "KEGG Drug") || addResource(metKeggID, i, term, "KEGG Genes")
                  || addResource(metKeggID, i, term, "KEGG Glycan") || addResource(metKeggID, i, term, "KEGG Pathway"));
  }


  private void addPubChemResources(CVTerm term, int i) {
    matlabFields.getCell(ModelField.metPubChemID.name())
                .map(metPubChemID -> addResource(metPubChemID, i, term, "PubChem-compound")
                  || addResource(metPubChemID, i, term, "PubChem-substance"));
  }
}
