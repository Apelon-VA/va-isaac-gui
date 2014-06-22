//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.08.20 at 02:56:45 PM CDT 
//


package gov.va.legoEdit.model.schemaModel;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for measurementConstant.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="measurementConstant">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="DOB"/>
 *     &lt;enumeration value="NOW"/>
 *     &lt;enumeration value="start active service"/>
 *     &lt;enumeration value="end active service"/>
 *     &lt;enumeration value="PNCS Value Field"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "measurementConstant")
@XmlEnum
public enum MeasurementConstant {

    DOB("DOB"),
    NOW("NOW"),
    @XmlEnumValue("start active service")
    START_ACTIVE_SERVICE("start active service"),
    @XmlEnumValue("end active service")
    END_ACTIVE_SERVICE("end active service"),
    @XmlEnumValue("PNCS Value Field")
    PNCS_VALUE_FIELD("PNCS Value Field");
    private final String value;

    MeasurementConstant(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static MeasurementConstant fromValue(String v) {
        for (MeasurementConstant c: MeasurementConstant.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}