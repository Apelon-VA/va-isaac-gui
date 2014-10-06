//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.09.30 at 06:15:10 PM PDT 
//


package org.hl7.cdsoutputspecification.r2;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import org.hl7.vmr.r2.CodedIdentifier;


/**
 * Specifies the entities related to the source clinical statement regarding the evaluated person of interest.  
 * 
 * <p>Java class for RelatedEntityOutputSpecification complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="RelatedEntityOutputSpecification">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="relationshipTemplate" type="{urn:hl7-org:vmr:r2}CodedIdentifier" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="potentialRelationshipTemplate" type="{urn:hl7-org:vmr:r2}CodedIdentifier" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="entityOutputSpecification" type="{urn:hl7-org:cdsoutputspecification:r2}EntityOutputSpecification" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RelatedEntityOutputSpecification", propOrder = {
    "relationshipTemplate",
    "potentialRelationshipTemplate",
    "entityOutputSpecification"
})
public class RelatedEntityOutputSpecification {

    protected List<CodedIdentifier> relationshipTemplate;
    protected List<CodedIdentifier> potentialRelationshipTemplate;
    protected List<EntityOutputSpecification> entityOutputSpecification;

    /**
     * Gets the value of the relationshipTemplate property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the relationshipTemplate property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRelationshipTemplate().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CodedIdentifier }
     * 
     * 
     */
    public List<CodedIdentifier> getRelationshipTemplate() {
        if (relationshipTemplate == null) {
            relationshipTemplate = new ArrayList<CodedIdentifier>();
        }
        return this.relationshipTemplate;
    }

    /**
     * Gets the value of the potentialRelationshipTemplate property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the potentialRelationshipTemplate property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPotentialRelationshipTemplate().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CodedIdentifier }
     * 
     * 
     */
    public List<CodedIdentifier> getPotentialRelationshipTemplate() {
        if (potentialRelationshipTemplate == null) {
            potentialRelationshipTemplate = new ArrayList<CodedIdentifier>();
        }
        return this.potentialRelationshipTemplate;
    }

    /**
     * Gets the value of the entityOutputSpecification property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the entityOutputSpecification property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getEntityOutputSpecification().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link EntityOutputSpecification }
     * 
     * 
     */
    public List<EntityOutputSpecification> getEntityOutputSpecification() {
        if (entityOutputSpecification == null) {
            entityOutputSpecification = new ArrayList<EntityOutputSpecification>();
        }
        return this.entityOutputSpecification;
    }

}
