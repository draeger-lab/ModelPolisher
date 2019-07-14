//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation,
// v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2018.04.27 at 01:32:33 PM CEST
//
package main.java.edu.ucsd.sbrg.miriam.xjc;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for restriction complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="restriction">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="statement" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="link" type="{http://www.biomodels.net/MIRIAM/}link" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="type" use="required" type="{http://www.w3.org/2001/XMLSchema}byte" />
 *       &lt;attribute name="desc" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "restriction", propOrder = {"statement", "link"})
public class Restriction {

  protected String statement;
  protected Link link;
  @XmlAttribute(name = "type", required = true)
  protected byte type;
  @XmlAttribute(name = "desc", required = true)
  protected String desc;


  /**
   * Gets the value of the statement property.
   * 
   * @return
   *         possible object is
   *         {@link String }
   */
  public String getStatement() {
    return statement;
  }


  /**
   * Sets the value of the statement property.
   * 
   * @param value
   *        allowed object is
   *        {@link String }
   */
  public void setStatement(String value) {
    this.statement = value;
  }


  /**
   * Gets the value of the link property.
   * 
   * @return
   *         possible object is
   *         {@link Link }
   */
  public Link getLink() {
    return link;
  }


  /**
   * Sets the value of the link property.
   * 
   * @param value
   *        allowed object is
   *        {@link Link }
   */
  public void setLink(Link value) {
    this.link = value;
  }


  /**
   * Gets the value of the type property.
   */
  public byte getType() {
    return type;
  }


  /**
   * Sets the value of the type property.
   */
  public void setType(byte value) {
    this.type = value;
  }


  /**
   * Gets the value of the desc property.
   * 
   * @return
   *         possible object is
   *         {@link String }
   */
  public String getDesc() {
    return desc;
  }


  /**
   * Sets the value of the desc property.
   * 
   * @param value
   *        allowed object is
   *        {@link String }
   */
  public void setDesc(String value) {
    this.desc = value;
  }
}