package edu.ucsd.sbrg;

import static java.text.MessageFormat.format;

import java.awt.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import edu.ucsd.sbrg.annotation.ModelAnnotator;
import edu.ucsd.sbrg.io.IOOptions;
import edu.ucsd.sbrg.io.ModelReader;
import edu.ucsd.sbrg.io.ModelWriter;
import edu.ucsd.sbrg.polishing.ModelPolisher;
import edu.ucsd.sbrg.util.*;
import org.sbml.jsbml.SBMLDocument;

import de.zbit.AppConf;
import de.zbit.Launcher;
import de.zbit.util.ResourceManager;
import de.zbit.util.logging.LogOptions;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.SBProperties;
import edu.ucsd.sbrg.db.adb.AnnotateDBOptions;
import edu.ucsd.sbrg.db.adb.AnnotateDB;
import edu.ucsd.sbrg.db.bigg.BiGGDB;
import edu.ucsd.sbrg.db.bigg.BiGGDBOptions;
import edu.ucsd.sbrg.db.DBConfig;

/**
 * The ModelPolisher class is the entry point of this application.
 * It extends the Launcher class and provides functionality to polish SBML models.
 * It handles command-line arguments to configure the polishing process, manages file input and output,
 * and integrates various utilities for processing SBML, JSON, and MatLab files. The class supports
 * operations such as reading, validating, and writing SBML documents, converting JSON and MatLab files
 * to SBML, and annotating models with data from BiGG.
 * <p>
 * The main functionalities include:
 * - Command-line argument parsing and processing.
 * - Batch processing of files and directories for model polishing.
 * - File type detection and appropriate handling of SBML, JSON, and MatLab files.
 * - HTML tag correction in SBML files.
 * - SBML document validation and conversion.
 * - Annotation of models using external databases.
 * - Output management including file writing, COMBINE archive creation, and compression.
 * <p>
 * This class also handles error logging and provides detailed logging of the processing steps.
 * 
 * @author Andreas Dr&auml;ger
 */
public class ModelPolisherCLILauncher extends Launcher {

  /**
   * Localization support.
   */
  private static final ResourceBundle baseBundle = ResourceManager.getBundle("edu.ucsd.sbrg.Messages");
  /**
   * Bundle for ModelPolisher logger messages
   */
  private static final ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");
  /**
   * A {@link Logger} for this class.
   */
  private static final Logger logger = Logger.getLogger(ModelPolisherCLILauncher.class.getName());

  private Parameters parameters;

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
    new ModelPolisherCLILauncher(args);
  }


  /**
   * Initializes super class with Commandline arguments, which are converted into {@link AppConf} and passed to
   * {@link ModelPolisherCLILauncher#commandLineMode(AppConf)}, which runs the rest of the program
   *
   * @param args
   *        Commandline arguments
   */
  public ModelPolisherCLILauncher(String... args) {
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

    try {
        parameters = new Parameters(args);
    } catch (IllegalArgumentException exc1) {
      throw new IllegalArgumentException(exc1.getLocalizedMessage());
    }
    DBConfig.initBiGG(args, parameters.annotateWithBiGG());
    DBConfig.initADB(args, parameters.addADBAnnotations());

    try {
      var input = parameters.input();
      var output = parameters.output();

      // Check if the input exists, throw an exception if it does not
      if (!input.exists()) {
        throw new IOException(format(MESSAGES.getString("READ_FILE_ERROR"),
                input.toString()));
      }

      // If the output is not a directory but the input is, log an error and return
      if (!output.isDirectory() && input.isDirectory()) {
        throw new IOException(format(MESSAGES.getString("WRITE_DIR_TO_FILE_ERROR"),
                input.getAbsolutePath(),
                output.getAbsolutePath()));
      }

      // Ensure the output directory or file's parent directory exists
      SBMLFileUtils.checkCreateOutDir(output);

      batchProcess(input, parameters.output());

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
   * Processes the specified input and output paths. If the input is a directory, it recursively processes each file within.
   *
   * @param input  Path to the input file or directory to be processed. This should correspond to {@link Parameters#input()}.
   * @param output Path to the output file or directory where processed files should be saved. This should correspond to {@link Parameters#output()}.
   * @throws IOException if the input file or directory does not exist, or if no files are found within the directory.
   * @throws XMLStreamException if an error occurs during file processing, propagated from {@link ModelPolisherCLILauncher#processFile(File, File)}.
   */
  private void batchProcess(File input, File output) throws IOException, XMLStreamException {
    // If the input is a directory, process each file within it
    if (input.isDirectory()) {
      File[] files = input.listFiles();

      if (files == null || files.length < 1) {
        logger.info(MESSAGES.getString("NO_FILES_ERROR"));
        return;
      }
      // Recursively process each file in the directory
      for (File file : files) {
        File target = SBMLFileUtils.getOutputFileName(file, output);
        batchProcess(file, target);
      }
    } else {
      // NOTE: input is a single file, but output can be a file or a directory
      // Adjust output file name if the output is a directory
      var newOutput = output.isDirectory() ? SBMLFileUtils.getOutputFileName(input, output) : output;

      processFile(input, newOutput);
    }
  }


  private void processFile(File input, File output) throws XMLStreamException, IOException {
    long startTime = System.currentTimeMillis();

    SBMLDocument doc = new ModelReader(parameters).read(input);
    if (doc == null) return;

    // Polish and annotate
    var mp = new ModelPolisher(parameters);
    mp.addObserver(new PolisherProgressBar());
    // Polish the document
    mp.polish(doc);

    var ma = new ModelAnnotator(parameters);
    ma.annotate(doc);

    new ModelWriter(parameters).write(doc, output, getVersionNumber());

    if (parameters.SBMLValidation()) {
      var mv = new ModelValidator(parameters);
      // use offline validation
      mv.validate(output, false);
    }

    // Log the time taken to process the file
    long timeTaken = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime);
    logger.info(String.format(MESSAGES.getString("FINISHED_TIME"), (timeTaken / 60), (timeTaken % 60)));
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
    options.add(AnnotateDBOptions.class);
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
    options.add(AnnotateDBOptions.class);
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
      return new URI(baseBundle.getString("LICENSE")).toURL();
    } catch (MalformedURLException | URISyntaxException exc) {
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
