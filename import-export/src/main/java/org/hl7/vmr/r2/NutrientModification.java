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
import org.hl7.cdsdt.r2.IVLPQ;


/**
 * Nutrient modifications allows the post-coordination of diets in cases where such post-coordination is required. Diets can vary greatly in how they are represented in terminologies. The most common use case for Nutrient modification is to represent a nutrient that can be either stated as a quantity or a range
 * 
 * NutrientModification consists of the nutrient (e.g., Sodium) and the amount in the diet (e.g., 20-30g). Note that nutrient is required and of type CD. The 'quantity' attribute is also required and can express a range.
 * 
 * <p>Java class for NutrientModification complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="NutrientModification">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:hl7-org:vmr:r2}ExtendedVmrTypeBase">
 *       &lt;sequence>
 *         &lt;element name="nutrient" type="{urn:hl7-org:cdsdt:r2}CD"/>
 *         &lt;element name="quantity" type="{urn:hl7-org:cdsdt:r2}IVL_PQ"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "NutrientModification", propOrder = {
    "nutrient",
    "quantity"
})
public class NutrientModification
    extends ExtendedVmrTypeBase
{

    @XmlElement(required = true)
    protected CD nutrient;
    @XmlElement(required = true)
    protected IVLPQ quantity;

    /**
     * Gets the value of the nutrient property.
     * 
     * @return
     *     possible object is
     *     {@link CD }
     *     
     */
    public CD getNutrient() {
        return nutrient;
    }

    /**
     * Sets the value of the nutrient property.
     * 
     * @param value
     *     allowed object is
     *     {@link CD }
     *     
     */
    public void setNutrient(CD value) {
        this.nutrient = value;
    }

    /**
     * Gets the value of the quantity property.
     * 
     * @return
     *     possible object is
     *     {@link IVLPQ }
     *     
     */
    public IVLPQ getQuantity() {
        return quantity;
    }

    /**
     * Sets the value of the quantity property.
     * 
     * @param value
     *     allowed object is
     *     {@link IVLPQ }
     *     
     */
    public void setQuantity(IVLPQ value) {
        this.quantity = value;
    }

}
