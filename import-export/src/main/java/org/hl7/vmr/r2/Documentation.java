//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.09.30 at 06:15:10 PM PDT 
//


package org.hl7.vmr.r2;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hl7.cdsdt.r2.CD;
import org.hl7.cdsdt.r2.ED;


/**
 * This type may be used to represent documentation that is either free text or richer in format (e.g., XML or HTML) where provenance is not relevant. The type of the documentation is determined by a code that represents the type of documentation ("e.g., a consult note, a provider instruction, a patient instruction, etc...). It is intended to represent comment fields and notes such as those associated with order entry forms. 
 * 
 * <p>Java class for Documentation complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Documentation">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:hl7-org:vmr:r2}ExtendedVmrTypeBase">
 *       &lt;sequence>
 *         &lt;element name="type" type="{urn:hl7-org:cdsdt:r2}CD" minOccurs="0"/>
 *         &lt;element name="content" type="{urn:hl7-org:cdsdt:r2}ED" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Documentation", propOrder = {
    "type",
    "content"
})
public class Documentation
    extends ExtendedVmrTypeBase
{

    protected CD type;
    protected ED content;

    /**
     * Gets the value of the type property.
     * 
     * @return
     *     possible object is
     *     {@link CD }
     *     
     */
    public CD getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link CD }
     *     
     */
    public void setType(CD value) {
        this.type = value;
    }

    /**
     * Gets the value of the content property.
     * 
     * @return
     *     possible object is
     *     {@link ED }
     *     
     */
    public ED getContent() {
        return content;
    }

    /**
     * Sets the value of the content property.
     * 
     * @param value
     *     allowed object is
     *     {@link ED }
     *     
     */
    public void setContent(ED value) {
        this.content = value;
    }

}