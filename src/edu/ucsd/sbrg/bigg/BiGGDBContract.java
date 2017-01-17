package edu.ucsd.sbrg.bigg;

public final class BiGGDBContract {

  public BiGGDBContract() {
  }

  public static abstract class Constants {

    // Tables
    public static final String COMPARTMENT = "compartment";
    public static final String COMPARTMENTALIZED_COMPONENT = "compartmentalized_component";
    public static final String COMPONENT = "component";
    public static final String DATABASE_VERSION = "database_version";
    public static final String DATA_SOURCE = "data_source";
    public static final String GENE = "gene";
    public static final String GENOME = "genome";
    public static final String GENOME_REGION = "genome_region";
    public static final String MCC = "model_compartmentalized_component";
    public static final String MODEL = "model";
    public static final String MODEL_REACTION = "model_reaction";
    public static final String OLD_BIGG_ID = "'old_bigg_id'";
    public static final String PUBLICATION = "publication";
    public static final String PUBLICATION_MODEL = "publication_model";
    public static final String REACTION = "reaction";
    public static final String REFSEQ_PATTERN = "'refseq_%%'";
    public static final String REFSEQ_NAME = "'refseq_name'";
    public static final String SYNONYM = "synonym";
    public static final String URL = "url";
    public static final String URL_PREFIX = "url_prefix";

    // Columns
    public static final String COLUMN_BIGG_ID = "bigg_id";
    public static final String COLUMN_CHARGE = "charge";
    public static final String COLUMN_COMPARTMENTALIZED_COMPONENT_ID = "compartmentalized_component_id";
    public static final String COLUMN_COMPONENT_ID = "component_id";
    public static final String COLUMN_DATA_SOURCE_ID = "data_source_id";
    public static final String COLUMN_DATE_TIME = "date_time";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_FIRST_CREATED = "first_created";
    public static final String COLUMN_FORMULA = "formula";
    public static final String COLUMN_GENE_REACTION_RULE = "gene_reaction_rule";
    public static final String COLUMN_GENOME_ID = "genome_id";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_MODEL_ID = "model_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_OME_ID = "ome_id";
    public static final String COLUMN_ORGANISM = "organism";
    public static final String COLUMN_PSEUDOREACTION = "pseudoreaction";
    public static final String COLUMN_PUBLICATION_ID = "publication_id";
    public static final String COLUMN_REACTION_ID = "reaction_id";
    public static final String COLUMN_REFERENCE_ID = "reference_id";
    public static final String COLUMN_REFERENCE_TYPE = "reference_type";
    public static final String COLUMN_SUBSYSTEM = "subsystem";
    public static final String COLUMN_TAXON_ID = "taxon_id";
    public static final String COLUMN_TYPE = "type";
  }
}
