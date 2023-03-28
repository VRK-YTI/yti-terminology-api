
package fi.vm.yti.terminology.api.model.ntrf;

import java.util.ArrayList;
import java.util.List;
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
 *         &lt;element ref="{}HLANG" maxOccurs="unbounded"/&gt;
 *         &lt;element ref="{}LINK" minOccurs="0"/&gt;
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
    "hlang",
    "link"
})
@XmlRootElement(name = "FOREWORD")
public class FOREWORD {

    @XmlElement(name = "HLANG", required = true)
    protected List<HLANG> hlang;
    @XmlElement(name = "LINK")
    protected LINK link;

    /**
     * Gets the value of the hlang property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the hlang property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getHLANG().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link HLANG }
     * 
     * 
     */
    public List<HLANG> getHLANG() {
        if (hlang == null) {
            hlang = new ArrayList<HLANG>();
        }
        return this.hlang;
    }

    /**
     * Gets the value of the link property.
     * 
     * @return
     *     possible object is
     *     {@link LINK }
     *     
     */
    public LINK getLINK() {
        return link;
    }

    /**
     * Sets the value of the link property.
     * 
     * @param value
     *     allowed object is
     *     {@link LINK }
     *     
     */
    public void setLINK(LINK value) {
        this.link = value;
    }

}
