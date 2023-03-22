
package fi.vm.yti.terminology.api.model.ntrf;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
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
 *       &lt;sequence&gt;
 *         &lt;element ref="{}TITLES"/&gt;
 *         &lt;element ref="{}SERIAL" minOccurs="0"/&gt;
 *         &lt;element ref="{}YEAR" minOccurs="0"/&gt;
 *         &lt;element ref="{}SOURCES" minOccurs="0"/&gt;
 *         &lt;element ref="{}WORKGROUP" minOccurs="0"/&gt;
 *         &lt;element ref="{}FINANCIERS" minOccurs="0"/&gt;
 *         &lt;element ref="{}COMMENTS" minOccurs="0"/&gt;
 *         &lt;element ref="{}FOREWORD" minOccurs="0"/&gt;
 *         &lt;element ref="{}INDEX" minOccurs="0"/&gt;
 *         &lt;element ref="{}REMK" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="trackChanges"&gt;
 *         &lt;simpleType&gt;
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}token"&gt;
 *             &lt;enumeration value="true"/&gt;
 *             &lt;enumeration value="false"/&gt;
 *           &lt;/restriction&gt;
 *         &lt;/simpleType&gt;
 *       &lt;/attribute&gt;
 *       &lt;attribute name="exportHierarchy"&gt;
 *         &lt;simpleType&gt;
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}token"&gt;
 *             &lt;enumeration value="true"/&gt;
 *             &lt;enumeration value="false"/&gt;
 *           &lt;/restriction&gt;
 *         &lt;/simpleType&gt;
 *       &lt;/attribute&gt;
 *       &lt;attribute name="motExportNumbTranslation"&gt;
 *         &lt;simpleType&gt;
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}token"&gt;
 *             &lt;enumeration value="true"/&gt;
 *             &lt;enumeration value="false"/&gt;
 *           &lt;/restriction&gt;
 *         &lt;/simpleType&gt;
 *       &lt;/attribute&gt;
 *       &lt;attribute name="motExportPict"&gt;
 *         &lt;simpleType&gt;
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}token"&gt;
 *             &lt;enumeration value="true"/&gt;
 *             &lt;enumeration value="false"/&gt;
 *           &lt;/restriction&gt;
 *         &lt;/simpleType&gt;
 *       &lt;/attribute&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "titles",
    "serial",
    "year",
    "sources",
    "workgroup",
    "financiers",
    "comments",
    "foreword",
    "index",
    "remk"
})
@XmlRootElement(name = "HEADER")
public class HEADER {

    @XmlElement(name = "TITLES", required = true)
    protected TITLES titles;
    @XmlElement(name = "SERIAL")
    protected String serial;
    @XmlElement(name = "YEAR")
    protected String year;
    @XmlElement(name = "SOURCES")
    protected SOURCES sources;
    @XmlElement(name = "WORKGROUP")
    protected WORKGROUP workgroup;
    @XmlElement(name = "FINANCIERS")
    protected FINANCIERS financiers;
    @XmlElement(name = "COMMENTS")
    protected COMMENTS comments;
    @XmlElement(name = "FOREWORD")
    protected FOREWORD foreword;
    @XmlElement(name = "INDEX")
    protected INDEX index;
    @XmlElement(name = "REMK")
    protected List<REMK> remk;
    @XmlAttribute(name = "trackChanges")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String trackChanges;
    @XmlAttribute(name = "exportHierarchy")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String exportHierarchy;
    @XmlAttribute(name = "motExportNumbTranslation")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String motExportNumbTranslation;
    @XmlAttribute(name = "motExportPict")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String motExportPict;

    /**
     * Gets the value of the titles property.
     * 
     * @return
     *     possible object is
     *     {@link TITLES }
     *     
     */
    public TITLES getTITLES() {
        return titles;
    }

    /**
     * Sets the value of the titles property.
     * 
     * @param value
     *     allowed object is
     *     {@link TITLES }
     *     
     */
    public void setTITLES(TITLES value) {
        this.titles = value;
    }

    /**
     * Gets the value of the serial property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSERIAL() {
        return serial;
    }

    /**
     * Sets the value of the serial property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSERIAL(String value) {
        this.serial = value;
    }

    /**
     * Gets the value of the year property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getYEAR() {
        return year;
    }

    /**
     * Sets the value of the year property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setYEAR(String value) {
        this.year = value;
    }

    /**
     * Gets the value of the sources property.
     * 
     * @return
     *     possible object is
     *     {@link SOURCES }
     *     
     */
    public SOURCES getSOURCES() {
        return sources;
    }

    /**
     * Sets the value of the sources property.
     * 
     * @param value
     *     allowed object is
     *     {@link SOURCES }
     *     
     */
    public void setSOURCES(SOURCES value) {
        this.sources = value;
    }

    /**
     * Gets the value of the workgroup property.
     * 
     * @return
     *     possible object is
     *     {@link WORKGROUP }
     *     
     */
    public WORKGROUP getWORKGROUP() {
        return workgroup;
    }

    /**
     * Sets the value of the workgroup property.
     * 
     * @param value
     *     allowed object is
     *     {@link WORKGROUP }
     *     
     */
    public void setWORKGROUP(WORKGROUP value) {
        this.workgroup = value;
    }

    /**
     * Gets the value of the financiers property.
     * 
     * @return
     *     possible object is
     *     {@link FINANCIERS }
     *     
     */
    public FINANCIERS getFINANCIERS() {
        return financiers;
    }

    /**
     * Sets the value of the financiers property.
     * 
     * @param value
     *     allowed object is
     *     {@link FINANCIERS }
     *     
     */
    public void setFINANCIERS(FINANCIERS value) {
        this.financiers = value;
    }

    /**
     * Gets the value of the comments property.
     * 
     * @return
     *     possible object is
     *     {@link COMMENTS }
     *     
     */
    public COMMENTS getCOMMENTS() {
        return comments;
    }

    /**
     * Sets the value of the comments property.
     * 
     * @param value
     *     allowed object is
     *     {@link COMMENTS }
     *     
     */
    public void setCOMMENTS(COMMENTS value) {
        this.comments = value;
    }

    /**
     * Gets the value of the foreword property.
     * 
     * @return
     *     possible object is
     *     {@link FOREWORD }
     *     
     */
    public FOREWORD getFOREWORD() {
        return foreword;
    }

    /**
     * Sets the value of the foreword property.
     * 
     * @param value
     *     allowed object is
     *     {@link FOREWORD }
     *     
     */
    public void setFOREWORD(FOREWORD value) {
        this.foreword = value;
    }

    /**
     * Gets the value of the index property.
     * 
     * @return
     *     possible object is
     *     {@link INDEX }
     *     
     */
    public INDEX getINDEX() {
        return index;
    }

    /**
     * Sets the value of the index property.
     * 
     * @param value
     *     allowed object is
     *     {@link INDEX }
     *     
     */
    public void setINDEX(INDEX value) {
        this.index = value;
    }

    /**
     * Gets the value of the remk property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the remk property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getREMK().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link REMK }
     * 
     * 
     */
    public List<REMK> getREMK() {
        if (remk == null) {
            remk = new ArrayList<REMK>();
        }
        return this.remk;
    }

    /**
     * Gets the value of the trackChanges property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTrackChanges() {
        return trackChanges;
    }

    /**
     * Sets the value of the trackChanges property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTrackChanges(String value) {
        this.trackChanges = value;
    }

    /**
     * Gets the value of the exportHierarchy property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getExportHierarchy() {
        return exportHierarchy;
    }

    /**
     * Sets the value of the exportHierarchy property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setExportHierarchy(String value) {
        this.exportHierarchy = value;
    }

    /**
     * Gets the value of the motExportNumbTranslation property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMotExportNumbTranslation() {
        return motExportNumbTranslation;
    }

    /**
     * Sets the value of the motExportNumbTranslation property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMotExportNumbTranslation(String value) {
        this.motExportNumbTranslation = value;
    }

    /**
     * Gets the value of the motExportPict property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMotExportPict() {
        return motExportPict;
    }

    /**
     * Sets the value of the motExportPict property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMotExportPict(String value) {
        this.motExportPict = value;
    }

}
