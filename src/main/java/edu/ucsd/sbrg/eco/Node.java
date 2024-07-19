package edu.ucsd.sbrg.eco;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.biojava.nbio.ontology.Term;

class Node {

  private Term term;
  private List<Node> parents;
  private List<Node> children;


  Node(Term term) {
    this.term = term;
    parents = new ArrayList<>();
    children = new ArrayList<>();
  }


  public void addChild(Node child) {
    if (!children.contains(child)) {
      children.add(child);
    }
  }


  public void addChild(Term child) {
    Node current = new Node(child);
    addChild(current);
  }


  public void addParent(Node parent) {
    if (!parents.contains(parent)) {
      parents.add(parent);
    }
  }


  public void addParent(Term parent) {
    Node current = new Node(parent);
    addParent(current);
  }


  public static void linkNodes(Node parent, Node child) {
    parent.addChild(child);
    child.addParent(parent);
  }


  public void setChildren(List<Node> children) {
    this.children = children;
  }


  public void setParents(List<Node> parents) {
    this.parents = parents;
  }


  public void setTerm(Term term) {
    this.term = term;
  }


  public List<Node> getChildren() {
    return children;
  }


  public List<Node> getParents() {
    return parents;
  }


  /**
   * Returns the Node containing the specified Term or null, if not present
   *
   */
  public Node findTerm(Term term) {
    if (this.getTerm().equals(term)) {
      return this;
    } else {
      Node node;
      node = findChild(this, term);
      if (node == null) {
        node = findParent(this, term);
      }
      return node;
    }
  }


  public Node findParent(Node node, Term term) {
    List<Node> parents = node.getParents();
    for (Node parent : parents) {
      if (parent.getTerm().equals(term)) {
        return parent;
      }
    }
    for (Node parent : parents) {
      Node retVal = findParent(parent, term);
      if (retVal != null) {
        return retVal;
      }
    }
    return null;
  }


  public Node findChild(Node node, Term term) {
    List<Node> children = node.getChildren();
    for (Node child : children) {
      if (child.getTerm().equals(term)) {
        return child;
      }
    }
    for (Node child : children) {
      Node retVal = findParent(child, term);
      if (retVal != null) {
        return retVal;
      }
    }
    return null;
  }


  public Term getTerm() {
    return term;
  }


  // Compare Terms as their should not be duplictes in parents or children,
  // parents and children need not be equal
  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Node node = (Node) o;
    return Objects.equals(term, node.term);
  }


  @Override
  public int hashCode() {
    return Objects.hash(term);
  }
}
