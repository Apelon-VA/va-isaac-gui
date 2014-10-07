//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.09.30 at 06:15:10 PM PDT 
//


package org.hl7.knowledgeartifact.r1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * The Split operator splits a string into a list of strings using a separator.
 * 
 * If the stringToSplit argument is null, the result is null.
 * 
 * If the stringToSplit argument does not contain any appearances of the separator, the result is a list of strings containing one element that is the value of the stringToSplit argument.
 * 
 * <p>Java class for Split complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Split">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:hl7-org:knowledgeartifact:r1}Expression">
 *       &lt;sequence>
 *         &lt;element name="stringToSplit" type="{urn:hl7-org:knowledgeartifact:r1}Expression"/>
 *         &lt;element name="separator" type="{urn:hl7-org:knowledgeartifact:r1}Expression" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Split", propOrder = {
    "stringToSplit",
    "separator"
})
public class Split
    extends Expression
{

    @XmlElement(required = true)
    protected Expression stringToSplit;
    protected Expression separator;

    /**
     * Gets the value of the stringToSplit property.
     * 
     * @return
     *     possible object is
     *     {@link Expression }
     *     
     */
    public Expression getStringToSplit() {
        return stringToSplit;
    }

    /**
     * Sets the value of the stringToSplit property.
     * 
     * @param value
     *     allowed object is
     *     {@link Expression }
     *     
     */
    public void setStringToSplit(Expression value) {
        this.stringToSplit = value;
    }

    /**
     * Gets the value of the separator property.
     * 
     * @return
     *     possible object is
     *     {@link Expression }
     *     
     */
    public Expression getSeparator() {
        return separator;
    }

    /**
     * Sets the value of the separator property.
     * 
     * @param value
     *     allowed object is
     *     {@link Expression }
     *     
     */
    public void setSeparator(Expression value) {
        this.separator = value;
    }

}