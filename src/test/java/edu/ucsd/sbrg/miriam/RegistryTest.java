package edu.ucsd.sbrg.miriam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import edu.ucsd.sbrg.miriam.models.Miriam;
import edu.ucsd.sbrg.miriam.models.Namespace;
import edu.ucsd.sbrg.miriam.models.Resource;

public class RegistryTest {

  private static Set<Namespace> namespaces;

  @BeforeAll
  public static void setUp() {
    Miriam miriam = RegistryProvider.getInstance().getMiriam();
    namespaces = new LinkedHashSet<>(miriam.getNamespaces().values());
  }


  @Test
  public void retrieveNamespaceNonCanonical() {
    for (Namespace ns : namespaces) {
      // Given sample identifier does not match provided pattern for GWAS and Pattern is wrong for GOLD genome
      String name = ns.getName();
      if (name.equals("GWAS Central Phenotype") || name.equals("GOLD genome")) {
        continue;
      }
      for (Resource res : ns.getResources()) {
        // id from sciwalker is slightly different, ignore for now
        // Panther Pattern is interpreted incorrectly - '^G|P|U|C|S\\d{5}$' should be '^[GPUCS]\\d{5}$' instead
        name = res.getName();
        if (name.equals("SciWalker Open Data") || name.equals("PANTHER Pathway Component at USC (Los Angeles)")) {
          continue;
        }
        String id = res.getSampleId();
        String urlPattern = res.getUrlPattern();
        String sourceURL = urlPattern.replaceAll("\\{\\$id}", id);
        Optional<String> canonicalURL = Registry.checkResourceUrl(sourceURL);
        assertTrue(canonicalURL.isPresent());
        canonicalURL.ifPresent(url -> {
          assertTrue(url.startsWith("https://identifiers.org"));
          List<String> parts = Registry.getPartsFromCanonicalURI(url);
          assertEquals(2, parts.size());
        });
        // While most provider inequalities result in correct resource resolution, a few need to be handled differently
        // TODO: add this handling
        // keep this in the comments for now:
        // String provider = parts.get(0);
        // ark is present twice, once nested in a namespace and once as a separate namespace, the created identifiers
        // URI, however, is resolvable, same problem with ebi.ac.uk/ena and others
        // if (targetPrefix.equals("bioproject") && provider.equals("dbest")
        // // BDGP EST at NCBI resolves to correct entry, but identifiers.org adress ebi instead of ncbi, this should be
        // // ok
        // || targetPrefix.equals("bdgp.est") && provider.equals("dbest")
        // || targetPrefix.equals("cath.superfamily") && provider.equals("cath")
        // || targetPrefix.equals("ena.embl") && provider.equals("dbest")
        // || targetPrefix.equals("ena.embl") && provider.equals("insdc")
        // || targetPrefix.equals("insdc") && provider.equals("dbest")
        // || targetPrefix.equals("insdc.cds") && provider.equals("dbest")
        // || targetPrefix.equals("kegg.metagenome") && provider.equals("kegg")
        // // linkedchemistry.info resolves to both chembl.compound and chembl.target. While this is wrong, chembl can
        // // still resolve this correctly. Leave for now
        // || targetPrefix.equals("chembl.compound") && provider.equals("chembl.target")
        // || targetPrefix.equals("minid") && provider.equals("ark")) {
        // } else {
        // assertEquals(targetPrefix, provider);
        // }
      }
    }
  }


  @Test
  public void checkPatternTest() {
    for (Namespace ns : namespaces) {
      // Given sample identifier does not match provided pattern for GWAS and Pattern is wrong for GOLD genome
      // Regex for Panther Pathway Component is wrong
      String name = ns.getName();
      if (name.equals("GWAS Central Phenotype") || name.equals("GOLD genome")
        || name.equals("PANTHER Pathway Component")) {
        continue;
      }
      String id = correctId(ns);
      String pattern = ns.getPattern();
      assertTrue(Registry.checkPattern(id, pattern));
    }
  }


  @Test
  public void getPartsFromCanonicalURITest() {
    for (Namespace ns : namespaces) {
      // Given sample identifier does not match provided pattern for GWAS and Pattern is wrong for GOLD genome
      // Regex for Panther Pathway Component is wrong
      String name = ns.getName();
      if (name.equals("GWAS Central Phenotype") || name.equals("GOLD genome")
        || name.equals("PANTHER Pathway Component")) {
        continue;
      }
      String resource = Registry.createURI(ns.getPrefix(), correctId(ns));
      List<String> parts = Registry.getPartsFromCanonicalURI(resource);
      assertEquals(2, parts.size());
      String provider = parts.get(0);
      String id = parts.get(1);
      String collection = Registry.getCollectionForPrefix(provider);
      String pattern = Registry.getPattern(collection);
      assertEquals(ns.getPattern(), pattern);
      assertTrue(Registry.checkPattern(id, pattern));
    }
  }


  private String correctId(Namespace ns) {
    String id = ns.getSampleId();
    String pattern = ns.getPattern();
    // Extract prefix if pattern in Lui, aka prefix is part of pattern but id does not contain it
    if (ns.isNamespaceEmbeddedInLui()) {
      Pattern prefixPattern = Pattern.compile("^\\^?.*?(?<prefix>\\w+)[\\\\]?:.*");
      Matcher patternWithPrefix = prefixPattern.matcher(pattern);
      if (patternWithPrefix.matches()) {
        String prefix = patternWithPrefix.group("prefix");
        if (!id.startsWith(prefix)) {
          id = prefix + ":" + id;
        }
      }
    }
    return id;
  }
}
