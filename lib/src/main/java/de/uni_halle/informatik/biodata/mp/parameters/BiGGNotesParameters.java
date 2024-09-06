package de.uni_halle.informatik.biodata.mp.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.SBProperties;
import de.uni_halle.informatik.biodata.mp.annotation.AnnotationOptions;

import java.io.File;

public class BiGGNotesParameters {

    @JsonProperty("no-model-notes")
    private boolean noModelNotes = AnnotationOptions.NO_MODEL_NOTES.getDefaultValue();
    @JsonProperty("model-notes-file")
    protected File modelNotesFile = AnnotationOptions.MODEL_NOTES_FILE.getDefaultValue();
    @JsonProperty("document-notes-file")
    protected File documentNotesFile = AnnotationOptions.DOCUMENT_NOTES_FILE.getDefaultValue();

    public BiGGNotesParameters() {
    }

    public BiGGNotesParameters(SBProperties args) {
        noModelNotes = args.getBooleanProperty(AnnotationOptions.NO_MODEL_NOTES);
        documentNotesFile = parseFileOption(args, AnnotationOptions.DOCUMENT_NOTES_FILE);
        modelNotesFile = parseFileOption(args, AnnotationOptions.MODEL_NOTES_FILE);
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
