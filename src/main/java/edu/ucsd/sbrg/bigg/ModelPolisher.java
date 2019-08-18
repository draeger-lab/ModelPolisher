/*
 * 
 */
package edu.ucsd.sbrg.bigg;

import static java.text.MessageFormat.format;

import java.awt.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import de.unirostock.sems.cbarchive.ArchiveEntry;
import de.unirostock.sems.cbarchive.CombineArchive;
import de.unirostock.sems.cbarchive.meta.OmexMetaDataObject;
import de.unirostock.sems.cbarchive.meta.omex.OmexDescription;
import de.unirostock.sems.cbarchive.meta.omex.VCard;
import org.sbml.jsbml.*;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.sbml.jsbml.util.SBMLtools;
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
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.SBProperties;
import edu.ucsd.sbrg.bigg.ModelPolisherOptions.Compression;
import edu.ucsd.sbrg.cobra.COBRAparser;
import edu.ucsd.sbrg.json.JSONparser;
import edu.ucsd.sbrg.util.UpdateListener;
import org.sbml.jsbml.xml.XMLNode;
import org.sbml.jsbml.xml.parsers.SBMLRDFAnnotationParser;
import org.w3c.tidy.Tidy;

/**
 * @author Andreas Dr&auml;ger
 */
public class ModelPolisher extends Launcher {

  /**
   * Helper class to store all parameters for running ModelPolisher in batch
   * mode.
   * 
   * @author Andreas Dr&auml;ger
   */
  private static final class Parameters {

    /**
     * @see ModelPolisherOptions#INCLUDE_ANY_URI
     */
    Boolean includeAnyURI = null;
    /**
     * @see ModelPolisherOptions#ANNOTATE_WITH_BIGG
     */
    Boolean annotateWithBiGG = null;
    /**
     * @see ModelPolisherOptions#OUTPUT_COMBINE
     */
    Boolean outputCOMBINE = null;
    /**
     * @see ModelPolisherOptions#ADD_ADB_ANNOTATIONS
     */
    Boolean addADAAnnotations = null;
    /**
     * @see ModelPolisherOptions#CHECK_MASS_BALANCE
     */
    Boolean checkMassBalance = null;
    /**
     * @see ModelPolisherOptions#NO_MODEL_NOTES
     */
    Boolean noModelNotes = null;
    /**
     * @see ModelPolisherOptions#COMPRESSION_TYPE
     */
    Compression compression = Compression.NONE;
    /**
     * Can be {@code null}
     * 
     * @see ModelPolisherOptions#DOCUMENT_NOTES_FILE
     */
    File documentNotesFile = null;
    /**
     * Can be {@code null} (then a default is used).
     * 
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
     * 
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
   *
   */
  private Parameters parameters;
  /**
   *
   */
  private static BiGGDB bigg = null;
  /**
   *
   */
  private static AnnotateDB adb = null;
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

  /**
   * Type of current input file
   */
  private FileType fileType;


  /*
   * (non-Javadoc)
   * @see de.zbit.Launcher#commandLineMode(de.zbit.AppConf)
   */
  @Override
  public void commandLineMode(AppConf appConf) {
    SBProperties args = appConf.getCmdArgs();
    // Gives users the choice to pass an alternative model notes XHTML file to
    // the program.
    try {
      batchProcess(new File(args.getProperty(IOOptions.INPUT)), new File(args.getProperty(IOOptions.OUTPUT)), args);
    } catch (XMLStreamException | IOException exc) {
      exc.printStackTrace();
    }
    if (bigg != null) {
      bigg.closeConnection();
    }
    if (adb != null) {
      adb.closeConnection();
    }
  }


  /**
   * @param input
   * @param output
   * @throws IOException
   * @throws XMLStreamException
   */
  private void batchProcess(File input, File output, SBProperties args) throws IOException, XMLStreamException {
    if (input == null || !input.exists()) {
      throw new IOException(format(mpMessageBundle.getString("READ_FILE_ERROR"), input.toString()));
    }
    initParameters(args);
    // We test if path denotes a file or directory by checking if it contains at least one period in its name. If so, we
    // assume it is a file.
    checkCreateOutDir(input, output);
    if (input.isFile()) {
      processFile(input, output, args);
    } else {
      if (!output.isDirectory()) {
        throw new IOException(format(mpMessageBundle.getString("WRITE_DIR_TO_FILE_ERROR"), output.getAbsolutePath()));
      }
      File[] files = input.listFiles();
      if (files == null) {
        logger.severe(mpMessageBundle.getString("NO_FILES_ERROR"));
        return;
      }
      for (File file : files) {
        File target;
        fileType = getFileType(file);
        if (!fileType.equals(FileType.SBML_FILE)) {
          target = new File(
            Utils.ensureSlash(output.getAbsolutePath()) + FileTools.removeFileExtension(file.getName()) + ".xml");
        } else {
          target = new File(Utils.ensureSlash(output.getAbsolutePath()) + file.getName());
        }
        batchProcess(file, target, args);
      }
    }
  }


  /**
   * @param args:
   *        Arguments from commandline
   */
  private void initParameters(SBProperties args) {
    parameters = new Parameters();
    String documentTitlePattern = null;
    if (args.containsKey(ModelPolisherOptions.DOCUMENT_TITLE_PATTERN)) {
      documentTitlePattern = args.getProperty(ModelPolisherOptions.DOCUMENT_TITLE_PATTERN);
    }
    double[] coefficients = null;
    if (args.containsKey(ModelPolisherOptions.FLUX_COEFFICIENTS)) {
      String c = args.getProperty(ModelPolisherOptions.FLUX_COEFFICIENTS);
      String[] coeff = c.substring(1, c.length() - 1).split(",");
      coefficients = new double[coeff.length];
      for (int i = 0; i < coeff.length; i++) {
        coefficients[i] = Double.parseDouble(coeff[i].trim());
      }
    }
    String[] fObj = null;
    if (args.containsKey(ModelPolisherOptions.FLUX_OBJECTIVES)) {
      String fObjectives = args.getProperty(ModelPolisherOptions.FLUX_OBJECTIVES);
      fObj = fObjectives.substring(1, fObjectives.length() - 1).split(":");
    }
    parameters.annotateWithBiGG = args.getBooleanProperty(ModelPolisherOptions.ANNOTATE_WITH_BIGG);
    parameters.outputCOMBINE = args.getBooleanProperty(ModelPolisherOptions.OUTPUT_COMBINE);
    parameters.addADAAnnotations = args.getBooleanProperty(ModelPolisherOptions.ADD_ADB_ANNOTATIONS);
    parameters.checkMassBalance = args.getBooleanProperty(ModelPolisherOptions.CHECK_MASS_BALANCE);
    parameters.noModelNotes = args.getBooleanProperty(ModelPolisherOptions.NO_MODEL_NOTES);
    parameters.compression =
      ModelPolisherOptions.Compression.valueOf(args.getProperty(ModelPolisherOptions.COMPRESSION_TYPE));
    parameters.documentNotesFile = parseFileOption(args, ModelPolisherOptions.DOCUMENT_NOTES_FILE);
    parameters.documentTitlePattern = documentTitlePattern;
    parameters.fluxCoefficients = coefficients;
    parameters.fluxObjectives = fObj;
    parameters.includeAnyURI = args.getBooleanProperty(ModelPolisherOptions.INCLUDE_ANY_URI);
    parameters.modelNotesFile = parseFileOption(args, ModelPolisherOptions.MODEL_NOTES_FILE);
    parameters.omitGenericTerms = args.getBooleanProperty(ModelPolisherOptions.OMIT_GENERIC_TERMS);
    parameters.sbmlValidation = args.getBooleanProperty(ModelPolisherOptions.SBML_VALIDATION);
  }


  /**
   * Scans the given command-line options for a specific file option and
   * returns the corresponding file if it exists, {@code null} otherwise.
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
      if (notesFile.exists() && notesFile.canRead()) {
        return notesFile;
      }
    }
    return null;
  }


  /**
   * @param input
   * @param output
   */
  private void checkCreateOutDir(File input, File output) {
    if (!output.exists() && (output.getName().lastIndexOf('.') < 0)
      && (!input.isFile() || !input.getName().equals(output.getName()))) {
      logger.info(format(mpMessageBundle.getString("DIRECTORY_CREATED"), output.getAbsolutePath()));
      output.mkdir();
    }
  }


  /**
   * @param input
   * @param output
   * @param args
   * @throws XMLStreamException
   * @throws IOException
   */
  private void processFile(File input, File output, SBProperties args) throws XMLStreamException, IOException {
    // get fileType array and check if any value is true
    fileType = getFileType(input);
    if (fileType.equals(FileType.UNKNOWN)) {
      return;
    }
    if (output.isDirectory()) {
      String fName = input.getName();
      if (!fileType.equals(FileType.SBML_FILE)) {
        fName = FileTools.removeFileExtension(fName) + ".xml";
      }
      output = new File(Utils.ensureSlash(output.getAbsolutePath()) + fName);
    }
    getDB(args);
    readAndPolish(input, output);
  }

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
   * Get file type from input file
   *
   * @param input
   *        File used in {@link #batchProcess}
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
   * Sets DB to use, depending on provided arguments:
   * If annotateWithBigg is true and all arguments are provided, PostgreSQL is
   * used, if arguments are missing, the local SQLite DB is used instead
   *
   * @param args:
   *        Arguments from Commandline
   */
  private void getDB(SBProperties args) {
    if (!parameters.annotateWithBiGG || bigg != null) {
      return;
    }

    String bigg_dbName = args.getProperty(BiGGDBOptions.BiGG_DBNAME);
    String bigg_host = args.getProperty(BiGGDBOptions.BiGG_HOST);
    String bigg_passwd = args.getProperty(BiGGDBOptions.BiGG_PASSWD);
    String bigg_port = args.getProperty(BiGGDBOptions.BiGG_PORT);
    String bigg_user = args.getProperty(BiGGDBOptions.BiGG_USER);

    boolean runPSQL_bigg = iStrNotNullOrEmpty(bigg_dbName);
    runPSQL_bigg &= iStrNotNullOrEmpty(bigg_host);
    runPSQL_bigg &= iStrNotNullOrEmpty(bigg_port);
    runPSQL_bigg &= iStrNotNullOrEmpty(bigg_user);
    if (runPSQL_bigg) {
      try {
        // Connect to PostgreSQL database and launch application:
        bigg = new BiGGDB(new PostgreSQLConnector(bigg_host, new Integer(bigg_port), bigg_user, bigg_passwd != null ? bigg_passwd : "", bigg_dbName));
      } catch (SQLException | ClassNotFoundException exc) {
        exc.printStackTrace();
        System.exit(1);
      }
    } else {
      try {
        bigg = new BiGGDB(new SQLiteConnector());
      } catch (SQLException | ClassNotFoundException exc) {
        exc.printStackTrace();
        System.exit(1);
      }
    }


    if(!parameters.addADAAnnotations || adb==null){
      String adb_dbName = args.getProperty(ADBOptions.ADB_DBNAME);
      String adb_host = args.getProperty(ADBOptions.ADB_HOST);
      String adb_passwd = args.getProperty(ADBOptions.ADB_PASSWD);
      String adb_port = args.getProperty(ADBOptions.ADB_PORT);
      String adb_user = args.getProperty(ADBOptions.ADB_USER);

      boolean runPSQL_adb = iStrNotNullOrEmpty(adb_dbName);

      runPSQL_adb &= iStrNotNullOrEmpty(adb_host);
      runPSQL_adb &= iStrNotNullOrEmpty(adb_port);
      runPSQL_adb &= iStrNotNullOrEmpty(adb_user);
      if (runPSQL_adb) {
        try {
          // Connect to PostgreSQL database and launch application:
          PostgreSQLConnector psqlConnect = new PostgreSQLConnector(adb_host, new Integer(adb_port), adb_user, adb_passwd != null ? adb_passwd : "", adb_dbName);
          adb = new AnnotateDB(psqlConnect);
        } catch (SQLException | ClassNotFoundException exc) {
          exc.printStackTrace();
          System.exit(1);
        }
      }
    }
  }


  /**
   * @param input
   * @param output
   * @throws XMLStreamException
   * @throws IOException
   */
  private void readAndPolish(File input, File output) throws XMLStreamException, IOException {
    long time = System.currentTimeMillis();
    logger.info(format(mpMessageBundle.getString("READ_FILE_INFO"), input.getAbsolutePath()));
    List<SBMLDocument> docs = new ArrayList<>();
    // reading or parsing input
    if (fileType.equals(FileType.MAT_FILE)) {
      docs.addAll(COBRAparser.read(input, parameters.omitGenericTerms));
    } else if (fileType.equals(FileType.JSON_FILE)) {
      docs.add(JSONparser.read(input));
    } else {
      checkHTMLTags(input);
      docs.add(SBMLReader.read(input, new UpdateListener()));
    }
    if (docs.size() == 0) {
      logger.severe(format(mpMessageBundle.getString("ALL_DOCS_PARSE_ERROR"), input.toString()));
      return;
    }
    int count = 0;
    for (SBMLDocument doc : docs) {
      if (count != 0) {
        String newPath = FileTools.removeFileExtension(output.getPath());
        if (count > 1) {
          newPath = newPath.substring(0, newPath.length() - 2);
        }
        newPath += format("_{0}.xml", count);
        output = new File(newPath);
      }
      polish(doc, output);
      count++;
    }
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
    if (!doc.isSetLevelAndVersion() || (doc.getLevelAndVersion().compareTo(ValuePair.of(3, 1)) < 0)) {
      logger.info(mpMessageBundle.getString("TRY_CONV_LVL3_V1"));
      SBMLtools.setLevelAndVersion(doc, 3, 1);
    }
    // Polishing
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
    doc = polisher.polish(doc);
    // Annotation
    if (parameters.annotateWithBiGG) {
      BiGGAnnotation annotation = new BiGGAnnotation(bigg, adb, polisher);
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
      doc = annotation.annotate(doc);
    }

    //producing & writing glossary
    String glossary = getGlossary(doc);

    String glossaryLocation = output.getAbsolutePath().substring(0,output.getAbsolutePath().lastIndexOf('.'))+"_glossary.rdf";
    logger.info(format(mpMessageBundle.getString("WRITE_RDF_FILE_INFO"), glossaryLocation));
    writeTidyRDF(new File(glossaryLocation), glossary);

    //writing polished model
    logger.info(format(mpMessageBundle.getString("WRITE_FILE_INFO"), output.getAbsolutePath()));
    TidySBMLWriter.write(doc, output, getClass().getSimpleName(), getVersionNumber(), ' ', (short) 2);

    //produce COMBINE archive and delete output model and glossary
    if(parameters.outputCOMBINE) {
      try {
        String combineArcLocation = output.getAbsolutePath().substring(0, output.getAbsolutePath().lastIndexOf('.')) + ".zip";
        
        CombineArchive ca = new CombineArchive(new File(combineArcLocation));

        File outputXML = new File(output.getAbsolutePath());
        File outputRDF = new File(glossaryLocation);

        ArchiveEntry SBMLOutput = ca.addEntry(
                outputXML,
                "model.xml",
                new URI("http://identifiers.org/combine.specifications/sbml"),
                true);

        ArchiveEntry RDFOutput = ca.addEntry(
                outputRDF,
                "glossary.rdf",
                new URI(""),
                true);

        logger.info(format(mpMessageBundle.getString("WRITE_RDF_FILE_INFO"), combineArcLocation));

        ca.pack();
        ca.close();

        boolean rdfDeleted = outputRDF.delete();
        boolean outputXMLDeleted = outputXML.delete();
        logger.info(format(mpMessageBundle.getString("DELETE_FILE"),
                outputXML.getAbsolutePath().substring(outputXML.getAbsolutePath().lastIndexOf('/')),
                outputXMLDeleted));
        logger.info(format(mpMessageBundle.getString("DELETE_FILE"),
                outputRDF.getAbsolutePath().substring(outputXML.getAbsolutePath().lastIndexOf('/')),
                rdfDeleted));
      } catch (Exception e) {
        logger.warning("Exception to produce COMBINE Archive: " + e.toString());
      }
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
      if ((parameters.sbmlValidation != null) && parameters.sbmlValidation) {
        validate(archive);
      }
    }
  }

  /**
   * @param doc
   */
  private String getGlossary(SBMLDocument doc) throws XMLStreamException {
    SBMLRDFAnnotationParser rdfParser = new SBMLRDFAnnotationParser();
    if(rdfParser.writeAnnotation(doc.getModel(),null)!=null && rdfParser.writeAnnotation(doc.getModel(),null).getChild(1)!=null) {
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
    }else{
        return "";
    }
  }

  /**
   * @param outputFile, rdfString
   */
  private void writeTidyRDF(File outputFile, String rdfString) throws FileNotFoundException, UnsupportedEncodingException {
    Tidy tidy = new Tidy();  // obtain a new Tidy instance

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

    Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"));
    InputStreamReader in = new InputStreamReader(new ByteArrayInputStream(rdfString.toString().getBytes("UTF-8")), "UTF-8");

    tidy.parse(in, out);
  }


  /**
   * @param filename
   */
  private void validate(String filename) {
    String output = "xml";
    String offcheck = "p,u";
    Map<String, String> parameters = new HashMap<>();
    parameters.put("output", output);
    parameters.put("offcheck", offcheck);
    logger.info("Validating  " + filename + "\n");
    SBMLErrorLog sbmlErrorLog = SBMLValidator.checkConsistency(filename, parameters);
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
   * if(iStream == null){
   * }
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


  /**
   * @param string
   * @return
   */
  private boolean iStrNotNullOrEmpty(String string) {
    return !(string == null || string.isEmpty());
  }
}
