package edu.ucsd.sbrg.parsers.models;

import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Metabolite {
  private static final Logger logger = Logger.getLogger(Metabolite.class.getName());

  @JsonProperty(required = true)
  private String id;
  @JsonProperty(required = true)
  private String name;
  @JsonProperty(required = true)
  private String compartment;
  private int charge;
  private String formula;
  private double bound;
  private Notes notes;
  private Annotation annotation;

  public Metabolite() {
    bound = 0;
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


  public String getCompartment() {
    return compartment;
  }


  public void setCompartment(String compartment) {
    Pattern validCompartmentCode = Pattern.compile("[a-z]{1,2}");
    if (validCompartmentCode.matcher(compartment).find()) {
      this.compartment = compartment;
    } else {
      logger.info(String.format("Compartment code '%s' in metabolite '%s' did not match pattern [a-z]{1,2}, trying to extract from id after parsing", compartment, id));
    }
  }


  public int getCharge() {
    return charge;
  }


  public void setCharge(int charge) {
    this.charge = charge;
  }


  public String getFormula() {
    return formula;
  }


  public void setFormula(String formula) {
    this.formula = formula;
  }


  public double getBound() {
    return bound;
  }


  public void setBound(double bound) {
    this.bound = bound;
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
