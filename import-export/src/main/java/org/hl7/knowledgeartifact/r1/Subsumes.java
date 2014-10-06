//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.09.30 at 06:15:10 PM PDT 
//


package org.hl7.knowledgeartifact.r1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * The Subsumes operator returns true if the operands were of the same code system, and the ancestor operand subsumed the descendant operand in the hierarchy of the code system. If the codes are the same code, the operator returns true.
 * 
 * <p>Java class for Subsumes complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Subsumes">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:hl7-org:knowledgeartifact:r1}Expression">
 *       &lt;sequence>
 *         &lt;element name="ancestor" type="{urn:hl7-org:knowledgeartifact:r1}Expression" minOccurs="0"/>
 *         &lt;element name="descendent" type="{urn:hl7-org:knowledgeartifact:r1}Expression" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Subsumes", propOrder = {
    "ancestor",
    "descendent"
})
public class Subsumes
    extends Expression
{

    protected Expression ancestor;
    protected Expression descendent;

    /**
     * Gets the value of the ancestor property.
     * 
     * @return
     *     possible object is
     *     {@link Expression }
     *     
     */
    public Expression getAncestor() {
        return ancestor;
    }

    /**
     * Sets the value of the ancestor property.
     * 
     * @param value
     *     allowed object is
     *     {@link Expression }
     *     
     */
    public void setAncestor(Expression value) {
        this.ancestor = value;
    }

    /**
     * Gets the value of the descendent property.
     * 
     * @return
     *     possible object is
     *     {@link Expression }
     *     
     */
    public Expression getDescendent() {
        return descendent;
    }

    /**
     * Sets the value of the descendent property.
     * 
     * @param value
     *     allowed object is
     *     {@link Expression }
     *     
     */
    public void setDescendent(Expression value) {
        this.descendent = value;
    }

}
