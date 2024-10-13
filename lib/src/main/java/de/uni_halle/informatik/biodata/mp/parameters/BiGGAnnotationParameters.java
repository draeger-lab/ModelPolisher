package de.uni_halle.informatik.biodata.mp.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.zbit.util.prefs.SBProperties;
import de.uni_halle.informatik.biodata.mp.annotation.AnnotationOptions;
import de.uni_halle.informatik.biodata.mp.db.bigg.BiGGDBOptions;

import java.util.Objects;

public class BiGGAnnotationParameters {

    @JsonProperty("annotate-with-bigg")
    protected boolean annotateWithBiGG = AnnotationOptions.ANNOTATE_WITH_BIGG.getDefaultValue();
    @JsonProperty("include-any-uri")
    protected boolean includeAnyURI = AnnotationOptions.INCLUDE_ANY_URI.getDefaultValue();
    @JsonProperty("document-title-pattern")
    protected String documentTitlePattern = AnnotationOptions.DOCUMENT_TITLE_PATTERN.getDefaultValue();
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
        annotateWithBiGG = args.getBooleanProperty(AnnotationOptions.ANNOTATE_WITH_BIGG);
        includeAnyURI = args.getBooleanProperty(AnnotationOptions.INCLUDE_ANY_URI);
        documentTitlePattern = args.getProperty(AnnotationOptions.DOCUMENT_TITLE_PATTERN);
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

    @Override
    public String toString() {
        return "BiGGAnnotationParameters{" +
                "annotateWithBiGG=" + annotateWithBiGG +
                ", includeAnyURI=" + includeAnyURI +
                ", documentTitlePattern='" + documentTitlePattern + '\'' +
                ", notesParameters=" + notesParameters +
                ", dbParameters=" + dbParameters +
                '}';
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BiGGAnnotationParameters that = (BiGGAnnotationParameters) o;
        return annotateWithBiGG == that.annotateWithBiGG && includeAnyURI == that.includeAnyURI && Objects.equals(documentTitlePattern, that.documentTitlePattern) && Objects.equals(notesParameters, that.notesParameters) && Objects.equals(dbParameters, that.dbParameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(annotateWithBiGG, includeAnyURI, documentTitlePattern, notesParameters, dbParameters);
    }
}
