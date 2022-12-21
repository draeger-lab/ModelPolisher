package edu.ucsd.sbrg.db;

final class BiGGDBContract {

  BiGGDBContract() {
  }

  static abstract class Constants {

    // Tables
    static final String COMPARTMENT = "compartment";
    static final String COMPARTMENTALIZED_COMPONENT = "compartmentalized_component";
    static final String COMPONENT = "component";
    static final String DATABASE_VERSION = "database_version";
    static final String DATA_SOURCE = "data_source";
    static final String GENE = "gene";
    static final String GENOME = "genome";
    static final String GENOME_REGION = "genome_region";
    static final String MCC = "model_compartmentalized_component";
    static final String MODEL = "model";
    static final String MODEL_REACTION = "model_reaction";
    static final String OLD_BIGG_ID = "'old_bigg_id'";
    static final String PUBLICATION = "publication";
    static final String PUBLICATION_MODEL = "publication_model";
    static final String REACTION = "reaction";
    static final String REFSEQ_PATTERN = "'refseq_%%'";
    static final String REFSEQ_NAME = "'refseq_name'";
    static final String SYNONYM = "synonym";
    static final String URL = "url";
    static final String URL_PREFIX = "url_prefix";
    // Columns
    static final String COLUMN_BIGG_ID = "bigg_id";
    static final String COLUMN_CHARGE = "charge";
    static final String COLUMN_COMPARTMENTALIZED_COMPONENT_ID = "compartmentalized_component_id";
    static final String COLUMN_COMPARTMENT_ID = "compartment_id";
    static final String COLUMN_COMPONENT_ID = "component_id";
    static final String COLUMN_DATA_SOURCE_ID = "data_source_id";
    static final String COLUMN_DATE_TIME = "date_time";
    static final String COLUMN_DESCRIPTION = "description";
    static final String COLUMN_FIRST_CREATED = "first_created";
    static final String COLUMN_FORMULA = "formula";
    static final String COLUMN_GENE_REACTION_RULE = "gene_reaction_rule";
    static final String COLUMN_GENOME_ID = "genome_id";
    static final String COLUMN_ID = "id";
    static final String COLUMN_MODEL_ID = "model_id";
    static final String COLUMN_NAME = "name";
    static final String COLUMN_OME_ID = "ome_id";
    static final String COLUMN_ORGANISM = "organism";
    static final String COLUMN_PSEUDOREACTION = "pseudoreaction";
    static final String COLUMN_PUBLICATION_ID = "publication_id";
    static final String COLUMN_REACTION_ID = "reaction_id";
    static final String COLUMN_REFERENCE_ID = "reference_id";
    static final String COLUMN_REFERENCE_TYPE = "reference_type";
    static final String COLUMN_SUBSYSTEM = "subsystem";
    static final String COLUMN_TAXON_ID = "taxon_id";
    static final String COLUMN_TYPE = "type";
    static final String COLUMN_SYNONYM = "synonym";
    static final String COLUMN_LOCUS_TAG = "locus_tag";
  }
}
