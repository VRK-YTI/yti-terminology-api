
package fi.vm.yti.terminology.api.model.ntrf;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
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
 *         &lt;element ref="{}HEADER" minOccurs="0"/&gt;
 *         &lt;sequence maxOccurs="unbounded" minOccurs="0"&gt;
 *           &lt;element ref="{}RECORD" minOccurs="0"/&gt;
 *           &lt;element ref="{}HEAD" minOccurs="0"/&gt;
 *           &lt;element ref="{}DIAG" minOccurs="0"/&gt;
 *           &lt;element ref="{}PICT" minOccurs="0"/&gt;
 *           &lt;element ref="{}P" minOccurs="0"/&gt;
 *           &lt;element ref="{}COMM" minOccurs="0"/&gt;
 *           &lt;element ref="{}REMK" minOccurs="0"/&gt;
 *         &lt;/sequence&gt;
 *         &lt;element ref="{}REFERENCES" minOccurs="0"/&gt;
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
    "header",
    "recordAndHEADAndDIAG",
    "references"
})
@XmlRootElement(name = "VOCABULARY")
public class VOCABULARY {

    @XmlElement(name = "HEADER")
    protected HEADER header;
    @XmlElements({
        @XmlElement(name = "RECORD", type = RECORD.class),
        @XmlElement(name = "HEAD", type = HEAD.class),
        @XmlElement(name = "DIAG", type = DIAG.class),
        @XmlElement(name = "PICT", type = PICT.class),
        @XmlElement(name = "P", type = P.class),
        @XmlElement(name = "COMM", type = COMM.class),
        @XmlElement(name = "REMK", type = REMK.class)
    })
    protected List<Object> recordAndHEADAndDIAG;
    @XmlElement(name = "REFERENCES")
    protected REFERENCES references;

    /**
     * Gets the value of the header property.
     * 
     * @return
     *     possible object is
     *     {@link HEADER }
     *     
     */
    public HEADER getHEADER() {
        return header;
    }

    /**
     * Sets the value of the header property.
     * 
     * @param value
     *     allowed object is
     *     {@link HEADER }
     *     
     */
    public void setHEADER(HEADER value) {
        this.header = value;
    }

    /**
     * Gets the value of the recordAndHEADAndDIAG property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the recordAndHEADAndDIAG property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRECORDAndHEADAndDIAG().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link COMM }
     * {@link DIAG }
     * {@link HEAD }
     * {@link P }
     * {@link PICT }
     * {@link RECORD }
     * {@link REMK }
     * 
     * 
     */
    public List<Object> getRECORDAndHEADAndDIAG() {
        if (recordAndHEADAndDIAG == null) {
            recordAndHEADAndDIAG = new ArrayList<Object>();
        }
        return this.recordAndHEADAndDIAG;
    }

    /**
     * Gets the value of the references property.
     * 
     * @return
     *     possible object is
     *     {@link REFERENCES }
     *     
     */
    public REFERENCES getREFERENCES() {
        return references;
    }

    /**
     * Sets the value of the references property.
     * 
     * @param value
     *     allowed object is
     *     {@link REFERENCES }
     *     
     */
    public void setREFERENCES(REFERENCES value) {
        this.references = value;
    }

}
