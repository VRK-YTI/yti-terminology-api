
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
 *         &lt;element ref="{}REFNAME"/&gt;
 *         &lt;element ref="{}REFTEXT"/&gt;
 *         &lt;element ref="{}REFLINK"/&gt;
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
    "refname",
    "reftext",
    "reflink"
})
@XmlRootElement(name = "REF")
public class REF {

    @XmlElement(name = "REFNAME", required = true)
    protected String refname;
    @XmlElement(name = "REFTEXT", required = true)
    protected REFTEXT reftext;
    @XmlElement(name = "REFLINK", required = true)
    protected String reflink;

    /**
     * Gets the value of the refname property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getREFNAME() {
        return refname;
    }

    /**
     * Sets the value of the refname property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setREFNAME(String value) {
        this.refname = value;
    }

    /**
     * Gets the value of the reftext property.
     * 
     * @return
     *     possible object is
     *     {@link REFTEXT }
     *     
     */
    public REFTEXT getREFTEXT() {
        return reftext;
    }

    /**
     * Sets the value of the reftext property.
     * 
     * @param value
     *     allowed object is
     *     {@link REFTEXT }
     *     
     */
    public void setREFTEXT(REFTEXT value) {
        this.reftext = value;
    }

    /**
     * Gets the value of the reflink property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getREFLINK() {
        return reflink;
    }

    /**
     * Sets the value of the reflink property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setREFLINK(String value) {
        this.reflink = value;
    }

}
