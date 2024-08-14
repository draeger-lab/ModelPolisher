package edu.ucsd.sbrg.resolver.identifiersorg;

import static java.text.MessageFormat.format;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.ucsd.sbrg.resolver.RegistryURI;
import edu.ucsd.sbrg.resolver.Registry;
import edu.ucsd.sbrg.resolver.identifiersorg.mapping.Namespace;
import edu.ucsd.sbrg.resolver.identifiersorg.mapping.RawIdentifiersOrgRegistry;
import edu.ucsd.sbrg.resolver.identifiersorg.mapping.Resource;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code IdentifiersOrg} class serves as a central hub for managing and processing identifiers related to the MIRIAM registry.
 * MIRIAM is a standard for annotating computational models in biology with machine-readable information.
 * <p>
 * This class provides static methods and utilities to handle, validate,
 * and correct resource URLs based on the MIRIAM standards. It ensures that identifiers and URLs conform to recognized formats and corrects common errors in identifiers from various
 * biological databases. The class also initializes necessary resources and configurations at the start through a static block.
 */
public class IdentifiersOrg implements Registry {

    private static final Logger logger = LoggerFactory.getLogger(IdentifiersOrg.class);

    private static final Map<String, String> NAMESPACE_NAME_BY_PREFIX;
    private static final Map<String, String> PATTERN_BY_NAMESPACE_NAME;
    private static final Map<String, String> PREFIX_BY_NAMESPACE_NAME;
    private static final Collection<Namespace> namespaces;

    static {
        RawIdentifiersOrgRegistry rawIdentifiersOrgRegistry;
        try {
            rawIdentifiersOrgRegistry = new IdentifiersOrgRegistryParser()
                    .parse(IdentifiersOrgRegistryParser.class.getResourceAsStream("IdentifiersOrg-Registry.json"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        namespaces = rawIdentifiersOrgRegistry.getPayload().get("namespaces");

        Map<String, String> namespaceNameByPrefix = new HashMap<>();
        Map<String, String> patternByNamespaceName = new HashMap<>();
        Map<String, String> prefixByNamespaceName = new HashMap<>();

        for (Namespace ns : namespaces) {
            String namespaceName = ns.getName();
            String pattern = ns.getPattern();
            String prefix = ns.getPrefix();
            namespaceNameByPrefix.put(prefix, namespaceName);
            patternByNamespaceName.put(namespaceName, pattern);
            prefixByNamespaceName.put(namespaceName, prefix);
        }

        NAMESPACE_NAME_BY_PREFIX = Collections.unmodifiableMap(namespaceNameByPrefix);
        PATTERN_BY_NAMESPACE_NAME = Collections.unmodifiableMap(patternByNamespaceName);
        PREFIX_BY_NAMESPACE_NAME = Collections.unmodifiableMap(prefixByNamespaceName);
    }

    @Override
    public String getNamespaceForPrefix(String prefix) {
        return NAMESPACE_NAME_BY_PREFIX.getOrDefault(prefix, "");
    }

    @Override
    public String getPrefixByNamespaceName(String namespaceName) {
        return PREFIX_BY_NAMESPACE_NAME.getOrDefault(namespaceName, "");
    }


    @Override
    public String getPatternByNamespaceName(String namespaceName) {
        return PATTERN_BY_NAMESPACE_NAME.getOrDefault(namespaceName, "");
    }

    /**
     * Checks and processes a given resource URL to ensure it conforms to expected formats and corrections.
     * This method handles specific cases such as URLs containing "omim", "ncbigi", and "reactome".
     * It also processes general identifiers.org URLs and other alternative formats.
     *
     * @param url The URL to be checked and potentially modified.
     * @return An {@link Optional} containing the processed URL if valid, or empty if the URL should be skipped.
     */
    @Override
    public Optional<RegistryURI> resolveBackwards(String url) {
        url = url.trim();

        if (isValid(url)) {
            return Optional.of(new IdentifiersOrgURI(url));
        }

        Optional<Pair<Namespace, Resource>> matchingNamespaceResource = backwardsResolveResourceUrl(url);
        if (matchingNamespaceResource.isPresent()) {
            var namespace = matchingNamespaceResource.get().getLeft();
            var resource = matchingNamespaceResource.get().getRight();

            // compare a Namespace with "isNamespaceEmbeddedInLui" true vs. one with false
            // for why this following block is necessary
            String id;
            Matcher matcher;
            if (namespace.isNamespaceEmbeddedInLui()) {
                // extract ID using the namespace ID-Pattern
                var pattern = IdentifiersOrgURIUtils.addJavaRegexCaptureGroup(namespace.getPattern());
                matcher = Pattern.compile(pattern).matcher(url);
            } else {
                // extract ID using the resource ID-Pattern
                matcher = Pattern.compile(getResourceUrlPattern(resource)).matcher(url);

            }
            if (matcher.find()) {
                id = matcher.group();
                return Optional.of(new IdentifiersOrgURI(namespace.getPrefix(), id));
            }
        }
        return Optional.empty();
    }

    /**
     *  Existing models on BiGG and Biomodels use some namespaces that were removed from identifiers.org.
     *  While for some namespaces updated mappings were available (see {@link #fixIdentifiersOrgUri}),
     *  this checks for those that could not be migrated.
     */
    @Override
    public boolean validRegistryUrlPrefix(RegistryURI uri) {
        var invalidNamespaces = Set.of("bind",
                "ensemblgenomes-gn",
                "ensemblgenomes-tr",
                "omim",
                "pseudo",
                "psimi",
                "refseq_locus_tag",
                "refseq_name",
                "refseq_old_locus_tag",
                "refseq_orf_id",
                "refseq_synonym",
                "sabiork",
                "unit",
                "unknown");
        return !invalidNamespaces.contains(uri.getPrefix().toLowerCase());
    }

    @Override
    public boolean isValid(String url) {
        // Compile a pattern to match identifiers.org URLs
        Pattern identifiersURL = Pattern.compile(IdentifiersOrgURI.IDENTIFIERS_ORG_ID_PATTERN);
        Matcher urlMatcher = identifiersURL.matcher(url);
        return urlMatcher.matches();
    }

    public static IdentifiersOrgURI fixIdentifiersOrgUri(IdentifiersOrgURI uri) {
        if (uri.getPrefix().equalsIgnoreCase("ncbigi")) {
            return new IdentifiersOrgURI("ncbigene", uri.getId());
        }

        if (uri.getPrefix().equalsIgnoreCase("reactome.reaction")) {
            return new IdentifiersOrgURI("reactome", uri.getId());
        }

        if (uri.getPrefix().equalsIgnoreCase("reactome.compound")) {
            return new IdentifiersOrgURI("reactome", "R-ALL-" + uri.getId());
        }

        if (uri.getPrefix().equalsIgnoreCase("biomodels.sbo")) {
            return new IdentifiersOrgURI("sbo", uri.getId());
        }

        if (uri.getPrefix().equalsIgnoreCase("inchi_key")) {
            return new IdentifiersOrgURI("inchikey", uri.getId());
        }

        if (uri.getPrefix().toLowerCase().startsWith("obo.")) {
            return new IdentifiersOrgURI(uri.getPrefix().replaceFirst("obo\\.", ""), uri.getId());
        }

        if (uri.getPrefix().equalsIgnoreCase("psimod") || uri.getPrefix().equals("psi-mod") ) {
            return new IdentifiersOrgURI("mod", uri.getId());
        }

        return uri;
    }


    private static Optional<Pair<Namespace, Resource>> backwardsResolveResourceUrl(String url) {
        url = IdentifiersOrgURIUtils.removeHttpProtocolFromUrl(url);
        List<Pair<Namespace, Resource>> matchingResources = new ArrayList<>();
        // Iterate over all namespaces to find matchingResources
        for (Namespace namespace : namespaces) {
            // Iterate over all resources within the namespace to find matchingResources
            for (Resource resource : namespace.getResources()) {
                var regExPattern = getResourceUrlPattern(resource);
                if (url.matches(regExPattern)) {
                    matchingResources.add(Pair.of(namespace, resource));
                }
            }
        }
        // Log a message if more than one match is found, indicating non-uniqueness
        if (matchingResources.size() > 1) {
            logger.info(format("Could not resolve identifiers.org collection for URL {0} uniquely", url));
        } else if (matchingResources.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(matchingResources.get(0));
    }


    private static String getResourceUrlPattern(Resource resource) {
        return IdentifiersOrgURIUtils.removeHttpProtocolFromUrl(
                resource.getUrlPattern().replaceAll("\\{\\$id}", "(<id>[)]*)"));
    }

}
