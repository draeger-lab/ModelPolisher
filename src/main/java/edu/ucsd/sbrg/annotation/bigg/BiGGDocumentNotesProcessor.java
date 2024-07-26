package edu.ucsd.sbrg.annotation.bigg;

import de.zbit.util.ResourceManager;
import edu.ucsd.sbrg.db.bigg.BiGGDB;
import edu.ucsd.sbrg.parameters.BiGGAnnotationParameters;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

public class BiGGDocumentNotesProcessor {

    static final Logger logger = Logger.getLogger(BiGGDocumentNotesProcessor.class.getName());
    private static final ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");

    private final BiGGAnnotationParameters parameters;
    private final BiGGDB bigg;

    public BiGGDocumentNotesProcessor(BiGGDB bigg, BiGGAnnotationParameters parameters) {
        this.parameters = parameters;
        this.bigg = bigg;
    }


    public void processNotes(SBMLDocument doc) throws BiGGAnnotationException, SQLException {
        var model = doc.getModel();
        try {
            // Process replacements for placeholders in the model notes
            Map<String, String> replacements = processReplacements(model);
            appendNotes(doc, replacements);
        } catch (IOException | XMLStreamException e) {
            throw new BiGGAnnotationException(MESSAGES.getString("FAILED_WRITE_NOTES"), e, doc);
        }
    }


    /**
     * Processes and replaces placeholders in the document title pattern and model notes with actual values from the model.
     * This method retrieves the model ID and organism information from the BiGG database, and uses these along with
     * other parameters to populate a map of replacements. These replacements are used later to substitute placeholders
     * in the SBMLDocument notes.
     *
     * @return A map of placeholder strings and their corresponding replacement values.
     */
    private Map<String, String> processReplacements(Model model) throws SQLException {
        // Retrieve the model ID
        String id = model.getId();
        // Attempt to retrieve the organism name associated with the model ID; use an empty string if not available
        String organism = bigg.getOrganism(id).orElse("");
        // Retrieve and process the document title pattern by replacing placeholders
        String name = parameters.documentTitlePattern();
        name = name.replace("[biggId]", id);
        name = name.replace("[organism]", organism);
        // Initialize a map to hold the replacement values
        Map<String, String> replacements = new HashMap<>();
        replacements.put("${organism}", organism);
        replacements.put("${title}", name);
        replacements.put("${bigg_id}", id);
        replacements.put("${year}", Integer.toString(Calendar.getInstance().get(Calendar.YEAR)));
        replacements.put("${bigg.timestamp}", bigg.getBiGGVersion().map(date -> format("{0,date}", date)).orElse(""));
        replacements.put("${species_table}", "");
        return replacements;
    }


    /**
     * This method appends notes to the SBMLDocument and its model by replacing placeholders in the notes files.
     * It handles both model-specific notes and document-wide notes.
     *
     * @param doc The SBMLDocument to which the notes will be appended.
     * @param replacements A map containing the placeholder text and their replacements.
     * @throws IOException If there is an error reading the notes files or writing to the document.
     * @throws XMLStreamException If there is an error processing the XML content of the notes.
     */
    private void appendNotes(SBMLDocument doc, Map<String, String> replacements) throws IOException, XMLStreamException {
        String modelNotesFile = "ModelNotes.html";
        String documentNotesFile = "SBMLDocumentNotes.html";

        // Determine the files to use for model and document notes based on user settings
        if (parameters.notesParameters().noModelNotes()) {
            modelNotesFile = null;
            documentNotesFile = null;
        } else {
            if (parameters.notesParameters().modelNotesFile() != null) {
                File modelNotes = parameters.notesParameters().modelNotesFile();
                modelNotesFile = modelNotes != null ? modelNotes.getAbsolutePath() : null;
            }
            if (parameters.notesParameters().documentNotesFile() != null) {
                File documentNotes = parameters.notesParameters().documentNotesFile();
                documentNotesFile = documentNotes != null ? documentNotes.getAbsolutePath() : null;
            }
        }

        // Append document notes if the title placeholder is present and the notes file is specified
        if (replacements.containsKey("${title}") && (documentNotesFile != null)) {
            doc.appendNotes(parseNotes(documentNotesFile, replacements));
        }

        // Append model notes if the notes file is specified
        if (modelNotesFile != null) {
            doc.getModel().appendNotes(parseNotes(modelNotesFile, replacements));
        }
    }


    /**
     * Parses the notes from a specified location and replaces placeholder tokens with actual values.
     * This method first attempts to read the resource from the classpath. If the resource is not found,
     * it falls back to reading from the filesystem. It processes the content line by line, starting to
     * append lines to the result after encountering a `<body>` tag and stopping after a `</body>` tag.
     * Any placeholders in the format `${placeholder}` found within the body are replaced with corresponding
     * values provided in the `replacements` map.
     *
     * @param location The relative path to the resource from this class.
     * @param replacements A map of placeholder tokens to their actual values to be replaced in the notes.
     * @return A string containing the processed notes with placeholders replaced by actual values.
     * @throws IOException If an I/O error occurs while reading the file.
     */
    private String parseNotes(String location, Map<String, String> replacements) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = getClass().getResourceAsStream(location);
             InputStreamReader isReader = new InputStreamReader((is != null) ? is : new FileInputStream(location));
             BufferedReader br = new BufferedReader(isReader)) {
            String line;
            boolean start = false;
            while (br.ready() && ((line = br.readLine()) != null)) {
                if (line.matches("\\s*<body.*")) {
                    start = true;
                }
                if (!start) {
                    continue;
                }
                if (line.matches(".*\\$\\{.*}.*")) {
                    for (String key : replacements.keySet()) {
                        line = line.replace(key, replacements.get(key));
                    }
                }
                sb.append(line);
                sb.append('\n');
                if (line.matches("\\s*</body.*")) {
                    break;
                }
            }
        }
        return sb.toString();
    }

}
