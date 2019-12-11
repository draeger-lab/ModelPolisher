package edu.ucsd.sbrg.parsers.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Root {

  @JsonProperty(required = true)
  private String id;
  private String name;
  private String description;
  private int version;
  @JsonProperty(required = true)
  private List<Reaction> reactions;
  @JsonProperty(required = true)
  private List<Metabolite> metabolites;
  @JsonProperty(required = true)
  private List<Gene> genes;
  private Compartments compartments;
  private Notes notes;
  private Annotation annotation;

  public Root() {
    version = 1;
  }


  public String getId() {
    return id;
  }


  public void setId(String id) {
    this.id = id;
  }


  public String getName() {
    return name;
  }


  public void setName(String name) {
    this.name = name;
  }


  public String getDescription() {
    return description;
  }


  public void setDescription(String description) {
    this.description = description;
  }


  public int getVersion() {
    return version;
  }


  public void setVersion(int version) {
    this.version = version;
  }


  public List<Reaction> getReactions() {
    return reactions;
  }


  public void setReactions(List<Reaction> reactions) {
    this.reactions = reactions;
  }


  public List<Metabolite> getMetabolites() {
    return metabolites;
  }


  public void setMetabolites(List<Metabolite> metabolites) {
    this.metabolites = metabolites;
  }


  public List<Gene> getGenes() {
    return genes;
  }


  public void setGenes(List<Gene> genes) {
    this.genes = genes;
  }


  public Compartments getCompartments() {
    return compartments;
  }


  public void setCompartments(Compartments compartments) {
    this.compartments = compartments;
  }


  public Notes getNotes() {
    return notes;
  }


  public void setNotes(Notes notes) {
    this.notes = notes;
  }


  public Annotation getAnnotation() {
    return annotation;
  }


  public void setAnnotation(Annotation annotation) {
    this.annotation = annotation;
  }
}
