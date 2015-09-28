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
import de.zbit.io.ZIPUtils;
import de.zbit.io.filefilter.SBFileFilter;
import de.zbit.util.ResourceManager;
import de.zbit.util.Utils;
import de.zbit.util.logging.LogOptions;
import de.zbit.util.logging.LogUtil;
import de.zbit.util.logging.OneLineFormatter;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.SBProperties;
import edu.ucsd.sbrg.bigg.ModelPolisherOptions.Compression;


/**
 * @author Andreas Dr&auml;ger
 *
 */
public class ModelPolisher extends Launcher {

  /**
   * Generated serial version identifier.
   */
  private static final long serialVersionUID = 7745344693995142413L;

  /**
   * Localization support.
   */
  private static final transient ResourceBundle baseBundle = ResourceManager.getBundle("edu.ucsd.sbrg.Messages");

  /**
   * A {@link Logger} for this class.
   */
  private static final transient Logger logger = Logger.getLogger(ModelPolisher.class.getName());

  /**
   * @param args
   */
  public ModelPolisher(String... args) {
    super(args);
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
        /* (non-Javadoc)
         * @see java.util.logging.StreamHandler#flush()
         */
        @Override
        public synchronized void flush() {
          System.out.flush();
        }
      }, getLogPackages());
    }
    try {
      // Connect to database and launch application:
      BiGGDB bigg = new BiGGDB(new PostgreSQLConnector(
        args.getProperty(DBOptions.HOST),
        args.getIntProperty(DBOptions.PORT),
        args.getProperty(DBOptions.USER),
        args.getProperty(DBOptions.PASSWD),
        args.getProperty(DBOptions.DBNAME)));

      // Gives users the choice to pass an alternative model notes XHTML file to the program.
      File modelNotesFile = null;
      if (args.containsKey(ModelPolisherOptions.MODEL_NOTES_FILE)) {
        modelNotesFile = new File(args.getProperty(ModelPolisherOptions.MODEL_NOTES_FILE));
      }
      String documentTitlePattern = null;
      if (args.containsKey(ModelPolisherOptions.DOCUMENT_TITLE_PATTERN)) {
        documentTitlePattern = args.getProperty(ModelPolisherOptions.DOCUMENT_TITLE_PATTERN);
      }

      // run polishing operations in background and parallel.
      batchProcess(bigg,
        new File(args.getProperty(IOOptions.INPUT)),
        new File(args.getProperty(IOOptions.OUTPUT)),
        ModelPolisherOptions.Compression.valueOf(args.getProperty(ModelPolisherOptions.COMPRESSION_TYPE)),
        args.getBooleanProperty(ModelPolisherOptions.OMIT_GENERIC_TERMS),
        modelNotesFile, documentTitlePattern,
        args.getBooleanProperty(ModelPolisherOptions.CHECK_MASS_BALANCE),
        args.getBooleanProperty(ModelPolisherOptions.SBML_VALIDATION));

    } catch (SBMLException | XMLStreamException | IOException | SQLException | ClassNotFoundException exc) {
      exc.printStackTrace();
    }
  }


  /**
   * 
   * @param bigg
   * @param input
   * @param output
   * @param compressOutput !
   * @param omitGenericTerms
   * @param modelNotesFile can be {@code null}.
   * @param documentTitlePattern can be {@code null} (then a default is used).
   * @param checkMassBalance
   * @param validateOutput
   * @throws XMLStreamException
   * @throws IOException
   */
  public void polish(BiGGDB bigg, File input, File output, Compression compressOutput, boolean omitGenericTerms, File modelNotesFile, String documentTitlePattern, boolean checkMassBalance, boolean validateOutput)
      throws XMLStreamException, IOException {
    long time = System.currentTimeMillis();

    logger.info(MessageFormat.format("Reading input file {0}.", input.getAbsolutePath()));
    SBMLDocument doc = SBMLReader.read(input);
    if (!doc.isSetLevelAndVersion() || (doc.getLevelAndVersion().compareTo(ValuePair.of(3, 1)) < 0)) {
      logger.info("Trying to convert the model to Level 3 Version 2.");
      org.sbml.jsbml.util.SBMLtools.setLevelAndVersion(doc, 3, 1);
    }

    SBMLPolisher polisher = new SBMLPolisher(bigg);
    polisher.setCheckMassBalance(checkMassBalance);
    polisher.setOmitGenericTerms(omitGenericTerms);
    if (modelNotesFile != null) {
      polisher.setModelNotesFile(modelNotesFile);
    }
    if (documentTitlePattern != null) {
      polisher.setDocumentTitlePattern(documentTitlePattern);
    }

    doc = polisher.polish(doc);

    // <?xml-stylesheet type="text/xsl" href="/Users/draeger/Documents/workspace/BioNetView/resources/edu/ucsd/sbrg/bigg/bigg_sbml.xsl"?>

    logger.info(MessageFormat.format("Writing output file {0}", output.getAbsolutePath()));
    TidySBMLWriter.write(doc, output, ' ', (short) 2);
    //SBMLWriter.write(doc, sbmlFile, ' ', (short) 2);

    if (compressOutput != Compression.NONE) {
      String fileExtension = compressOutput.getFileExtension();
      String archive = output.getAbsolutePath() + "." + fileExtension;
      logger.info(MessageFormat.format("Packing archive file {0}", archive));
      switch (compressOutput) {
      case ZIP:
        ZIPUtils.ZIPcompress(new String[] {output.getAbsolutePath()}, archive, "SBML Archive", true);
        break;
      case GZIP:
        ZIPUtils.GZip(output.getAbsolutePath(), archive);
        break;
      default:
        break;
      }
      if (validateOutput) {
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

    logger.info(MessageFormat.format(
      "There {0,choice,0#are no errors|1#is one error|1<are {0,number,integer} errors} in file {1}.",
      sbmlErrorLog.getErrorCount(), filename));

    // printErrors
    for (int j = 0; j < sbmlErrorLog.getErrorCount(); j++) {
      SBMLError error = sbmlErrorLog.getError(j);
      logger.warning(error.toString());
    }
  }


  /**
   * 
   * @param bigg
   * @param input
   * @param output
   * @param compressOutput
   * @param omitGenericTerms
   * @param modelNotesFile can be {@code null}
   * @param documentTitlePattern can be {@code null}
   * @param validateOutput
   * @param checkMassBalance
   * @throws IOException
   * @throws XMLStreamException
   */
  public void batchProcess(BiGGDB bigg, File input, File output, ModelPolisherOptions.Compression compressOutput, boolean omitGenericTerms, File modelNotesFile, String documentTitlePattern, boolean checkMassBalance, boolean validateOutput) throws IOException, XMLStreamException {
    if (!output.exists() && !output.isFile() && !(input.isFile() && input.getName().equals(output.getName()))) {
      logger.info(MessageFormat.format("Creating directory {0}.", output.getAbsolutePath()));
      output.mkdir();
    }
    if (input.isFile()) {
      if (SBFileFilter.isSBMLFile(input)) {
        if (output.isDirectory()) {
          output = new File(Utils.ensureSlash(output.getAbsolutePath()) + input.getName());
        }
        polish(bigg, input, output, compressOutput, omitGenericTerms, modelNotesFile, documentTitlePattern, checkMassBalance, validateOutput);
      }
    } else {
      if (!output.isDirectory()) {
        throw new IOException(MessageFormat.format("Cannot write to file {0}.", output.getAbsolutePath()));
      }
      for (File file : input.listFiles()) {
        File target = new File(Utils.ensureSlash(output.getAbsolutePath()) + file.getName());
        batchProcess(bigg, file, target, compressOutput, omitGenericTerms, modelNotesFile, documentTitlePattern, checkMassBalance, validateOutput);
      }
    }
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
   * @see de.zbit.Launcher#getInstitute()
   */
  @Override
  public String getInstitute() {
    return baseBundle.getString("INSTITUTE");
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
      return new URL("http://creativecommons.org/licenses/by-nc-sa/4.0/");
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


  /* (non-Javadoc)
   * @see de.zbit.Launcher#getYearOfProgramRelease()
   */
  @Override
  public short getYearOfProgramRelease() {
    return 2015;
  }


  /* (non-Javadoc)
   * @see de.zbit.Launcher#getYearWhenProjectWasStarted()
   */
  @Override
  public short getYearWhenProjectWasStarted() {
    return 2014;
  }


  /* (non-Javadoc)
   * @see de.zbit.Launcher#initGUI(de.zbit.AppConf)
   */
  @Override
  public Window initGUI(AppConf appConf) {
    return null;
  }


  /**
   * @param args
   */
  public static void main(String[] args) {
    new ModelPolisher(args);
  }

}
