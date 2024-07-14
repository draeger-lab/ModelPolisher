package edu.ucsd.sbrg.annotation;

import edu.ucsd.sbrg.db.MemorizedQuery;
import edu.ucsd.sbrg.db.bigg.BiGGDB;
import edu.ucsd.sbrg.db.bigg.BiGGDBContract;
import edu.ucsd.sbrg.identifiersorg.IdentifiersOrg;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.ext.fbc.GeneProduct;

import java.util.List;
import java.util.Optional;

public class AnnotationUtils {

    /**
     * Attempts to extract a BiGG ID that conforms to the BiGG ID specification from the BiGG knowledgebase. This method
     * processes annotations for biological entities such as {@link Species}, {@link Reaction}, or {@link GeneProduct}.
     * Each entity's annotations are provided as a list of URIs, which are then parsed to retrieve the BiGG ID.
     *
     * @param resources A list of URIs containing annotations for the biological entity.
     * @param type The type of the biological entity, which can be one of the following:
     *             {@link BiGGDBContract.Constants#TYPE_SPECIES}, {@link BiGGDBContract.Constants#TYPE_REACTION}, or
     *             {@link BiGGDBContract.Constants#TYPE_GENE_PRODUCT}.
     * @return An {@link Optional <String>} containing the BiGG ID if it could be successfully retrieved, otherwise {@link Optional#empty()}.
     */
    public static Optional<String> getBiGGIdFromResources(List<String> resources, String type) {
        for (String resource : resources) {
            Optional<String> id = IdentifiersOrg.checkResourceUrl(resource)
                    .map(IdentifiersOrg::getPartsFromIdentifiersURI)
                    .flatMap(parts -> getBiggIdFromParts(parts, type));
            if (id.isPresent()) {
                return id;
            }
        }
        return Optional.empty();
    }

    /**
     * Attempts to retrieve a BiGG identifier from the BiGG Knowledgebase using a given prefix and identifier. This method
     * is used for specific biological entities such as species, reactions, or gene products.
     *
     * @param parts A list containing two elements: the prefix and the identifier, both extracted from an identifiers.org URI.
     * @param type  The type of biological entity for which the ID is being retrieved. Valid types are defined in
     *              {@link BiGGDBContract.Constants} and include TYPE_SPECIES, TYPE_REACTION, and TYPE_GENE_PRODUCT.
     * @return An {@link Optional<String>} containing the BiGG ID if found, otherwise {@link Optional#empty()}.
     */
    private static Optional<String> getBiggIdFromParts(List<String> parts, String type) {
        String prefix = parts.get(0);
        String synonymId = parts.get(1);
        if (MemorizedQuery.isDataSource(prefix)) {
            Optional<String> id = BiGGDB.getBiggIdFromSynonym(prefix, synonymId, type);
            if (id.isPresent()) {
                return id;
            }
        }
        return Optional.empty();
    }
}
