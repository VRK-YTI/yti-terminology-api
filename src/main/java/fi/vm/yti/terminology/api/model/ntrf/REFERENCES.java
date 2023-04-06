
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
 *       &lt;choice maxOccurs="unbounded" minOccurs="0"&gt;
 *         &lt;element ref="{}REF"/&gt;
 *         &lt;element ref="{}REFHEAD"/&gt;
 *       &lt;/choice&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "refOrREFHEAD"
})
@XmlRootElement(name = "REFERENCES")
public class REFERENCES {

    @XmlElements({
        @XmlElement(name = "REF", type = REF.class),
        @XmlElement(name = "REFHEAD", type = String.class)
    })
    protected List<Object> refOrREFHEAD;

    /**
     * Gets the value of the refOrREFHEAD property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the refOrREFHEAD property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getREFOrREFHEAD().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link REF }
     * {@link String }
     * 
     * 
     */
    public List<Object> getREFOrREFHEAD() {
        if (refOrREFHEAD == null) {
            refOrREFHEAD = new ArrayList<Object>();
        }
        return this.refOrREFHEAD;
    }

}
