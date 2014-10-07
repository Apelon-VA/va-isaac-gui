//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.09.30 at 06:15:10 PM PDT 
//


package org.hl7.vmr.r2;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import org.hl7.cdsdt.r2.CD;
import org.hl7.cdsdt.r2.IVLTS;


/**
 * The abstract base class for an observation which represents a result (e.g., a laboratory value), a clinical finding (e.g., sitting, tachypneic, rebound tenderness), or an inferred finding such as one produced by a CDS system (e.g., patient is in need of an HbA1c test).
 * 
 * <p>Java class for ObservationBase complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ObservationBase">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:hl7-org:vmr:r2}ClinicalStatement">
 *       &lt;sequence>
 *         &lt;element name="observationFocus" type="{urn:hl7-org:cdsdt:r2}CD"/>
 *         &lt;element name="observationMethod" type="{urn:hl7-org:cdsdt:r2}CD" minOccurs="0"/>
 *         &lt;element name="targetBodySite" type="{urn:hl7-org:vmr:r2}BodySite" minOccurs="0"/>
 *         &lt;element name="interpretation" type="{urn:hl7-org:cdsdt:r2}CD" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="observationEventTime" type="{urn:hl7-org:cdsdt:r2}IVL_TS" minOccurs="0"/>
 *         &lt;element name="status" type="{urn:hl7-org:cdsdt:r2}CD" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ObservationBase", propOrder = {
    "observationFocus",
    "observationMethod",
    "targetBodySite",
    "interpretation",
    "observationEventTime",
    "status"
})
@XmlSeeAlso({
    ObservationResult.class,
    CompositeObservationResult.class
})
public abstract class ObservationBase
    extends ClinicalStatement
{

    @XmlElement(required = true)
    protected CD observationFocus;
    protected CD observationMethod;
    protected BodySite targetBodySite;
    protected List<CD> interpretation;
    protected IVLTS observationEventTime;
    protected CD status;

    /**
     * Gets the value of the observationFocus property.
     * 
     * @return
     *     possible object is
     *     {@link CD }
     *     
     */
    public CD getObservationFocus() {
        return observationFocus;
    }

    /**
     * Sets the value of the observationFocus property.
     * 
     * @param value
     *     allowed object is
     *     {@link CD }
     *     
     */
    public void setObservationFocus(CD value) {
        this.observationFocus = value;
    }

    /**
     * Gets the value of the observationMethod property.
     * 
     * @return
     *     possible object is
     *     {@link CD }
     *     
     */
    public CD getObservationMethod() {
        return observationMethod;
    }

    /**
     * Sets the value of the observationMethod property.
     * 
     * @param value
     *     allowed object is
     *     {@link CD }
     *     
     */
    public void setObservationMethod(CD value) {
        this.observationMethod = value;
    }

    /**
     * Gets the value of the targetBodySite property.
     * 
     * @return
     *     possible object is
     *     {@link BodySite }
     *     
     */
    public BodySite getTargetBodySite() {
        return targetBodySite;
    }

    /**
     * Sets the value of the targetBodySite property.
     * 
     * @param value
     *     allowed object is
     *     {@link BodySite }
     *     
     */
    public void setTargetBodySite(BodySite value) {
        this.targetBodySite = value;
    }

    /**
     * Gets the value of the interpretation property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the interpretation property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getInterpretation().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CD }
     * 
     * 
     */
    public List<CD> getInterpretation() {
        if (interpretation == null) {
            interpretation = new ArrayList<CD>();
        }
        return this.interpretation;
    }

    /**
     * Gets the value of the observationEventTime property.
     * 
     * @return
     *     possible object is
     *     {@link IVLTS }
     *     
     */
    public IVLTS getObservationEventTime() {
        return observationEventTime;
    }

    /**
     * Sets the value of the observationEventTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link IVLTS }
     *     
     */
    public void setObservationEventTime(IVLTS value) {
        this.observationEventTime = value;
    }

    /**
     * Gets the value of the status property.
     * 
     * @return
     *     possible object is
     *     {@link CD }
     *     
     */
    public CD getStatus() {
        return status;
    }

    /**
     * Sets the value of the status property.
     * 
     * @param value
     *     allowed object is
     *     {@link CD }
     *     
     */
    public void setStatus(CD value) {
        this.status = value;
    }

}