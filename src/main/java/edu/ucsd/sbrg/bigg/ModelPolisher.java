/*
 * 
 */
package edu.ucsd.sbrg.bigg;

import static java.text.MessageFormat.format;

import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLError;
import org.sbml.jsbml.SBMLErrorLog;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.TidySBMLWriter;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.sbml.jsbml.ext.fbc.converters.CobraToFbcV2Converter;
import org.sbml.jsbml.util.SBMLtools;
import org.sbml.jsbml.util.ValuePair;
import org.sbml.jsbml.validator.SBMLValidator;
import org.sbml.jsbml.validator.offline.LoggingValidationContext;
import org.sbml.jsbml.xml.XMLNode;
import org.sbml.jsbml.xml.parsers.SBMLRDFAnnotationParser;
import org.w3c.tidy.Tidy;

import de.unirostock.sems.cbarchive.CombineArchive;
import de.zbit.AppConf;
import de.zbit.Launcher;
import de.zbit.io.FileTools;
import de.zbit.io.ZIPUtils;
import de.zbit.io.filefilter.SBFileFilter;
import de.zbit.util.ResourceManager;
import de.zbit.util.Utils;
import de.zbit.util.logging.LogOptions;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.SBProperties;
import edu.ucsd.sbrg.bigg.ModelPolisherOptions.Compression;
import edu.ucsd.sbrg.db.ADBOptions;
import edu.ucsd.sbrg.db.AnnotateDB;
import edu.ucsd.sbrg.db.BiGGDB;
import edu.ucsd.sbrg.db.BiGGDBOptions;
import edu.ucsd.sbrg.db.DBConfig;
import edu.ucsd.sbrg.parsers.COBRAparser;
import edu.ucsd.sbrg.parsers.JSONparser;
import edu.ucsd.sbrg.util.GPRParser;
import edu.ucsd.sbrg.util.SBMLUtils;
import edu.ucsd.sbrg.util.UpdateListener;

/**
 * @author Andreas Dr&auml;ger
 */
public class ModelPolisher extends Launcher {

  /**
   *
   */
  private Parameters parameters;
  /**
   * Type of current input file
   */
  private FileType fileType;
  /**
   * Localization support.
   */
  private static final transient ResourceBundle baseBundle = ResourceManager.getBundle("edu.ucsd.sbrg.Messages");
  /**
   * Bundle for ModelPolisher logger messages
   */
  public static transient ResourceBundle mpMessageBundle = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");
  /**
   * A {@link Logger} for this class.
   */
  private static final transient Logger logger = Logger.getLogger(ModelPolisher.class.getName());
  /**
   * Generated serial version identifier.
   */
  private static final long serialVersionUID = 7745344693995142413L;

  /**
   * Possible FileTypes of input file
   */
  private enum FileType {
    SBML_FILE,
    MAT_FILE,
    JSON_FILE,
    UNKNOWN
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    new ModelPolisher(args);
  }


  /**
   * @param args
   */
  public ModelPolisher(String... args) {
    super(args);
  }


  /*
   * (non-Javadoc)
   * @see de.zbit.Launcher#addCopyrightToSplashScreen()
   */
  @Override
  protected boolean addCopyrightToSplashScreen() {
    return false;
  }


  /*
   * (non-Javadoc)
   * @see de.zbit.Launcher#addVersionNumberToSplashScreen()
   */
  @Override
  protected boolean addVersionNumberToSplashScreen() {
    return false;
  }


  /*
   * (non-Javadoc)
   * @see de.zbit.Launcher#commandLineMode(de.zbit.AppConf)
   */
  @Override
  public void commandLineMode(AppConf appConf) {
    SBProperties args = appConf.getCmdArgs();
    parameters = Parameters.init(args);
    DBConfig.initBiGG(args, parameters.annotateWithBiGG);
    DBConfig.initADB(args, parameters.addADBAnnotations);
    // Gives users the choice to pass an alternative model notes XHTML file to the program.
    try {
      batchProcess(new File(args.getProperty(IOOptions.INPUT)), new File(args.getProperty(IOOptions.OUTPUT)));
    } catch (XMLStreamException | IOException exc) {
      exc.printStackTrace();
    }
    // make sure DB connections are closed in case of exception
    finally {
      if (BiGGDB.inUse()) {
        BiGGDB.close();
      }
      if (AnnotateDB.inUse()) {
        AnnotateDB.close();
      }
    }
  }


  /**
   * @param input:
   *        Path to input file/directory to process
   * @param output:
   *        Path to output file/directory
   * @throws IOException
   *         if input file is not found, or no file is present in input directory
   * @throws XMLStreamException
   *         propagated from {@link #processFile(File, File)}
   */
  private void batchProcess(File input, File output) throws IOException, XMLStreamException {
    if (!input.exists()) {
      throw new IOException(format(mpMessageBundle.getString("READ_FILE_ERROR"), input.toString()));
    }
    // Create output directory if output is a directory or create output file's directory if output is a file
    checkCreateOutDir(output);
    if (!input.isFile()) {
      if (!output.isDirectory()) {
        // input == dir && output != dir -> should only happen if already inside a directory and trying to recurse,
        // which is not supported
        logger.warning(format(mpMessageBundle.getString("WRITE_DIR_TO_FILE_ERROR"), input.getAbsolutePath(),
          output.getAbsolutePath()));
        return;
      }
      File[] files = input.listFiles();
      if (files == null || files.length < 1) {
        throw new IOException(mpMessageBundle.getString("NO_FILES_ERROR"));
      }
      for (File file : files) {
        File target = getOutputFileName(file, output);
        batchProcess(file, target);
      }
    } else {
      // NOTE: input is a single file, but output can be a file or a directory, i.e. for multimodel files (MAT format)
      processFile(input, output);
    }
  }


  /**
   * @param file:
   *        File to get name for in input directory
   * @param output:
   *        Path to output directory
   * @return File in output directory with correct file ending for SBML
   */
  private File getOutputFileName(File file, File output) {
    fileType = getFileType(file);
    if (!fileType.equals(FileType.SBML_FILE)) {
      return new File(
        Utils.ensureSlash(output.getAbsolutePath()) + FileTools.removeFileExtension(file.getName()) + ".xml");
    } else {
      return new File(Utils.ensureSlash(output.getAbsolutePath()) + file.getName());
    }
  }


  /**
   * @param output:
   *        File denoting output location
   */
  private void checkCreateOutDir(File output) {
    logger.info(format(mpMessageBundle.getString("OUTPUT_FILE_DESC"), isDirectory(output) ? "directory" : "file"));
    /*
     * ModelPolisher.isDirectory() checks if output location contains ., if so it is assumed to be a file,
     * else it is assumed to be a directory
     */
    // output is directory
    if (isDirectory(output) && !output.exists()) {
      logger.info(format(mpMessageBundle.getString("CREATING_DIRECTORY"), output.getAbsolutePath()));
      if (output.mkdirs()) {
        logger.fine(format(mpMessageBundle.getString("DIRECTORY_CREATED"), output.getAbsolutePath()));
      } else {
        logger.severe(format(mpMessageBundle.getString("DIRECTORY_CREATION_FAILED"), output.getAbsolutePath()));
        exit();
      }
    }
    // output is a file
    else {
      // check if directory of outfile exist and create if required
      if (!output.getParentFile().exists()) {
        logger.info(format(mpMessageBundle.getString("CREATING_DIRECTORY"), output.getParentFile().getAbsolutePath()));
        if (output.getParentFile().mkdirs()) {
          logger.fine(format(mpMessageBundle.getString("DIRECTORY_CREATED"), output.getParentFile().getAbsolutePath()));
        } else {
          logger.severe(
            format(mpMessageBundle.getString("DIRECTORY_CREATION_FAILED"), output.getParentFile().getAbsolutePath()));
          exit();
        }
      }
    }
  }


  /**
   * @param file
   */
  public boolean isDirectory(File file) {
    /*
     * file = d1/d2/d3 is taken as a file by method file.isDirectory()
     * Check if file is directory by checking presence or '.' in output.getName()
     */
    return !file.getName().contains(".");
  }


  /**
   * @param input:
   *        input file
   * @param output:
   *        output file or directory
   * @throws XMLStreamException
   *         propagated from {@link #readAndPolish(File, File)}
   * @throws IOException
   *         propagated from {@link #readAndPolish(File, File)}
   */
  private void processFile(File input, File output) throws XMLStreamException, IOException {
    // get fileType array and check if any value is true
    fileType = getFileType(input);
    if (fileType.equals(FileType.UNKNOWN)) {
      // TODO: move into resources for internationalization
      logger.warning(format("Encountered file of unknown type in input : \"{0}\", skipping.", input.getPath()));
      return;
    }
    if (output.isDirectory()) {
      output = getOutputFileName(input, output);
    }
    readAndPolish(input, output);
  }


  /**
   * Get file type from input file
   *
   * @param input
   *        File used in {@link #batchProcess(File, File)}
   * @return FileType of given file, only SBML, MatLab and JSON files are supported
   */
  private FileType getFileType(File input) {
    if (SBFileFilter.isSBMLFile(input)) {
      return FileType.SBML_FILE;
    } else if (SBFileFilter.hasFileType(input, SBFileFilter.FileType.MAT_FILES)) {
      return FileType.MAT_FILE;
    } else if (SBFileFilter.hasFileType(input, SBFileFilter.FileType.JSON_FILES)) {
      return FileType.JSON_FILE;
    } else {
      return FileType.UNKNOWN;
    }
  }


  /**
   * @param input:
   *        Input file in either SBML, MAT or JSON format
   * @param output:
   *        Output file in SBML format
   * @throws XMLStreamException
   *         propagated from {@link #polish(SBMLDocument, File)}
   * @throws IOException
   *         propagated from {@link #polish(SBMLDocument, File)}
   */
  private void readAndPolish(File input, File output) throws XMLStreamException, IOException {
    long time = System.currentTimeMillis();
    logger.info(format(mpMessageBundle.getString("READ_FILE_INFO"), input.getAbsolutePath()));
    SBMLDocument doc;
    // reading or parsing input
    if (fileType.equals(FileType.MAT_FILE)) {
      doc = COBRAparser.read(input, parameters.omitGenericTerms);
    } else if (fileType.equals(FileType.JSON_FILE)) {
      doc = JSONparser.read(input);
    } else {
      checkHTMLTags(input);
      doc = SBMLReader.read(input, new UpdateListener());
    }
    if (doc == null) {
      logger.severe(format(mpMessageBundle.getString("ALL_DOCS_PARSE_ERROR"), input.toString()));
      return;
    }
    polish(doc, output);
    // Clear map for next model
    SBMLUtils.cleanGPRMap();
    GPRParser.clearAssociationMap();
    time = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - time);
    logger.info(String.format(mpMessageBundle.getString("FINISHED_TIME"), (time / 60), (time % 60)));
  }


  /**
   * Replaces wrong html tags in a SBML model with body tags
   *
   * @param input:
   *        SBML file
   */
  private void checkHTMLTags(File input) {
    FileInputStream iStream = null;
    try {
      iStream = new FileInputStream(input);
    } catch (FileNotFoundException exc) {
      logger.severe(format(mpMessageBundle.getString("READ_FILE_ERROR"), input.toPath()));
    }
    // If it's null, something went horribly wrong and a nullPointerException should be thrown
    BufferedReader reader = new BufferedReader(new InputStreamReader(iStream));
    // Replace tags and replace file for processing
    try {
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        sb.append(line).append("\n");
      }
      reader.close();
      String doc = sb.toString();
      if (!doc.contains("<html ")) {
        logger.fine(mpMessageBundle.getString("TAGS_FINE_INFO"));
        return;
      }
      doc = doc.replaceAll("<html ", "<body ");
      doc = doc.replaceAll("</html>", "</body>");
      // Preserve a copy of the original.
      try {
        Path output = Paths.get(input.getAbsolutePath() + ".bak");
        Files.copy(input.toPath(), output);
      } catch (IOException e) {
        // We assume it was already corrected
        logger.info(mpMessageBundle.getString("SKIP_TAG_REPLACEMENT"));
        return;
      }
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(input)));
      writer.write(doc);
      logger.info(format(mpMessageBundle.getString("WROTE_CORRECT_HTML"), input.toPath()));
      writer.close();
    } catch (IOException exc) {
      logger.severe(mpMessageBundle.getString("READ_HTML_ERROR"));
    }
  }


  /**
   * @param doc
   * @param output
   * @throws IOException
   * @throws XMLStreamException
   */
  private void polish(SBMLDocument doc, File output) throws IOException, XMLStreamException {
    doc = checkLevelAndVersion(doc);
    // Polishing
    SBMLPolisher polisher = initializeSBMLPolisher();
    doc = polisher.polish(doc);
    // Annotation
    if (parameters.annotateWithBiGG) {
      BiGGAnnotation annotation = setAnnotationParameters();
      doc = annotation.annotate(doc);
    }
    // writing polished model
    logger.info(format(mpMessageBundle.getString("WRITE_FILE_INFO"), output.getAbsolutePath()));
    TidySBMLWriter.write(doc, output, getClass().getSimpleName(), getVersionNumber(), ' ', (short) 2);
    // produce COMBINE archive and delete output model and glossary
    if (parameters.outputCOMBINE) {
      // producing & writing glossary
      writeGlossary(doc, output);
      writeCombineArchive(output);
    }
    if (parameters.compression != Compression.NONE) {
      String fileExtension = parameters.compression.getFileExtension();
      String archive = output.getAbsolutePath() + "." + fileExtension;
      logger.info(format(mpMessageBundle.getString("ARCHIVE"), archive));
      switch (parameters.compression) {
      case ZIP:
        ZIPUtils.ZIPcompress(new String[] {output.getAbsolutePath()}, archive, "SBML Archive", true);
        break;
      case GZIP:
        ZIPUtils.GZip(output.getAbsolutePath(), archive);
        break;
      default:
        break;
      }
      if (!output.delete()) {
        logger.warning(String.format("Failed to delete output file '%s' after compression.", output.getAbsolutePath()));
      }
      if (parameters.sbmlValidation) {
        validate(archive, false);
      }
    }
  }


  /**
   * Make sure SBML Level and Version are 3.1, so that needed plugins work
   * 
   * @param doc:
   *        SBMLDocument
   */
  private SBMLDocument checkLevelAndVersion(SBMLDocument doc) {
    if (!doc.isSetLevelAndVersion() || (doc.getLevelAndVersion().compareTo(ValuePair.of(3, 1)) < 0)) {
      logger.info(mpMessageBundle.getString("TRY_CONV_LVL3_V1"));
      SBMLtools.setLevelAndVersion(doc, 3, 1);
    }
    CobraToFbcV2Converter converter = new CobraToFbcV2Converter();
    return converter.convert(doc);
  }


  /**
   * @return SBMLPolisher object with all relevant paramters set
   */
  private SBMLPolisher initializeSBMLPolisher() {
    SBMLPolisher polisher = new SBMLPolisher();
    polisher.setCheckMassBalance(parameters.checkMassBalance);
    polisher.setOmitGenericTerms(parameters.omitGenericTerms);
    if (parameters.includeAnyURI != null) {
      polisher.setIncludeAnyURI(parameters.includeAnyURI);
    }
    if (parameters.documentTitlePattern != null) {
      polisher.setDocumentTitlePattern(parameters.documentTitlePattern);
    }
    polisher.setFluxCoefficients(parameters.fluxCoefficients);
    polisher.setFluxObjectives(parameters.fluxObjectives);
    return polisher;
  }


  /**
   * @return BiGGAnnotation object used for annotation with BiGG
   */
  private BiGGAnnotation setAnnotationParameters() {
    BiGGAnnotation annotation = new BiGGAnnotation();
    if ((parameters.noModelNotes != null) && parameters.noModelNotes) {
      annotation.setDocumentNotesFile(null);
      annotation.setModelNotesFile(null);
    } else {
      if (parameters.documentNotesFile != null) {
        annotation.setDocumentNotesFile(parameters.documentNotesFile);
      }
      if (parameters.modelNotesFile != null) {
        annotation.setModelNotesFile(parameters.modelNotesFile);
      }
    }
    return annotation;
  }


  /**
   * @param doc:
   *        SBMLDocument to write glossary for
   * @param output:
   *        File SBMLDocument is written to
   * @throws XMLStreamException:
   *         propagated from {@link #getGlossary(SBMLDocument)} and #TidySBMLWriter.write
   * @throws IOException:
   *         propagated from {@link #writeTidyRDF(File, String)}
   */
  private void writeGlossary(SBMLDocument doc, File output) throws XMLStreamException, IOException {
    String glossary = getGlossary(doc);
    String glossaryLocation =
      output.getAbsolutePath().substring(0, output.getAbsolutePath().lastIndexOf('.')) + "_glossary.rdf";
    logger.info(format(mpMessageBundle.getString("WRITE_RDF_FILE_INFO"), glossaryLocation));
    writeTidyRDF(new File(glossaryLocation), glossary);
  }


  /**
   * @param output:
   *        Output SBML file, location is used to get archive files
   */
  private void writeCombineArchive(File output) {
    try {
      String baseLocation = output.getAbsolutePath().substring(0, output.getAbsolutePath().lastIndexOf('.'));
      String glossaryLocation = baseLocation + "_glossary.rdf";
      String combineArcLocation = baseLocation + ".zip";
      // check if archive file exists and delete
      File caFile = new File(combineArcLocation);
      if (caFile.exists()) {
        if (!caFile.delete()) {
          logger.severe(format("Failed to delete archive file \"{0}\"", caFile.getPath()));
        }
      }
      // build and pack archive
      CombineArchive ca = new CombineArchive(caFile);
      File outputXML = new File(output.getAbsolutePath());
      File outputRDF = new File(glossaryLocation);
      ca.addEntry(outputXML, "model.xml", new URI("http://identifiers.org/combine.specifications/sbml"), true);
      ca.addEntry(outputRDF, "glossary.rdf",
        // generated from https://sems.uni-rostock.de/trac/combine-ext/wiki/CombineFormatizer
        new URI("http://purl.org/NET/mediatypes/application/rdf+xml"), true);
      logger.info(format(mpMessageBundle.getString("WRITE_RDF_FILE_INFO"), combineArcLocation));
      ca.pack();
      ca.close();
      // clean up original of packed files
      boolean rdfDeleted = outputRDF.delete();
      boolean outputXMLDeleted = outputXML.delete();
      logger.info(format(mpMessageBundle.getString("DELETE_FILE"), outputXML.getParent(), outputXMLDeleted));
      logger.info(format(mpMessageBundle.getString("DELETE_FILE"), outputRDF.getParent(), rdfDeleted));
    } catch (Exception e) {
      logger.warning("Exception while producing COMBINE Archive:");
      e.printStackTrace();
    }
  }


  /**
   * @param doc:
   *        SBMLDocument to produce glossary for
   * @return Glossary as XMLString or empty string, if either model is null or has no children
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
   * @param outputFile,
   *        rdfString
   */
  private void writeTidyRDF(File outputFile, String rdfString) throws FileNotFoundException {
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
    Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8));
    InputStreamReader in = new InputStreamReader(new ByteArrayInputStream(rdfString.getBytes(StandardCharsets.UTF_8)),
      StandardCharsets.UTF_8);
    tidy.parse(in, out);
    try {
      in.close();
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  /**
   * @param filename
   */
  private void validate(String filename, boolean online) {
    if (online) {
      logger.info(String.format("Validating '%s' using online validator.", filename));
      String output = "xml";
      String offcheck = "p,u";
      Map<String, String> parameters = new HashMap<>();
      parameters.put("output", output);
      parameters.put("offcheck", offcheck);
      logger.info("Validating  " + filename + "\n");
      SBMLErrorLog sbmlErrorLog = SBMLValidator.checkConsistency(filename, parameters);
      handleErrorLog(sbmlErrorLog, filename);
    } else {
      logger.info(String.format("Validating '%s' using offline validator.", filename));
      SBMLDocument doc = null;
      try {
        InputStream istream;
        if (filename.endsWith(".gz")) {
          istream = ZIPUtils.GUnzipStream(filename);
        } else {
          istream = new FileInputStream(filename);
        }
        doc = SBMLReader.read(istream);
      } catch (XMLStreamException | IOException e) {
        e.printStackTrace();
      }
      if (doc != null) {
        LoggingValidationContext context = new LoggingValidationContext(doc.getLevel(), doc.getVersion());
        context.loadConstraints(SBMLDocument.class);
        context.validate(doc);
        SBMLErrorLog sbmlErrorLog = context.getErrorLog();
        handleErrorLog(sbmlErrorLog, filename);
      } else {
        logger.severe(String.format("Failed reading file '%s' for offline validation.", filename));
      }
    }
  }


  /**
   * @param sbmlErrorLog
   * @param filename
   */
  private void handleErrorLog(SBMLErrorLog sbmlErrorLog, String filename) {
    if (sbmlErrorLog != null) {
      logger.info(format(mpMessageBundle.getString("VAL_ERR_COUNT"), sbmlErrorLog.getErrorCount(), filename));
      // printErrors
      for (int j = 0; j < sbmlErrorLog.getErrorCount(); j++) {
        SBMLError error = sbmlErrorLog.getError(j);
        logger.warning(error.toString());
      }
    } else {
      logger.info(mpMessageBundle.getString("VAL_ERROR"));
    }
  }


  /*
   * (non-Javadoc)
   * @see de.zbit.Launcher#getCitation(boolean)
   */
  @Override
  public String getCitation(boolean HTMLstyle) {
    if (HTMLstyle) {
      return mpMessageBundle.getString("CITATION_HTML");
    }
    return mpMessageBundle.getString("CITATION");
  }


  /*
   * (non-Javadoc)
   * @see de.zbit.Launcher#getCmdLineOptions()
   */
  @Override
  public List<Class<? extends KeyProvider>> getCmdLineOptions() {
    List<Class<? extends KeyProvider>> options = new LinkedList<>();
    options.add(LogOptions.class);
    options.add(BiGGDBOptions.class);
    options.add(ADBOptions.class);
    options.add(ModelPolisherOptions.class);
    options.add(IOOptions.class);
    return options;
  }


  /*
   * (non-Javadoc)
   * @see de.zbit.Launcher#getInstitute()
   */
  @Override
  public String getInstitute() {
    return baseBundle.getString("INSTITUTE");
  }


  /*
   * (non-Javadoc)
   * @see de.zbit.Launcher#getInteractiveOptions()
   */
  @Override
  public List<Class<? extends KeyProvider>> getInteractiveOptions() {
    List<Class<? extends KeyProvider>> options = new LinkedList<>();
    options.add(BiGGDBOptions.class);
    options.add(ADBOptions.class);
    options.add(ModelPolisherOptions.class);
    return options;
  }


  /*
   * (non-Javadoc)
   * @see de.zbit.Launcher#getLogPackages()
   */
  @Override
  public String[] getLogPackages() {
    return new String[] {"edu.ucsd", "de.zbit"};
  }


  /*
   * (non-Javadoc)
   * @see de.zbit.Launcher#getOrganization()
   */
  @Override
  public String getOrganization() {
    return baseBundle.getString("ORGANIZATION");
  }


  /*
   * (non-Javadoc)
   * @see de.zbit.Launcher#getProvider()
   */
  @Override
  public String getProvider() {
    return baseBundle.getString("PROVIDER");
  }


  /*
   * (non-Javadoc)
   * @see de.zbit.Launcher#getURLlicenseFile()
   */
  @Override
  public URL getURLlicenseFile() {
    try {
      return new URL(baseBundle.getString("LICENSE"));
    } catch (MalformedURLException exc) {
      return null;
    }
  }


  /*
   * (non-Javadoc)
   * @see de.zbit.Launcher#getURLOnlineUpdate()
   */
  @Override
  public URL getURLOnlineUpdate() {
    return null;
  }


  /*
   * (non-Javadoc)
   * @see de.zbit.Launcher#getVersionNumber()
   */
  @Override
  public String getVersionNumber() {
    String version = getClass().getPackage().getImplementationVersion();
    return version == null ? "?" : version;
  }


  /*
   * (non-Javadoc)
   * @see de.zbit.Launcher#getYearOfProgramRelease()
   */
  @Override
  public short getYearOfProgramRelease() {
    try {
      return Short.parseShort(baseBundle.getString("YEAR"));
    } catch (NumberFormatException exc) {
      return (short) Calendar.getInstance().get(Calendar.YEAR);
    }
  }


  /*
   * (non-Javadoc)
   * @see de.zbit.Launcher#getYearWhenProjectWasStarted()
   */
  @Override
  public short getYearWhenProjectWasStarted() {
    try {
      return Short.parseShort(baseBundle.getString("INCEPTION_YEAR"));
    } catch (Throwable t) {
      return (short) 2014;
    }
  }


  /*
   * (non-Javadoc)
   * @see de.zbit.Launcher#initGUI(de.zbit.AppConf)
   */
  @Override
  public Window initGUI(AppConf appConf) {
    return null;
  }
}
