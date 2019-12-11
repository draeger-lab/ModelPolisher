package edu.ucsd.sbrg.parsers.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Reaction {
  @JsonProperty(required = true)
  private String id;
  @JsonProperty(required = true)
  private String name;
  @JsonProperty(required = true)
  private Metabolites metabolites;
  @JsonProperty(value = "gene_reaction_rule", required = true)
  private String geneReactionRule;
  @JsonProperty(value = "lower_bound", required = true)
  private double lowerBound;
  @JsonProperty(value = "upper_bound", required = true)
  private double upperBound;
  @JsonProperty(value = "objective_coefficient")
  private double objectiveCoefficient;
  private String subsystem;
  private Notes notes;
  private Annotation annotation;

  public Reaction() {
    objectiveCoefficient = 0;
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

  public Metabolites getMetabolites() {
    return metabolites;
  }

  public void setMetabolites(Metabolites metabolites) {
    this.metabolites = metabolites;
  }

  public String getGeneReactionRule() {
    return geneReactionRule;
  }

  public void setGeneReactionRule(String geneReactionRule) {
    this.geneReactionRule = geneReactionRule;
  }

  public double getLowerBound() {
    return lowerBound;
  }

  public void setLowerBound(double lowerBound) {
    this.lowerBound = lowerBound;
  }

  public double getUpperBound() {
    return upperBound;
  }

  public void setUpperBound(double upperBound) {
    this.upperBound = upperBound;
  }

  public double getObjectiveCoefficient() {
    return objectiveCoefficient;
  }

  public void setObjectiveCoefficient(double objectiveCoefficient) {
    this.objectiveCoefficient = objectiveCoefficient;
  }

  public String getSubsystem() {
    return subsystem;
  }

  public void setSubsystem(String subsystem) {
    this.subsystem = subsystem;
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
