//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2018.08.20 at 09:11:46 AM EEST 
//


package fi.vm.yti.terminology.api.model.ntrf;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for LANGType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="LANGType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded" minOccurs="0">
 *         &lt;element name="TE" type="{}TEType"/>
 *         &lt;element name="DEF" type="{}DEFType"/>
 *         &lt;element name="NOTE" type="{}NOTEType"/>
 *         &lt;element name="SY" type="{}SYType"/>
 *         &lt;element name="STE" type="{}STEType"/>
 *         &lt;element name="DTEB" type="{}DTEBType"/>
 *         &lt;element name="ASTE" type="{}ASTEType"/>
 *         &lt;element name="DTEA" type="{}DTEAType"/>
 *       &lt;/choice>
 *       &lt;attribute name="value" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LANGType", propOrder = {
    "teOrDEFOrNOTE"
})
public class LANGType {

    @XmlElements({
        @XmlElement(name = "TE", type = TEType.class),
        @XmlElement(name = "DEF", type = DEFType.class),
        @XmlElement(name = "NOTE", type = NOTEType.class),
        @XmlElement(name = "SY", type = SYType.class),
        @XmlElement(name = "STE", type = STEType.class),
        @XmlElement(name = "DTEB", type = DTEBType.class),
        @XmlElement(name = "ASTE", type = ASTEType.class),
        @XmlElement(name = "DTEA", type = DTEAType.class)
    })
    protected List<Object> teOrDEFOrNOTE;
    @XmlAttribute(name = "value")
    protected String valueAttribute;

    /**
     * Gets the value of the teOrDEFOrNOTE property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the teOrDEFOrNOTE property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTEOrDEFOrNOTE().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TEType }
     * {@link DEFType }
     * {@link NOTEType }
     * {@link SYType }
     * {@link STEType }
     * {@link DTEBType }
     * {@link ASTEType }
     * {@link DTEAType }
     * 
     * 
     */
    public List<Object> getTEOrDEFOrNOTE() {
        if (teOrDEFOrNOTE == null) {
            teOrDEFOrNOTE = new ArrayList<Object>();
        }
        return this.teOrDEFOrNOTE;
    }

    /**
     * Gets the value of the valueAttribute property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getValueAttribute() {
        return valueAttribute;
    }

    /**
     * Sets the value of the valueAttribute property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setValueAttribute(String value) {
        this.valueAttribute = value;
    }

}