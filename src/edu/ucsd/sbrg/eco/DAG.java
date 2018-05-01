package edu.ucsd.sbrg.eco;

import org.biojava.nbio.ontology.Term;

public class DAG {

  private Node root;


  DAG(Term term) {
    root = new Node(term);
  }


  public Node getRoot() {
    return root;
  }


  public void traverse(Node node, int counter, StringBuilder sb) {
    counter++;
    sb.append("|");
    for (int i = 0; i < counter; i++) {
      sb.append("-");
    }
    sb.append("|").append(node.getTerm().getName()).append("\n");
    for (Node child : node.getChildren()) {
      traverse(child, counter, sb);
    }
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    traverse(root, -1, sb);
    return sb.toString();
  }
}
