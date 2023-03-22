
package fi.vm.yti.terminology.api.model.ntrf;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for languages.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <pre>
 * &lt;simpleType name="languages"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}token"&gt;
 *     &lt;enumeration value="af"/&gt;
 *     &lt;enumeration value="am"/&gt;
 *     &lt;enumeration value="ar"/&gt;
 *     &lt;enumeration value="as"/&gt;
 *     &lt;enumeration value="az"/&gt;
 *     &lt;enumeration value="be"/&gt;
 *     &lt;enumeration value="bg"/&gt;
 *     &lt;enumeration value="bn"/&gt;
 *     &lt;enumeration value="br"/&gt;
 *     &lt;enumeration value="bs"/&gt;
 *     &lt;enumeration value="ca"/&gt;
 *     &lt;enumeration value="cs"/&gt;
 *     &lt;enumeration value="da"/&gt;
 *     &lt;enumeration value="de"/&gt;
 *     &lt;enumeration value="el"/&gt;
 *     &lt;enumeration value="en"/&gt;
 *     &lt;enumeration value="es"/&gt;
 *     &lt;enumeration value="et"/&gt;
 *     &lt;enumeration value="eu"/&gt;
 *     &lt;enumeration value="fa"/&gt;
 *     &lt;enumeration value="fi"/&gt;
 *     &lt;enumeration value="fo"/&gt;
 *     &lt;enumeration value="fr"/&gt;
 *     &lt;enumeration value="gl"/&gt;
 *     &lt;enumeration value="gu"/&gt;
 *     &lt;enumeration value="ha"/&gt;
 *     &lt;enumeration value="he"/&gt;
 *     &lt;enumeration value="hi"/&gt;
 *     &lt;enumeration value="hr"/&gt;
 *     &lt;enumeration value="hu"/&gt;
 *     &lt;enumeration value="hy"/&gt;
 *     &lt;enumeration value="id"/&gt;
 *     &lt;enumeration value="ig"/&gt;
 *     &lt;enumeration value="is"/&gt;
 *     &lt;enumeration value="it"/&gt;
 *     &lt;enumeration value="ja"/&gt;
 *     &lt;enumeration value="ka"/&gt;
 *     &lt;enumeration value="kk"/&gt;
 *     &lt;enumeration value="km"/&gt;
 *     &lt;enumeration value="kn"/&gt;
 *     &lt;enumeration value="ks"/&gt;
 *     &lt;enumeration value="ky"/&gt;
 *     &lt;enumeration value="la"/&gt;
 *     &lt;enumeration value="ln"/&gt;
 *     &lt;enumeration value="lt"/&gt;
 *     &lt;enumeration value="lv"/&gt;
 *     &lt;enumeration value="mk"/&gt;
 *     &lt;enumeration value="ml"/&gt;
 *     &lt;enumeration value="mn"/&gt;
 *     &lt;enumeration value="mr"/&gt;
 *     &lt;enumeration value="ms"/&gt;
 *     &lt;enumeration value="nl"/&gt;
 *     &lt;enumeration value="no"/&gt;
 *     &lt;enumeration value="or"/&gt;
 *     &lt;enumeration value="pa"/&gt;
 *     &lt;enumeration value="pl"/&gt;
 *     &lt;enumeration value="ps"/&gt;
 *     &lt;enumeration value="pt"/&gt;
 *     &lt;enumeration value="pt-br"/&gt;
 *     &lt;enumeration value="pt-pt"/&gt;
 *     &lt;enumeration value="ro"/&gt;
 *     &lt;enumeration value="ru"/&gt;
 *     &lt;enumeration value="si"/&gt;
 *     &lt;enumeration value="sk"/&gt;
 *     &lt;enumeration value="sl"/&gt;
 *     &lt;enumeration value="sq"/&gt;
 *     &lt;enumeration value="sr"/&gt;
 *     &lt;enumeration value="st"/&gt;
 *     &lt;enumeration value="sv"/&gt;
 *     &lt;enumeration value="sw"/&gt;
 *     &lt;enumeration value="ta"/&gt;
 *     &lt;enumeration value="te"/&gt;
 *     &lt;enumeration value="tg"/&gt;
 *     &lt;enumeration value="th"/&gt;
 *     &lt;enumeration value="tk"/&gt;
 *     &lt;enumeration value="tl"/&gt;
 *     &lt;enumeration value="tr"/&gt;
 *     &lt;enumeration value="uk"/&gt;
 *     &lt;enumeration value="ur"/&gt;
 *     &lt;enumeration value="uz"/&gt;
 *     &lt;enumeration value="vi"/&gt;
 *     &lt;enumeration value="xh"/&gt;
 *     &lt;enumeration value="yo"/&gt;
 *     &lt;enumeration value="zc"/&gt;
 *     &lt;enumeration value="zh"/&gt;
 *     &lt;enumeration value="zt"/&gt;
 *     &lt;enumeration value="zu"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "languages")
@XmlEnum
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
