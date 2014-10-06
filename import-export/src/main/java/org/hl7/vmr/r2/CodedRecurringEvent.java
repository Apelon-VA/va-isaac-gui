//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.09.30 at 06:15:10 PM PDT 
//


package org.hl7.vmr.r2;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import org.hl7.cdsdt.r2.CD;


/**
 * Specification of a repetitive schedule element as a code.
 * 
 * <p>Java class for CodedRecurringEvent complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="CodedRecurringEvent">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:hl7-org:vmr:r2}CycleEventTiming">
 *       &lt;sequence>
 *         &lt;element name="repeatCode" type="{urn:hl7-org:cdsdt:r2}CD"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CodedRecurringEvent", propOrder = {
    "repeatCode"
})
public class CodedRecurringEvent
    extends CycleEventTiming
{

    @XmlElement(required = true)
    protected CD repeatCode;

    /**
     * Gets the value of the repeatCode property.
     * 
     * @return
     *     possible object is
     *     {@link CD }
     *     
     */
    public CD getRepeatCode() {
        return repeatCode;
    }

    /**
     * Sets the value of the repeatCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link CD }
     *     
     */
    public void setRepeatCode(CD value) {
        this.repeatCode = value;
    }

}
