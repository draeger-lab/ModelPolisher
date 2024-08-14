package edu.ucsd.sbrg.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.zbit.util.prefs.SBProperties;
import edu.ucsd.sbrg.db.bigg.BiGGDBOptions;

public class BiGGAnnotationParameters {

    @JsonProperty("annotate-with-bigg")
    protected boolean annotateWithBiGG = ModelPolisherOptions.ANNOTATE_WITH_BIGG.getDefaultValue();
    @JsonProperty("include-any-uri")
    protected boolean includeAnyURI = ModelPolisherOptions.INCLUDE_ANY_URI.getDefaultValue();
    @JsonProperty("document-title-pattern")
    protected String documentTitlePattern = ModelPolisherOptions.DOCUMENT_TITLE_PATTERN.getDefaultValue();
    @JsonProperty("notes")
    protected BiGGNotesParameters notesParameters = new BiGGNotesParameters();
    @JsonProperty("db-config")
    protected DBParameters dbParameters = new DBParameters();

    public BiGGAnnotationParameters() {
    }

    public BiGGAnnotationParameters(boolean annotateWithBiGG,
                                    boolean includeAnyURI,
                                    String documentTitlePattern,
                                    BiGGNotesParameters notesParameters,
                                    DBParameters dbParameters) {
        this.annotateWithBiGG = annotateWithBiGG;
        this.includeAnyURI = includeAnyURI;
        this.documentTitlePattern = documentTitlePattern;
        this.notesParameters = notesParameters;
        this.dbParameters = dbParameters;
    }

    public BiGGAnnotationParameters(SBProperties args) {
        annotateWithBiGG = args.getBooleanProperty(ModelPolisherOptions.ANNOTATE_WITH_BIGG);
        includeAnyURI = args.getBooleanProperty(ModelPolisherOptions.INCLUDE_ANY_URI);
        documentTitlePattern = args.getProperty(ModelPolisherOptions.DOCUMENT_TITLE_PATTERN);
        notesParameters = new BiGGNotesParameters(args);
        dbParameters = new DBParameters(
                args.getProperty(BiGGDBOptions.DBNAME),
                args.getProperty(BiGGDBOptions.HOST),
                args.getProperty(BiGGDBOptions.PASSWD),
                args.getIntProperty(BiGGDBOptions.PORT),
                args.getProperty(BiGGDBOptions.USER));
    }

    public boolean includeAnyURI() {
        return includeAnyURI;
    }

    public boolean annotateWithBiGG() {
        return annotateWithBiGG;
    }

    public String documentTitlePattern() {
        return documentTitlePattern;
    }

    public BiGGNotesParameters notesParameters() {
        return notesParameters;
    }

    public DBParameters dbParameters() {
        return dbParameters;
    }
}
