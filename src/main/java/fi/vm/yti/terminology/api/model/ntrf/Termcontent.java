
package fi.vm.yti.terminology.api.model.ntrf;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for termcontent complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="termcontent"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element ref="{}EQUI" minOccurs="0"/&gt;
 *         &lt;element ref="{}TERM"/&gt;
 *         &lt;element ref="{}HOGR" minOccurs="0"/&gt;
 *         &lt;element ref="{}GEOG" minOccurs="0"/&gt;
 *         &lt;element ref="{}TYPT" minOccurs="0"/&gt;
 *         &lt;element ref="{}PHR" minOccurs="0"/&gt;
 *         &lt;element ref="{}PRON" minOccurs="0"/&gt;
 *         &lt;element ref="{}ETYM" minOccurs="0"/&gt;
 *         &lt;element ref="{}SUBJ" minOccurs="0"/&gt;
 *         &lt;element ref="{}SCOPE" minOccurs="0"/&gt;
 *         &lt;element ref="{}SOURF" minOccurs="0"/&gt;
 *         &lt;element ref="{}STAT" minOccurs="0"/&gt;
 *         &lt;element ref="{}ADD" minOccurs="0"/&gt;
 *         &lt;element ref="{}REMK" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "termcontent", propOrder = {
    "equi",
    "term",
    "hogr",
    "geog",
    "typt",
    "phr",
    "pron",
    "etym",
    "subj",
    "scope",
    "sourf",
    "statelem",
    "add",
    "remk"
})
@XmlSeeAlso({
    STE.class,
    DTEB.class,
    DTEA.class,
    DTE.class,
    SY.class,
    TE.class
})
public class Termcontent {

    @XmlElement(name = "EQUI")
    protected EQUI equi;
    @XmlElement(name = "TERM", required = true)
    protected TERM term;
    @XmlElement(name = "HOGR")
    protected String hogr;
    @XmlElement(name = "GEOG")
    protected String geog;
    @XmlElement(name = "TYPT")
    protected String typt;
    @XmlElement(name = "PHR")
    protected String phr;
    @XmlElement(name = "PRON")
    protected String pron;
    @XmlElement(name = "ETYM")
    protected String etym;
    @XmlElement(name = "SUBJ")
    protected SUBJ subj;
    @XmlElement(name = "SCOPE")
    protected SCOPE scope;
    @XmlElement(name = "SOURF")
    protected SOURF sourf;
    @XmlElement(name = "STAT")
    protected String statelem;
    @XmlElement(name = "ADD")
    protected String add;
    @XmlElement(name = "REMK")
    protected REMK remk;

    /**
     * Gets the value of the equi property.
     * 
     * @return
     *     possible object is
     *     {@link EQUI }
     *     
     */
    public EQUI getEQUI() {
        return equi;
    }

    /**
     * Sets the value of the equi property.
     * 
     * @param value
     *     allowed object is
     *     {@link EQUI }
     *     
     */
    public void setEQUI(EQUI value) {
        this.equi = value;
    }

    /**
     * Gets the value of the term property.
     * 
     * @return
     *     possible object is
     *     {@link TERM }
     *     
     */
    public TERM getTERM() {
        return term;
    }

    /**
     * Sets the value of the term property.
     * 
     * @param value
     *     allowed object is
     *     {@link TERM }
     *     
     */
    public void setTERM(TERM value) {
        this.term = value;
    }

    /**
     * Gets the value of the hogr property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getHOGR() {
        return hogr;
    }

    /**
     * Sets the value of the hogr property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setHOGR(String value) {
        this.hogr = value;
    }

    /**
     * Gets the value of the geog property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGEOG() {
        return geog;
    }

    /**
     * Sets the value of the geog property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGEOG(String value) {
        this.geog = value;
    }

    /**
     * Gets the value of the typt property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTYPT() {
        return typt;
    }

    /**
     * Sets the value of the typt property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTYPT(String value) {
        this.typt = value;
    }

    /**
     * Gets the value of the phr property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPHR() {
        return phr;
    }

    /**
     * Sets the value of the phr property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPHR(String value) {
        this.phr = value;
    }

    /**
     * Gets the value of the pron property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPRON() {
        return pron;
    }

    /**
     * Sets the value of the pron property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPRON(String value) {
        this.pron = value;
    }

    /**
     * Gets the value of the etym property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getETYM() {
        return etym;
    }

    /**
     * Sets the value of the etym property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setETYM(String value) {
        this.etym = value;
    }

    /**
     * Gets the value of the subj property.
     * 
     * @return
     *     possible object is
     *     {@link SUBJ }
     *     
     */
    public SUBJ getSUBJ() {
        return subj;
    }

    /**
     * Sets the value of the subj property.
     * 
     * @param value
     *     allowed object is
     *     {@link SUBJ }
     *     
     */
    public void setSUBJ(SUBJ value) {
        this.subj = value;
    }

    /**
     * Gets the value of the scope property.
     * 
     * @return
     *     possible object is
     *     {@link SCOPE }
     *     
     */
    public SCOPE getSCOPE() {
        return scope;
    }

    /**
     * Sets the value of the scope property.
     * 
     * @param value
     *     allowed object is
     *     {@link SCOPE }
     *     
     */
    public void setSCOPE(SCOPE value) {
        this.scope = value;
    }

    /**
     * Gets the value of the sourf property.
     * 
     * @return
     *     possible object is
     *     {@link SOURF }
     *     
     */
    public SOURF getSOURF() {
        return sourf;
    }

    /**
     * Sets the value of the sourf property.
     * 
     * @param value
     *     allowed object is
     *     {@link SOURF }
     *     
     */
    public void setSOURF(SOURF value) {
        this.sourf = value;
    }

    /**
     * Gets the value of the statelem property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStatelem() {
        return statelem;
    }

    /**
     * Sets the value of the statelem property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStatelem(String value) {
        this.statelem = value;
    }

    /**
     * Gets the value of the add property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getADD() {
        return add;
    }

    /**
     * Sets the value of the add property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setADD(String value) {
        this.add = value;
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

}
