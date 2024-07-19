package edu.ucsd.sbrg;

import org.sbml.jsbml.AbstractNamedSBase;
import org.sbml.jsbml.AbstractSBase;
import org.sbml.jsbml.CVTerm;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class TestUtils {

    public static Parameters initParameters() {
        return initParameters(Map.of());
    }

    public static Parameters initParameters(Map<String, Object> params) {
//        props.setProperty(ModelPolisherOptions.DOCUMENT_TITLE_PATTERN.getOptionName(),
//                "");

        var map = new HashMap<>(params);
        map.putIfAbsent("compressionType", ModelPolisherOptions.Compression.NONE.name());

        return new Parameters(map);
    }


    public static void assertCVTermIsPresent(AbstractNamedSBase entity,
                                             CVTerm.Type t,
                                             CVTerm.Qualifier q) {
        assertCVTermIsPresent(entity, t, q, null, "CVTerm of type " + t.toString()
                + ", " + q.toString() + " not present on entity "
                + entity.getId());
    }

    public static void assertCVTermIsPresent(AbstractNamedSBase entity,
                                             CVTerm.Type t,
                                             CVTerm.Qualifier q,
                                             String uri) {
        assertCVTermIsPresent(entity, t, q, uri, "CVTerm of type " + t.toString()
                + ", " + q.toString() + " with URI " + uri + " not present on entity "
        + entity.getId());
    }


    public static void assertCVTermIsPresent(AbstractNamedSBase entity,
                                             CVTerm.Type t,
                                             CVTerm.Qualifier q,
                                             String uri,
                                             String message) {
        var cvTerms = entity.getCVTerms();
        for (var term : cvTerms) {
            for (var r : term.getResources()) {
                if (term.getQualifierType().equals(t)
                        && (null == q || term.getQualifier().equals(q))
                        && (null == uri || r.equals(uri))) {
                    assertTrue(true);
                    return;
                }
            }
        }
        fail(message);
    }

    public static void assertCVTermsArePresent(AbstractSBase entity,
                                               CVTerm.Type t,
                                               CVTerm.Qualifier q,
                                               Collection<String> uris,
                                               String message) {
        var cvTerms = entity.getCVTerms();
        for(var term : cvTerms) {
            // this is just an awkward way to implement set difference, sorry bout that
            Collection<String> us = null;
            if(null != uris) {
                us = new HashSet<>(new HashSet<>(uris));
                us.removeAll(term.getResources());
            }
            // <- until here

            if (term.getQualifierType().equals(t)
                    && (null == q  || term.getQualifier().equals(q))
                    && (null == uris || us.isEmpty())) {
                assertTrue(true);
                return;
            }
        }
        fail(message + "Instead: " + entity.getCVTerms().stream()
                .map(CVTerm::getResources)
                .flatMap(List::stream)
                .collect(Collectors.joining("\n")));
    }

    /**
     * This is a development-time utility, which serves to quickly get a glimpse
     * of which CV Terms are present on an entity to ease writing assertions.
     */
    public static String printCVTerm(CVTerm c) {
        var newline = System.lineSeparator();
        return String.join(newline, c.getQualifierType().toString(),
                c.getQualifier().toString(),
                String.join(", ", c.getResources()));
    }

    /**
     * This is a development-time utility, which serves to quickly print assertions.
     */
    public static String printCVTermAssertion(CVTerm c) {
        var newline = System.lineSeparator();
        return "assertCVTermIsPresent(__," + newline
                + "CVTerm.Type." + c.getQualifierType().toString() + "," + newline
                + "CVTerm.Qualifier." + c.getQualifier().toString() + "," + newline
                + "\"" + String.join("\", \"", c.getResources())
                + "\");";
    }

    public static String printAllCVTerms(AbstractNamedSBase entity) {
        var newline = System.lineSeparator();
        return entity.getCVTerms().stream().map(TestUtils::printCVTermAssertion).collect(Collectors.joining(newline));
    }

}
