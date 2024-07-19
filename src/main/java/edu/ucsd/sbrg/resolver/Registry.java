package edu.ucsd.sbrg.resolver;

import java.util.Optional;

public interface Registry {

    String getNamespaceForPrefix (String prefix);

    String getPrefixByNamespaceName(String namespaceName);

    String getPatternByNamespaceName(String namespaceName);

    Optional<RegistryURI> findRegistryUrlForOtherUrl(String url);

    boolean validRegistryUrlPrefix(RegistryURI uri);

    boolean isValid(String url);
}
