package edu.ucsd.sbrg.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.zbit.util.prefs.SBProperties;
import edu.ucsd.sbrg.ModelPolisherOptions;
import edu.ucsd.sbrg.db.adb.AnnotateDBOptions;

public class ADBAnnotationParameters {

    @JsonProperty("add-adb-annotations")
    protected boolean addADBAnnotations = ModelPolisherOptions.ADD_ADB_ANNOTATIONS.getDefaultValue();
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
        addADBAnnotations = args.getBooleanProperty(ModelPolisherOptions.ADD_ADB_ANNOTATIONS);
        dbParameters = new DBParameters(
                args.getProperty(AnnotateDBOptions.DBNAME),
                args.getProperty(AnnotateDBOptions.HOST),
                args.getProperty(AnnotateDBOptions.PASSWD),
                args.getIntProperty(AnnotateDBOptions.PORT),
                args.getProperty(AnnotateDBOptions.USER));
    }

    public boolean addADBAnnotations() {
        return addADBAnnotations;
    }

    public DBParameters dbParameters() {
        return dbParameters;
    }
}