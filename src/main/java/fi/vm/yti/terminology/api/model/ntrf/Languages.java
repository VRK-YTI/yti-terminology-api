//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2018.09.18 at 09:30:55 AM EEST 
//


package fi.vm.yti.terminology.api.model.ntrf;

import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for languages.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="languages">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}token">
 *     &lt;enumeration value="af"/>
 *     &lt;enumeration value="am"/>
 *     &lt;enumeration value="ar"/>
 *     &lt;enumeration value="as"/>
 *     &lt;enumeration value="az"/>
 *     &lt;enumeration value="be"/>
 *     &lt;enumeration value="bg"/>
 *     &lt;enumeration value="bn"/>
 *     &lt;enumeration value="br"/>
 *     &lt;enumeration value="bs"/>
 *     &lt;enumeration value="ca"/>
 *     &lt;enumeration value="cs"/>
 *     &lt;enumeration value="da"/>
 *     &lt;enumeration value="de"/>
 *     &lt;enumeration value="el"/>
 *     &lt;enumeration value="en"/>
 *     &lt;enumeration value="es"/>
 *     &lt;enumeration value="et"/>
 *     &lt;enumeration value="eu"/>
 *     &lt;enumeration value="fa"/>
 *     &lt;enumeration value="fi"/>
 *     &lt;enumeration value="fo"/>
 *     &lt;enumeration value="fr"/>
 *     &lt;enumeration value="gl"/>
 *     &lt;enumeration value="gu"/>
 *     &lt;enumeration value="ha"/>
 *     &lt;enumeration value="he"/>
 *     &lt;enumeration value="hi"/>
 *     &lt;enumeration value="hr"/>
 *     &lt;enumeration value="hu"/>
 *     &lt;enumeration value="hy"/>
 *     &lt;enumeration value="id"/>
 *     &lt;enumeration value="ig"/>
 *     &lt;enumeration value="is"/>
 *     &lt;enumeration value="it"/>
 *     &lt;enumeration value="ja"/>
 *     &lt;enumeration value="ka"/>
 *     &lt;enumeration value="kk"/>
 *     &lt;enumeration value="km"/>
 *     &lt;enumeration value="kn"/>
 *     &lt;enumeration value="ks"/>
 *     &lt;enumeration value="ky"/>
 *     &lt;enumeration value="la"/>
 *     &lt;enumeration value="ln"/>
 *     &lt;enumeration value="lt"/>
 *     &lt;enumeration value="lv"/>
 *     &lt;enumeration value="mk"/>
 *     &lt;enumeration value="ml"/>
 *     &lt;enumeration value="mn"/>
 *     &lt;enumeration value="mr"/>
 *     &lt;enumeration value="ms"/>
 *     &lt;enumeration value="nl"/>
 *     &lt;enumeration value="no"/>
 *     &lt;enumeration value="or"/>
 *     &lt;enumeration value="pa"/>
 *     &lt;enumeration value="pl"/>
 *     &lt;enumeration value="ps"/>
 *     &lt;enumeration value="pt"/>
 *     &lt;enumeration value="pt-br"/>
 *     &lt;enumeration value="pt-pt"/>
 *     &lt;enumeration value="ro"/>
 *     &lt;enumeration value="ru"/>
 *     &lt;enumeration value="si"/>
 *     &lt;enumeration value="sk"/>
 *     &lt;enumeration value="sl"/>
 *     &lt;enumeration value="sq"/>
 *     &lt;enumeration value="sr"/>
 *     &lt;enumeration value="st"/>
 *     &lt;enumeration value="sv"/>
 *     &lt;enumeration value="sw"/>
 *     &lt;enumeration value="ta"/>
 *     &lt;enumeration value="te"/>
 *     &lt;enumeration value="tg"/>
 *     &lt;enumeration value="th"/>
 *     &lt;enumeration value="tk"/>
 *     &lt;enumeration value="tl"/>
 *     &lt;enumeration value="tr"/>
 *     &lt;enumeration value="uk"/>
 *     &lt;enumeration value="ur"/>
 *     &lt;enumeration value="uz"/>
 *     &lt;enumeration value="vi"/>
 *     &lt;enumeration value="xh"/>
 *     &lt;enumeration value="yo"/>
 *     &lt;enumeration value="zc"/>
 *     &lt;enumeration value="zh"/>
 *     &lt;enumeration value="zt"/>
 *     &lt;enumeration value="zu"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "languages")
@XmlEnum
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2018-09-18T09:30:55+03:00", comments = "JAXB RI v2.2.8-b130911.1802")
public enum Languages {

    @XmlEnumValue("af")
    AF("af"),
    @XmlEnumValue("am")
    AM("am"),
    @XmlEnumValue("ar")
    AR("ar"),
    @XmlEnumValue("as")
    AS("as"),
    @XmlEnumValue("az")
    AZ("az"),
    @XmlEnumValue("be")
    BE("be"),
    @XmlEnumValue("bg")
    BG("bg"),
    @XmlEnumValue("bn")
    BN("bn"),
    @XmlEnumValue("br")
    BR("br"),
    @XmlEnumValue("bs")
    BS("bs"),
    @XmlEnumValue("ca")
    CA("ca"),
    @XmlEnumValue("cs")
    CS("cs"),
    @XmlEnumValue("da")
    DA("da"),
    @XmlEnumValue("de")
    DE("de"),
    @XmlEnumValue("el")
    EL("el"),
    @XmlEnumValue("en")
    EN("en"),
    @XmlEnumValue("es")
    ES("es"),
    @XmlEnumValue("et")
    ET("et"),
    @XmlEnumValue("eu")
    EU("eu"),
    @XmlEnumValue("fa")
    FA("fa"),
    @XmlEnumValue("fi")
    FI("fi"),
    @XmlEnumValue("fo")
    FO("fo"),
    @XmlEnumValue("fr")
    FR("fr"),
    @XmlEnumValue("gl")
    GL("gl"),
    @XmlEnumValue("gu")
    GU("gu"),
    @XmlEnumValue("ha")
    HA("ha"),
    @XmlEnumValue("he")
    HE("he"),
    @XmlEnumValue("hi")
    HI("hi"),
    @XmlEnumValue("hr")
    HR("hr"),
    @XmlEnumValue("hu")
    HU("hu"),
    @XmlEnumValue("hy")
    HY("hy"),
    @XmlEnumValue("id")
    ID("id"),
    @XmlEnumValue("ig")
    IG("ig"),
    @XmlEnumValue("is")
    IS("is"),
    @XmlEnumValue("it")
    IT("it"),
    @XmlEnumValue("ja")
    JA("ja"),
    @XmlEnumValue("ka")
    KA("ka"),
    @XmlEnumValue("kk")
    KK("kk"),
    @XmlEnumValue("km")
    KM("km"),
    @XmlEnumValue("kn")
    KN("kn"),
    @XmlEnumValue("ks")
    KS("ks"),
    @XmlEnumValue("ky")
    KY("ky"),
    @XmlEnumValue("la")
    LA("la"),
    @XmlEnumValue("ln")
    LN("ln"),
    @XmlEnumValue("lt")
    LT("lt"),
    @XmlEnumValue("lv")
    LV("lv"),
    @XmlEnumValue("mk")
    MK("mk"),
    @XmlEnumValue("ml")
    ML("ml"),
    @XmlEnumValue("mn")
    MN("mn"),
    @XmlEnumValue("mr")
    MR("mr"),
    @XmlEnumValue("ms")
    MS("ms"),
    @XmlEnumValue("nl")
    NL("nl"),
    @XmlEnumValue("no")
    NO("no"),
    @XmlEnumValue("or")
    OR("or"),
    @XmlEnumValue("pa")
    PA("pa"),
    @XmlEnumValue("pl")
    PL("pl"),
    @XmlEnumValue("ps")
    PS("ps"),
    @XmlEnumValue("pt")
    PT("pt"),
    @XmlEnumValue("pt-br")
    PT_BR("pt-br"),
    @XmlEnumValue("pt-pt")
    PT_PT("pt-pt"),
    @XmlEnumValue("ro")
    RO("ro"),
    @XmlEnumValue("ru")
    RU("ru"),
    @XmlEnumValue("si")
    SI("si"),
    @XmlEnumValue("sk")
    SK("sk"),
    @XmlEnumValue("sl")
    SL("sl"),
    @XmlEnumValue("sq")
    SQ("sq"),
    @XmlEnumValue("sr")
    SR("sr"),
    @XmlEnumValue("st")
    ST("st"),
    @XmlEnumValue("sv")
    SV("sv"),
    @XmlEnumValue("sw")
    SW("sw"),
    @XmlEnumValue("ta")
    TA("ta"),
    @XmlEnumValue("te")
    TE("te"),
    @XmlEnumValue("tg")
    TG("tg"),
    @XmlEnumValue("th")
    TH("th"),
    @XmlEnumValue("tk")
    TK("tk"),
    @XmlEnumValue("tl")
    TL("tl"),
    @XmlEnumValue("tr")
    TR("tr"),
    @XmlEnumValue("uk")
    UK("uk"),
    @XmlEnumValue("ur")
    UR("ur"),
    @XmlEnumValue("uz")
    UZ("uz"),
    @XmlEnumValue("vi")
    VI("vi"),
    @XmlEnumValue("xh")
    XH("xh"),
    @XmlEnumValue("yo")
    YO("yo"),
    @XmlEnumValue("zc")
    ZC("zc"),
    @XmlEnumValue("zh")
    ZH("zh"),
    @XmlEnumValue("zt")
    ZT("zt"),
    @XmlEnumValue("zu")
    ZU("zu");
    private final String value;

    Languages(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static Languages fromValue(String v) {
        for (Languages c: Languages.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}