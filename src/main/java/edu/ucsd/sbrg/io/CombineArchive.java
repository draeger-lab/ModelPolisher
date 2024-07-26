package edu.ucsd.sbrg.io;

import de.unirostock.sems.cbarchive.CombineArchiveException;
import de.zbit.util.ResourceManager;
import org.jdom2.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.sbml.jsbml.xml.XMLNode;
import org.sbml.jsbml.xml.parsers.SBMLRDFAnnotationParser;
import org.w3c.tidy.Tidy;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

/**
 * The {@code CombineArchive} class provides functionality to create a COMBINE archive from an SBML document.
 * It supports writing an RDF glossary derived from the SBML document and packaging both the SBML file and its
 * RDF glossary into a single COMBINE archive file. This class handles the creation, formatting, and management
 * of files necessary for the archive, ensuring they adhere to the COMBINE specification.
 *
 * <p>Key functionalities include:</p>
 * <ul>
 *   <li>Generating RDF glossary from the SBML document annotations.</li>
 *   <li>Writing the RDF glossary to a file with proper formatting using JTidy.</li>
 *   <li>Creating a COMBINE archive that includes the SBML file and the RDF glossary.</li>
 *   <li>Handling file operations such as checking for existing files, deleting, and writing new files.</li>
 * </ul>
 *
 * <p>This class is essential for users looking to export SBML models and their annotations in a standardized
 * archive format that can be easily shared and processed by various bioinformatics tools.</p>
 */
public class CombineArchive {

  private static final Logger logger = Logger.getLogger(CombineArchive.class.getName());
  private static final ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");
  public static final String COMBINE_SPECIFICATION = "https://identifiers.org/combine.specifications/sbml";
  public static final String RDF_MEDIATYPE = "https://purl.org/NET/mediatypes/application/rdf+xml";

  private final SBMLDocument doc;
  private final File output;

  public CombineArchive(SBMLDocument doc, File output) {
    this.doc = doc;
    this.output = output;
  }


  public File write() throws IOException, XMLStreamException, ParseException, URISyntaxException, JDOMException, CombineArchiveException, TransformerException {
    writeGlossary();
    return writeCombineArchive();
  }


  /**
   * Writes the RDF glossary to a file. The glossary is generated from the SBML document associated with this instance.
   * The RDF file is named based on the output file's name with "_glossary.rdf" appended.
   *
   * @throws XMLStreamException if there is an error in generating the glossary from the SBML document.
   * @throws IOException if there is an error writing the glossary to the file system.
   */
  private void writeGlossary() throws XMLStreamException, IOException {
    // Generate the glossary from the SBML document
    String glossary = getGlossary(doc);
    // Determine the location for the RDF file
    String glossaryLocation =
      output.getAbsolutePath().substring(0, output.getAbsolutePath().lastIndexOf('.')) + "_glossary.rdf";
    // Log the location where the RDF file will be written
    logger.info(format(MESSAGES.getString("WRITE_RDF_FILE_INFO"), glossaryLocation));
    // Write the glossary to the specified RDF file
    Tidy tidy = tidyParser();
    try (var out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(glossaryLocation), StandardCharsets.UTF_8));
         var in = new InputStreamReader(new ByteArrayInputStream(glossary.getBytes(StandardCharsets.UTF_8)),
                 StandardCharsets.UTF_8)) {
      tidy.parse(in, out);
    }
  }

  private @NotNull Tidy tidyParser() {
    Tidy tidy = new Tidy(); // obtain a new Tidy instance
    tidy.setDropEmptyParas(false);
    tidy.setHideComments(false);
    tidy.setIndentContent(true);
    tidy.setInputEncoding("UTF-8");
    tidy.setOutputEncoding("UTF-8");
    tidy.setQuiet(true);
    tidy.setSmartIndent(true);
    tidy.setTrimEmptyElements(true);
    tidy.setWraplen(0);
    tidy.setWrapAttVals(false);
    tidy.setWrapScriptlets(true);
    tidy.setLiteralAttribs(true);
    tidy.setXmlOut(true);
    tidy.setXmlSpace(true);
    tidy.setXmlTags(true);
    tidy.setSpaces(2);
    return tidy;
  }


  /**
   * Generates an RDF glossary for the given SBMLDocument. This method parses the document's model
   * and its components (species, reactions, compartments, and gene products) to construct an RDF
   * representation of annotations. If the model or any essential components are not annotated,
   * it returns an empty string.
   *
   * @param doc The SBMLDocument for which the glossary is to be generated.
   * @return A string containing the RDF glossary in XML format, or an empty string if the model
   *         or required annotations are missing.
   * @throws XMLStreamException If there is an error during the XML processing.
   */
  private String getGlossary(SBMLDocument doc) throws XMLStreamException {
    SBMLRDFAnnotationParser rdfParser = new SBMLRDFAnnotationParser();
    if (rdfParser.writeAnnotation(doc.getModel(), null) != null
      && rdfParser.writeAnnotation(doc.getModel(), null).getChild(1) != null) {
      XMLNode node = rdfParser.writeAnnotation(doc.getModel(), null).getChild(1);
      for (Species s : doc.getModel().getListOfSpecies()) {
        XMLNode tempNode = rdfParser.writeAnnotation(s, null);
        if (tempNode != null && tempNode.getChildCount() != 0) {
          node.addChild(tempNode.getChild(1));
        }
      }
      for (Reaction r : doc.getModel().getListOfReactions()) {
        XMLNode tempNode = rdfParser.writeAnnotation(r, null);
        if (tempNode != null && tempNode.getChildCount() != 0) {
          node.addChild(tempNode.getChild(1));
        }
      }
      for (Compartment c : doc.getModel().getListOfCompartments()) {
        XMLNode tempNode = rdfParser.writeAnnotation(c, null);
        if (tempNode != null && tempNode.getChildCount() != 0) {
          node.addChild(tempNode.getChild(1));
        }
      }
      if (doc.getModel().isSetPlugin(FBCConstants.shortLabel)) {
        FBCModelPlugin fbcModelPlugin = (FBCModelPlugin) doc.getModel().getPlugin(FBCConstants.shortLabel);
        for (GeneProduct gP : fbcModelPlugin.getListOfGeneProducts()) {
          XMLNode tempNode = rdfParser.writeAnnotation(gP, null);
          if (tempNode != null && tempNode.getChildCount() != 0) {
            node.addChild(tempNode.getChild(1));
          }
        }
      }
      return node.toXMLString();
    } else {
      return "";
    }
  }


  /**
   * Creates a COMBINE archive containing the SBML model and its associated RDF glossary.
   * The method first checks if an existing archive file is present and deletes it if found.
   * It then creates a new archive, adds the SBML model and RDF glossary as entries, and finally packs and closes the archive.
   * After packing, it cleans up the original files used in the archive.
   */
  private File writeCombineArchive() throws IOException, ParseException, JDOMException, CombineArchiveException, URISyntaxException, TransformerException {
      // Determine the base file path without extension
      String baseLocation = output.getAbsolutePath().substring(0, output.getAbsolutePath().lastIndexOf('.'));
      // Specify the locations for the glossary RDF and the COMBINE archive
      String glossaryLocation = baseLocation + "_glossary.rdf";
      String combineArcLocation = baseLocation + ".zip";

      // Check if the COMBINE archive file already exists and attempt to delete it
      File caFile = new File(combineArcLocation);
      if (caFile.exists() && !caFile.delete()) {
        logger.severe(format("Failed to delete existing archive file \"{0}\"", caFile.getPath()));
      }

      File outputXML = new File(output.getAbsolutePath());
      File outputRDF = new File(glossaryLocation);

      // Create a new COMBINE archive and add entries for the model XML and glossary RDF
      try(de.unirostock.sems.cbarchive.CombineArchive ca = new de.unirostock.sems.cbarchive.CombineArchive(caFile)) {
        ca.addEntry(outputXML, "model.xml", new URI(COMBINE_SPECIFICATION), true);
        ca.addEntry(outputRDF, "glossary.rdf", new URI(RDF_MEDIATYPE), true);
        logger.info(format(MESSAGES.getString("WRITE_RDF_FILE_INFO"), combineArcLocation));

        // Pack and close the archive
        ca.pack();
      }

      // Delete the original files that were packed into the archive
      boolean rdfDeleted = outputRDF.delete();
      boolean outputXMLDeleted = outputXML.delete();
      logger.info(format(MESSAGES.getString("DELETE_FILE"), outputXML.getParent(), outputXMLDeleted));
      logger.info(format(MESSAGES.getString("DELETE_FILE"), outputRDF.getParent(), rdfDeleted));
      return caFile;
  }


}
