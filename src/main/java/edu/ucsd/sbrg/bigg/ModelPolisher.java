package edu.ucsd.sbrg.bigg;

import static java.text.MessageFormat.format;

import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLError;
import org.sbml.jsbml.SBMLErrorLog;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.TidySBMLWriter;
import org.sbml.jsbml.ext.fbc.converters.CobraToFbcV2Converter;
import org.sbml.jsbml.util.SBMLtools;
import org.sbml.jsbml.util.ValuePair;
import org.sbml.jsbml.validator.SBMLValidator;
import org.sbml.jsbml.validator.offline.LoggingValidationContext;

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
import edu.ucsd.sbrg.bigg.annotation.BiGGAnnotation;
import edu.ucsd.sbrg.bigg.polishing.SBMLPolisher;
import edu.ucsd.sbrg.db.ADBOptions;
import edu.ucsd.sbrg.db.AnnotateDB;
import edu.ucsd.sbrg.db.BiGGDB;
import edu.ucsd.sbrg.db.BiGGDBOptions;
import edu.ucsd.sbrg.db.DBConfig;
import edu.ucsd.sbrg.parsers.cobra.COBRAParser;
import edu.ucsd.sbrg.parsers.json.JSONConverter;
import edu.ucsd.sbrg.parsers.json.JSONParser;
import edu.ucsd.sbrg.util.CombineArchive;
import edu.ucsd.sbrg.util.GPRParser;
import edu.ucsd.sbrg.util.SBMLUtils;
import edu.ucsd.sbrg.util.UpdateListener;

/**
 * The ModelPolisher class is the entry point of this application.
 * It extends the Launcher class and provides functionality to polish SBML models.
 * It handles command-line arguments to configure the polishing process, manages file input and output,
 * and integrates various utilities for processing SBML, JSON, and MatLab files. The class supports
 * operations such as reading, validating, and writing SBML documents, converting JSON and MatLab files
 * to SBML, and annotating models with data from BiGG.
 *
 * The main functionalities include:
 * - Command-line argument parsing and processing.
 * - Batch processing of files and directories for model polishing.
 * - File type detection and appropriate handling of SBML, JSON, and MatLab files.
 * - HTML tag correction in SBML files.
 * - SBML document validation and conversion.
 * - Annotation of models using external databases.
 * - Output management including file writing, COMBINE archive creation, and compression.
 *
 * This class also handles error logging and provides detailed logging of the processing steps.
 * 
 * @author Andreas Dr&auml;ger
 */
public class ModelPolisher extends Launcher {

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
  private static final transient ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");
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
   * Entry point
   *
   * @param args
   *        Commandline arguments passed, use help flag to obtain usage information
   */
  public static void main(String[] args) {
    // Workaround for docker container
    String home = System.getenv("HOME");
    // / should only be home if system user has no corresponding home folder
    if ("/".equals(home)) {
      System.setProperty("java.util.prefs.systemRoot", ".java");
      System.setProperty("java.util.prefs.userRoot", ".java/.userPrefs");
    }
    new ModelPolisher(args);
  }


  /**
   * Initializes super class with Commandline arguments, which are converted into {@link AppConf} and passsed to
   * {@link ModelPolisher#commandLineMode(AppConf)}, which runs the rest of the program
   *
   * @param args
   *        Commandline arguments
   */
  public ModelPolisher(String... args) {
    super(args);
  }


  /**
   * Starts ModelPolisher with given commandline arguments and initializes {@link Parameters} and database connections
   *
   * @param appConf
   *        from super class initialization, holds commandline arguments
   * @see de.zbit.Launcher#commandLineMode(de.zbit.AppConf)
   */
  @Override
  public void commandLineMode(AppConf appConf) {
    SBProperties args = appConf.getCmdArgs();
    initParameters(args);
    try {
      batchProcess(Parameters.get().input(), Parameters.get().output());
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
   * Initializes parameters and database connections
   *
   * @param args
   *        Commandline arguments
   */
  private void initParameters(SBProperties args) {
    try {
      Parameters.init(args);
    } catch (IllegalArgumentException exc) {
      throw new IllegalArgumentException(exc.getLocalizedMessage());
    }
    DBConfig.initBiGG(args, Parameters.get().annotateWithBiGG());
    DBConfig.initADB(args, Parameters.get().addADBAnnotations());
  }


  /**
   * Processes the specified input and output paths. If the input is a directory, it recursively processes each file within.
   * It ensures that the output directory exists before processing starts.
   *
   * @param input  Path to the input file or directory to be processed. This should correspond to {@link Parameters#input()}.
   * @param output Path to the output file or directory where processed files should be saved. This should correspond to {@link Parameters#output()}.
   * @throws IOException if the input file or directory does not exist, or if no files are found within the directory.
   * @throws XMLStreamException if an error occurs during file processing, propagated from {@link ModelPolisher#processFile(File, File)}.
   */
  private void batchProcess(File input, File output) throws IOException, XMLStreamException {
    // Check if the input exists, throw an exception if it does not
    if (!input.exists()) {
      throw new IOException(format(MESSAGES.getString("READ_FILE_ERROR"), input.toString()));
    }
    // Ensure the output directory or file's parent directory exists
    checkCreateOutDir(output);
    // If the input is a directory, process each file within it
    if (input.isDirectory()) {
      // If the output is not a directory but the input is, log an error and return
      if (!output.isDirectory()) {
        logger.info(format(MESSAGES.getString("WRITE_DIR_TO_FILE_ERROR"), input.getAbsolutePath(), output.getAbsolutePath()));
        return;
      }
      // List all files in the input directory
      File[] files = input.listFiles();
      // If no files are found, throw an exception
      if (files == null || files.length < 1) {
        throw new IllegalArgumentException(MESSAGES.getString("NO_FILES_ERROR"));
      }
      // Recursively process each file in the directory
      for (File file : files) {
        File target = getOutputFileName(file, output);
        batchProcess(file, target);
      }
    } else {
      // NOTE: input is a single file, but output can be a file or a directory
      processFile(input, output);
    }
  }


  /**
   * Creates output directory or output parent directory, if necessary
   *
   * @param output:
   *        File denoting output location
   */
  private void checkCreateOutDir(File output) {
    logger.info(format(MESSAGES.getString("OUTPUT_FILE_DESC"), isDirectory(output) ? "directory" : "file"));
    // ModelPolisher.isDirectory() checks if output location contains ., if so it is assumed to be a file
    // output is directory
    if (isDirectory(output) && !output.exists()) {
      logger.info(format(MESSAGES.getString("CREATING_DIRECTORY"), output.getAbsolutePath()));
      if (output.mkdirs()) {
        logger.fine(format(MESSAGES.getString("DIRECTORY_CREATED"), output.getAbsolutePath()));
      } else {
        logger.severe(format(MESSAGES.getString("DIRECTORY_CREATION_FAILED"), output.getAbsolutePath()));
        exit();
      }
    }
    // output is a file
    else {
      // check if directory of outfile exist and create if required
      if (!output.getParentFile().exists()) {
        logger.info(format(MESSAGES.getString("CREATING_DIRECTORY"), output.getParentFile().getAbsolutePath()));
        if (output.getParentFile().mkdirs()) {
          logger.fine(format(MESSAGES.getString("DIRECTORY_CREATED"), output.getParentFile().getAbsolutePath()));
        } else {
          logger.severe(
            format(MESSAGES.getString("DIRECTORY_CREATION_FAILED"), output.getParentFile().getAbsolutePath()));
          exit();
        }
      }
    }
  }


  /**
   * Fix output file name to contain xml extension
   *
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
   * Check if file is directory by calling {@link File#isDirectory()} on an existing file or check presence of '.' in
   * output.getName(), if this is not the case
   *
   * @param file
   */
  private boolean isDirectory(File file) {
    // file = d1/d2/d3 is taken as a file by method file.isDirectory()
    if (file.exists()) {
      return file.isDirectory();
    } else {
      return !file.getName().contains(".");
    }
  }


  /**
   * Processes the input file by determining its type and applying necessary preprocessing steps.
   * If the file type is unknown, it attempts to update SBML files with top-level namespace declarations,
   * which might be present due to specific tools like CarveMe. If the file remains unknown after attempting
   * to update, it logs a warning and returns without further processing.
   * If the output path is a directory, it adjusts the output file name based on the input file's type and name.
   * Finally, it calls the method to read and polish the file.
   *
   * @param input  The input file to be processed.
   * @param output The output file or directory where the processed file should be saved.
   * @throws XMLStreamException If an XML processing error occurs.
   * @throws IOException If an I/O error occurs.
   */
  private void processFile(File input, File output) throws XMLStreamException, IOException {
    // Determine the file type of the input file
    fileType = getFileType(input);
    // Handle unknown file types by checking and updating HTML tags
    if (fileType.equals(FileType.UNKNOWN)) {
      checkHTMLTags(input);
      fileType = getFileType(input); // Re-check file type after updating tags
      // Abort processing if file type is still unknown
      if (fileType.equals(FileType.UNKNOWN)) {
        logger.warning(format(MESSAGES.getString("INPUT_UNKNOWN"), input.getPath()));
        return;
      }
    }
    // Adjust output file name if the output is a directory
    if (output.isDirectory()) {
      output = getOutputFileName(input, output);
    }
    // Read and polish the file
    readAndPolish(input, output);
  }


  /**
   * Determines the type of the input file based on its extension or content.
   * This method checks if the file is an SBML, MatLab, or JSON file by utilizing the {@link SBFileFilter} class.
   *
   * @param input The file whose type needs to be determined.
   * @return FileType The type of the file, which can be SBML_FILE, MAT_FILE, JSON_FILE, or UNKNOWN if the type cannot be determined.
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
   * This method reads an input file, determines its type (SBML, MAT, or JSON), and applies the appropriate
   * parsing and polishing processes. The result is written to the specified output file in SBML format.
   * 
   * The method logs the start of the reading process, determines the file type, and uses the corresponding
   * parser to convert the file into an SBMLDocument. If the file is an SBML file, it first checks and corrects
   * HTML tags. After parsing, if the document is null (indicating a parsing failure), it logs an error and exits.
   * Otherwise, it proceeds to polish the document and logs the time taken for the entire process upon completion.
   *
   * @param input  The input file which can be in SBML, MAT, or JSON format.
   * @param output The output file where the polished SBML will be saved.
   * @throws XMLStreamException If an error occurs during XML parsing or writing.
   * @throws IOException If an I/O error occurs during file reading or writing.
   */
  private void readAndPolish(File input, File output) throws XMLStreamException, IOException {
    long startTime = System.currentTimeMillis();
    logger.info(format(MESSAGES.getString("READ_FILE_INFO"), input.getAbsolutePath()));
    SBMLDocument doc;

    // Determine the file type and parse accordingly
    if (fileType.equals(FileType.MAT_FILE)) {
      doc = COBRAParser.read(input);
    } else if (fileType.equals(FileType.JSON_FILE)) {
      doc = JSONParser.read(input);
    } else {
      checkHTMLTags(input);
      doc = SBMLReader.read(input, new UpdateListener());
    }

    // Check if the document was successfully parsed
    if (doc == null) {
      logger.severe(format(MESSAGES.getString("ALL_DOCS_PARSE_ERROR"), input.toString()));
      return;
    }

    // Polish the document and write to output
    polish(doc, output);

    // Clear temporary data structures used during parsing
    SBMLUtils.clearGPRMap();
    GPRParser.clearAssociationMap();

    // Log the time taken to process the file
    long timeTaken = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime);
    logger.info(String.format(MESSAGES.getString("FINISHED_TIME"), (timeTaken / 60), (timeTaken % 60)));
  }


  /**
   * Replaces incorrect HTML tags in an SBML file with correct body tags and creates a backup of the original file.
   * This method reads the input SBML file, checks for incorrect HTML tags, and replaces them with the correct tags.
   * It also creates a backup of the original file before making any changes.
   *
   * @param input The SBML file to be checked and corrected.
   */
  private void checkHTMLTags(File input) {
    // Replace tags and replace file for processing
    try (FileInputStream iStream = new FileInputStream(input);
        BufferedReader reader = new BufferedReader(new InputStreamReader(iStream))) {
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        sb.append(line).append("\n");
      }
      String doc = sb.toString();
      // Check if the document contains incorrect HTML tags
      if (!doc.contains("<html xmlns") && !doc.contains("sbml:") && !doc.contains("html:html")) {
        logger.fine(MESSAGES.getString("TAGS_FINE_INFO"));
        return;
      }
      // this is here now for top level namespace declarations until proper handling for such models is clear. See issue
      // #100 -> does not work for models where sbml namespace is intertwined into another, need to find another
      // solution
      // doc = doc.replaceAll("sbml:", "");
      // doc = doc.replaceAll("xmlns:sbml", "xmlns");
      doc = doc.replaceAll("html:html", "html:body");
      doc = doc.replaceAll("<html xmlns", "<body xmlns");
      doc = doc.replaceAll("</html>", "</body>");
      // Create a backup of the original file before modifying it
      try {
        Path output = Paths.get(input.getAbsolutePath() + ".bak");
        Files.copy(input.toPath(), output, StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
        // TODO: change this logging, we overwrite the file
        // We assume it was already corrected
        logger.info(MESSAGES.getString("SKIP_TAG_REPLACEMENT"));
        return;
      }
      // Write the corrected document back to the original file
      try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(input)))) {
        writer.write(doc);
        logger.info(format(MESSAGES.getString("WROTE_CORRECT_HTML"), input.toPath()));
      }
    } catch (FileNotFoundException exc) {
      logger.severe(format(MESSAGES.getString("READ_FILE_ERROR"), input.toPath()));
    } catch (IOException e) {
      logger.severe(MESSAGES.getString("READ_HTML_ERROR"));
    }
  }


  /**
   * This method orchestrates the polishing process of an SBML document, including annotation, JSON conversion, file writing, 
   * COMBINE archive creation, and compression. It ensures the model exists within the document before proceeding with further tasks.
   *
   * @param doc    The SBMLDocument to be polished.
   * @param output The file where the polished SBML document will be written.
   * @throws IOException         If an I/O error occurs during file writing or archive creation.
   * @throws XMLStreamException  If an error occurs during XML processing.
   */
  private void polish(SBMLDocument doc, File output) throws IOException, XMLStreamException {
    if (doc.getModel() == null) {
      logger.severe(MESSAGES.getString("MODEL_MISSING"));
      return;
    }
    // Retrieve global parameters for the polishing process
    Parameters parameters = Parameters.get();
    // Ensure the document is at the correct SBML level and version
    doc = checkLevelAndVersion(doc);
    // Perform the polishing operations on the document
    SBMLPolisher polisher = new SBMLPolisher();
    doc = polisher.polish(doc);
    // Annotate the document if the parameters specify
    if (parameters.annotateWithBiGG()) {
      BiGGAnnotation annotation = new BiGGAnnotation();
      doc = annotation.annotate(doc);
    }
    // Convert and write the document to JSON if specified
    if (parameters.writeJSON()) {
      String out = output.getAbsolutePath().replaceAll("\\.xml", ".json");
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(out))) {
        writer.write(JSONConverter.getJSONDocument(doc));
      }
    }
    // writing polished model
    logger.info(format(MESSAGES.getString("WRITE_FILE_INFO"), output.getAbsolutePath()));
    TidySBMLWriter.write(doc, output, getClass().getSimpleName(), getVersionNumber(), ' ', (short) 2);
    // Handle COMBINE archive creation if specified
    if (parameters.outputCOMBINE()) {
      CombineArchive combineArchive = new CombineArchive(doc, output);
      combineArchive.write();
    }
    // Handle file compression based on the specified method
    if (parameters.compression() != Compression.NONE) {
      String fileExtension = parameters.compression().getFileExtension();
      String archive = output.getAbsolutePath() + "." + fileExtension;
      logger.info(format(MESSAGES.getString("ARCHIVE"), archive));
      switch (parameters.compression()) {
      case ZIP:
        ZIPUtils.ZIPcompress(new String[] {output.getAbsolutePath()}, archive, "SBML Archive", true);
        break;
      case GZIP:
        ZIPUtils.GZip(output.getAbsolutePath(), archive);
        break;
      default:
        break;
      }
      // Delete the original output file if compression is successful
      if (!output.delete()) {
        logger.warning(format(MESSAGES.getString("REMOVE_ZIP_INPUT_FAIL"), output.getAbsolutePath()));
      }
      // Perform SBML validation if specified
      if (parameters.SBMLValidation()) {
        // use offline validation
        validate(archive, false);
      }
    }
  }


  /**
   * Ensures that the SBML document is set to Level 3 and Version 1, which are required for compatibility with necessary plugins.
   * If the document is not already at this level and version, it updates the document to meet these specifications.
   * After ensuring the document is at the correct level and version, it converts the document using the CobraToFbcV2Converter.
   * 
   * @param doc The SBMLDocument to be checked and potentially converted.
   * @return The SBMLDocument after potentially updating its level and version and converting it.
   */
  private SBMLDocument checkLevelAndVersion(SBMLDocument doc) {
    if (!doc.isSetLevelAndVersion() || (doc.getLevelAndVersion().compareTo(ValuePair.of(3, 1)) < 0)) {
      logger.info(MESSAGES.getString("TRY_CONV_LVL3_V1"));
      SBMLtools.setLevelAndVersion(doc, 3, 1);
    }
    // Initialize the converter for Cobra to FBC version 2
    CobraToFbcV2Converter converter = new CobraToFbcV2Converter();
    // Convert the document and return the converted document
    return converter.convert(doc);
  }


  /**
   * Validates an SBML file either online or offline based on the provided parameters.
   * Online validation refers to checking the file against a remote service or database, using specific parameters for the validation process.
   * Offline validation involves reading the file locally, handling different compression formats if necessary, and validating the SBML document against local constraints.
   * Errors encountered during the validation process are logged for further analysis.
   *
   * @param filename The path to the SBML file to be validated.
   * @param online   A boolean flag indicating whether to perform online (true) or offline (false) validation.
   */
  private void validate(String filename, boolean online) {
    if (online) {
      logger.info(format(MESSAGES.getString("VAL_ONLINE"), filename));
      String output = "xml";
      String offcheck = "p,u";
      Map<String, String> parameters = new HashMap<>();
      parameters.put("output", output);
      parameters.put("offcheck", offcheck);
      logger.info("Validating " + filename + "\n");
      SBMLErrorLog sbmlErrorLog = SBMLValidator.checkConsistency(filename, parameters);
      handleErrorLog(sbmlErrorLog, filename);
    } else {
      logger.info(format(MESSAGES.getString("VAL_OFFLINE"), filename));
      SBMLDocument doc = null;
      try {
        InputStream istream;
        if (filename.endsWith(".gz")) {
          istream = ZIPUtils.GUnzipStream(filename);
        } else if (filename.endsWith(".zip")) {
          istream = ZIPUtils.ZIPunCompressStream(filename);
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
        logger.severe(format(MESSAGES.getString("VAL_OFFLINE_FAIL"), filename));
      }
    }
  }


  /**
   * @param sbmlErrorLog
   * @param filename
   */
  private void handleErrorLog(SBMLErrorLog sbmlErrorLog, String filename) {
    if (sbmlErrorLog != null) {
      logger.info(format(MESSAGES.getString("VAL_ERR_COUNT"), sbmlErrorLog.getErrorCount(), filename));
      // printErrors
      for (int j = 0; j < sbmlErrorLog.getErrorCount(); j++) {
        SBMLError error = sbmlErrorLog.getError(j);
        logger.warning(error.toString());
      }
    } else {
      logger.info(MESSAGES.getString("VAL_ERROR"));
    }
  }


  /*
   * (non-Javadoc)
   * @see de.zbit.Launcher#getCitation(boolean)
   */
  @Override
  public String getCitation(boolean HTMLstyle) {
    if (HTMLstyle) {
      return MESSAGES.getString("CITATION_HTML");
    }
    return MESSAGES.getString("CITATION");
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
   * This method is inherited from the base class and is not utilized in this CLI application.
   * The ModelPolisher application does not implement a graphical user interface.
   * 
   * @return Always returns false as no GUI is created.
   * @see de.zbit.Launcher#addCopyrightToSplashScreen()
   */
  @Override
  protected boolean addCopyrightToSplashScreen() {
    return false;
  }


  /*
   * This method is inherited from the base class and is not utilized in this CLI application.
   * The ModelPolisher application does not implement a graphical user interface.
   * 
   * @return Always returns false as no GUI is created.
   * @see de.zbit.Launcher#addVersionNumberToSplashScreen()
   */
  @Override
  protected boolean addVersionNumberToSplashScreen() {
    return false;
  }


  /**
   * This method is inherited from the base class and is not utilized in this CLI application.
   * The ModelPolisher application does not implement a graphical user interface.
   * 
   * @param appConf The application configuration settings, not used in this context.
   * @return Always returns null as no GUI is created.
   * @see de.zbit.Launcher#initGUI(de.zbit.AppConf)
   */
  @Override
  public Window initGUI(AppConf appConf) {
    return null;
  }
}
