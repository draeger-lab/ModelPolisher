package edu.ucsd.sbrg.annotation.bigg;

import edu.ucsd.sbrg.annotation.AbstractAnnotator;
import edu.ucsd.sbrg.db.bigg.BiGGDB;
import edu.ucsd.sbrg.db.bigg.BiGGDBContract;
import edu.ucsd.sbrg.db.bigg.BiGGId;
import edu.ucsd.sbrg.parameters.BiGGAnnotationParameters;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import edu.ucsd.sbrg.resolver.Registry;
import edu.ucsd.sbrg.resolver.RegistryURI;
import edu.ucsd.sbrg.resolver.identifiersorg.IdentifiersOrgURI;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.ext.fbc.GeneProduct;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Stream.concat;

public abstract class AbstractBiGGAnnotator<SBMLElement> extends AbstractAnnotator<SBMLElement> {

    protected final BiGGDB bigg;
    protected final Registry registry;
    protected final BiGGAnnotationParameters biGGAnnotationParameters;

    public AbstractBiGGAnnotator(BiGGDB bigg, BiGGAnnotationParameters biGGAnnotationParameters, Registry registry) {
        super();
        this.bigg = bigg;
        this.registry = registry;
        this.biGGAnnotationParameters = biGGAnnotationParameters;
    }

    public AbstractBiGGAnnotator(BiGGDB bigg, BiGGAnnotationParameters biGGAnnotationParameters, Registry registry, List<ProgressObserver> observers) {
        super(observers);
        this.bigg = bigg;
        this.registry = registry;
        this.biGGAnnotationParameters = biGGAnnotationParameters;
    }

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
    public Optional<BiGGId> getBiGGIdFromResources(List<String> resources, String type) throws SQLException {
        var identifiersOrgUrisStream = resources.stream()
                .filter(registry::isValid)
                .map(IdentifiersOrgURI::new)
                .filter(registry::validRegistryUrlPrefix);

        var resolvedIdentifiersOrgUrisStream = resources.stream()
                .filter(r -> !registry.isValid(r))
                .map(registry::findRegistryUrlForOtherUrl)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(registry::validRegistryUrlPrefix);

        var uris = Stream.concat(identifiersOrgUrisStream, resolvedIdentifiersOrgUrisStream).toList();
        for (var uri : uris ) {
            var biggId = getBiggIdFromParts(uri, type);
            if (biggId.isPresent()) {
                return biggId;
            }
        }
        return Optional.empty();
    }

    /**
     * Attempts to retrieve a BiGG identifier from the BiGG Knowledgebase using a given prefix and identifier. This method
     * is used for specific biological entities such as species, reactions, or gene products.
     *
     * @param type  The type of biological entity for which the ID is being retrieved. Valid types are defined in
     *              {@link BiGGDBContract.Constants} and include TYPE_SPECIES, TYPE_REACTION, and TYPE_GENE_PRODUCT.
     * @return An {@link Optional<String>} containing the BiGG ID if found, otherwise {@link Optional#empty()}.
     */
    private Optional<BiGGId> getBiggIdFromParts(RegistryURI uri, String type) throws SQLException {
        if (bigg.isDataSource(uri.getPrefix())) {
            Optional<BiGGId> id = bigg.getBiggIdFromSynonym(uri.getPrefix(), uri.getId(), type);
            if (id.isPresent()) {
                return id;
            }
        }
        return Optional.empty();
    }

}