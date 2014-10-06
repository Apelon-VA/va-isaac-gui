//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.09.30 at 06:15:10 PM PDT 
//


package org.hl7.kaoutput.r1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import org.hl7.cdsdt.r2.II;
import org.hl7.cdsoutput.r2.CDSOutput;
import org.hl7.knowledgeartifact.r1.ActionGroup;


/**
 * <p>Java class for CDSActionGroupResponse complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="CDSActionGroupResponse">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:hl7-org:cdsoutput:r2}CDSOutput">
 *       &lt;sequence>
 *         &lt;element name="patientId" type="{urn:hl7-org:cdsdt:r2}II"/>
 *         &lt;element name="actionGroup" type="{urn:hl7-org:knowledgeartifact:r1}ActionGroup"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CDSActionGroupResponse", propOrder = {
    "patientId",
    "actionGroup"
})
public class CDSActionGroupResponse
    extends CDSOutput
{

    @XmlElement(required = true)
    protected II patientId;
    @XmlElement(required = true)
    protected ActionGroup actionGroup;

    /**
     * Gets the value of the patientId property.
     * 
     * @return
     *     possible object is
     *     {@link II }
     *     
     */
    public II getPatientId() {
        return patientId;
    }

    /**
     * Sets the value of the patientId property.
     * 
     * @param value
     *     allowed object is
     *     {@link II }
     *     
     */
    public void setPatientId(II value) {
        this.patientId = value;
    }

    /**
     * Gets the value of the actionGroup property.
     * 
     * @return
     *     possible object is
     *     {@link ActionGroup }
     *     
     */
    public ActionGroup getActionGroup() {
        return actionGroup;
    }

    /**
     * Sets the value of the actionGroup property.
     * 
     * @param value
     *     allowed object is
     *     {@link ActionGroup }
     *     
     */
    public void setActionGroup(ActionGroup value) {
        this.actionGroup = value;
    }

}
