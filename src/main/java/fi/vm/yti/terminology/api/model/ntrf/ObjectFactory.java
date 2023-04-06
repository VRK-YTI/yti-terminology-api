
package fi.vm.yti.terminology.api.model.ntrf;

import javax.xml.namespace.QName;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the fi.vm.yti.terminology.api.model.ntrf package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Styling_QNAME = new QName("", "styling");
    private final static QName _SERIAL_QNAME = new QName("", "SERIAL");
    private final static QName _YEAR_QNAME = new QName("", "YEAR");
    private final static QName _HOGR_QNAME = new QName("", "HOGR");
    private final static QName _ADD_QNAME = new QName("", "ADD");
    private final static QName _NAME_QNAME = new QName("", "NAME");
    private final static QName _TITLE_QNAME = new QName("", "TITLE");
    private final static QName _FUNC_QNAME = new QName("", "FUNC");
    private final static QName _ORG_QNAME = new QName("", "ORG");
    private final static QName _NUMB_QNAME = new QName("", "NUMB");
    private final static QName _POSI_QNAME = new QName("", "POSI");
    private final static QName _SUB_QNAME = new QName("", "SUB");
    private final static QName _SUP_QNAME = new QName("", "SUP");
    private final static QName _POS_QNAME = new QName("", "POS");
    private final static QName _GEND_QNAME = new QName("", "GEND");
    private final static QName _INFL_QNAME = new QName("", "INFL");
    private final static QName _GEOG_QNAME = new QName("", "GEOG");
    private final static QName _TYPT_QNAME = new QName("", "TYPT");
    private final static QName _PHR_QNAME = new QName("", "PHR");
    private final static QName _PRON_QNAME = new QName("", "PRON");
    private final static QName _ETYM_QNAME = new QName("", "ETYM");
    private final static QName _STAT_QNAME = new QName("", "STAT");
    private final static QName _EXTE_QNAME = new QName("", "EXTE");
    private final static QName _DES_QNAME = new QName("", "DES");
    private final static QName _ACRO_QNAME = new QName("", "ACRO");
    private final static QName _FORMULA_QNAME = new QName("", "FORMULA");
    private final static QName _EXNO_QNAME = new QName("", "EXNO");
    private final static QName _ASTE_QNAME = new QName("", "ASTE");
    private final static QName _APDAT_QNAME = new QName("", "APDAT");
    private final static QName _TIT_QNAME = new QName("", "TIT");
    private final static QName _INST_QNAME = new QName("", "INST");
    private final static QName _CLASPEC_QNAME = new QName("", "CLASPEC");
    private final static QName _CLASYS_QNAME = new QName("", "CLASYS");
    private final static QName _CRDAT_QNAME = new QName("", "CRDAT");
    private final static QName _CRBY_QNAME = new QName("", "CRBY");
    private final static QName _CREA_QNAME = new QName("", "CREA");
    private final static QName _UPDAT_QNAME = new QName("", "UPDAT");
    private final static QName _UPBY_QNAME = new QName("", "UPBY");
    private final static QName _UPDA_QNAME = new QName("", "UPDA");
    private final static QName _CHDAT_QNAME = new QName("", "CHDAT");
    private final static QName _CHBY_QNAME = new QName("", "CHBY");
    private final static QName _CHECK_QNAME = new QName("", "CHECK");
    private final static QName _APBY_QNAME = new QName("", "APBY");
    private final static QName _APPR_QNAME = new QName("", "APPR");
    private final static QName _BR_QNAME = new QName("", "BR");
    private final static QName _REFNAME_QNAME = new QName("", "REFNAME");
    private final static QName _B_QNAME = new QName("", "B");
    private final static QName _I_QNAME = new QName("", "I");
    private final static QName _REFLINK_QNAME = new QName("", "REFLINK");
    private final static QName _REFHEAD_QNAME = new QName("", "REFHEAD");
    private final static QName _SYNT_QNAME = new QName("", "SYNT");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: fi.vm.yti.terminology.api.model.ntrf
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link VOCABULARY }
     * 
     */
    public VOCABULARY createVOCABULARY() {
        return new VOCABULARY();
    }

    /**
     * Create an instance of {@link HEADER }
     * 
     */
    public HEADER createHEADER() {
        return new HEADER();
    }

    /**
     * Create an instance of {@link TITLES }
     * 
     */
    public TITLES createTITLES() {
        return new TITLES();
    }

    /**
     * Create an instance of {@link HLANG }
     * 
     */
    public HLANG createHLANG() {
        return new HLANG();
    }

    /**
     * Create an instance of {@link SOURCES }
     * 
     */
    public SOURCES createSOURCES() {
        return new SOURCES();
    }

    /**
     * Create an instance of {@link SOURC }
     * 
     */
    public SOURC createSOURC() {
        return new SOURC();
    }

    /**
     * Create an instance of {@link REMK }
     * 
     */
    public REMK createREMK() {
        return new REMK();
    }

    /**
     * Create an instance of {@link LINK }
     * 
     */
    public LINK createLINK() {
        return new LINK();
    }

    /**
     * Create an instance of {@link SOURF }
     * 
     */
    public SOURF createSOURF() {
        return new SOURF();
    }

    /**
     * Create an instance of {@link WORKGROUP }
     * 
     */
    public WORKGROUP createWORKGROUP() {
        return new WORKGROUP();
    }

    /**
     * Create an instance of {@link MEMBER }
     * 
     */
    public MEMBER createMEMBER() {
        return new MEMBER();
    }

    /**
     * Create an instance of {@link FINANCIERS }
     * 
     */
    public FINANCIERS createFINANCIERS() {
        return new FINANCIERS();
    }

    /**
     * Create an instance of {@link COMMENTS }
     * 
     */
    public COMMENTS createCOMMENTS() {
        return new COMMENTS();
    }

    /**
     * Create an instance of {@link FOREWORD }
     * 
     */
    public FOREWORD createFOREWORD() {
        return new FOREWORD();
    }

    /**
     * Create an instance of {@link INDEX }
     * 
     */
    public INDEX createINDEX() {
        return new INDEX();
    }

    /**
     * Create an instance of {@link RECORD }
     * 
     */
    public RECORD createRECORD() {
        return new RECORD();
    }

    /**
     * Create an instance of {@link SUBJ }
     * 
     */
    public SUBJ createSUBJ() {
        return new SUBJ();
    }

    /**
     * Create an instance of {@link GRAM }
     * 
     */
    public GRAM createGRAM() {
        return new GRAM();
    }

    /**
     * Create an instance of {@link LANG }
     * 
     */
    public LANG createLANG() {
        return new LANG();
    }

    /**
     * Create an instance of {@link TE }
     * 
     */
    public TE createTE() {
        return new TE();
    }

    /**
     * Create an instance of {@link Termcontent }
     * 
     */
    public Termcontent createTermcontent() {
        return new Termcontent();
    }

    /**
     * Create an instance of {@link EQUI }
     * 
     */
    public EQUI createEQUI() {
        return new EQUI();
    }

    /**
     * Create an instance of {@link TERM }
     * 
     */
    public TERM createTERM() {
        return new TERM();
    }

    /**
     * Create an instance of {@link SCOPE }
     * 
     */
    public SCOPE createSCOPE() {
        return new SCOPE();
    }

    /**
     * Create an instance of {@link SY }
     * 
     */
    public SY createSY() {
        return new SY();
    }

    /**
     * Create an instance of {@link DTE }
     * 
     */
    public DTE createDTE() {
        return new DTE();
    }

    /**
     * Create an instance of {@link DTEA }
     * 
     */
    public DTEA createDTEA() {
        return new DTEA();
    }

    /**
     * Create an instance of {@link DTEB }
     * 
     */
    public DTEB createDTEB() {
        return new DTEB();
    }

    /**
     * Create an instance of {@link RCON }
     * 
     */
    public RCON createRCON() {
        return new RCON();
    }

    /**
     * Create an instance of {@link DEF }
     * 
     */
    public DEF createDEF() {
        return new DEF();
    }

    /**
     * Create an instance of {@link BCON }
     * 
     */
    public BCON createBCON() {
        return new BCON();
    }

    /**
     * Create an instance of {@link NCON }
     * 
     */
    public NCON createNCON() {
        return new NCON();
    }

    /**
     * Create an instance of {@link SCON }
     * 
     */
    public SCON createSCON() {
        return new SCON();
    }

    /**
     * Create an instance of {@link ECON }
     * 
     */
    public ECON createECON() {
        return new ECON();
    }

    /**
     * Create an instance of {@link RCONEXT }
     * 
     */
    public RCONEXT createRCONEXT() {
        return new RCONEXT();
    }

    /**
     * Create an instance of {@link BCONEXT }
     * 
     */
    public BCONEXT createBCONEXT() {
        return new BCONEXT();
    }

    /**
     * Create an instance of {@link NCONEXT }
     * 
     */
    public NCONEXT createNCONEXT() {
        return new NCONEXT();
    }

    /**
     * Create an instance of {@link EXPLAN }
     * 
     */
    public EXPLAN createEXPLAN() {
        return new EXPLAN();
    }

    /**
     * Create an instance of {@link NOTE }
     * 
     */
    public NOTE createNOTE() {
        return new NOTE();
    }

    /**
     * Create an instance of {@link ILLU }
     * 
     */
    public ILLU createILLU() {
        return new ILLU();
    }

    /**
     * Create an instance of {@link EXAMP }
     * 
     */
    public EXAMP createEXAMP() {
        return new EXAMP();
    }

    /**
     * Create an instance of {@link CX }
     * 
     */
    public CX createCX() {
        return new CX();
    }

    /**
     * Create an instance of {@link STE }
     * 
     */
    public STE createSTE() {
        return new STE();
    }

    /**
     * Create an instance of {@link RELIA }
     * 
     */
    public RELIA createRELIA() {
        return new RELIA();
    }

    /**
     * Create an instance of {@link CLAS }
     * 
     */
    public CLAS createCLAS() {
        return new CLAS();
    }

    /**
     * Create an instance of {@link COMM }
     * 
     */
    public COMM createCOMM() {
        return new COMM();
    }

    /**
     * Create an instance of {@link DEVREC }
     * 
     */
    public DEVREC createDEVREC() {
        return new DEVREC();
    }

    /**
     * Create an instance of {@link HEAD }
     * 
     */
    public HEAD createHEAD() {
        return new HEAD();
    }

    /**
     * Create an instance of {@link BR }
     * 
     */
    public BR createBR() {
        return new BR();
    }

    /**
     * Create an instance of {@link DIAG }
     * 
     */
    public DIAG createDIAG() {
        return new DIAG();
    }

    /**
     * Create an instance of {@link ILLT }
     * 
     */
    public ILLT createILLT() {
        return new ILLT();
    }

    /**
     * Create an instance of {@link PICT }
     * 
     */
    public PICT createPICT() {
        return new PICT();
    }

    /**
     * Create an instance of {@link P }
     * 
     */
    public P createP() {
        return new P();
    }

    /**
     * Create an instance of {@link REFERENCES }
     * 
     */
    public REFERENCES createREFERENCES() {
        return new REFERENCES();
    }

    /**
     * Create an instance of {@link REF }
     * 
     */
    public REF createREF() {
        return new REF();
    }

    /**
     * Create an instance of {@link REFTEXT }
     * 
     */
    public REFTEXT createREFTEXT() {
        return new REFTEXT();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Object }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link Object }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "styling")
    public JAXBElement<Object> createStyling(Object value) {
        return new JAXBElement<Object>(_Styling_QNAME, Object.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "SERIAL")
    public JAXBElement<String> createSERIAL(String value) {
        return new JAXBElement<String>(_SERIAL_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "YEAR")
    public JAXBElement<String> createYEAR(String value) {
        return new JAXBElement<String>(_YEAR_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "HOGR")
    public JAXBElement<String> createHOGR(String value) {
        return new JAXBElement<String>(_HOGR_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "ADD")
    public JAXBElement<String> createADD(String value) {
        return new JAXBElement<String>(_ADD_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "NAME")
    public JAXBElement<String> createNAME(String value) {
        return new JAXBElement<String>(_NAME_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "TITLE")
    public JAXBElement<String> createTITLE(String value) {
        return new JAXBElement<String>(_TITLE_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "FUNC")
    public JAXBElement<String> createFUNC(String value) {
        return new JAXBElement<String>(_FUNC_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "ORG")
    public JAXBElement<String> createORG(String value) {
        return new JAXBElement<String>(_ORG_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "NUMB")
    public JAXBElement<String> createNUMB(String value) {
        return new JAXBElement<String>(_NUMB_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "POSI")
    public JAXBElement<String> createPOSI(String value) {
        return new JAXBElement<String>(_POSI_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "SUB", substitutionHeadNamespace = "", substitutionHeadName = "styling")
    public JAXBElement<String> createSUB(String value) {
        return new JAXBElement<String>(_SUB_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "SUP", substitutionHeadNamespace = "", substitutionHeadName = "styling")
    public JAXBElement<String> createSUP(String value) {
        return new JAXBElement<String>(_SUP_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "POS")
    public JAXBElement<String> createPOS(String value) {
        return new JAXBElement<String>(_POS_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "GEND")
    public JAXBElement<String> createGEND(String value) {
        return new JAXBElement<String>(_GEND_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "INFL")
    public JAXBElement<String> createINFL(String value) {
        return new JAXBElement<String>(_INFL_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "GEOG")
    public JAXBElement<String> createGEOG(String value) {
        return new JAXBElement<String>(_GEOG_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "TYPT")
    public JAXBElement<String> createTYPT(String value) {
        return new JAXBElement<String>(_TYPT_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "PHR")
    public JAXBElement<String> createPHR(String value) {
        return new JAXBElement<String>(_PHR_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "PRON")
    public JAXBElement<String> createPRON(String value) {
        return new JAXBElement<String>(_PRON_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "ETYM")
    public JAXBElement<String> createETYM(String value) {
        return new JAXBElement<String>(_ETYM_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "STAT")
    public JAXBElement<String> createSTAT(String value) {
        return new JAXBElement<String>(_STAT_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Termcontent }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link Termcontent }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "EXTE")
    public JAXBElement<Termcontent> createEXTE(Termcontent value) {
        return new JAXBElement<Termcontent>(_EXTE_QNAME, Termcontent.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Termcontent }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link Termcontent }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "DES")
    public JAXBElement<Termcontent> createDES(Termcontent value) {
        return new JAXBElement<Termcontent>(_DES_QNAME, Termcontent.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Termcontent }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link Termcontent }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "ACRO")
    public JAXBElement<Termcontent> createACRO(Termcontent value) {
        return new JAXBElement<Termcontent>(_ACRO_QNAME, Termcontent.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "FORMULA")
    public JAXBElement<String> createFORMULA(String value) {
        return new JAXBElement<String>(_FORMULA_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "EXNO")
    public JAXBElement<String> createEXNO(String value) {
        return new JAXBElement<String>(_EXNO_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Termcontent }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link Termcontent }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "ASTE")
    public JAXBElement<Termcontent> createASTE(Termcontent value) {
        return new JAXBElement<Termcontent>(_ASTE_QNAME, Termcontent.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "APDAT")
    public JAXBElement<String> createAPDAT(String value) {
        return new JAXBElement<String>(_APDAT_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "TIT")
    public JAXBElement<String> createTIT(String value) {
        return new JAXBElement<String>(_TIT_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "INST")
    public JAXBElement<String> createINST(String value) {
        return new JAXBElement<String>(_INST_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "CLASPEC")
    public JAXBElement<String> createCLASPEC(String value) {
        return new JAXBElement<String>(_CLASPEC_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "CLASYS")
    public JAXBElement<String> createCLASYS(String value) {
        return new JAXBElement<String>(_CLASYS_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "CRDAT")
    public JAXBElement<String> createCRDAT(String value) {
        return new JAXBElement<String>(_CRDAT_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "CRBY")
    public JAXBElement<String> createCRBY(String value) {
        return new JAXBElement<String>(_CRBY_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "CREA")
    public JAXBElement<String> createCREA(String value) {
        return new JAXBElement<String>(_CREA_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "UPDAT")
    public JAXBElement<String> createUPDAT(String value) {
        return new JAXBElement<String>(_UPDAT_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "UPBY")
    public JAXBElement<String> createUPBY(String value) {
        return new JAXBElement<String>(_UPBY_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "UPDA")
    public JAXBElement<String> createUPDA(String value) {
        return new JAXBElement<String>(_UPDA_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "CHDAT")
    public JAXBElement<String> createCHDAT(String value) {
        return new JAXBElement<String>(_CHDAT_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "CHBY")
    public JAXBElement<String> createCHBY(String value) {
        return new JAXBElement<String>(_CHBY_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "CHECK")
    public JAXBElement<String> createCHECK(String value) {
        return new JAXBElement<String>(_CHECK_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "APBY")
    public JAXBElement<String> createAPBY(String value) {
        return new JAXBElement<String>(_APBY_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "APPR")
    public JAXBElement<String> createAPPR(String value) {
        return new JAXBElement<String>(_APPR_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BR }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link BR }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "BR", substitutionHeadNamespace = "", substitutionHeadName = "styling")
    public JAXBElement<BR> createBR(BR value) {
        return new JAXBElement<BR>(_BR_QNAME, BR.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "REFNAME")
    public JAXBElement<String> createREFNAME(String value) {
        return new JAXBElement<String>(_REFNAME_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "B", substitutionHeadNamespace = "", substitutionHeadName = "styling")
    public JAXBElement<String> createB(String value) {
        return new JAXBElement<String>(_B_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "I", substitutionHeadNamespace = "", substitutionHeadName = "styling")
    public JAXBElement<String> createI(String value) {
        return new JAXBElement<String>(_I_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "REFLINK")
    public JAXBElement<String> createREFLINK(String value) {
        return new JAXBElement<String>(_REFLINK_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "REFHEAD")
    public JAXBElement<String> createREFHEAD(String value) {
        return new JAXBElement<String>(_REFHEAD_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "SYNT")
    public JAXBElement<String> createSYNT(String value) {
        return new JAXBElement<String>(_SYNT_QNAME, String.class, null, value);
    }

}
