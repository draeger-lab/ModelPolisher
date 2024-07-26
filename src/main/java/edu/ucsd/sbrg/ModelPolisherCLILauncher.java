package edu.ucsd.sbrg;

import static java.text.MessageFormat.format;

import java.awt.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import edu.ucsd.sbrg.annotation.AnnotationException;
import edu.ucsd.sbrg.annotation.adb.ADBSBMLAnnotator;
import edu.ucsd.sbrg.annotation.bigg.BiGGSBMLAnnotator;
import edu.ucsd.sbrg.io.*;
import edu.ucsd.sbrg.parameters.CommandLineParameters;
import edu.ucsd.sbrg.db.adb.AnnotateDB;
import edu.ucsd.sbrg.db.bigg.BiGGDB;
import edu.ucsd.sbrg.polishing.SBMLPolisher;
import edu.ucsd.sbrg.reporting.PolisherProgressBar;
import edu.ucsd.sbrg.reporting.ProgressInitialization;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import edu.ucsd.sbrg.resolver.Registry;
import edu.ucsd.sbrg.resolver.identifiersorg.IdentifiersOrg;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;

import de.zbit.AppConf;
import de.zbit.Launcher;
import de.zbit.util.ResourceManager;
import de.zbit.util.logging.LogOptions;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.SBProperties;
import edu.ucsd.sbrg.db.adb.AnnotateDBOptions;
import edu.ucsd.sbrg.db.bigg.BiGGDBOptions;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;

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
 * - Output management including file writing, COMBINE archive creation, and outputType.
 * <p>
 * This class also handles error logging and provides detailed logging of the processing steps.
 * 
 * @author Andreas Dr&auml;ger
 */
public class ModelPolisherCLILauncher extends Launcher {

  private static final ResourceBundle baseBundle = ResourceManager.getBundle("edu.ucsd.sbrg.Messages");
  private static final ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");
  private static final Logger logger = Logger.getLogger(ModelPolisherCLILauncher.class.getName());

  private CommandLineParameters parameters;
  private Registry registry;
  private BiGGDB bigg;
  private AnnotateDB adb;

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
   * @param appConf
   *        from super class initialization, holds commandline arguments
   * @see de.zbit.Launcher#commandLineMode(de.zbit.AppConf)
   */
  @Override
  public void commandLineMode(AppConf appConf) {
    SBProperties args = appConf.getCmdArgs();

    parameters = new CommandLineParameters(args);
    registry = new IdentifiersOrg();

    if (parameters.annotationParameters().biggAnnotationParameters().annotateWithBiGG()) {
      this.bigg = new BiGGDB(parameters.annotationParameters().biggAnnotationParameters().dbParameters());
    }

    if (parameters.annotationParameters().adbAnnotationParameters().addADBAnnotations()) {
      this.adb = new AnnotateDB(parameters.annotationParameters().adbAnnotationParameters().dbParameters());
    }

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

    } catch (ModelReaderException | ModelValidatorException | ModelWriterException | IOException |
             AnnotationException e) {
      // TODO: produce some user-friendly output and log to a file that can be provided for trouble-shooting
      throw new RuntimeException(e);
    } catch (IllegalArgumentException exc1) {
      // TODO: produce some user-friendly output and log to a file that can be provided for trouble-shooting
      throw new IllegalArgumentException(exc1.getLocalizedMessage());
    } catch (SQLException e) {
      // TODO: produce some user-friendly output and log to a file that can be provided for trouble-shooting
      throw new RuntimeException(e);
    }
  }


  /**
   * Processes the specified input and output paths. If the input is a directory, it recursively processes each file within.
   *
   * @param input  Path to the input file or directory to be processed. This should correspond to {@link CommandLineParameters#input()}.
   * @param output Path to the output file or directory where processed files should be saved. This should correspond to {@link CommandLineParameters#output()}.
   */
  private void batchProcess(File input, File output) throws ModelReaderException, ModelWriterException, ModelValidatorException, AnnotationException, SQLException {
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


  private void processFile(File input, File output) throws ModelReaderException, ModelWriterException, ModelValidatorException, AnnotationException, SQLException {
    long startTime = System.currentTimeMillis();

    SBMLDocument doc = new ModelReader(parameters.sboParameters(), registry).read(input);
    // TODO: we should be doing better sanity checking here; e.g.: just validate the SBML
    if (doc == null) return;
    Model model = doc.getModel();
    if (model == null) {
      logger.severe(MESSAGES.getString("MODEL_MISSING"));
      // TODO: handle this better
      throw new RuntimeException(MESSAGES.getString("MODEL_MISSING"));
    }

    List<ProgressObserver> polishingObservers = List.of(new PolisherProgressBar());
    int count = getPolishingTaskCount(model);
    for (var o : polishingObservers) {
      o.initialize(new ProgressInitialization(count));
    }

    new SBMLPolisher(
            parameters.polishingParameters(),
            parameters.sboParameters(),
            registry, polishingObservers).polish(doc);

    for (var o : polishingObservers) {
      o.finish(null);
    }

    List<ProgressObserver> annotationObservers = List.of(new PolisherProgressBar());
    int annotationTaskCount = getAnnotationTaskCount(model);
    for (var o : annotationObservers) {
      o.initialize(new ProgressInitialization(annotationTaskCount));
    }

    if (parameters.annotationParameters().biggAnnotationParameters().annotateWithBiGG()) {
      new BiGGSBMLAnnotator(bigg, parameters.annotationParameters().biggAnnotationParameters(), parameters.sboParameters(),
              registry, annotationObservers).annotate(doc);
    }

    if (parameters.annotationParameters().adbAnnotationParameters().addADBAnnotations()) {
      new ADBSBMLAnnotator(adb, parameters.annotationParameters().adbAnnotationParameters()).annotate(doc);
    }

    for (var o : annotationObservers) {
      o.finish(null);
    }

    output = new ModelWriter(parameters.outputType()).write(doc, output);

    if (parameters.SBMLValidation()) {
      var mv = new ModelValidator();
      // use offline validation
      mv.validate(output);
    }

    // Log the time taken to process the file
    long timeTaken = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime);
    logger.info(String.format(MESSAGES.getString("FINISHED_TIME"), (timeTaken / 60), (timeTaken % 60)));
  }

  private int getPolishingTaskCount(Model model) {
    // Calculate the total number of tasks to initialize the progress bar.
    int count = 1 // Account for model properties
            // + model.getUnitDefinitionCount()
            // TODO: see UnitPolisher TODO for why UnitDefinitionCount is replaced by 1
            + 1
            + model.getCompartmentCount()
            + model.getParameterCount()
            + model.getReactionCount()
            + model.getSpeciesCount()
            + model.getInitialAssignmentCount();

    // Include tasks from FBC plugin if present.
    if (model.isSetPlugin(FBCConstants.shortLabel)) {
      FBCModelPlugin fbcModelPlug = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
      count += fbcModelPlug.getObjectiveCount() + fbcModelPlug.getGeneProductCount();
    }
    return count;
  }

  private int getAnnotationTaskCount(Model model) {
    int annotationTaskCount = model.getCompartmentCount() + model.getSpeciesCount() + model.getReactionCount() + 50;
    if (model.isSetPlugin(FBCConstants.shortLabel)) {
      FBCModelPlugin fbcModelPlugin = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
      annotationTaskCount += fbcModelPlugin.getGeneProductCount();
    }
    return annotationTaskCount;
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
