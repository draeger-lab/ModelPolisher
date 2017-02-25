CREATE TABLE chromosome (
    id integer NOT NULL,
    ncbi_accession character varying(200),
    genome_id integer
);

CREATE TABLE compartment (
    id integer NOT NULL,
    bigg_id character varying,
    name character varying
);

CREATE TABLE compartmentalized_component (
    id integer NOT NULL,
    component_id integer NOT NULL,
    compartment_id integer NOT NULL
);

CREATE TABLE complex (
    id integer NOT NULL
);

CREATE TABLE complex_composition (
    complex_id integer NOT NULL,
    component_id integer NOT NULL,
    stoichiometry integer
);

CREATE TABLE component (
    id integer NOT NULL,
    bigg_id character varying,
    name character varying,
    type character varying(20)
);

CREATE TABLE data_source (
    id integer NOT NULL,
    bigg_id character varying NOT NULL,
    name character varying(100),
    url_prefix character varying
);

CREATE TABLE database_version (
    is_version is_version NOT NULL,
    date_time timestamp without time zone NOT NULL
);

CREATE TABLE deprecated_id (
    id integer NOT NULL,
    type deprecated_id_types,
    deprecated_id character varying,
    ome_id integer
);

CREATE TABLE dna (
    id integer NOT NULL,
    genome_region_id integer,
    dna_type character varying(20)
);

CREATE TABLE dna_binding_site (
    id integer NOT NULL,
    centerpos integer,
    width integer
);

CREATE TABLE escher_map (
    id integer NOT NULL,
    map_name character varying NOT NULL,
    map_data bytea NOT NULL,
    model_id integer NOT NULL,
    priority integer NOT NULL
);

CREATE TABLE escher_map_matrix (
    id integer NOT NULL,
    ome_id integer NOT NULL,
    type escher_map_matrix_type NOT NULL,
    escher_map_id integer NOT NULL,
    escher_map_element_id character varying(50)
);

CREATE TABLE gene (
    id integer NOT NULL,
    name character varying,
    locus_tag character varying,
    mapped_to_genbank boolean NOT NULL,
    alternative_transcript_of integer
);

CREATE TABLE gene_group (
    id integer NOT NULL,
    name character varying(100)
);

CREATE TABLE gene_grouping (
    group_id integer NOT NULL,
    gene_id integer NOT NULL
);

CREATE TABLE gene_reaction_matrix (
    id integer NOT NULL,
    model_gene_id integer NOT NULL,
    model_reaction_id integer NOT NULL
);

CREATE TABLE genome (
    id integer NOT NULL,
    accession_type character varying(200) NOT NULL,
    accession_value character varying(200) NOT NULL,
    organism character varying(200),
    taxon_id character varying(200),
    ncbi_assembly_id character varying(200)
);

CREATE TABLE genome_region (
    id integer NOT NULL,
    chromosome_id integer,
    bigg_id character varying NOT NULL,
    leftpos integer,
    rightpos integer,
    strand character varying(1),
    type character varying(20)
);

CREATE TABLE genome_region_map (
    genome_region_id_1 integer NOT NULL,
    genome_region_id_2 integer NOT NULL,
    distance integer
);

CREATE TABLE metabolite (
    id integer NOT NULL
);

CREATE TABLE model (
    id integer NOT NULL,
    bigg_id character varying NOT NULL,
    genome_id integer,
    organism character varying(200),
    published_filename character varying
);

CREATE TABLE model_compartmentalized_component (
    id integer NOT NULL,
    model_id integer NOT NULL,
    compartmentalized_component_id integer NOT NULL,
    formula character varying,
    charge integer
);

CREATE TABLE model_count (
    id integer NOT NULL,
    model_id integer NOT NULL,
    reaction_count integer,
    gene_count integer,
    metabolite_count integer
);

CREATE TABLE model_gene (
    id integer NOT NULL,
    model_id integer NOT NULL,
    gene_id integer NOT NULL
);

CREATE TABLE model_reaction (
    id integer NOT NULL,
    reaction_id integer NOT NULL,
    model_id integer NOT NULL,
    copy_number integer NOT NULL,
    objective_coefficient numeric NOT NULL,
    lower_bound numeric NOT NULL,
    upper_bound numeric NOT NULL,
    gene_reaction_rule character varying NOT NULL,
    original_gene_reaction_rule character varying,
    subsystem character varying
);

CREATE TABLE motif (
    id integer NOT NULL,
    pval double precision,
    bound_component_id integer
);

CREATE TABLE old_id_model_synonym (
    id integer NOT NULL,
    type old_id_synonym_type,
    synonym_id integer NOT NULL,
    ome_id integer NOT NULL
);

CREATE TABLE publication (
    id integer NOT NULL,
    reference_type reference_type,
    reference_id character varying
);

CREATE TABLE publication_model (
    model_id integer NOT NULL,
    publication_id integer NOT NULL
);

CREATE TABLE reaction (
    id integer NOT NULL,
    type character varying(20),
    bigg_id character varying NOT NULL,
    name character varying,
    reaction_hash character varying NOT NULL,
    pseudoreaction boolean
);

CREATE TABLE reaction_matrix (
    id integer NOT NULL,
    reaction_id integer NOT NULL,
    compartmentalized_component_id integer NOT NULL,
    stoichiometry numeric
);

CREATE TABLE rna (
    id integer NOT NULL,
    genome_region_id integer
);

CREATE TABLE synonym (
    id integer NOT NULL,
    ome_id integer,
    synonym character varying,
    type synonym_type,
    data_source_id integer
);

CREATE TABLE tu (
    id integer NOT NULL
);

CREATE TABLE tu_genes (
    tu_id integer NOT NULL,
    gene_id integer NOT NULL
);

.read converted.sql
.save bigg.sqlite
