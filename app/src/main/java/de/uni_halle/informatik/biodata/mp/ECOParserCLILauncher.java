package de.uni_halle.informatik.biodata.mp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Set;

import de.uni_halle.informatik.biodata.mp.eco.DAG;
import de.uni_halle.informatik.biodata.mp.eco.Node;
import org.biojava.nbio.ontology.Ontology;
import org.biojava.nbio.ontology.Term;
import org.biojava.nbio.ontology.Triple;
import org.biojava.nbio.ontology.io.OboParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ECOParserCLILauncher {

  private static Ontology ontology;
  private static final Logger logger = LoggerFactory.getLogger(ECOParserCLILauncher.class);

  /**
   * @see <a href="https://github.com/draeger-lab/ModelPolisher/issues/5">Issue 5</>
   */
  enum ConfidenceScoreTerm {
    BIOCHEMICAL("ECO:0000002", 0),
    GENETIC("ECO:0000073", 1),
    SEQUENCE("ECO:0000044", 2),
    PHYSIOLOGICAL("ECO:0005551", 3),
    MODELING("0000001", 4);

    private final String ECOTerm;
    private final int confidenceScore;


    ConfidenceScoreTerm(String ECOTerm, int confidenceScore) {
      this.ECOTerm = ECOTerm;
      this.confidenceScore = confidenceScore;
    }


    public String getECOTerm() {
      return ECOTerm;
    }


    public int getConfidenceScore() {
      return confidenceScore;
    }


    public static String getECOTermFromScore(int confidenceScore) {
      for (ConfidenceScoreTerm confidenceScoreTerm : ConfidenceScoreTerm.values()) {
        if (confidenceScoreTerm.getConfidenceScore() == confidenceScore) {
          return confidenceScoreTerm.getECOTerm();
        }
      }
      return "";
    }
  }


  public static void main(String[] args) throws IOException, ParseException {
    ECOParserCLILauncher.parseECO();
  }


  public static String getECOTermFromScore(int confidenceScores) {
    return ConfidenceScoreTerm.getECOTermFromScore(confidenceScores);
  }


  public static int getConfidenceScoreForTerm(String query) {
    if (!ontology.containsTerm(query)) {
      // logger.sever(mpMessageBundle.get("TERM_NOT_IN_ONTOLOGY"))
      return -1;
    }
    Term term = ontology.getTerm(query);
    for (ConfidenceScoreTerm confidenceScoreTerm : ConfidenceScoreTerm.values()) {
      if (term.getName().equals(confidenceScoreTerm.getECOTerm())) {
        return confidenceScoreTerm.getConfidenceScore();
      }
    }
    return -1;
  }


  public static void parseECO() throws IOException, ParseException {
    String ecoName = "Evidence and Conclusion Ontology";
    String ecoDesc = "TODO";
    OboParser parser = new OboParser();
    try (InputStream inputStream = ECOParserCLILauncher.class.getResourceAsStream("eco.obo");
         BufferedReader oboReader = new BufferedReader(new InputStreamReader(inputStream))) {
      ontology = parser.parseOBO(oboReader, ecoName, ecoDesc);
      Term rootTerm = ontology.getTerm("ECO:0000000");
      DAG ontologyDAG = new DAG(rootTerm);
      traverse(ontologyDAG.getRoot());
    }
  }


  public static String getResourceURL(String term) {
    return "http://identifiers-org/eco/" + term;
  }


  /**
   * Traverse top-down from root and build the DAG
   *
   */
  private static void traverse(Node node) {
    Set<Triple> triples = ontology.getTriples(null, node.getTerm(), null);
    for (Triple triple : triples) {
      Term subject = triple.getSubject();
      Node child = new Node(subject);
      Node.linkNodes(node, child);
      traverse(child);
    }
  }
}
