
package fi.vm.yti.terminology.api.model.ntrf;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlElementRefs;
import jakarta.xml.bind.annotation.XmlMixed;
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
 *         &lt;element ref="{}BCON"/&gt;
 *         &lt;element ref="{}NCON"/&gt;
 *         &lt;element ref="{}SCON"/&gt;
 *         &lt;element ref="{}RCON"/&gt;
 *         &lt;element ref="{}ECON"/&gt;
 *         &lt;element ref="{}RCONEXT"/&gt;
 *         &lt;element ref="{}BCONEXT"/&gt;
 *         &lt;element ref="{}NCONEXT"/&gt;
 *         &lt;element ref="{}SOURF"/&gt;
 *         &lt;element ref="{}STAT"/&gt;
 *         &lt;element ref="{}ADD"/&gt;
 *         &lt;element ref="{}REMK"/&gt;
 *         &lt;element ref="{}styling"/&gt;
 *         &lt;element ref="{}LINK"/&gt;
 *         &lt;element ref="{}HOGR"/&gt;
 *         &lt;element ref="{}EXNO"/&gt;
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
    "content"
})
@XmlRootElement(name = "EXAMP")
public class EXAMP {

    @XmlElementRefs({
        @XmlElementRef(name = "BCON", type = BCON.class, required = false),
        @XmlElementRef(name = "NCON", type = NCON.class, required = false),
        @XmlElementRef(name = "SCON", type = SCON.class, required = false),
        @XmlElementRef(name = "RCON", type = RCON.class, required = false),
        @XmlElementRef(name = "ECON", type = ECON.class, required = false),
        @XmlElementRef(name = "RCONEXT", type = RCONEXT.class, required = false),
        @XmlElementRef(name = "BCONEXT", type = BCONEXT.class, required = false),
        @XmlElementRef(name = "NCONEXT", type = NCONEXT.class, required = false),
        @XmlElementRef(name = "SOURF", type = SOURF.class, required = false),
        @XmlElementRef(name = "STAT", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "ADD", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "REMK", type = REMK.class, required = false),
        @XmlElementRef(name = "styling", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "LINK", type = LINK.class, required = false),
        @XmlElementRef(name = "HOGR", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "EXNO", type = JAXBElement.class, required = false)
    })
    @XmlMixed
    protected List<Object> content;

    /**
     * Gets the value of the content property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
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
     * {@link BCON }
     * {@link BCONEXT }
     * {@link ECON }
     * {@link LINK }
     * {@link NCON }
     * {@link NCONEXT }
     * {@link RCON }
     * {@link RCONEXT }
     * {@link REMK }
     * {@link SCON }
     * {@link SOURF }
     * {@link JAXBElement }{@code <}{@link BR }{@code >}
     * {@link JAXBElement }{@code <}{@link Object }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link String }
     * 
     * 
     */
    public List<Object> getContent() {
        if (content == null) {
            content = new ArrayList<Object>();
        }
        return this.content;
    }

}
