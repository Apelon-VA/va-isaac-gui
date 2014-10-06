//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.09.30 at 06:15:10 PM PDT 
//


package org.hl7.knowledgeartifact.r1;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * The BinaryExpression type defines the abstract base type for all expressions that take two arguments.
 * 
 * <p>Java class for BinaryExpression complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="BinaryExpression">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:hl7-org:knowledgeartifact:r1}Expression">
 *       &lt;sequence>
 *         &lt;element name="operand" type="{urn:hl7-org:knowledgeartifact:r1}Expression" maxOccurs="2" minOccurs="2"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BinaryExpression", propOrder = {
    "operand"
})
@XmlSeeAlso({
    GreaterOrEqual.class,
    ProperIncludedIn.class,
    NotEqual.class,
    Equal.class,
    Greater.class,
    Add.class,
    Less.class,
    Subtract.class,
    Contains.class,
    ProperIncludes.class,
    Multiply.class,
    Includes.class,
    Divide.class,
    IfNull.class,
    Begins.class,
    IncludedIn.class,
    Before.class,
    After.class,
    Overlaps.class,
    Modulo.class,
    OverlapsAfter.class,
    LessOrEqual.class,
    Ends.class,
    Difference.class,
    Power.class,
    TruncatedDivide.class,
    Meets.class,
    In.class,
    Log.class,
    OverlapsBefore.class
})
public class BinaryExpression
    extends Expression
{

    @XmlElement(required = true)
    protected List<Expression> operand;

    /**
     * Gets the value of the operand property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the operand property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getOperand().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Expression }
     * 
     * 
     */
    public List<Expression> getOperand() {
        if (operand == null) {
            operand = new ArrayList<Expression>();
        }
        return this.operand;
    }

}
