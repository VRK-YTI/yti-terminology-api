
package fi.vm.yti.terminology.api.model.ntrf;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;attribute name="value" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
 *       &lt;attribute name="gend"&gt;
 *         &lt;simpleType&gt;
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}token"&gt;
 *             &lt;enumeration value="m"/&gt;
 *             &lt;enumeration value="n"/&gt;
 *             &lt;enumeration value="f"/&gt;
 *           &lt;/restriction&gt;
 *         &lt;/simpleType&gt;
 *       &lt;/attribute&gt;
 *       &lt;attribute name="pos" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
 *       &lt;attribute name="infl" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
 *       &lt;attribute name="synt" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "content"
})
@XmlRootElement(name = "GRAM")
public class GRAM {

    @XmlValue
    protected String content;
    @XmlAttribute(name = "value")
    @XmlSchemaType(name = "anySimpleType")
    protected String value;
    @XmlAttribute(name = "gend")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String gend;
    @XmlAttribute(name = "pos")
    @XmlSchemaType(name = "anySimpleType")
    protected String pos;
    @XmlAttribute(name = "infl")
    @XmlSchemaType(name = "anySimpleType")
    protected String infl;
    @XmlAttribute(name = "synt")
    @XmlSchemaType(name = "anySimpleType")
    protected String synt;

    /**
     * Gets the value of the content property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the value of the content property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setContent(String value) {
        this.content = value;
    }

    /**
     * Gets the value of the value property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the value of the gend property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGend() {
        return gend;
    }

    /**
     * Sets the value of the gend property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGend(String value) {
        this.gend = value;
    }

    /**
     * Gets the value of the pos property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPos() {
        return pos;
    }

    /**
     * Sets the value of the pos property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPos(String value) {
        this.pos = value;
    }

    /**
     * Gets the value of the infl property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getInfl() {
        return infl;
    }

    /**
     * Sets the value of the infl property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setInfl(String value) {
        this.infl = value;
    }

    /**
     * Gets the value of the synt property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSynt() {
        return synt;
    }

    /**
     * Sets the value of the synt property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSynt(String value) {
        this.synt = value;
    }

}
