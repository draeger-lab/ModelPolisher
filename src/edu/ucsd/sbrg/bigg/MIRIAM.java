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
  asap(pairOf("asap", "^[A-Za-z0-9-]+$")),
  /**
   * https://www.molecular-networks.com/biopath3/biopath/mols/
   * TODO: what is the actual pattern? using any character!
   */
  biopath_molecule(pairOf("https://www.molecular-networks.com/biopath3/biopath/mols/", ".*")),
  /**
   * https://www.molecular-networks.com/biopath3/biopath/rxn/
   * TODO: what is the actual pattern? using any character!
   */
  biopath_reaction(pairOf("https://www.molecular-networks.com/biopath3/biopath/rxn/", ".*")),
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
  ccds(pairOf("ccds", "^CCDS\\d+\\.\\d+$")),
  /**
   * 
   */
  chebi(pairOf("chebi", "^CHEBI:\\d+$")),
  /**
   * 
   */
  ecogene(pairOf("ecogene", "^EG\\d+$")),
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
  go(pairOf("go", "^GO:\\d{7}$")),
  /**
   * 
   */
  goa(pairOf("goa", "^([A-N,R-Z][0-9][A-Z][A-Z, 0-9][A-Z, 0-9][0-9])|([O,P,Q][0-9][A-Z, 0-9][A-Z, 0-9][A-Z, 0-9][0-9])$")),
  /**
   * 
   */
  hgnc(pairOf("hgnc", "^((HGNC|hgnc):)?\\d{1,5}$")),
  /**
   * 
   */
  hdmb(pairOf("hmdb", "^HMDB\\d{5}$")),
  /**
   * 
   */
  hprd(pairOf("hprd", "^\\d+$")),
  /**
   * 
   */
  HSSP(pairOf("hssp", "^\\w{4}$")),
  /**
   * 
   */
  interpro(pairOf("interpro", "^IPR\\d{6}$")),
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
    lipidmaps(pairOf("lipidmaps", "^LM(FA|GL|GP|SP|ST|PR|SL|PK)[0-9]{4}([0-9a-zA-Z]{4,6})?$")),
    /**
     * MetaNetx integrates various information from genome-scale metabolic network
     * reconstructions such as information on reactions, metabolites and
     * compartments. This information undergoes a reconciliation process to
     * minimise for discrepancies between different data sources, and makes the
     * data accessible under a common namespace. This collection references
     * chemical or metabolic components.
     */
    mnx_chemical(pairOf("metanetx.chemical", "^MNXM\\d+$")),
    /**
     * MetaNetx integrates various information from genome-scale metabolic network
     * reconstructions such as information on reactions, metabolites and
     * compartments. This information undergoes a reconciliation process to
     * minimise for discrepancies between different data sources, and makes the
     * data accessible under a common namespace. This collection references
     * reactions.
     */
    mnx_equation(pairOf("metanetx.reaction", "^MNXR\\d+$")),
    /**
     * 
     */
    biocyc(pairOf("biocyc", "^[A-Z-0-9]+(?<!CHEBI)(\\:)?[A-Za-z0-9+_.%-]+$")),
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
     * Protein Data Bank
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
    subtilist(pairOf("subtilist", "^BG\\d+$")),
    /**
     * Reactome
     */
    reactome(pairOf("reactome", "^REACT_\\d+(\\.\\d+)?$")),
    /**
     * RHEA
     */
    rhea(pairOf("rhea", "^\\d{5}$")),
    /**
     * 
     */
    REBASE(pairOf("rebase", "^\\d+$")),
    /**
     * The Reference Sequence (RefSeq) collection aims to provide a comprehensive,
     * integrated, non-redundant set of sequences, including genomic DNA,
     * transcript (RNA), and protein products.
     */
    RefSeq(pairOf("refseq", "^((AC|AP|NC|NG|NM|NP|NR|NT|NW|XM|XP|XR|YP|ZP)_\\d+|(NZ\\_[A-Z]{4}\\d+))(\\.\\d+)?$")),
    /**
     * Online Mendelian Inheritance in Man a catalog of human genes and genetic disorders.
     */
    omim(pairOf("omim", "^[*#+%^]?\\d{6}$")),
    /**
     * 
     */
    seed(pairOf("seed", "^\\d+\\.\\d+$"), pairOf("seed.compound", "^cpd\\d+$")),
    /**
     * 
     */
    sgd(pairOf("sgd", "^((S\\d+$)|(Y[A-Z]{2}\\d{3}[a-zA-Z](\\-[A-Z])?))$")),
    /**
     * 
     */
    TubercuList(pairOf("myco.tuber", "^Rv\\d{4}(A|B|c)?$")),
    /**
     * The University of Minnesota Biocatalysis/Biodegradation Database (UM-BBD)
     * contains information on microbial biocatalytic reactions and biodegradation
     * pathways for primarily xenobiotic, chemical compounds. The goal of the
     * UM-BBD is to provide information on microbial enzyme-catalyzed reactions
     * that are important for biotechnology. This collection refers to compound
     * information.
     */
    umbbd_compound(pairOf("umbbd.compound", "^c\\d+$")),
    /**
     * 
     */
    //TODO
    UniProtKB_Swiss_Prot(
      pairOf("uniprot.isoform", "^([A-N,R-Z][0-9][A-Z][A-Z, 0-9][A-Z, 0-9][0-9])|([O,P,Q][0-9][A-Z, 0-9][A-Z, 0-9][A-Z, 0-9][0-9])(\\-\\d+)$"),
      pairOf("uniprot", "^([A-N,R-Z][0-9]([A-Z][A-Z, 0-9][A-Z, 0-9][0-9]){1,2})|([O,P,Q][0-9][A-Z, 0-9][A-Z, 0-9][A-Z, 0-9][0-9])(\\.\\d+)?$"),
      pairOf("ipi", "^IPI\\d{8}$")),

      /**
       * http://www.grenoble.prabi.fr/obiwarehouse/unipathway/ucr?upid=
       * TODO What is the actual pattern? Using any character!
       */
      unipathway_reaction(pairOf("http://www.grenoble.prabi.fr/obiwarehouse/unipathway/ucr?upid=", ".*")),
      /**
       * UniPathway is a manually curated resource of enzyme-catalyzed and
       * spontaneous chemical reactions. It provides a hierarchical representation
       * of metabolic pathways and a controlled vocabulary for pathway annotation in
       * UniProtKB. UniPathway data are cross-linked to existing metabolic resources
       * such as ChEBI/Rhea, KEGG and MetaCyc. This collection references compounds.
       */
      uniprot(pairOf("uniprot", "^([A-N,R-Z][0-9]([A-Z][A-Z, 0-9][A-Z, 0-9][0-9]){1,2})|([O,P,Q][0-9][A-Z, 0-9][A-Z, 0-9][A-Z, 0-9][0-9])(\\.\\d+)?$")),
      /**
       * 
       */
      UPA(pairOf("unipathway.compound", "^UPC\\d{5}$"), pairOf("unipathway", "^UPA\\d{5}$")),
      /**
       * See http://www.ebi.ac.uk/miriam/main/collections/MIR:00000383
       */
      INCHI(pairOf("inchi", "^InChI\\=1S\\/[A-Za-z0-9]+(\\/[cnpqbtmsih][A-Za-z0-9\\-\\+\\(\\)\\,]+)+$")),
      /**
       * See http://www.ebi.ac.uk/miriam/main/collections/MIR:00000019
       */
      DOI(pairOf("doi", "^(doi\\:)?\\d{2}\\.\\d{4}.*$")),
      /**
       * See http://www.ebi.ac.uk/miriam/main/collections/MIR:00000015
       */
      PUBMED(pairOf("pubmed", "^\\d+$")),
      /**
       * See http://www.ebi.ac.uk/miriam/main/collections/MIR:00000004
       */
      EC_CODE(pairOf("ec-code", "^\\d+\\.-\\.-\\.-|\\d+\\.\\d+\\.-\\.-|\\d+\\.\\d+\\.\\d+\\.-|\\d+\\.\\d+\\.\\d+\\.(n)?\\d+$"));

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
   */
  private static final Map<String, MIRIAM> catalog2MIRIAM = new HashMap<String, MIRIAM>();

  /**
   * 
   * @param catalog
   * @return
   */
  public static MIRIAM toMIRIAM(String catalog) {
    MIRIAM m = catalog2MIRIAM.get(catalog);
    if (m == null) {
      logger.severe(MessageFormat.format("Unknown database ''{0}''.", catalog));
    }
    return m;
  }

  /**
   * 
   * @param catalog
   * @param identifier
   * @return
   */
  public static String toResourceURL(MIRIAM catalog, String identifier) {
    if (catalog != null) {
      return catalog.getURL(identifier);
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

  static {
    for (MIRIAM m : MIRIAM.values()) {

      String miriam = m.toString().trim().replace(" ", "").replaceAll("[/-]", "_");
      switch (m) {
      case CAS:
        catalog2MIRIAM.put("CASNUMBER", m);
        catalog2MIRIAM.put("CASID", m);
        break;
      case chebi:
        catalog2MIRIAM.put("CHEBIID", m);
        catalog2MIRIAM.put("ChEBI", m);
        break;
      case EC_CODE:
        catalog2MIRIAM.put("ec_code", m);
        break;
      case biopath_molecule:
        catalog2MIRIAM.put("BioPath.molecule", m);
        break;
      case biopath_reaction:
        catalog2MIRIAM.put("BioPath.reaction", m);
        break;
      case KEGGID:
        catalog2MIRIAM.put("KEGG", m);
        break;
      case GeneID:
        catalog2MIRIAM.put("NCBIGene", m);
        break;
      default:
        break;
      }

      catalog2MIRIAM.put(miriam, m);
      if (m.catalogToIdPatterns != null) {
        for (String catalog : m.getCatalogs()) {
          catalog2MIRIAM.put(catalog, m);
        }
      }
    }
  }

  /**
   * 
   */
  private MIRIAM() {
  }

  /**
   * 
   * @param tpyes
   */
  private MIRIAM(Pair<String, String>... tpyes) {
    this();
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
    case chebi:
      if (!id.startsWith("CHEBI:")) {
        return "CHEBI:" + id;
      }
      break;
    case GI:
      if (!id.startsWith("GI")) {
        return "GI:" + id;
      }
      break;
    case go:
      if (!id.startsWith("GO:")) {
        return "GO:" + id;
      }
      break;
    case KEGGID:
      if ((id.length() > 1) && Character.isLowerCase(id.charAt(0))) {
        return StringTools.firstLetterUpperCase(id);
      }
      break;
    case reactome:
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
    if ((catalogToIdPatterns == null) || (catalogToIdPatterns.size() == 0)) {
      logger.fine(MessageFormat.format(
        "Database ''{0}'' is not registered in MIRIAM. Cannot address identifier ''{1}''.",
        this, id));
    } else {
      id = checkId(id);
      switch (this) {
      case biopath_molecule:
      case biopath_reaction:
      case unipathway_reaction:
        // TODO: In these cases we don't have patterns and cannot guarantee the correctness of the id!
        return catalogToIdPatterns.keySet().iterator().next().toString() + id;
      default:
        for (Map.Entry<String, Pattern> entry : catalogToIdPatterns.entrySet()) {
          if (entry.getValue().matcher(id).matches()) {
            return String.format(urlPattern, entry.getKey(), id);
          }
        }
        if (this != BRENDA) {
          logger.severe(MessageFormat.format("Invalid identifier ''{0}'' for database ''{1}''", id, this));
        }
        break;
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
