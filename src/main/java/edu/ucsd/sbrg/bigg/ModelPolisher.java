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
   * Processes given input and output parameters and recurses, if input denotes a directory.
   * Creates output directory, if not present
   *
   * @param input:
   *        Path to input file/directory to process, corresponds to {@link Parameters#input()} at top level
   * @param output:
   *        Path to output file/directory, corresponds to {@link Parameters#output()}
   * @throws IOException
   *         if input file is not found, or no file is present in the input directory
   * @throws XMLStreamException
   *         propagated from {@link ModelPolisher#processFile(File, File)}
   */
  private void batchProcess(File input, File output) throws IOException, XMLStreamException {
    if (!input.exists()) {
      throw new IOException(format(MESSAGES.getString("READ_FILE_ERROR"), input.toString()));
    }
    // Create output directory if output is a directory or create output file's directory if output is a file
    checkCreateOutDir(output);
    // Move down into the directory
    if (input.isDirectory()) {
      if (!output.isDirectory()) {
        // input == dir && output != dir -> should only happen if already inside a directory and trying to recurse,
        // which is not supported
        // Couldn't this be done in IOOptions using dependencies?
        logger.info(
          format(MESSAGES.getString("WRITE_DIR_TO_FILE_ERROR"), input.getAbsolutePath(), output.getAbsolutePath()));
        return;
      }
      File[] files = input.listFiles();
      if (files == null || files.length < 1) {
        throw new IllegalArgumentException(MESSAGES.getString("NO_FILES_ERROR"));
      }
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
   * Preprocessing based on file type, i.e. setting correct output file extension and applying workaround for SBML files
   * with top level namespace declarations
   *
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
      // do this for now to update SBML files with top level namespace declarations (Possibly from CarveMe)
      // should skip invocation of most of the code later on as tags are already replaced
      checkHTMLTags(input);
      fileType = getFileType(input);
      // did not fix the issue, abort
      if (fileType.equals(FileType.UNKNOWN)) {
        logger.warning(format(MESSAGES.getString("INPUT_UNKNOWN"), input.getPath()));
        return;
      }
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
   * Read input file and dispatch polishing tasks for different possible file types
   *
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
    logger.info(format(MESSAGES.getString("READ_FILE_INFO"), input.getAbsolutePath()));
    SBMLDocument doc;
    // reading or parsing input
    if (fileType.equals(FileType.MAT_FILE)) {
      doc = COBRAParser.read(input);
    } else if (fileType.equals(FileType.JSON_FILE)) {
      doc = JSONParser.read(input);
    } else {
      checkHTMLTags(input);
      doc = SBMLReader.read(input, new UpdateListener());
    }
    if (doc == null) {
      logger.severe(format(MESSAGES.getString("ALL_DOCS_PARSE_ERROR"), input.toString()));
      return;
    }
    polish(doc, output);
    // Clear map for next model
    SBMLUtils.clearGPRMap();
    GPRParser.clearAssociationMap();
    time = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - time);
    logger.info(String.format(MESSAGES.getString("FINISHED_TIME"), (time / 60), (time % 60)));
  }


  /**
   * Replaces wrong html tags in a SBML model with body tags
   *
   * @param input:
   *        SBML file
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
      // replace wrong tags
      doc = doc.replaceAll("<html xmlns", "<body xmlns");
      doc = doc.replaceAll("</html>", "</body>");
      // Preserve a copy of the original.
      try {
        Path output = Paths.get(input.getAbsolutePath() + ".bak");
        Files.copy(input.toPath(), output, StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
        // TODO: change this logging, we overwrite the file
        // We assume it was already corrected
        logger.info(MESSAGES.getString("SKIP_TAG_REPLACEMENT"));
        return;
      }
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(input)));
      writer.write(doc);
      logger.info(format(MESSAGES.getString("WROTE_CORRECT_HTML"), input.toPath()));
      writer.close();
    } catch (FileNotFoundException exc) {
      logger.severe(format(MESSAGES.getString("READ_FILE_ERROR"), input.toPath()));
    } catch (IOException e) {
      logger.severe(MESSAGES.getString("READ_HTML_ERROR"));
    }
  }


  /**
   * Dispatch central tasks like polishing, annotation, combine archive creation, compression and validation
   *
   * @param doc
   * @param output
   * @throws IOException
   * @throws XMLStreamException
   */
  private void polish(SBMLDocument doc, File output) throws IOException, XMLStreamException {
    if (doc.getModel() == null) {
      logger.severe(MESSAGES.getString("MODEL_MISSING"));
      return;
    }
    Parameters parameters = Parameters.get();
    doc = checkLevelAndVersion(doc);
    // Polishing
    SBMLPolisher polisher = new SBMLPolisher();
    doc = polisher.polish(doc);
    // Annotation
    if (parameters.annotateWithBiGG()) {
      BiGGAnnotation annotation = new BiGGAnnotation();
      doc = annotation.annotate(doc);
    }
    if (parameters.writeJSON()) {
      String out = output.getAbsolutePath().replaceAll("\\.xml", ".json");
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(out))) {
        writer.write(JSONConverter.getJSONDocument(doc));
      }
    }
    // writing polished model
    logger.info(format(MESSAGES.getString("WRITE_FILE_INFO"), output.getAbsolutePath()));
    TidySBMLWriter.write(doc, output, getClass().getSimpleName(), getVersionNumber(), ' ', (short) 2);
    // produce COMBINE archive and delete output model and glossary
    // TODO: do compression and combine output work together?
    if (parameters.outputCOMBINE()) {
      // producing & writing glossary
      CombineArchive combineArchive = new CombineArchive(doc, output);
      combineArchive.write();
    }
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
      if (!output.delete()) {
        logger.warning(format(MESSAGES.getString("REMOVE_ZIP_INPUT_FAIL"), output.getAbsolutePath()));
      }
      if (parameters.SBMLValidation()) {
        // use offline validation
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
      logger.info(MESSAGES.getString("TRY_CONV_LVL3_V1"));
      SBMLtools.setLevelAndVersion(doc, 3, 1);
    }
    CobraToFbcV2Converter converter = new CobraToFbcV2Converter();
    return converter.convert(doc);
  }


  /**
   * @param filename
   */
  private void validate(String filename, boolean online) {
    if (online) {
      logger.info(format(MESSAGES.getString("VAL_ONLINE"), filename));
      String output = "xml";
      String offcheck = "p,u";
      Map<String, String> parameters = new HashMap<>();
      parameters.put("output", output);
      parameters.put("offcheck", offcheck);
      logger.info("Validating  " + filename + "\n");
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
   * @see de.zbit.Launcher#initGUI(de.zbit.AppConf)
   */
  @Override
  public Window initGUI(AppConf appConf) {
    return null;
  }
}
