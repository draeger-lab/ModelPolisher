package edu.ucsd.sbrg.bigg;

public final class AnnotateDBContract {
    static abstract class Constants {
            // Tables
            static final String MAPPING_VIEW = "mapping_view";
            // Columns
            static final String ID = "id";
            static final String SOURCE_NAMESPACE = "source_namespace";
            static final String SOURCE_MIRIAM = "source_miriam";
            static final String SOURCE_TERM = "source_term";
            static final String QUALIFIER = "qualifier";
            static final String TARGET_NAMESPACE = "target_namespace";
            static final String TARGET_MIRIAM = "target_miriam";
            static final String TARGET_TERM = "target_term";
            static final String EVIDENCE_SOURCE = "evidence_source";
            static final String EVIDENCE_VERSION = "evidence_version";
            static final String EVIDENCE = "evidence";
    }
}
