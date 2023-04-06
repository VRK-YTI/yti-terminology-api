
package fi.vm.yti.terminology.api.model.ntrf;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
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
 *         &lt;element ref="{}TE" minOccurs="0"/&gt;
 *         &lt;element ref="{}SY" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;sequence&gt;
 *           &lt;element ref="{}DTE" maxOccurs="unbounded" minOccurs="0"/&gt;
 *           &lt;element ref="{}DTEA" maxOccurs="unbounded" minOccurs="0"/&gt;
 *           &lt;element ref="{}DTEB" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;/sequence&gt;
 *         &lt;element ref="{}EXTE" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element ref="{}DES" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element ref="{}ACRO" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element ref="{}RCON" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;choice&gt;
 *           &lt;element ref="{}DEF" maxOccurs="unbounded" minOccurs="0"/&gt;
 *           &lt;element ref="{}EXPLAN" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;/choice&gt;
 *         &lt;element ref="{}NOTE" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element ref="{}EXAMP" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element ref="{}CX" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element ref="{}STE" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element ref="{}ASTE" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element ref="{}REMK" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element ref="{}SOURC" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element ref="{}APDAT" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="value" use="required" type="{}languages" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "te",
    "sy",
    "dte",
    "dtea",
    "dteb",
    "exte",
    "des",
    "acro",
    "rcon",
    "def",
    "explan",
    "note",
    "examp",
    "cx",
    "ste",
    "aste",
    "remk",
    "sourc",
    "apdat"
})
@XmlRootElement(name = "LANG")
public class LANG {

    @XmlElement(name = "TE")
    protected TE te;
    @XmlElement(name = "SY")
    protected List<SY> sy;
    @XmlElement(name = "DTE")
    protected List<DTE> dte;
    @XmlElement(name = "DTEA")
    protected List<DTEA> dtea;
    @XmlElement(name = "DTEB")
    protected List<DTEB> dteb;
    @XmlElement(name = "EXTE")
    protected List<Termcontent> exte;
    @XmlElement(name = "DES")
    protected List<Termcontent> des;
    @XmlElement(name = "ACRO")
    protected List<Termcontent> acro;
    @XmlElement(name = "RCON")
    protected List<RCON> rcon;
    @XmlElement(name = "DEF")
    protected List<DEF> def;
    @XmlElement(name = "EXPLAN")
    protected List<EXPLAN> explan;
    @XmlElement(name = "NOTE")
    protected List<NOTE> note;
    @XmlElement(name = "EXAMP")
    protected List<EXAMP> examp;
    @XmlElement(name = "CX")
    protected List<CX> cx;
    @XmlElement(name = "STE")
    protected List<STE> ste;
    @XmlElement(name = "ASTE")
    protected List<Termcontent> aste;
    @XmlElement(name = "REMK")
    protected List<REMK> remk;
    @XmlElement(name = "SOURC")
    protected List<SOURC> sourc;
    @XmlElement(name = "APDAT")
    protected String apdat;
    @XmlAttribute(name = "value", required = true)
    protected Languages value;

    /**
     * Gets the value of the te property.
     * 
     * @return
     *     possible object is
     *     {@link TE }
     *     
     */
    public TE getTE() {
        return te;
    }

    /**
     * Sets the value of the te property.
     * 
     * @param value
     *     allowed object is
     *     {@link TE }
     *     
     */
    public void setTE(TE value) {
        this.te = value;
    }

    /**
     * Gets the value of the sy property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the sy property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSY().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SY }
     * 
     * 
     */
    public List<SY> getSY() {
        if (sy == null) {
            sy = new ArrayList<SY>();
        }
        return this.sy;
    }

    /**
     * Gets the value of the dte property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the dte property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDTE().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DTE }
     * 
     * 
     */
    public List<DTE> getDTE() {
        if (dte == null) {
            dte = new ArrayList<DTE>();
        }
        return this.dte;
    }

    /**
     * Gets the value of the dtea property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the dtea property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDTEA().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DTEA }
     * 
     * 
     */
    public List<DTEA> getDTEA() {
        if (dtea == null) {
            dtea = new ArrayList<DTEA>();
        }
        return this.dtea;
    }

    /**
     * Gets the value of the dteb property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the dteb property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDTEB().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DTEB }
     * 
     * 
     */
    public List<DTEB> getDTEB() {
        if (dteb == null) {
            dteb = new ArrayList<DTEB>();
        }
        return this.dteb;
    }

    /**
     * Gets the value of the exte property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the exte property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getEXTE().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Termcontent }
     * 
     * 
     */
    public List<Termcontent> getEXTE() {
        if (exte == null) {
            exte = new ArrayList<Termcontent>();
        }
        return this.exte;
    }

    /**
     * Gets the value of the des property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the des property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDES().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Termcontent }
     * 
     * 
     */
    public List<Termcontent> getDES() {
        if (des == null) {
            des = new ArrayList<Termcontent>();
        }
        return this.des;
    }

    /**
     * Gets the value of the acro property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the acro property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getACRO().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Termcontent }
     * 
     * 
     */
    public List<Termcontent> getACRO() {
        if (acro == null) {
            acro = new ArrayList<Termcontent>();
        }
        return this.acro;
    }

    /**
     * Gets the value of the rcon property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the rcon property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRCON().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link RCON }
     * 
     * 
     */
    public List<RCON> getRCON() {
        if (rcon == null) {
            rcon = new ArrayList<RCON>();
        }
        return this.rcon;
    }

    /**
     * Gets the value of the def property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the def property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDEF().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DEF }
     * 
     * 
     */
    public List<DEF> getDEF() {
        if (def == null) {
            def = new ArrayList<DEF>();
        }
        return this.def;
    }

    /**
     * Gets the value of the explan property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the explan property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getEXPLAN().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link EXPLAN }
     * 
     * 
     */
    public List<EXPLAN> getEXPLAN() {
        if (explan == null) {
            explan = new ArrayList<EXPLAN>();
        }
        return this.explan;
    }

    /**
     * Gets the value of the note property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the note property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getNOTE().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link NOTE }
     * 
     * 
     */
    public List<NOTE> getNOTE() {
        if (note == null) {
            note = new ArrayList<NOTE>();
        }
        return this.note;
    }

    /**
     * Gets the value of the examp property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the examp property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getEXAMP().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link EXAMP }
     * 
     * 
     */
    public List<EXAMP> getEXAMP() {
        if (examp == null) {
            examp = new ArrayList<EXAMP>();
        }
        return this.examp;
    }

    /**
     * Gets the value of the cx property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the cx property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCX().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CX }
     * 
     * 
     */
    public List<CX> getCX() {
        if (cx == null) {
            cx = new ArrayList<CX>();
        }
        return this.cx;
    }

    /**
     * Gets the value of the ste property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the ste property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSTE().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link STE }
     * 
     * 
     */
    public List<STE> getSTE() {
        if (ste == null) {
            ste = new ArrayList<STE>();
        }
        return this.ste;
    }

    /**
     * Gets the value of the aste property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the aste property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getASTE().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Termcontent }
     * 
     * 
     */
    public List<Termcontent> getASTE() {
        if (aste == null) {
            aste = new ArrayList<Termcontent>();
        }
        return this.aste;
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
     * Gets the value of the sourc property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the sourc property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSOURC().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SOURC }
     * 
     * 
     */
    public List<SOURC> getSOURC() {
        if (sourc == null) {
            sourc = new ArrayList<SOURC>();
        }
        return this.sourc;
    }

    /**
     * Gets the value of the apdat property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAPDAT() {
        return apdat;
    }

    /**
     * Sets the value of the apdat property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAPDAT(String value) {
        this.apdat = value;
    }

    /**
     * Gets the value of the value property.
     * 
     * @return
     *     possible object is
     *     {@link Languages }
     *     
     */
    public Languages getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     * @param value
     *     allowed object is
     *     {@link Languages }
     *     
     */
    public void setValue(Languages value) {
        this.value = value;
    }

}
