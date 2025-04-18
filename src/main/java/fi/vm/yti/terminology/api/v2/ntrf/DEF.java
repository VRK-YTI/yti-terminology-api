//
// This file was generated by the Eclipse Implementation of JAXB, v4.0.0 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
//


package fi.vm.yti.terminology.api.v2.ntrf;

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
 * <pre>{@code
 * <complexType>
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <choice maxOccurs="unbounded" minOccurs="0">
 *         <element ref="{}BCON"/>
 *         <element ref="{}NCON"/>
 *         <element ref="{}SCON"/>
 *         <element ref="{}RCON"/>
 *         <element ref="{}ECON"/>
 *         <element ref="{}RCONEXT"/>
 *         <element ref="{}BCONEXT"/>
 *         <element ref="{}NCONEXT"/>
 *         <element ref="{}SOURF"/>
 *         <element ref="{}STAT"/>
 *         <element ref="{}ADD"/>
 *         <element ref="{}REMK"/>
 *         <element ref="{}styling"/>
 *         <element ref="{}LINK"/>
 *         <element ref="{}HOGR"/>
 *         <element ref="{}FORMULA"/>
 *       </choice>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "content"
})
@XmlRootElement(name = "DEF")
public class DEF {

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
        @XmlElementRef(name = "FORMULA", type = JAXBElement.class, required = false)
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
     * This is why there is not a {@code set} method for the content property.
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
     * @return
     *     The value of the content property.
     */
    public List<Object> getContent() {
        if (content == null) {
            content = new ArrayList<>();
        }
        return this.content;
    }

}
