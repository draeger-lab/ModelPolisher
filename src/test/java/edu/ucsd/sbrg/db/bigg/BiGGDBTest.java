package edu.ucsd.sbrg.db.bigg;

import edu.ucsd.sbrg.annotation.bigg.BiGGDBContainerTest;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
public class BiGGDBTest extends BiGGDBContainerTest {


    @Test
    public void getSubsystems() throws SQLException {
        var org = bigg.getOrganism("iJO1366");
        assertTrue(org.isPresent());
        assertEquals("Escherichia coli str. K-12 substr. MG1655", org.get());
    }


    /**
     * This test serves primarily as documentation and to raise awareness for
     * new data sources in case of DB update.
     */
    @Test
    public void dataSources() throws SQLException {
        var dataSources = bigg.getAllBiggIds("data_source");
        assertEquals(Set.of("asap",
                        "biocyc",
                        "ccds",
                        "chebi",
                        "deprecated",
                        "ec-code",
                        "ecogene",
                        "EnsemblGenomes-Gn",
                        "EnsemblGenomes-Tr",
                        "envipath",
                        "go",
                        "goa",
                        "hmdb",
                        "hprd",
                        "IMGT/GENE-DB",
                        "inchi_key",
                        "interpro",
                        "kegg.compound",
                        "kegg.drug",
                        "kegg.glycan",
                        "kegg.reaction",
                        "lipidmaps",
                        "metanetx.chemical",
                        "metanetx.reaction",
                        "ncbigene",
                        "ncbigi",
                        "old_bigg_id",
                        "omim",
                        "pdb",
                        "PSEUDO",
                        "reactome.compound",
                        "reactome.reaction",
                        "REBASE",
                        "refseq_locus_tag",
                        "refseq_name",
                        "refseq_old_locus_tag",
                        "refseq_orf_id",
                        "refseq_synonym",
                        "rhea",
                        "sabiork",
                        "seed.compound",
                        "seed.reaction",
                        "sgd",
                        "slm",
                        "subtilist",
                        "uniprot"),
                dataSources);
    }

}
