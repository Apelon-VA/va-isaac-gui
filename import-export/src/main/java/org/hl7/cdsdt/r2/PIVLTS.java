//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.09.30 at 06:15:10 PM PDT 
//


package org.hl7.cdsdt.r2;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * An interval of time that recurs periodically. PIVL has two properties, phase and period/frequency. phase specifies the "interval prototype" that is repeated on the period/frequency.
 * 
 * <p>Java class for PIVL_TS complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="PIVL_TS">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:hl7-org:cdsdt:r2}QTY">
 *       &lt;sequence>
 *         &lt;element name="phase" type="{urn:hl7-org:cdsdt:r2}IVL_TS" minOccurs="0"/>
 *         &lt;element name="period" type="{urn:hl7-org:cdsdt:r2}PQ" minOccurs="0"/>
 *         &lt;element name="frequency" type="{urn:hl7-org:cdsdt:r2}RTO" minOccurs="0"/>
 *         &lt;element name="count" type="{urn:hl7-org:cdsdt:r2}INT" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="alignment" type="{urn:hl7-org:cdsdt:r2}CalendarCycle" />
 *       &lt;attribute name="isFlexible" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PIVL_TS", propOrder = {
    "phase",
    "period",
    "frequency",
    "count"
})
public class PIVLTS
    extends QTY
{

    protected IVLTS phase;
    protected PQ period;
    protected RTO frequency;
    protected INT count;
    @XmlAttribute(name = "alignment")
    protected CalendarCycle alignment;
    @XmlAttribute(name = "isFlexible")
    protected Boolean isFlexible;

    /**
     * Gets the value of the phase property.
     * 
     * @return
     *     possible object is
     *     {@link IVLTS }
     *     
     */
    public IVLTS getPhase() {
        return phase;
    }

    /**
     * Sets the value of the phase property.
     * 
     * @param value
     *     allowed object is
     *     {@link IVLTS }
     *     
     */
    public void setPhase(IVLTS value) {
        this.phase = value;
    }

    /**
     * Gets the value of the period property.
     * 
     * @return
     *     possible object is
     *     {@link PQ }
     *     
     */
    public PQ getPeriod() {
        return period;
    }

    /**
     * Sets the value of the period property.
     * 
     * @param value
     *     allowed object is
     *     {@link PQ }
     *     
     */
    public void setPeriod(PQ value) {
        this.period = value;
    }

    /**
     * Gets the value of the frequency property.
     * 
     * @return
     *     possible object is
     *     {@link RTO }
     *     
     */
    public RTO getFrequency() {
        return frequency;
    }

    /**
     * Sets the value of the frequency property.
     * 
     * @param value
     *     allowed object is
     *     {@link RTO }
     *     
     */
    public void setFrequency(RTO value) {
        this.frequency = value;
    }

    /**
     * Gets the value of the count property.
     * 
     * @return
     *     possible object is
     *     {@link INT }
     *     
     */
    public INT getCount() {
        return count;
    }

    /**
     * Sets the value of the count property.
     * 
     * @param value
     *     allowed object is
     *     {@link INT }
     *     
     */
    public void setCount(INT value) {
        this.count = value;
    }

    /**
     * Gets the value of the alignment property.
     * 
     * @return
     *     possible object is
     *     {@link CalendarCycle }
     *     
     */
    public CalendarCycle getAlignment() {
        return alignment;
    }

    /**
     * Sets the value of the alignment property.
     * 
     * @param value
     *     allowed object is
     *     {@link CalendarCycle }
     *     
     */
    public void setAlignment(CalendarCycle value) {
        this.alignment = value;
    }

    /**
     * Gets the value of the isFlexible property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isIsFlexible() {
        return isFlexible;
    }

    /**
     * Sets the value of the isFlexible property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setIsFlexible(Boolean value) {
        this.isFlexible = value;
    }

}
