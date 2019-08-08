package edu.ucsd.sbrg.bigg;

public final class AnnotateDBContract {
    static abstract class Constants {
            // Tables
            static final String MAPPING_VIEW = "mapping_view";
            static final String ADB_COLLECTION = "adb_collection";
            // Column
            static final String COLUMN_ID = "id";
            static final String COLUMN_SOURCE_NAMESPACE = "source_namespace";
            static final String COLUMN_SOURCE_MIRIAM = "source_miriam";
            static final String COLUMN_SOURCE_TERM = "source_term";
            static final String COLUMN_QUALIFIER = "qualifier";
            static final String COLUMN_TARGET_NAMESPACE = "target_namespace";
            static final String COLUMN_TARGET_MIRIAM = "target_miriam";
            static final String COLUMN_TARGET_TERM = "target_term";
            static final String COLUMN_EVIDENCE_SOURCE = "evidence_source";
            static final String COLUMN_EVIDENCE_VERSION = "evidence_version";
            static final String COLUMN_EVIDENCE = "evidence";
            static final String COLUMN_NAMESPACE = "namespace";
            static final String COLUMN_URLPATTERN = "urlpattern";
    }
}
