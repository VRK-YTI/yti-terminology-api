//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2018.08.20 at 09:11:46 AM EEST 
//


package fi.vm.yti.terminology.api.model.ntrf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for HEADType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="HEADType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded" minOccurs="0">
 *         &lt;element name="LANG" type="{}LANGType"/>
 *         &lt;element name="NCON" type="{}NCONType"/>
 *         &lt;element name="ECON" type="{}ECONType"/>
 *         &lt;element name="RCON" type="{}RCONType"/>
 *         &lt;element name="CLAS" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="CHECK" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="BCON" type="{}BCONType"/>
 *         &lt;element name="CRDAT" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="SUBJ" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="LINK" type="{}LINKType"/>
 *       &lt;/choice>
 *       &lt;attribute name="level" type="{http://www.w3.org/2001/XMLSchema}byte" />
 *       &lt;attribute name="numb" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="stat" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="upda" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="height" type="{http://www.w3.org/2001/XMLSchema}short" />
 *       &lt;attribute name="heightcm" type="{http://www.w3.org/2001/XMLSchema}float" />
 *       &lt;attribute name="href" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="lock" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="resolution" type="{http://www.w3.org/2001/XMLSchema}byte" />
 *       &lt;attribute name="scale" type="{http://www.w3.org/2001/XMLSchema}float" />
 *       &lt;attribute name="width" type="{http://www.w3.org/2001/XMLSchema}short" />
 *       &lt;attribute name="widthcm" type="{http://www.w3.org/2001/XMLSchema}float" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HEADType", propOrder = {
    "content"
})
public class HEADType {

    @XmlElementRefs({
        @XmlElementRef(name = "LANG", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "LINK", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "ECON", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "CLAS", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "SUBJ", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "RCON", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "NCON", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "CHECK", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "BCON", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "CRDAT", type = JAXBElement.class, required = false)
    })
    @XmlMixed
    protected List<Serializable> content;
    @XmlAttribute(name = "level")
    protected Byte level;
    @XmlAttribute(name = "numb")
    protected String numb;
    @XmlAttribute(name = "stat")
    protected String stat;
    @XmlAttribute(name = "upda")
    protected String upda;
    @XmlAttribute(name = "height")
    protected Short height;
    @XmlAttribute(name = "heightcm")
    protected Float heightcm;
    @XmlAttribute(name = "href")
    protected String href;
    @XmlAttribute(name = "lock")
    protected String lock;
    @XmlAttribute(name = "name")
    protected String name;
    @XmlAttribute(name = "resolution")
    protected Byte resolution;
    @XmlAttribute(name = "scale")
    protected Float scale;
    @XmlAttribute(name = "width")
    protected Short width;
    @XmlAttribute(name = "widthcm")
    protected Float widthcm;

    /**
     * Gets the value of the content property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the content property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getContent().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link LANGType }{@code >}
     * {@link JAXBElement }{@code <}{@link LINKType }{@code >}
     * {@link JAXBElement }{@code <}{@link ECONType }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link String }
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link RCONType }{@code >}
     * {@link JAXBElement }{@code <}{@link NCONType }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link BCONType }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * 
     */
    public List<Serializable> getContent() {
        if (content == null) {
            content = new ArrayList<Serializable>();
        }
        return this.content;
    }

    /**
     * Gets the value of the level property.
     * 
     * @return
     *     possible object is
     *     {@link Byte }
     *     
     */
    public Byte getLevel() {
        return level;
    }

    /**
     * Sets the value of the level property.
     * 
     * @param value
     *     allowed object is
     *     {@link Byte }
     *     
     */
    public void setLevel(Byte value) {
        this.level = value;
    }

    /**
     * Gets the value of the numb property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNumb() {
        return numb;
    }

    /**
     * Sets the value of the numb property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNumb(String value) {
        this.numb = value;
    }

    /**
     * Gets the value of the stat property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStat() {
        return stat;
    }

    /**
     * Sets the value of the stat property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStat(String value) {
        this.stat = value;
    }

    /**
     * Gets the value of the upda property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUpda() {
        return upda;
    }

    /**
     * Sets the value of the upda property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUpda(String value) {
        this.upda = value;
    }

    /**
     * Gets the value of the height property.
     * 
     * @return
     *     possible object is
     *     {@link Short }
     *     
     */
    public Short getHeight() {
        return height;
    }

    /**
     * Sets the value of the height property.
     * 
     * @param value
     *     allowed object is
     *     {@link Short }
     *     
     */
    public void setHeight(Short value) {
        this.height = value;
    }

    /**
     * Gets the value of the heightcm property.
     * 
     * @return
     *     possible object is
     *     {@link Float }
     *     
     */
    public Float getHeightcm() {
        return heightcm;
    }

    /**
     * Sets the value of the heightcm property.
     * 
     * @param value
     *     allowed object is
     *     {@link Float }
     *     
     */
    public void setHeightcm(Float value) {
        this.heightcm = value;
    }

    /**
     * Gets the value of the href property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getHref() {
        return href;
    }

    /**
     * Sets the value of the href property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setHref(String value) {
        this.href = value;
    }

    /**
     * Gets the value of the lock property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLock() {
        return lock;
    }

    /**
     * Sets the value of the lock property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLock(String value) {
        this.lock = value;
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the resolution property.
     * 
     * @return
     *     possible object is
     *     {@link Byte }
     *     
     */
    public Byte getResolution() {
        return resolution;
    }

    /**
     * Sets the value of the resolution property.
     * 
     * @param value
     *     allowed object is
     *     {@link Byte }
     *     
     */
    public void setResolution(Byte value) {
        this.resolution = value;
    }

    /**
     * Gets the value of the scale property.
     * 
     * @return
     *     possible object is
     *     {@link Float }
     *     
     */
    public Float getScale() {
        return scale;
    }

    /**
     * Sets the value of the scale property.
     * 
     * @param value
     *     allowed object is
     *     {@link Float }
     *     
     */
    public void setScale(Float value) {
        this.scale = value;
    }

    /**
     * Gets the value of the width property.
     * 
     * @return
     *     possible object is
     *     {@link Short }
     *     
     */
    public Short getWidth() {
        return width;
    }

    /**
     * Sets the value of the width property.
     * 
     * @param value
     *     allowed object is
     *     {@link Short }
     *     
     */
    public void setWidth(Short value) {
        this.width = value;
    }

    /**
     * Gets the value of the widthcm property.
     * 
     * @return
     *     possible object is
     *     {@link Float }
     *     
     */
    public Float getWidthcm() {
        return widthcm;
    }

    /**
     * Sets the value of the widthcm property.
     * 
     * @param value
     *     allowed object is
     *     {@link Float }
     *     
     */
    public void setWidthcm(Float value) {
        this.widthcm = value;
    }

}