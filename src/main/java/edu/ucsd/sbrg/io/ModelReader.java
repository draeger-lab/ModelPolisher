package edu.ucsd.sbrg.io;

import de.zbit.util.ResourceManager;
import edu.ucsd.sbrg.Parameters;
import edu.ucsd.sbrg.io.parsers.cobra.COBRAParser;
import edu.ucsd.sbrg.io.parsers.json.JSONParser;
import edu.ucsd.sbrg.resolver.Registry;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.text.MessageFormat.format;

public class ModelReader {

    private static final ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");
    private static final Logger logger = Logger.getLogger(ModelReader.class.getName());

    private final Parameters parameters;
    private final Registry registry;

    public ModelReader(Parameters parameters, Registry registry) {
        this.parameters = parameters;
        this.registry = registry;
    }


    public SBMLDocument read(File input) throws IOException, XMLStreamException {
        logger.info(format(MESSAGES.getString("READ_FILE_INFO"), input.getAbsolutePath()));
        // Determine the file type of the input file
        var fileType = SBMLFileUtils.getFileType(input);
        // Handle unknown file types by checking and updating HTML tags
        if (fileType.equals(SBMLFileUtils.FileType.UNKNOWN)) {
            checkHTMLTags(input);
            fileType = SBMLFileUtils.getFileType(input); // Re-check file type after updating tags
            // Abort processing if file type is still unknown
            if (fileType.equals(SBMLFileUtils.FileType.UNKNOWN)) {
                logger.warning(format(MESSAGES.getString("INPUT_UNKNOWN"), input.getPath()));
                // TODO: this is not graceful
                throw new IllegalArgumentException("Could not identify file type. Supported file types are: "
                + Arrays.stream(SBMLFileUtils.FileType.values())
                        .map(SBMLFileUtils.FileType::name)
                        .collect(Collectors.joining(", ")));
            }
        }

        SBMLDocument doc;

        // Determine the file type and parse accordingly
        if (fileType.equals(SBMLFileUtils.FileType.MAT_FILE)) {
            doc = new COBRAParser(parameters, registry).parse(input);
        } else if (fileType.equals(SBMLFileUtils.FileType.JSON_FILE)) {
            doc = new JSONParser(registry).parse(input);
        } else {
            checkHTMLTags(input);
            doc = SBMLReader.read(input, new UpdateListener());
        }

        // Check if the document was successfully parsed
        if (doc == null) {
            logger.severe(format(MESSAGES.getString("ALL_DOCS_PARSE_ERROR"), input.toString()));
            throw new RuntimeException("Document parsing failed.");
        }
        return doc;
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

    @Override
    public String toString() {
        return "ModelReader{" +
                "parameters=" + parameters +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModelReader that = (ModelReader) o;
        return Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(parameters);
    }
}
