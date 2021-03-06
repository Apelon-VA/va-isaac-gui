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
import javax.xml.bind.annotation.XmlType;
import org.hl7.cdsdt.r2.CD;
import org.hl7.cdsdt.r2.INT;
import org.hl7.cdsdt.r2.ST;


/**
 * Contains information about the protocol under which the vaccine was administered.
 * 
 * <p>Java class for VaccinationProtocol complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="VaccinationProtocol">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:hl7-org:vmr:r2}ExtendedVmrTypeBase">
 *       &lt;sequence>
 *         &lt;element name="series" type="{urn:hl7-org:cdsdt:r2}ST" minOccurs="0"/>
 *         &lt;element name="numberOfDosesInSeries" type="{urn:hl7-org:cdsdt:r2}INT" minOccurs="0"/>
 *         &lt;element name="positionInSeries" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="authority" type="{urn:hl7-org:vmr:r2}Organization" minOccurs="0"/>
 *         &lt;element name="targetedDisease" type="{urn:hl7-org:cdsdt:r2}CD" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="vaccineGroup" type="{urn:hl7-org:cdsdt:r2}CD" minOccurs="0"/>
 *         &lt;element name="evaluationStatus" type="{urn:hl7-org:cdsdt:r2}CD" minOccurs="0"/>
 *         &lt;element name="evaluationStatusReason" type="{urn:hl7-org:cdsdt:r2}CD" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "VaccinationProtocol", propOrder = {
    "series",
    "numberOfDosesInSeries",
    "positionInSeries",
    "authority",
    "targetedDisease",
    "vaccineGroup",
    "evaluationStatus",
    "evaluationStatusReason"
})
public class VaccinationProtocol
    extends ExtendedVmrTypeBase
{

    protected ST series;
    protected INT numberOfDosesInSeries;
    protected Integer positionInSeries;
    protected Organization authority;
    protected List<CD> targetedDisease;
    protected CD vaccineGroup;
    protected CD evaluationStatus;
    protected List<CD> evaluationStatusReason;

    /**
     * Gets the value of the series property.
     * 
     * @return
     *     possible object is
     *     {@link ST }
     *     
     */
    public ST getSeries() {
        return series;
    }

    /**
     * Sets the value of the series property.
     * 
     * @param value
     *     allowed object is
     *     {@link ST }
     *     
     */
    public void setSeries(ST value) {
        this.series = value;
    }

    /**
     * Gets the value of the numberOfDosesInSeries property.
     * 
     * @return
     *     possible object is
     *     {@link INT }
     *     
     */
    public INT getNumberOfDosesInSeries() {
        return numberOfDosesInSeries;
    }

    /**
     * Sets the value of the numberOfDosesInSeries property.
     * 
     * @param value
     *     allowed object is
     *     {@link INT }
     *     
     */
    public void setNumberOfDosesInSeries(INT value) {
        this.numberOfDosesInSeries = value;
    }

    /**
     * Gets the value of the positionInSeries property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getPositionInSeries() {
        return positionInSeries;
    }

    /**
     * Sets the value of the positionInSeries property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setPositionInSeries(Integer value) {
        this.positionInSeries = value;
    }

    /**
     * Gets the value of the authority property.
     * 
     * @return
     *     possible object is
     *     {@link Organization }
     *     
     */
    public Organization getAuthority() {
        return authority;
    }

    /**
     * Sets the value of the authority property.
     * 
     * @param value
     *     allowed object is
     *     {@link Organization }
     *     
     */
    public void setAuthority(Organization value) {
        this.authority = value;
    }

    /**
     * Gets the value of the targetedDisease property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the targetedDisease property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTargetedDisease().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CD }
     * 
     * 
     */
    public List<CD> getTargetedDisease() {
        if (targetedDisease == null) {
            targetedDisease = new ArrayList<CD>();
        }
        return this.targetedDisease;
    }

    /**
     * Gets the value of the vaccineGroup property.
     * 
     * @return
     *     possible object is
     *     {@link CD }
     *     
     */
    public CD getVaccineGroup() {
        return vaccineGroup;
    }

    /**
     * Sets the value of the vaccineGroup property.
     * 
     * @param value
     *     allowed object is
     *     {@link CD }
     *     
     */
    public void setVaccineGroup(CD value) {
        this.vaccineGroup = value;
    }

    /**
     * Gets the value of the evaluationStatus property.
     * 
     * @return
     *     possible object is
     *     {@link CD }
     *     
     */
    public CD getEvaluationStatus() {
        return evaluationStatus;
    }

    /**
     * Sets the value of the evaluationStatus property.
     * 
     * @param value
     *     allowed object is
     *     {@link CD }
     *     
     */
    public void setEvaluationStatus(CD value) {
        this.evaluationStatus = value;
    }

    /**
     * Gets the value of the evaluationStatusReason property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the evaluationStatusReason property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getEvaluationStatusReason().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CD }
     * 
     * 
     */
    public List<CD> getEvaluationStatusReason() {
        if (evaluationStatusReason == null) {
            evaluationStatusReason = new ArrayList<CD>();
        }
        return this.evaluationStatusReason;
    }

}
