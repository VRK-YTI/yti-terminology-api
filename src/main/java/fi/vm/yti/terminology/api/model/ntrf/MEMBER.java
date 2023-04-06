
package fi.vm.yti.terminology.api.model.ntrf;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element ref="{}NAME"/&gt;
 *         &lt;element ref="{}REMK" minOccurs="0"/&gt;
 *         &lt;element ref="{}TITLE" minOccurs="0"/&gt;
 *         &lt;element ref="{}FUNC" minOccurs="0"/&gt;
 *         &lt;element ref="{}ORG" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "name",
    "remk",
    "title",
    "func",
    "org"
})
@XmlRootElement(name = "MEMBER")
public class MEMBER {

    @XmlElement(name = "NAME", required = true)
    protected String name;
    @XmlElement(name = "REMK")
    protected REMK remk;
    @XmlElement(name = "TITLE")
    protected String title;
    @XmlElement(name = "FUNC")
    protected String func;
    @XmlElement(name = "ORG")
    protected String org;

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNAME() {
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
    public void setNAME(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the remk property.
     * 
     * @return
     *     possible object is
     *     {@link REMK }
     *     
     */
    public REMK getREMK() {
        return remk;
    }

    /**
     * Sets the value of the remk property.
     * 
     * @param value
     *     allowed object is
     *     {@link REMK }
     *     
     */
    public void setREMK(REMK value) {
        this.remk = value;
    }

    /**
     * Gets the value of the title property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTITLE() {
        return title;
    }

    /**
     * Sets the value of the title property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTITLE(String value) {
        this.title = value;
    }

    /**
     * Gets the value of the func property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFUNC() {
        return func;
    }

    /**
     * Sets the value of the func property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFUNC(String value) {
        this.func = value;
    }

    /**
     * Gets the value of the org property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getORG() {
        return org;
    }

    /**
     * Sets the value of the org property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setORG(String value) {
        this.org = value;
    }

}
