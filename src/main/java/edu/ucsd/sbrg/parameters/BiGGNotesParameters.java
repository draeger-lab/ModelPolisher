package edu.ucsd.sbrg.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.SBProperties;
import edu.ucsd.sbrg.ModelPolisherOptions;

import java.io.File;

public class BiGGNotesParameters {

    @JsonProperty("no-model-notes")
    private boolean noModelNotes = ModelPolisherOptions.NO_MODEL_NOTES.getDefaultValue();
    @JsonProperty("model-notes-file")
    protected File modelNotesFile = ModelPolisherOptions.MODEL_NOTES_FILE.getDefaultValue();
    @JsonProperty("document-notes-file")
    protected File documentNotesFile = ModelPolisherOptions.DOCUMENT_NOTES_FILE.getDefaultValue();

    public BiGGNotesParameters() {
    }

    public BiGGNotesParameters(SBProperties args) {
        noModelNotes = args.getBooleanProperty(ModelPolisherOptions.NO_MODEL_NOTES);
        documentNotesFile = parseFileOption(args, ModelPolisherOptions.DOCUMENT_NOTES_FILE);
        modelNotesFile = parseFileOption(args, ModelPolisherOptions.MODEL_NOTES_FILE);
    }

    private File parseFileOption(SBProperties args, Option<File> option) {
        if (args.containsKey(option)) {
            File notesFile = new File(args.getProperty(option));
            if (notesFile.exists() && notesFile.canRead()) {
                return notesFile;
            }
        }
        return null;
    }


    public File documentNotesFile() {
        return documentNotesFile;
    }

    public File modelNotesFile() {
        return modelNotesFile;
    }

    public boolean noModelNotes() {
        return noModelNotes;
    }


}
