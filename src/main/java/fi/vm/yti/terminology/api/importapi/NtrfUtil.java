package fi.vm.yti.terminology.api.importapi;

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import fi.vm.yti.terminology.api.model.ntrf.LINK;
import fi.vm.yti.terminology.api.model.ntrf.VOCABULARY;
import fi.vm.yti.terminology.api.model.termed.Graph;
import jakarta.xml.bind.*;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.util.List;

public class NtrfUtil {

    public static VOCABULARY unmarshallXmlDocument(String message) throws JAXBException, XMLStreamException {
        JAXBContext jc = JAXBContext.newInstance(VOCABULARY.class);
        // Disable DOCTYPE-directive from incoming file.
        XMLInputFactory xif = XMLInputFactory.newFactory();
        xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        // Unmarshall XMl with JAXB
        Reader inReader = new StringReader(message);
        XMLStreamReader xsr = xif.createXMLStreamReader(inReader);
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        // At last, resolve ntrf-POJO's
        return (VOCABULARY) unmarshaller.unmarshal(xsr);
    }

    public static String getLink(String uri, String href, List<Serializable> linkContent, String type) {
        StringBuilder builder = new StringBuilder();
        builder.append("<a href='")
                .append(uri)
                .append(getCleanRef(href, type))
                .append(">")
                .append(parseHrefText(linkContent))
                .append("</a>");

        return builder.toString();
    }

    public static String parseHrefText(List<Serializable> content) {
        String hrefText = "";
        for (Serializable c : content) {
            if (c instanceof JAXBElement) {
                JAXBElement el = (JAXBElement) c;
                if (el.getName().toString().equalsIgnoreCase("HOGR")) {
                    hrefText = hrefText.trim() + " (" + el.getValue().toString() + ")";
                }
            } else if (c instanceof String) {
                hrefText = hrefText + c;
            }
        }
        // Remove newlines just in case
        hrefText = hrefText.replaceAll("\n", "");
        return escapeStringContent(hrefText.trim());
    }

    public static String parseLinkRef(LINK li, Graph vocabulary) {
        String linkRef = li.getHref();
        // Remove "href:" from string
        if (linkRef.startsWith("href:")) {
            linkRef = linkRef.substring(5);
        }
        if (linkRef.startsWith("#")) {
            // internal reference, generate url for it.
            if (vocabulary.getUri().endsWith("/")) {
                linkRef = vocabulary.getUri() + linkRef.substring(1);
            } else {
                linkRef = vocabulary.getUri() + "/" + linkRef.substring(1);
            }
        }
        return linkRef;
    }

    public static String escapeStringContent(String s) {
        Escaper escaper = Escapers.builder()
                .addEscape('&', "&amp;")
                .addEscape('\"', "&quot;")
                .addEscape('\'', "&apos;")
                .addEscape('<', "&lt;")
                .addEscape('>', "&gt;")
                .build();

        return escaper.escape(s);
    }

    private static String getCleanRef(String refString, String datatype) {
        String ref = "";
        if (refString.startsWith("#")) {
            String hrefid = refString.substring(1);
            ref = ref.concat(hrefid + "'");
        } else
            ref = ref.concat(refString + "'");
        if (datatype != null && !datatype.isEmpty()) {
            ref = ref.concat(" property ='" + datatype + "'");
        }
        return ref;
    }

}
