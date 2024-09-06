package de.uni_halle.informatik.biodata.mp.resolver.identifiersorg;

import de.uni_halle.informatik.biodata.mp.db.bigg.BiGGId;
import de.uni_halle.informatik.biodata.mp.resolver.RegistryURI;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IdentifiersOrgURI implements RegistryURI, Comparable<IdentifiersOrgURI> {

    public static final String IDENTIFIERS_ORG_ID_PATTERN = "(https?://)?(www\\.)?identifiers\\.org/(?<prefix>.*?)/(?<id>.*)";

    private String prefix;
    private String id;

    public IdentifiersOrgURI(String url) {
        Matcher identifiersURL =
                Pattern.compile(IDENTIFIERS_ORG_ID_PATTERN).matcher(url);
        if (identifiersURL.matches()) {
            prefix = identifiersURL.group("prefix").toLowerCase();
            id = identifiersURL.group("id");
        }
    }

    public IdentifiersOrgURI(String prefix, String id) {
        this.prefix = prefix.toLowerCase();
        this.id = id;
    }

    public IdentifiersOrgURI(String prefix, BiGGId id) {
        this.prefix = prefix.toLowerCase();
        this.id = id.getAbbreviation();
    }

    public IdentifiersOrgURI(String prefix, Object id) {
        this.prefix = prefix.toLowerCase();
        this.id = id.toString();
    }

    @Override
    public String getURI() {
        return "https://identifiers.org/" + prefix + "/" + id;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public String getId() {
        return id;
    }


    @Override
    public String toString() {
        return "IdentifiersOrgURI{" +
                "prefix='" + prefix + '\'' +
                ", id='" + id + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdentifiersOrgURI that = (IdentifiersOrgURI) o;
        return Objects.equals(prefix, that.prefix) && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix, id);
    }

    @Override
    public int compareTo(IdentifiersOrgURI uri) {
        return this.getURI().compareTo(uri.getURI());
    }
}
