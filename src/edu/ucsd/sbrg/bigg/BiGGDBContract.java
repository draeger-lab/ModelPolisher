package edu.ucsd.sbrg.bigg;

public final class BiGGDBContract {

	public BiGGDBContract() {
	}

	public static abstract class BiGGDBConstants {

		// simple

		public static final String BIGG_ID = "bigg_id";
		public static final String CHARGE = "charge";
		public static final String COMPARTMENT = "compartment";
		public static final String COMPARTMENTALIZED_COMPONENT = "compartmentalized_component cc";
		public static final String COMPARTMENTALIZED_COMPONENT_ID = "compartmentalized_component_id";
		public static final String COMPONENT = "component";
		public static final String COMPONENT_C = "component c";
		public static final String COMPONENT_ID = "component_id";
		public static final String DATABASE_VERSION = "database_version";
		public static final String DATA_SOURCE = "data_source d";
		public static final String DATE_TIME = "date_time";
		public static final String DESCRIPTION = "description";
		public static final String FIRST_CREATED = "first_created";
		public static final String FORMULA = "formula";
		public static final String GENE_REACTION_RULE = "gene_reaction_rule";
		public static final String GENOME = "genome g";
		public static final String GENOME_ID = "genome_id";
		public static final String GENOME_REGION = "genome_region gr";
		public static final String ID = "id";
		public static final String MCC = "model_compartmentalized_component mcc";
		public static final String MODEL = "model";
		public static final String MODEL_M = "model m";
		public static final String MODEL_ID = "model_id";
		public static final String MODEL_REACTION = "model_reaction mr";
		public static final String NAME = "name";
		public static final String OLD_BIGG_ID = "'old_bigg_id'";
		public static final String OME_ID = "ome_id";
		public static final String ORGANISM = "organism";
		public static final String PSEUDOREACTION = "pseudoreaction";
		public static final String PUBLICATION = "publication p";
		public static final String PUBLICATION_MODEL = "publication_model pm";
		public static final String REACTION = "reaction";
		public static final String REACTION_R = "reaction r";
		public static final String REACTION_ID = "reaction_id";
		public static final String REFERENCE_ID = "reference_id";
		public static final String REFERENCE_TYPE = "reference_type";
		public static final String REFSEQ_PATTERN = "'refseq_%%'";
		public static final String REFSEQ_NAME = "'refseq_name'";
		public static final String SUBSYSTEM = "subsystem";
		public static final String SYNONYM = "synonym s";
		public static final String TAXON_ID = "taxon_id";
		public static final String TYPE = "type";
		public static final String URL = "url";
		public static final String URL_PREFIX = "url_prefix";

		// composite

		public static final String C_ID = "c.id";
		public static final String C_BIGG_ID = "c.bigg_id" ;
		public static final String CC_COMPONENT_ID = "cc.component_id" ;
		public static final String CC_ID = "cc.id" ;
		public static final String D_BIGG_ID = "d.bigg_id";
		public static final String D_ID = "d.id";
		public static final String G_ORGANISM = "g.organism";
		public static final String G_ID = "g.id";
		public static final String GR_BIGG_ID = "gr.bigg_id";
		public static final String GR_ID = "gr.id";
		public static final String M_BIGG_ID = "m.bigg_id";
		public static final String M_GENOME_ID = "m.genome_id";
		public static final String M_ID = "m.id";
		public static final String MCC_CHARGE = "mcc.charge";
		public static final String MCC_COMPARTMENTALIZED_COMPONENT_ID = "mcc.compartmentalized_component_id";
		public static final String MCC_FORMULA = "mcc.formula";
		public static final String MCC_MODEL_ID = "mcc.model_id";
		public static final String MR_GENE_REACTION_RULE = "mr.gene_reaction_rule";
		public static final String MR_MODEL_ID = "mr.model_id";
		public static final String MR_REACTION_ID = "mr.reaction_id";
		public static final String MR_SUBSYSTEM = "mr.subsystem";
		public static final String P_ID = "p.id";
		public static final String P_REFERENCE_ID = "p.reference_id";
		public static final String P_REFERENCE_TYPE = "p.reference_type";
		public static final String PM_MODEL_ID = "pm.model_id";
		public static final String PM_PUBLICATION_ID = "pm.publication_id";
		public static final String R_BIGG_ID = "r.bigg_id";
		public static final String R_ID = "r.id";
		public static final String S_DATA_SOURCE_ID = "s.data_source_id";
		public static final String S_OME_ID = "s.ome_id";
		public static final String S_SYNONYM = "s.synonym";
	}

}
