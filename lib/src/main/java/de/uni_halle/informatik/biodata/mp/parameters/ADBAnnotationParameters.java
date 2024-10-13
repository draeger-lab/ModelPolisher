package de.uni_halle.informatik.biodata.mp.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.zbit.util.prefs.SBProperties;
import de.uni_halle.informatik.biodata.mp.annotation.AnnotationOptions;
import de.uni_halle.informatik.biodata.mp.db.adb.AnnotateDBOptions;

import java.util.Objects;

public class ADBAnnotationParameters {

    @JsonProperty("annotate-with-adb")
    protected boolean annotateWithAdb = AnnotationOptions.ADD_ADB_ANNOTATIONS.getDefaultValue();
    @JsonProperty("db-config")
    protected DBParameters dbParameters = new DBParameters(
            AnnotateDBOptions.DBNAME.getDefaultValue(),
            AnnotateDBOptions.HOST.getDefaultValue(),
            AnnotateDBOptions.PASSWD.getDefaultValue(),
            AnnotateDBOptions.PORT.getDefaultValue(),
            AnnotateDBOptions.USER.getDefaultValue()
    );

    public ADBAnnotationParameters() {
    }

    public ADBAnnotationParameters(SBProperties args) {
        annotateWithAdb = args.getBooleanProperty(AnnotationOptions.ADD_ADB_ANNOTATIONS);
        dbParameters = new DBParameters(
                args.getProperty(AnnotateDBOptions.DBNAME),
                args.getProperty(AnnotateDBOptions.HOST),
                args.getProperty(AnnotateDBOptions.PASSWD),
                args.getIntProperty(AnnotateDBOptions.PORT),
                args.getProperty(AnnotateDBOptions.USER));
    }

    public boolean annotateWithAdb() {
        return annotateWithAdb;
    }

    public DBParameters dbParameters() {
        return dbParameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ADBAnnotationParameters that = (ADBAnnotationParameters) o;
        return annotateWithAdb == that.annotateWithAdb && Objects.equals(dbParameters, that.dbParameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(annotateWithAdb, dbParameters);
    }

    @Override
    public String toString() {
        return "ADBAnnotationParameters{" +
                "annotateWithAdb=" + annotateWithAdb +
                ", dbParameters=" + dbParameters +
                '}';
    }
}
