/**
 * 
 */
package edu.ucsd.sbrg.bigg;

import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.ConsoleHandler;
import java.util.logging.ErrorManager;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLError;
import org.sbml.jsbml.SBMLErrorLog;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.TidySBMLWriter;
import org.sbml.jsbml.util.ValuePair;
import org.sbml.jsbml.validator.SBMLValidator;

import de.zbit.AppConf;
import de.zbit.Launcher;
import de.zbit.io.FileTools;
import de.zbit.io.ZIPUtils;
import de.zbit.io.filefilter.SBFileFilter;
import de.zbit.util.ResourceManager;
import de.zbit.util.Utils;
import de.zbit.util.logging.LogOptions;
import de.zbit.util.logging.LogUtil;
import de.zbit.util.logging.OneLineFormatter;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.SBProperties;
import edu.ucsd.sbrg.bigg.ModelPolisherOptions.Compression;
import edu.ucsd.sbrg.cobra.COBRAparser;
import edu.ucsd.sbrg.util.UpdateListener;


/**
 * @author Andreas Dr&auml;ger
 *
 */
public class ModelPolisher extends Launcher {

  /**
   * Helper class to store all parameters for running ModelPolisher in batch mode.
   * 
   * @author Andreas Dr&auml;ger
   */
  private static final class Parameters {
    /**
     * @see ModelPolisherOptions#CHECK_MASS_BALANCE
     */
    Boolean checkMassBalance = null;
    /**
     * @see ModelPolisherOptions#COMPRESSION_TYPE
     */
    Compression compression = Compression.NONE;
    /**
     * Can be {@code null}
     * @see ModelPolisherOptions#DOCUMENT_NOTES_FILE
     */
    File documentNotesFile = null;
    /**
     * Can be {@code null} (then a default is used).
     * @see ModelPolisherOptions#DOCUMENT_TITLE_PATTERN
     */
    String documentTitlePattern = null;
    /**
     * @see ModelPolisherOptions#FLUX_COEFFICIENTS
     */
    double[] fluxCoefficients = null;
    /**
     * @see ModelPolisherOptions#FLUX_OBJECTIVES
     */
    String[] fluxObjectives = null;
    /**
     * Can be {@code null}
     * @see ModelPolisherOptions#MODEL_NOTES_FILE
     */
    File modelNotesFile = null;
    /**
     * @see ModelPolisherOptions#OMIT_GENERIC_TERMS
     */
    Boolean omitGenericTerms = null;
    /**
     * @see ModelPolisherOptions#SBML_VALIDATION
     */
    Boolean sbmlValidation = null;
  }

  /**
   * Localization support.
   */
  private static final transient ResourceBundle baseBundle = ResourceManager.getBundle("edu.ucsd.sbrg.Messages");

  /**
   * A {@link Logger} for this class.
   */
  private static final transient Logger logger = Logger.getLogger(ModelPolisher.class.getName());

  /**
   * Generated serial version identifier.
   */
  private static final long serialVersionUID = 7745344693995142413L;

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

  /* (non-Javadoc)
   * @see de.zbit.Launcher#addCopyrightToSplashScreen()
   */
  @Override
  protected boolean addCopyrightToSplashScreen() {
    return false;
  }

  /* (non-Javadoc)
   * @see de.zbit.Launcher#addVersionNumberToSplashScreen()
   */
  @Override
  protected boolean addVersionNumberToSplashScreen() {
    return false;
  }

  /**
   * 
   * @param bigg
   * @param input
   * @param output
   * @param parameters
   * @throws IOException
   * @throws XMLStreamException
   */
  public void batchProcess(BiGGDB bigg, File input, File output, Parameters parameters) throws IOException,
  XMLStreamException {

    if (!output.exists() && !output.isFile()
        && !(input.isFile() && input.getName().equals(output.getName()))) {
      logger.info(MessageFormat.format("Creating directory {0}.",
        output.getAbsolutePath()));
      output.mkdir();
    }
    if (input.isFile()) {
      boolean matFile = SBFileFilter.hasFileType(input, SBFileFilter.FileType.MAT_FILES);
      if (SBFileFilter.isSBMLFile(input) || matFile) {
        if (output.isDirectory()) {
          String fName = input.getName();
          if (matFile) {
            fName = FileTools.removeFileExtension(fName) + ".xml";
          }
          output = new File(Utils.ensureSlash(output.getAbsolutePath()) + fName);
        }
        polish(bigg, input, output, parameters);
      }
    } else {
      if (!output.isDirectory()) {
        throw new IOException(MessageFormat.format("Cannot write to file {0}.",
          output.getAbsolutePath()));
      }
      for (File file : input.listFiles()) {
        File target = new File(Utils.ensureSlash(output.getAbsolutePath()) + file.getName());
        batchProcess(bigg, file, target, parameters);
      }
    }
  }

  /* (non-Javadoc)
   * @see de.zbit.Launcher#commandLineMode(de.zbit.AppConf)
   */
  @Override
  public void commandLineMode(AppConf appConf) {
    SBProperties args = appConf.getCmdArgs();
    if (args.containsKey(LogOptions.LOG_FILE)) {
      LogUtil.addHandler(new ConsoleHandler() {
        /**
         * Formatter
         */
        OneLineFormatter formatter = new OneLineFormatter(false, false, false);
        /* (non-Javadoc)
         * @see java.util.logging.StreamHandler#flush()
         */
        @Override
        public synchronized void flush() {
          System.out.flush();
        }
        /* (non-Javadoc)
         * @see java.util.logging.ConsoleHandler#publish(java.util.logging.LogRecord)
         */
        @Override
        public void publish(LogRecord record) {
          if (record.getLevel().intValue() == Level.INFO.intValue()) {
            try {
              String message = formatter.format(record);
              System.out.write(message.getBytes());
            } catch (IOException exc) {
              reportError(null, exc, ErrorManager.FORMAT_FAILURE);
              return;
            }
          }
        }
      }, getLogPackages());
    }
    try {
      // Connect to database and launch application:
      String passwd = args.getProperty(DBOptions.PASSWD);
      BiGGDB bigg = new BiGGDB(new PostgreSQLConnector(
        args.getProperty(DBOptions.HOST),
        args.getIntProperty(DBOptions.PORT),
        args.getProperty(DBOptions.USER),
        passwd != null ? passwd : "",
          args.getProperty(DBOptions.DBNAME)));

      // Gives users the choice to pass an alternative model notes XHTML file to the program.
      File modelNotesFile = parseFileOption(args, ModelPolisherOptions.MODEL_NOTES_FILE);
      File documentNotesFile = parseFileOption(args, ModelPolisherOptions.DOCUMENT_NOTES_FILE);
      String documentTitlePattern = null;
      if (args.containsKey(ModelPolisherOptions.DOCUMENT_TITLE_PATTERN)) {
        documentTitlePattern = args.getProperty(ModelPolisherOptions.DOCUMENT_TITLE_PATTERN);
      }

      double[] coefficients = null;
      if (args.containsKey(ModelPolisherOptions.FLUX_COEFFICIENTS)) {
        String c = args.getProperty(ModelPolisherOptions.FLUX_COEFFICIENTS);
        String coeff[] = c.substring(1, c.length() - 1).split(",");
        coefficients = new double[coeff.length];
        for (int i = 0; i < coeff.length; i++) {
          coefficients[i] = Double.parseDouble(coeff[i].trim());
        }
      }
      String fObj[] = null;
      if (args.containsKey(ModelPolisherOptions.FLUX_OBJECTIVES)) {
        String fObjectives = args.getProperty(ModelPolisherOptions.FLUX_OBJECTIVES);
        fObj = fObjectives.substring(1, fObjectives.length() - 1).split(":");
      }

      Parameters parameters = new Parameters();
      parameters.checkMassBalance = args.getBooleanProperty(ModelPolisherOptions.CHECK_MASS_BALANCE);
      parameters.compression = ModelPolisherOptions.Compression.valueOf(args.getProperty(ModelPolisherOptions.COMPRESSION_TYPE));
      parameters.documentNotesFile = documentNotesFile;
      parameters.documentTitlePattern = documentTitlePattern;
      parameters.fluxCoefficients = coefficients;
      parameters.fluxObjectives = fObj;
      parameters.modelNotesFile = modelNotesFile;
      parameters.omitGenericTerms = args.getBooleanProperty(ModelPolisherOptions.OMIT_GENERIC_TERMS);
      parameters.sbmlValidation = args.getBooleanProperty(ModelPolisherOptions.SBML_VALIDATION);

      // run polishing operations in background and parallel.
      batchProcess(bigg,
        new File(args.getProperty(IOOptions.INPUT)),
        new File(args.getProperty(IOOptions.OUTPUT)),
        parameters);


    } catch (SBMLException | XMLStreamException | IOException | SQLException | ClassNotFoundException exc) {
      exc.printStackTrace();
    }
  }

  /* (non-Javadoc)
   * @see de.zbit.Launcher#getCitation(boolean)
   */
  @Override
  public String getCitation(boolean HTMLstyle) {
    if (HTMLstyle) {
      return
          "<dl>\n" +
          "  <dt>King ZA, Lu JS, Dr&#228;ger A, Miller PC, Federowicz S, Lerman JA, Ebrahim A, Palsson BO, and Lewis NE. (2015).\n" +
          "    <dd>\n" +
          "      BiGG Models: A platform for integrating, standardizing, and sharing genome-scale models. <i>Nucl Acids Res</i>.\n" +
          "      <a href=\"https://dx.doi.org/10.1093/nar/gkv1049\" target=\"_blank\"\n" +
          "      title=\"Access the publication about BiGG Models knowledgebase\">doi:10.1093/nar/gkv1049</a>\n" +
          "    </dd>\n" +
          "  </dt>\n" +
          "</dl>";
    }
    return "King ZA, Lu JS, Dräger A, Miller PC, Federowicz S, Lerman JA, Ebrahim A, Palsson BO, and Lewis NE. (2015). " +
    "BiGG Models: A platform for integrating, standardizing, and sharing genome-scale models. Nucl Acids Res, " +
    "doi:10.1093/nar/gkv1049.";
  }

  /* (non-Javadoc)
   * @see de.zbit.Launcher#getCmdLineOptions()
   */
  @Override
  public List<Class<? extends KeyProvider>> getCmdLineOptions() {
    List<Class<? extends KeyProvider>> options = new LinkedList<>();
    options.add(LogOptions.class);
    options.add(DBOptions.class);
    options.add(ModelPolisherOptions.class);
    options.add(IOOptions.class);
    return options;
  }

  /* (non-Javadoc)
   * @see de.zbit.Launcher#getInstitute()
   */
  @Override
  public String getInstitute() {
    return baseBundle.getString("INSTITUTE");
  }

  /* (non-Javadoc)
   * @see de.zbit.Launcher#getInteractiveOptions()
   */
  @Override
  public List<Class<? extends KeyProvider>> getInteractiveOptions() {
    List<Class<? extends KeyProvider>> options = new LinkedList<>();
    options.add(DBOptions.class);
    options.add(ModelPolisherOptions.class);
    return options;
  }

  /* (non-Javadoc)
   * @see de.zbit.Launcher#getLogPackages()
   */
  @Override
  public String[] getLogPackages() {
    return new String[] {"edu.ucsd", "de.zbit"};
  }

  /* (non-Javadoc)
   * @see de.zbit.Launcher#getOrganization()
   */
  @Override
  public String getOrganization() {
    return baseBundle.getString("ORGANIZATION");
  }

  /* (non-Javadoc)
   * @see de.zbit.Launcher#getProvider()
   */
  @Override
  public String getProvider() {
    return baseBundle.getString("PROVIDER");
  }

  /* (non-Javadoc)
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

  /* (non-Javadoc)
   * @see de.zbit.Launcher#getURLOnlineUpdate()
   */
  @Override
  public URL getURLOnlineUpdate() {
    return null;
  }

  /* (non-Javadoc)
   * @see de.zbit.Launcher#getVersionNumber()
   */
  @Override
  public String getVersionNumber() {
    String version = getClass().getPackage().getImplementationVersion();
    return version == null ? "?" : version;
  }

  /* (non-Javadoc)
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

  /* (non-Javadoc)
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

  /* (non-Javadoc)
   * @see de.zbit.Launcher#initGUI(de.zbit.AppConf)
   */
  @Override
  public Window initGUI(AppConf appConf) {
    return null;
  }

  /**
   * Scans the given command-line options for a specific file option and returns
   * the corresponding file if it exists, {@code null} otherwise.
   * 
   * @param args
   *        command-line options.
   * @param option
   *        a specific file option to look for.
   * @return a {@link File} object that corresponds to a desired command-line
   *         option, or {@code null} if it does not exist.
   */
  private File parseFileOption(SBProperties args, Option<File> option) {
    if (args.containsKey(option)) {
      File notesFile = new File(args.getProperty(option));
      if ((notesFile != null) && notesFile.exists() && notesFile.canRead()) {
        return notesFile;
      }
    }
    return null;
  }

  /**
   * 
   * @param bigg
   * @param input
   * @param output
   * @param parameters
   * @throws XMLStreamException
   * @throws IOException
   */
  public void polish(BiGGDB bigg, File input, File output, Parameters parameters)
      throws XMLStreamException, IOException {
    long time = System.currentTimeMillis();
    logger.info(MessageFormat.format("Reading input file {0}.",
      input.getAbsolutePath()));
    SBMLDocument doc = null;
    if (SBFileFilter.hasFileType(input, SBFileFilter.FileType.MAT_FILES)) {
      doc = COBRAparser.read(input, parameters.omitGenericTerms);
    } else {
      doc = SBMLReader.read(input, new UpdateListener());
    }
    if (!doc.isSetLevelAndVersion()
        || (doc.getLevelAndVersion().compareTo(ValuePair.of(3, 1)) < 0)) {
      logger.info("Trying to convert the model to Level 3 Version 2.");
      org.sbml.jsbml.util.SBMLtools.setLevelAndVersion(doc, 3, 1);
    }
    SBMLPolisher polisher = new SBMLPolisher(bigg);
    polisher.setCheckMassBalance(parameters.checkMassBalance);
    polisher.setOmitGenericTerms(parameters.omitGenericTerms);
    if (parameters.documentNotesFile != null) {
      polisher.setDocumentNotesFile(parameters.documentNotesFile);
    }
    if (parameters.modelNotesFile != null) {
      polisher.setModelNotesFile(parameters.modelNotesFile);
    }
    if (parameters.documentTitlePattern != null) {
      polisher.setDocumentTitlePattern(parameters.documentTitlePattern);
    }
    polisher.setFluxCoefficients(parameters.fluxCoefficients);
    polisher.setFluxObjectives(parameters.fluxObjectives);
    doc = polisher.polish(doc);
    // <?xml-stylesheet type="text/xsl"
    // href="/Users/draeger/Documents/workspace/BioNetView/resources/edu/ucsd/sbrg/bigg/bigg_sbml.xsl"?>
    logger.info(MessageFormat.format("Writing output file {0}",
      output.getAbsolutePath()));
    TidySBMLWriter.write(doc, output, getClass().getSimpleName(), getVersionNumber(), ' ', (short) 2);
    // SBMLWriter.write(doc, sbmlFile, ' ', (short) 2);
    if (parameters.compression != Compression.NONE) {
      String fileExtension = parameters.compression.getFileExtension();
      String archive = output.getAbsolutePath() + "." + fileExtension;
      logger.info(MessageFormat.format("Packing archive file {0}", archive));
      switch (parameters.compression) {
      case ZIP:
        ZIPUtils.ZIPcompress(new String[] {output.getAbsolutePath()}, archive,
          "SBML Archive", true);
        break;
      case GZIP:
        ZIPUtils.GZip(output.getAbsolutePath(), archive);
        break;
      default:
        break;
      }
      if ((parameters.sbmlValidation != null) && parameters.sbmlValidation) {
        validate(archive);
      }
    }
    time = System.currentTimeMillis() - time;
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(time);
    logger.info(MessageFormat.format("Done ({0,time,ss.sss} s).", calendar.getTime()));
  }

  /**
   * 
   * @param filename
   */
  private void validate(String filename) {
    //org.sbml.jsbml.validator.SBMLValidator.main(new String[] {"-d", "p,u", compressedOutput});
    String output = "xml";
    String offcheck = "p,u";
    HashMap<String, String> parameters = new HashMap<String, String>();
    parameters.put("output", output);
    parameters.put("offcheck", offcheck);

    logger.info("Validating  " + filename + "\n");

    SBMLErrorLog sbmlErrorLog = SBMLValidator.checkConsistency(filename, parameters);

    if (sbmlErrorLog != null) {
      logger.info(MessageFormat.format(
        "There {0,choice,0#are no errors|1#is one error|1<are {0,number,integer} errors} in file {1}.",
        sbmlErrorLog.getErrorCount(), filename));

      // printErrors
      for (int j = 0; j < sbmlErrorLog.getErrorCount(); j++) {
        SBMLError error = sbmlErrorLog.getError(j);
        logger.warning(error.toString());
      }
    } else {
      logger.info("No SBML validation possible, process terminated with errors.");
    }
  }

}
