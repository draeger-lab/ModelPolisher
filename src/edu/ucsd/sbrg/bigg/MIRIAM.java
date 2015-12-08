/**
 * 
 */
package edu.ucsd.sbrg.bigg;

import static org.sbml.jsbml.util.Pair.pairOf;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.sbml.jsbml.util.Pair;
import org.sbml.jsbml.util.StringTools;


/**
 * @author Andreas Dr&auml;ger
 *
 */
@SuppressWarnings("unchecked")
public enum MIRIAM {

  /**
   * 
   */
  ASAP(pairOf("asap", "^[A-Za-z0-9-]+$")),
  /**
   * 
   */
  //TODO
  BIOPATH(),
  /**
   * 
   */
  BRENDA(pairOf("brenda", "^((\\d+\\.-\\.-\\.-)|(\\d+\\.\\d+\\.-\\.-)|(\\d+\\.\\d+\\.\\d+\\.-)|(\\d+\\.\\d+\\.\\d+\\.\\d+))$")),
  /**
   * 
   */
  CAS(pairOf("cas", "^\\d{1,7}\\-\\d{2}\\-\\d$")),
  /**
   * 
   */
  CCDS(pairOf("ccds", "^CCDS\\d+\\.\\d+$")),
  /**
   * 
   */
  CHEBI(pairOf("chebi", "^CHEBI:\\d+$")),
  /**
   * 
   */
  EcoGene(pairOf("ecogene", "^EG\\d+$")),
  /**
   * 
   */
  //TODO: multiple choices: Bacteria, Fungi, Metazoa, Plants, Protists... -> lookup linage through taxon id?
  EnsemblGenomes(pairOf("ensembl.bacteria", "^((EB\\w+)|([A-Z0-9]+\\_[A-Z0-9]+))$"), pairOf("ensembl.fungi", "^[A-Z-a-z0-9]+$"), pairOf("ensembl.metazoa", "^\\w+(\\.)?\\d+$"), pairOf("ensembl.plant", "^\\w+(\\.\\d+)?(\\.\\d+)?$"), pairOf("ensembl.protist", "^\\w+$"), pairOf("ensembl", "^((ENS[A-Z]*[FPTG]\\d{11}(\\.\\d+)?)|(FB\\w{2}\\d{7})|(Y[A-Z]{2}\\d{3}[a-zA-Z](\\-[A-Z])?)|([A-Z_a-z0-9]+(\\.)?(t)?(\\d+)?([a-z])?))$")),
  /**
   * 
   */
  EnsemblGenomes_Gn(pairOf("ensembl.bacteria", "^((EB\\w+)|([A-Z0-9]+\\_[A-Z0-9]+))$"), pairOf("ensembl.fungi", "^[A-Z-a-z0-9]+$"), pairOf("ensembl.metazoa", "^\\w+(\\.)?\\d+$"), pairOf("ensembl.plant", "^\\w+(\\.\\d+)?(\\.\\d+)?$"), pairOf("ensembl.protist", "^\\w+$"), pairOf("ensembl", "^((ENS[A-Z]*[FPTG]\\d{11}(\\.\\d+)?)|(FB\\w{2}\\d{7})|(Y[A-Z]{2}\\d{3}[a-zA-Z](\\-[A-Z])?)|([A-Z_a-z0-9]+(\\.)?(t)?(\\d+)?([a-z])?))$")),
  /**
   * 
   */
  EnsemblGenomes_Tr(pairOf("ensembl.bacteria", "^((EB\\w+)|([A-Z0-9]+\\_[A-Z0-9]+))$"), pairOf("ensembl.fungi", "^[A-Z-a-z0-9]+$"), pairOf("ensembl.metazoa", "^\\w+(\\.)?\\d+$"), pairOf("ensembl.plant", "^\\w+(\\.\\d+)?(\\.\\d+)?$"), pairOf("ensembl.protist", "^\\w+$"), pairOf("ensembl", "^((ENS[A-Z]*[FPTG]\\d{11}(\\.\\d+)?)|(FB\\w{2}\\d{7})|(Y[A-Z]{2}\\d{3}[a-zA-Z](\\-[A-Z])?)|([A-Z_a-z0-9]+(\\.)?(t)?(\\d+)?([a-z])?))$")),
  /**
   * 
   */
  GeneID(pairOf("ncbigene", "^\\d+$")),
  /**
   * 
   */
  GI(pairOf("ncbigi", "^(GI|gi)\\:\\d+$")),
  /**
   * 
   */
  GO(pairOf("go", "^GO:\\d{7}$")),
  /**
   * 
   */
  GOA(pairOf("goa", "^([A-N,R-Z][0-9][A-Z][A-Z, 0-9][A-Z, 0-9][0-9])|([O,P,Q][0-9][A-Z, 0-9][A-Z, 0-9][A-Z, 0-9][0-9])$")),
  /**
   * 
   */
  HGNC(pairOf("hgnc", "^((HGNC|hgnc):)?\\d{1,5}$")),
  /**
   * 
   */
  HMDB(pairOf("hmdb", "^HMDB\\d{5}$")),
  /**
   * 
   */
  HPRD(pairOf("hprd", "^\\d+$")),
  /**
   * 
   */
  HSSP(pairOf("hssp", "^\\w{4}$")),
  /**
   * 
   */
  //TODO: unclear which one to pick: IMGT HLA or LIGM
  IMGT_GENE_DB(),
  /**
   * 
   */
  InterPro(pairOf("interpro", "^IPR\\d{6}$")),
  /**
   * 
   */
  KEGGID(
    pairOf("kegg.compound", "^C\\d+$"),
    pairOf("kegg.drug", "^D\\d+$"),
    pairOf("kegg.genes", "^\\w+:[\\w\\d\\.-]*$"),
    pairOf("kegg.glycan", "^G\\d+$"),
    pairOf("kegg.pathway", "^\\w{2,4}\\d{5}$"),
    pairOf("kegg.reaction", "^R\\d+$")),
    /**
     * 
     */
    LIPIDMAPS(pairOf("lipidmaps", "^LM(FA|GL|GP|SP|ST|PR|SL|PK)[0-9]{4}([0-9a-zA-Z]{4,6})?$")),
    /**
     * 
     */
    //TODO: BioCyc is not MetaCyc!
    METACYC(pairOf("biocyc", "^[A-Z-0-9]+(?<!CHEBI)(\\:)?[A-Za-z0-9+_.%-]+$")),
    /**
     * The Mouse Genome Database (MGD) project includes data on gene
     * characterization, nomenclature, mapping, gene homologies among mammals,
     * sequence links, phenotypes, allelic variants and mutants, and strain data.
     */
    MGI(pairOf("mgd", "^MGI:\\d+$")),
    /**
     * 
     */
    //TODO: unclear which database is meant.
    MIM(),
    /**
     * 
     */
    PDB(pairOf("pdb", "^[0-9][A-Za-z0-9]{3}$")),
    /**
     * 
     */
    //TODO: unclear, which database is meant.
    PSEUDO(),
    /**
     * 
     */
    //TODO: there is also http://identifiers.org/pubchem.substance/ - which one to choose?
    PUBCHEMID(pairOf("pubchem.compound", "^\\d{1,7}\\-\\d{2}\\-\\d$"), pairOf("pubchem.substance", "^\\d+$")),
    /**
     * SubtiList serves to collate and integrate various aspects of the genomic
     * information from <i>B. subtilis</i>, the paradigm of sporulating
     * Gram-positive bacteria. SubtiList provides a complete dataset of DNA and
     * protein sequences derived from the paradigm strain <i>B. subtilis</i> 168,
     * linked to the relevant annotations and functional assignments.
     */
    SubtiList(pairOf("subtilist", "^BG\\d+$")),
    /**
     * 
     */
    REACTOME(pairOf("reactome", "^REACT_\\d+(\\.\\d+)?$")),
    /**
     * 
     */
    REBASE(pairOf("rebase", "^\\d+$")),
    /**
     * 
     */
    SEED(pairOf("seed", "^\\d+\\.\\d+$"), pairOf("seed.compound", "^cpd\\d+$")),
    /**
     * 
     */
    SGD(pairOf("sgd", "^((S\\d+$)|(Y[A-Z]{2}\\d{3}[a-zA-Z](\\-[A-Z])?))$")),
    /**
     * 
     */
    TubercuList(pairOf("myco.tuber", "^Rv\\d{4}(A|B|c)?$")),
    /**
     * 
     */
    //TODO
    UniProtKB_Swiss_Prot(
      pairOf("uniprot.isoform", "^([A-N,R-Z][0-9][A-Z][A-Z, 0-9][A-Z, 0-9][0-9])|([O,P,Q][0-9][A-Z, 0-9][A-Z, 0-9][A-Z, 0-9][0-9])(\\-\\d+)$"),
      pairOf("uniprot", "^([A-N,R-Z][0-9]([A-Z][A-Z, 0-9][A-Z, 0-9][0-9]){1,2})|([O,P,Q][0-9][A-Z, 0-9][A-Z, 0-9][A-Z, 0-9][0-9])(\\.\\d+)?$"),
      pairOf("unipathway", "^UPA\\d{5}$"),
      pairOf("ipi", "^IPI\\d{8}$")),
      /**
       * 
       */
      //TODO this needs to be checked!!! Is this really the right resource?
      //TODO: needs different qualifier!
      UniProtKB_TrEMBL(
        pairOf("uniprot.isoform", "^([A-N,R-Z][0-9][A-Z][A-Z, 0-9][A-Z, 0-9][0-9])|([O,P,Q][0-9][A-Z, 0-9][A-Z, 0-9][A-Z, 0-9][0-9])(\\-\\d+)$"),
        pairOf("uniprot", "^([A-N,R-Z][0-9]([A-Z][A-Z, 0-9][A-Z, 0-9][0-9]){1,2})|([O,P,Q][0-9][A-Z, 0-9][A-Z, 0-9][A-Z, 0-9][0-9])(\\.\\d+)?$"),
        pairOf("unipathway", "^UPA\\d{5}$"),
        pairOf("ipi", "^IPI\\d{8}$")),
        /**
         * 
         */
        UPA(pairOf("unipathway.compound", "^UPC\\d{5}$"), pairOf("unipathway", "^UPA\\d{5}$"));

  /**
   * 
   */
  private static final transient Logger logger = Logger.getLogger(MIRIAM.class.getName());

  /**
   * 
   */
  private static final transient String urlPattern = "http://identifiers.org/%s/%s";

  /**
   * 
   * @param catalog
   * @param id
   * @return
   */
  public static String getURL(String catalog, String id) {
    try {
      MIRIAM c = MIRIAM.valueOf(catalog);
      return c.getURL(id);
    } catch (Throwable exc) {
    }
    return null;
  }

  /**
   * 
   * @param catalog
   * @return
   */
  public static MIRIAM toMIRIAM(String catalog) {
    String miriam = null;
    try {
      miriam = catalog.trim().replace(" ", "").replaceAll("[/-]", "_");
      if (miriam.equalsIgnoreCase("CASNUMBER") || miriam.equalsIgnoreCase("CASID")) {
        return CAS;
      } else if (miriam.equalsIgnoreCase("CHEBIID") || miriam.equalsIgnoreCase("ChEBI")) {
        return CHEBI;
      } else if (miriam.equalsIgnoreCase("KEGG")) {
        return KEGGID;
      } else if (miriam.equalsIgnoreCase("MetaCyc")) {
        return METACYC;
      } else if (miriam.equalsIgnoreCase("Reactome")) {
        return REACTOME;
      } else if (miriam.equalsIgnoreCase("BioPath")) {
        return BIOPATH;
      } else if (miriam.equalsIgnoreCase("NCBI Gene")) {
        return GeneID;
      }
      return MIRIAM.valueOf(miriam);
    } catch (Throwable t) {
    }
    logger.severe(MessageFormat.format("Unknown database ''{0}''.", catalog));
    return null;
  }

  /**
   * 
   * @param catalog
   * @param identifier
   * @return
   */
  public static String toResourceURL(MIRIAM catalog, String identifier) {
    if (catalog != null) {
      return catalog.getURL(catalog.checkId(identifier));
    }
    logger.severe(MessageFormat.format(
      "Entry ''{0}'' is not a valid identifier for database ''{1}''.",
      identifier, catalog));
    return null;
  }

  /**
   * 
   * @param catalog
   * @param identifier
   * @return
   */
  public static String toResourceURL(String catalog, String identifier) {
    return toResourceURL(toMIRIAM(catalog), identifier);
  }

  /**
   * 
   */
  private Map<String, Pattern> catalogToIdPatterns;

  /**
   * 
   * @param tpyes
   */
  private MIRIAM(Pair<String, String>... tpyes) {
    catalogToIdPatterns = new HashMap<String, Pattern>();
    if (tpyes != null) {
      for (Pair<String, String> type : tpyes) {
        catalogToIdPatterns.put(type.getKey(), Pattern.compile(type.getValue()));
      }
    }
  }

  /**
   * 
   * @return
   */
  public Set<String> getCatalogs() {
    return catalogToIdPatterns.keySet();
  }

  /**
   * 
   * @param catalog
   * @return
   */
  public Pattern getPattern(String catalog) {
    return catalogToIdPatterns.get(catalog);
  }

  /**
   * 
   * @return
   */
  public Map<String, Pattern> getCatalogsAndPatterns(){
    Map<String, Pattern> map = new HashMap<String, Pattern>();
    for (Map.Entry<String, Pattern> entry : catalogToIdPatterns.entrySet()) {
      map.put(entry.getKey(), entry.getValue());
    }
    return Collections.unmodifiableMap(map);
  }

  /**
   * 
   * @param id
   * @return
   */
  public String checkId(String id) {
    id = id.trim();
    switch (this) {
    case CHEBI:
      if (!id.startsWith("CHEBI:")) {
        return "CHEBI:" + id;
      }
      break;
    case GI:
      if (!id.startsWith("GI")) {
        return "GI:" + id;
      }
      break;
    case GO:
      if (!id.startsWith("GO:")) {
        return "GO:" + id;
      }
      break;
    case KEGGID:
      if ((id.length() > 1) && Character.isLowerCase(id.charAt(0))) {
        return StringTools.firstLetterUpperCase(id);
      }
      break;
    case REACTOME:
      if (!id.startsWith("REACT_")) {
        return "REACT_" + id;
      }
    default:
      break;
    }
    return id;
  }

  /**
   * 
   * @param id
   * @return
   */
  public String getURL(String id) {
    if (catalogToIdPatterns.size() == 0) {
      logger.fine(MessageFormat.format(
        "Database ''{0}'' is not registered in MIRIAM. Cannot address identifier ''{1}''.",
        this, id));
    } else {
      id = checkId(id);
      for (Map.Entry<String, Pattern> entry : catalogToIdPatterns.entrySet()) {
        if (entry.getValue().matcher(id).matches()) {
          return String.format(urlPattern, entry.getKey(), id);
        }
      }
      if (this != BRENDA) {
        logger.severe(MessageFormat.format("Invalid identifier ''{0}'' for database ''{1}''", id, this));
      }
    }
    return null;
  }

  /**
   * 
   * @param args
   */
  public static void main(String args[]) {
    System.out.println("Name | Catalog | URL | Pattern");
    System.out.println("---- | ------- | --- | -------");
    for (MIRIAM m : MIRIAM.values()) {
      for (String catalog : m.getCatalogs()) {
        System.out.println(m + " | " + catalog + " | " + "http://identifiers.org/" + catalog + "/ | " + m.getPattern(catalog));
      }
    }
  }

}
