//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation,
// v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2018.04.27 at 01:32:33 PM CEST
//
package edu.ucsd.sbrg.miriam.xjc;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * <p>
 * Java class for element complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="element">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "element", propOrder = {"value"})
public class Element {

  @XmlValue
  protected String value;


  /**
   * Gets the value of the value property.
   * 
   * @return
   *         possible object is
   *         {@link String }
   */
  public String getValue() {
    return value;
  }


  /**
   * Sets the value of the value property.
   * 
   * @param value
   *        allowed object is
   *        {@link String }
   */
  public void setValue(String value) {
    this.value = value;
  }
}
