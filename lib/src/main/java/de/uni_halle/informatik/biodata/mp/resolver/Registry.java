package de.uni_halle.informatik.biodata.mp.resolver;

import java.util.Optional;

public interface Registry {

    String getNamespaceForPrefix (String prefix);

    String getPrefixByNamespaceName(String namespaceName);

    String getPatternByNamespaceName(String namespaceName);

    Optional<RegistryURI> resolveBackwards(String url);

    boolean validRegistryUrlPrefix(RegistryURI uri);

    boolean isValid(String url);

    boolean identifiesBiGG(String prefix);
}
