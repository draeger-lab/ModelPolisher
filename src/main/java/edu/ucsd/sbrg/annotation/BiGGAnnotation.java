package edu.ucsd.sbrg.annotation;

import static java.text.MessageFormat.format;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.swing.tree.TreeNode;
import javax.xml.stream.XMLStreamException;

import edu.ucsd.sbrg.Parameters;
import edu.ucsd.sbrg.miriam.Registry;
import edu.ucsd.sbrg.util.GeneProductReferencesAnnotator;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.CVTerm.Qualifier;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.sbml.jsbml.util.Pair;

import de.zbit.util.ResourceManager;
import de.zbit.util.progressbar.AbstractProgressBar;
import de.zbit.util.progressbar.ProgressBar;
import edu.ucsd.sbrg.db.BiGGDB;
import edu.ucsd.sbrg.db.BiGGDBContract;
import edu.ucsd.sbrg.db.QueryOnce;


/**
 * This class is responsible for annotating SBML models using data from the BiGG database.
 * It handles the addition of annotations related to compartments, species, reactions, and gene products.
 * 
 * @author Thomas Zajac
 *         This code runs only, if ANNOTATE_WITH_BIGG is true
 */
public class BiGGAnnotation {

  /**
   * A {@link Logger} for this class.
   */
  static final transient Logger logger = Logger.getLogger(BiGGAnnotation.class.getName());
  /**
   * Localization support.
   */
  private static final transient ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");
  /**
   * Progressbar for the whole annotation process
   */
  private AbstractProgressBar progress;
  /**
   * Variable tracking gene product count for progress bar, as it changes dynamically during processing
   */
  private int initialGeneProducts;
  private final Parameters parameters;

  public BiGGAnnotation(Parameters parameters) {
      this.parameters = parameters;
  }
  

  /**
   * Annotates an SBMLDocument using data from the BiGG Knowledgebase. This method processes various components of the
   * SBML model such as compartments, species, reactions, and gene products by adding relevant annotations from BiGG.
   * It also handles the addition of publications and notes related to the model.
   *
   * @param doc The SBMLDocument that contains the model to be annotated.
   * @return The annotated SBMLDocument.
   */
  public SBMLDocument annotate(SBMLDocument doc) {
    if (!doc.isSetModel()) {
      logger.info(MESSAGES.getString("NO_MODEL_FOUND"));
      return doc;
    }
    Model model = doc.getModel();
    // Initialize the count for progress tracking, adding a buffer to ensure progress does not reach 100% prematurely, as new gene products are added
    // dynamically
    int count = model.getCompartmentCount() + model.getSpeciesCount() + model.getReactionCount() + 50;
    // Check for the FBC plugin and adjust the count based on the number of gene products
    if (model.isSetPlugin(FBCConstants.shortLabel)) {
      FBCModelPlugin fbcModelPlugin = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
      initialGeneProducts = fbcModelPlugin.getGeneProductCount();
      count += initialGeneProducts;
    }
    // Set up the progress bar for tracking annotation progress
    progress = new ProgressBar(count);
    // Process replacements for placeholders in the model notes
    Map<String, String> replacements = processReplacements(model);
    // Annotate the model with general information
    ModelAnnotation modelAnnotation = new ModelAnnotation(model, parameters);
    modelAnnotation.annotate();
    // Annotate various components of the model
    annotatePublications(model);
    annotateListOfCompartments(model);
    annotateListOfSpecies(model);
    annotateListOfReactions(model);
    annotateListOfGeneProducts(model);
    // Append notes to the document, handling potential I/O and XML exceptions
    try {
      appendNotes(doc, replacements);
    } catch (IOException | XMLStreamException exc) {
      logger.warning(MESSAGES.getString("FAILED_WRITE_NOTES"));
    }
    // Merge all MIRIAM annotations to ensure they are correctly grouped and sorted
    mergeMIRIAMannotations(doc);
    // Finalize the progress once all tasks are completed
    if (progress != null) {
      progress.finished();
    }
    return doc;
  }


  /**
   * Processes and replaces placeholders in the document title pattern and model notes with actual values from the model.
   * This method retrieves the model ID and organism information from the BiGG database, and uses these along with
   * other parameters to populate a map of replacements. These replacements are used later to substitute placeholders
   * in the SBMLDocument notes.
   *
   * @param model The {@link Model} contained within the {@link SBMLDocument} passed to {@link #annotate(SBMLDocument)}.
   * @return A map of placeholder strings and their corresponding replacement values.
   */
  private Map<String, String> processReplacements(Model model) {
    // Retrieve the model ID
    String id = model.getId();
    // Attempt to retrieve the organism name associated with the model ID; use an empty string if not available
    String organism = BiGGDB.getOrganism(id).orElse("");
    // Retrieve and process the document title pattern by replacing placeholders
    String name = parameters.documentTitlePattern();
    name = name.replace("[biggId]", id);
    name = name.replace("[organism]", organism);
    // Initialize a map to hold the replacement values
    Map<String, String> replacements = new HashMap<>();
    replacements.put("${organism}", organism);
    replacements.put("${title}", name);
    replacements.put("${bigg_id}", id);
    replacements.put("${year}", Integer.toString(Calendar.getInstance().get(Calendar.YEAR)));
    replacements.put("${bigg.timestamp}", BiGGDB.getBiGGVersion().map(date -> format("{0,date}", date)).orElse(""));
    replacements.put("${species_table}", "");
    // Set the model name to the organism name if it is not already set
    if (!model.isSetName()) {
      model.setName(organism);
    }
    return replacements;
  }


  /**
   * This method appends notes to the SBMLDocument and its model by replacing placeholders in the notes files.
   * It handles both model-specific notes and document-wide notes.
   *
   * @param doc The SBMLDocument to which the notes will be appended.
   * @param replacements A map containing the placeholder text and their replacements.
   * @throws IOException If there is an error reading the notes files or writing to the document.
   * @throws XMLStreamException If there is an error processing the XML content of the notes.
   */
  private void appendNotes(SBMLDocument doc, Map<String, String> replacements) throws IOException, XMLStreamException {
    String modelNotesFile = "ModelNotes.html";
    String documentNotesFile = "SBMLDocumentNotes.html";
    
    // Determine the files to use for model and document notes based on user settings
    if (parameters.noModelNotes()) {
      modelNotesFile = null;
      documentNotesFile = null;
    } else {
      if (parameters.modelNotesFile() != null) {
        File modelNotes = parameters.modelNotesFile();
        modelNotesFile = modelNotes != null ? modelNotes.getAbsolutePath() : null;
      }
      if (parameters.documentNotesFile() != null) {
        File documentNotes = parameters.documentNotesFile();
        documentNotesFile = documentNotes != null ? documentNotes.getAbsolutePath() : null;
      }
    }
    
    // Append document notes if the title placeholder is present and the notes file is specified
    if (replacements.containsKey("${title}") && (documentNotesFile != null)) {
      doc.appendNotes(parseNotes(documentNotesFile, replacements));
    }
    
    // Append model notes if the notes file is specified
    if (modelNotesFile != null) {
      doc.getModel().appendNotes(parseNotes(modelNotesFile, replacements));
    }
  }


  /**
   * Recursively goes through all annotations in the given {@link SBase} and
   * alphabetically sort annotations after grouping them by {@link org.sbml.jsbml.CVTerm.Qualifier}.
   *
   * @param sbase:
   *        {@link SBase} to start the merging process at, corresponding to an instance of {@link SBMLDocument} here,
   *        though also used to pass current {@link SBase} during recursion
   */
  private void mergeMIRIAMannotations(SBase sbase) {
    if (sbase.isSetAnnotation()) {
      SortedMap<Qualifier, SortedSet<String>> miriam = new TreeMap<>();
      boolean doMerge = hashMIRIAMuris(sbase, miriam);
      if (doMerge) {
        sbase.getAnnotation().unsetCVTerms();
        for (Entry<Qualifier, SortedSet<String>> entry : miriam.entrySet()) {
          logger.info(format(MESSAGES.getString("MERGING_MIRIAM_RESOURCES"), entry.getKey(),
            sbase.getClass().getSimpleName(), sbase.getId()));
          sbase.addCVTerm(new CVTerm(entry.getKey(), entry.getValue().toArray(new String[0])));
        }
      }
    }
    for (int i = 0; i < sbase.getChildCount(); i++) {
      TreeNode node = sbase.getChildAt(i);
      if (node instanceof SBase) {
        mergeMIRIAMannotations((SBase) node);
      }
    }
  }


  /**
   * Evaluates and merges CVTerm annotations for a given SBase element. This method checks each CVTerm associated with
   * the SBase and determines if there are multiple CVTerms with the same Qualifier that need merging. It also corrects
   * invalid qualifiers based on the type of SBase (Model or other biological elements).
   *
   * @param sbase The SBase element whose annotations are to be evaluated and potentially merged.
   * @param miriam A sorted map that groups CVTerm resources by their qualifiers.
   * @return true if there are CVTerms with the same qualifier that need to be merged, false otherwise.
   */
  private boolean hashMIRIAMuris(SBase sbase, SortedMap<Qualifier, SortedSet<String>> miriam) {
    boolean doMerge = false;
    for (int i = 0; i < sbase.getCVTermCount(); i++) {
      CVTerm term = sbase.getCVTerm(i);
      Qualifier qualifier = term.getQualifier();
      if (miriam.containsKey(qualifier)) {
        doMerge = true;
      } else {
        if (sbase instanceof Model) {
          if (!qualifier.isModelQualifier()) {
            logger.info(format(MESSAGES.getString("CORRECTING_INVALID_QUALIFIERS"),
              qualifier.getElementNameEquivalent(), sbase.getId()));
            qualifier = Qualifier.getModelQualifierFor(qualifier.getElementNameEquivalent());
          }
        } else if (!qualifier.isBiologicalQualifier()) {
          logger.info(format(MESSAGES.getString("CORRECTING_INVALID_MODEL_QUALIFIER"),
            qualifier.getElementNameEquivalent(), sbase.getClass().getSimpleName(), sbase.getId()));
          qualifier = Qualifier.getBiologicalQualifierFor(qualifier.getElementNameEquivalent());
        }
        miriam.put(qualifier, new TreeSet<>());
      }
      miriam.get(qualifier).addAll(term.getResources());
    }
    return doMerge;
  }


  /**
   * This method annotates a given {@link Model} with publication references retrieved from the BiGG database.
   * It is specifically designed to work with models that are part of the BiGG database. The method first checks
   * if the model exists in the BiGG database. If it does, it retrieves a list of publications associated with
   * the model's ID. Each publication is then converted into a URI and added to the model as a {@link CVTerm}
   * with the qualifier {@link CVTerm.Qualifier#BQM_IS_DESCRIBED_BY}.
   *
   * @param model the {@link Model} to be annotated, which is contained within an {@link SBMLDocument} passed to {@link #annotate(SBMLDocument)}
   */
  private void annotatePublications(Model model) {
    progress.DisplayBar("Annotating Publications (1/5)  ");
    String id = model.getId();
    if (!QueryOnce.isModel(id)) {
      return;
    }
    List<Pair<String, String>> publications = BiGGDB.getPublications(id);
    int numPublications;
    if ((numPublications = publications.size()) > 0) {
      String[] resources = new String[numPublications];
      int i = 0;
      for (Pair<String, String> publication : publications) {
        resources[i++] = Registry.createURI(publication.getKey(), publication.getValue());
      }
      model.addCVTerm(new CVTerm(CVTerm.Qualifier.BQM_IS_DESCRIBED_BY, resources));
    }
  }


  /**
   * This method delegates the annotation processing for each compartment within the provided {@link Model}.
   * It iterates over all compartments in the model, updating the progress display and invoking the annotation
   * process for each compartment.
   *
   * @param model the {@link Model} contained within the {@link SBMLDocument} that is passed to {@link #annotate(SBMLDocument)}
   */
  private void annotateListOfCompartments(Model model) {
    for (Compartment compartment : model.getListOfCompartments()) {
      progress.DisplayBar("Annotating Compartments (2/5)  ");
      CompartmentAnnotation compartmentAnnotation = new CompartmentAnnotation(compartment);
      compartmentAnnotation.annotate();
    }
  }


  /**
   * Delegates annotation processing for all chemical species contained in the {@link Model}.
   * This method iterates over each species in the model and applies specific annotations.
   *
   * @param model {@link Model} contained within {@link SBMLDocument} passed to {@link #annotate(SBMLDocument)}
   */
  private void annotateListOfSpecies(Model model) {
    for (Species species : model.getListOfSpecies()) {
      progress.DisplayBar("Annotating Species (3/5)  ");
      SpeciesAnnotation speciesAnnotation = new SpeciesAnnotation(species, parameters);
      speciesAnnotation.annotate();
    }
  }

  
  /**
   * Attempts to extract a BiGG ID that conforms to the BiGG ID specification from the BiGG knowledgebase. This method
   * processes annotations for biological entities such as {@link Species}, {@link Reaction}, or {@link GeneProduct}.
   * Each entity's annotations are provided as a list of URIs, which are then parsed to retrieve the BiGG ID.
   *
   * @param resources A list of URIs containing annotations for the biological entity.
   * @param type The type of the biological entity, which can be one of the following:
   *             {@link BiGGDBContract.Constants#TYPE_SPECIES}, {@link BiGGDBContract.Constants#TYPE_REACTION}, or
   *             {@link BiGGDBContract.Constants#TYPE_GENE_PRODUCT}.
   * @return An {@link Optional<String>} containing the BiGG ID if it could be successfully retrieved, otherwise {@link Optional#empty()}.
   */
  public static Optional<String> getBiGGIdFromResources(List<String> resources, String type) {
    for (String resource : resources) {
      Optional<String> id = Registry.checkResourceUrl(resource)
                                    .map(Registry::getPartsFromIdentifiersURI)
                                    .flatMap(parts -> getBiggIdFromParts(parts, type));
      if (id.isPresent()) {
        return id;
      }
    }
    return Optional.empty();
  }


  /**
   * Attempts to retrieve a BiGG identifier from the BiGG Knowledgebase using a given prefix and identifier. This method
   * is used for specific biological entities such as species, reactions, or gene products.
   *
   * @param parts A list containing two elements: the prefix and the identifier, both extracted from an identifiers.org URI.
   * @param type  The type of biological entity for which the ID is being retrieved. Valid types are defined in
   *              {@link BiGGDBContract.Constants} and include TYPE_SPECIES, TYPE_REACTION, and TYPE_GENE_PRODUCT.
   * @return An {@link Optional<String>} containing the BiGG ID if found, otherwise {@link Optional#empty()}.
   */
  private static Optional<String> getBiggIdFromParts(List<String> parts, String type) {
    String prefix = parts.get(0);
    String synonymId = parts.get(1);
    if (QueryOnce.isDataSource(prefix)) {
      Optional<String> id = BiGGDB.getBiggIdFromSynonym(prefix, synonymId, type);
      if (id.isPresent()) {
        return id;
      }
    }
    return Optional.empty();
  }


  /**
   * Delegates the annotation process for each reaction in the given SBML model.
   * This method iterates over all reactions in the model, updates the progress display,
   * and invokes the annotation for each reaction.
   *
   * @param model The SBML model containing reactions to be annotated. It is part of the {@link SBMLDocument} passed to {@link #annotate(SBMLDocument)}.
   */
  private void annotateListOfReactions(Model model) {
    for (Reaction reaction : model.getListOfReactions()) {
      progress.DisplayBar("Annotating Reactions (4/5)  ");
      ReactionAnnotation reactionAnnotation = new ReactionAnnotation(reaction, parameters);
      reactionAnnotation.annotate();
    }
  }


  /**
   * This method handles the annotation of gene products in a given SBML model. It checks if the model has the FBC plugin
   * set and then proceeds to annotate each gene product found within the model. The progress bar is updated to reflect
   * the number of gene products being annotated.
   *
   * @param model The SBML model containing gene products to be annotated. It must be an instance of {@link Model} 
   *              contained within an {@link SBMLDocument} that is passed to {@link #annotate(SBMLDocument)}.
   */
  private void annotateListOfGeneProducts(Model model) {
    // Check if the FBC plugin is set in the model
    if (model.isSetPlugin(FBCConstants.shortLabel)) {
      FBCModelPlugin fbcModelPlugin = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
      
      // Calculate the change in the number of gene products to update the progress bar accordingly
      int changed = fbcModelPlugin.getNumGeneProducts() - initialGeneProducts;
      if (changed > 0) {
        long current = progress.getCallNumber();
        // Adjust the total number of calls for the progress bar by subtracting the placeholder count
        progress.setNumberOfTotalCalls(progress.getNumberOfTotalCalls() + changed - 50);
        progress.setCallNr(current);
      }

      var gprAnnotator = new GeneProductReferencesAnnotator();
      // Iterate over each gene product and annotate it
      for (GeneProduct geneProduct : fbcModelPlugin.getListOfGeneProducts()) {
        progress.DisplayBar("Annotating Gene Products (5/5)  ");
        GeneProductAnnotation geneProductAnnotation = new GeneProductAnnotation(geneProduct, gprAnnotator, parameters);
        geneProductAnnotation.annotate();
      }
    }
  }


  /**
   * Parses the notes from a specified location and replaces placeholder tokens with actual values.
   * This method first attempts to read the resource from the classpath. If the resource is not found,
   * it falls back to reading from the filesystem. It processes the content line by line, starting to
   * append lines to the result after encountering a `<body>` tag and stopping after a `</body>` tag.
   * Any placeholders in the format `${placeholder}` found within the body are replaced with corresponding
   * values provided in the `replacements` map.
   *
   * @param location The relative path to the resource from this class.
   * @param replacements A map of placeholder tokens to their actual values to be replaced in the notes.
   * @return A string containing the processed notes with placeholders replaced by actual values.
   * @throws IOException If an I/O error occurs while reading the file.
   */
  private String parseNotes(String location, Map<String, String> replacements) throws IOException {
    StringBuilder sb = new StringBuilder();
    try (InputStream is = getClass().getResourceAsStream(location);
        InputStreamReader isReader = new InputStreamReader((is != null) ? is : new FileInputStream(new File(location)));
        BufferedReader br = new BufferedReader(isReader)) {
      String line;
      boolean start = false;
      while (br.ready() && ((line = br.readLine()) != null)) {
        if (line.matches("\\s*<body.*")) {
          start = true;
        }
        if (!start) {
          continue;
        }
        if (line.matches(".*\\$\\{.*}.*")) {
          for (String key : replacements.keySet()) {
            line = line.replace(key, replacements.get(key));
          }
        }
        sb.append(line);
        sb.append('\n');
        if (line.matches("\\s*</body.*")) {
          break;
        }
      }
    }
    return sb.toString();
  }
}
