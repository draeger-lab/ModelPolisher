//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation,
// v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2018.04.27 at 01:32:33 PM CEST
//
package edu.ucsd.sbrg.miriam.xjc;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for uris complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="uris">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="uri" type="{http://www.biomodels.net/MIRIAM/}uri" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "uris", propOrder = {"uri"})
public class Uris {

  @XmlElement(required = true)
  protected List<Uri> uri;


  /**
   * Gets the value of the uri property.
   * <p>
   * This accessor method returns a reference to the live list,
   * not a snapshot. Therefore any modification you make to the
   * returned list will be present inside the JAXB object.
   * This is why there is not a <CODE>set</CODE> method for the uri property.
   * <p>
   * For example, to add a new item, do as follows:
   * 
   * <pre>
   * getUri().add(newItem);
   * </pre>
   * <p>
   * Objects of the following type(s) are allowed in the list
   * {@link Uri }
   */
  public List<Uri> getUri() {
    if (uri == null) {
      uri = new ArrayList<Uri>();
    }
    return this.uri;
  }
}
