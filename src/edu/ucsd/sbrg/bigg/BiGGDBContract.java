package edu.ucsd.sbrg.bigg;

public final class BiGGDBContract {

	public BiGGDBContract() {
	}

	public static abstract class BiGGDBConstants {

		// simple

		public static final String BIGG_ID = "bigg_id";
		public static final String CHARGE = "charge";
		public static final String COMPARTMENT = "compartment";
		public static final String COMPARTMENTALIZED_COMPONENT = "compartmentalized_component";
		public static final String COMPARTMENTALIZED_COMPONENT_ID = "compartmentalized_component_id";
		public static final String COMPONENT = "component";
		public static final String COMPONENT_ID = "component_id";
		public static final String DATABASE_VERSION = "database_version";
		public static final String DATA_SOURCE = "data_source";
		public static final String DATE_TIME = "date_time";
		public static final String DESCRIPTION = "description";
		public static final String FIRST_CREATED = "first_created";
		public static final String FORMULA = "formula";
		public static final String GENE_REACTION_RULE = "gene_reaction_rule";
		public static final String GENOME = "genome";
		public static final String GENOME_ID = "genome_id";
		public static final String GENOME_REGION = "genome_region";
		public static final String ID = "id";
		public static final String MODEL = "model";
		public static final String MODEL_ID = "model_id";
		public static final String MODEl_REACTION = "model_reaction";
		public static final String NAME = "name";
		public static final String OLD_BIGG_ID = "'old_bigg_id'";
		public static final String OME_ID = "ome_id";
		public static final String ORGANISM = "organism";
		public static final String PSEUDOREACTION = "pseudoreaction";
		public static final String PUBLICATION = "publication";
		public static final String PUBLICATION_MODEL = "publication_model";
		public static final String REACTION = "reaction";
		public static final String REACTION_ID = "reaction_id";
		public static final String REFERENCE_ID = "reference_id";
		public static final String REFERENCE_TYPE = "reference_type";
		public static final String REFSEQ_PATTERN = "'refseq_%%'";
		public static final String REFSEQ_NAME = "'refseq_name'";
		public static final String SUBSYSTEM = "subsystem";
		public static final String SYNONYM = "synonym";
		public static final String TAXON_ID = "taxon_id";
		public static final String TYPE = "type";
		public static final String URL = "url";
		public static final String URL_PREFIX = "url_prefix";

		// composite

		public static final String C_ID = COMPONENT + "." + ID;
		public static final String C_BIGG_ID = COMPONENT + "." + BIGG_ID;
		public static final String CC_COMPONENT_ID = COMPARTMENTALIZED_COMPONENT + "." + COMPONENT_ID;
		public static final String CC_ID = COMPARTMENTALIZED_COMPONENT + "." + ID;
		public static final String D_BIGG_ID = DATA_SOURCE + "." + BIGG_ID;
		public static final String D_ID = DATA_SOURCE + "." + ID;
		public static final String G_ORGANISM = GENOME + "." + ORGANISM;
		public static final String G_ID = GENOME + "." + ID;
		public static final String GR_BIGG_ID = GENOME_REGION + "." + BIGG_ID;
		public static final String GR_ID = GENOME_REGION + "." + ID;
		public static final String M_BIGG_ID = MODEL + "." + BIGG_ID;
		public static final String M_GENOME_ID = MODEL + "." + GENOME_ID;
		public static final String M_ID = MODEL + "." + ID;
		public static final String MCC = MODEL + "." + COMPARTMENTALIZED_COMPONENT;
		public static final String MCC_CC_ID = MCC + "." + CC_ID;
		public static final String MCC_CHARGE = MCC + "." + CHARGE;
		public static final String MCC_COMPARTMENTALIZED_COMPONENT_ID = MCC + "." + COMPARTMENTALIZED_COMPONENT_ID;
		public static final String MCC_FORMULA = MCC + "." + FORMULA;
		public static final String MCC_MODEL_ID = MCC + "." + MODEL_ID;
		public static final String MR_GENE_REACTION_RULE = MODEl_REACTION + "." + GENE_REACTION_RULE;
		public static final String MR_MODEL_ID = MODEl_REACTION + "." + MODEL_ID;
		public static final String MR_REACTION_ID = MODEl_REACTION + "." + REACTION_ID;
		public static final String MR_SUBSYSTEM = MODEl_REACTION + "." + SUBSYSTEM;
		public static final String P_ID = PUBLICATION + "." + ID;
		public static final String P_REFERENCE_ID = PUBLICATION + "." + REFERENCE_ID;
		public static final String P_REFERENCE_TYPE = PUBLICATION + "." + REFERENCE_TYPE;
		public static final String PM_MODEL_ID = PUBLICATION_MODEL + "." + MODEL_ID;
		public static final String PM_PUBLICATION_ID = PUBLICATION_MODEL + "." + P_ID;
		public static final String R_BIGG_ID = REACTION + "." + BIGG_ID;
		public static final String R_ID = REACTION + "." + ID;
		public static final String S_DATA_SOURCE_ID = SYNONYM + "." + D_ID;
		public static final String S_OME_ID = SYNONYM + "." + OME_ID;
		public static final String S_SYNONYM = SYNONYM + "." + SYNONYM;
	}

}
