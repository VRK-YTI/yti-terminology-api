package fi.vm.yti.terminology.ntrf;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.util.*;
import java.util.regex.Pattern;

public class NtrfConverter {

    enum Elements {
        VOCABULARY, NUMB, DEF, RECORD, TE, TERM, LANG, DTE, CX, SOURF, ADD, LINK, EXPLAN, EXAMP, NOTE, RCON, TYPT, TYPR, SOURC, GRAM ;
    }

    private static final Map<String, String> LINK_MAP = new HashMap<>();
    private static final Pattern LINK_PATTERN = Pattern.compile("@@(.*?)\\sWITH LANG(.*)$");
    private static final List<String> IGNORED = List.of(Elements.TYPT.name(),
            Elements.SOURC.name(),
            Elements.GRAM.name(),
            Elements.TYPR.name());

    private void constructLinkMap(Document document) {
        var records = document.getElementsByTagName(Elements.RECORD.name());

        for (var i=0; i<records.getLength(); i++) {
            var childNodes = records.item(i).getChildNodes();
            String conceptId = null;
            for (var j=0; j<childNodes.getLength(); j++) {
                var item = childNodes.item(j);
                if (item.getNodeName().equals(Elements.NUMB.name())) {
                    conceptId = "concept-" + item.getTextContent();
                }
                if (item.getNodeName().equals(Elements.TE.name())) {
                    LINK_MAP.put(item.getTextContent(), conceptId);
                }
            }
        }
    }
    private void convert(Document document) throws ParserConfigurationException, TransformerException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document result = docBuilder.newDocument();

        var root = result.createElement(Elements.VOCABULARY.name());

        var records = document.getElementsByTagName(Elements.RECORD.name());

        for (var i=0; i<records.getLength(); i++) {
            var createdLanguages = new HashMap<String, Element>();
            var terms = new HashMap<String, List<Element>>();

            var rec = result.createElement(Elements.RECORD.name());

            var childNodes = records.item(i).getChildNodes();
            for (var j=0; j<childNodes.getLength(); j++) {
                var item = childNodes.item(j);
                if (item.getNodeType() != Node.ELEMENT_NODE || IGNORED.contains(item.getNodeName())) {
                    continue;
                }

                handleLanguageElement(result, createdLanguages, item);
                var langElement = getLangElement(createdLanguages, item);

                if (item.getNodeName().equals(Elements.NUMB.name())) {
                    rec.setAttribute("numb", "concept-" + item.getTextContent());
                } else if (item.getNodeName().equals(Elements.TE.name())) {
                    handleTerm(result, langElement, item, terms);
                } else if (item.getNodeName().equals(Elements.DEF.name())) {
                    var def = handleContentWithLinks(result, item, Elements.DEF);
                    langElement.appendChild(def);
                } else if (item.getNodeName().equals(Elements.SOURF.name())) {
                    var sourf = result.createElement(Elements.SOURF.name());
                    sourf.setTextContent(formatText(item.getTextContent()));
                    langElement.appendChild(sourf);
                } else if (item.getNodeName().equals(Elements.ADD.name())) {
                    Node lastTerm = getLastTerm(createdLanguages, item);
                    if (lastTerm != null) {
                        var add = handleContentWithLinks(result, item, Elements.ADD);
                        lastTerm.appendChild(add);
                    }
                } else if (item.getNodeName().equals(Elements.CX.name()) || item.getNodeName().equals(Elements.EXPLAN.name())) {
                    var note = handleContentWithLinks(result, item, Elements.NOTE);
                    langElement.appendChild(note);
                } else if (item.getNodeName().equals(Elements.EXAMP.name())) {
                    // TODO: this is not supported by NTRF importer
                    var note = handleContentWithLinks(result, item, Elements.EXAMP);
                    langElement.appendChild(note);
                }
                else {
                    System.out.println("Unhandled element: " + item.getNodeName());
                }
            }

            createdLanguages.forEach((key, value) -> rec.appendChild(value));
            root.appendChild(rec);
        }
        result.appendChild(root);
        writeXml(result);
    }

    private static Node getLastTerm(HashMap<String, Element> createdLanguages, Node item) {
        var langElem = getLangElement(createdLanguages, item);
        var childNodes = langElem.getChildNodes();

        Node lastTerm = null;

        for (var i=0; i<childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (Arrays.asList(Elements.TE.name(), Elements.DTE.name()).contains(node.getNodeName())) {
                lastTerm = node;
            }
        }
        return lastTerm;
    }

    private static Element handleContentWithLinks(Document result, Node item, Elements nodeName) {

        var contentElement = result.createElement(nodeName.name());
        var childNodes = item.getChildNodes();
        for (var x=0; x<childNodes.getLength(); x++) {
            var node = childNodes.item(x);
            if (node.getNodeType() == Node.TEXT_NODE) {
                contentElement.appendChild(result.createTextNode(formatText(node.getTextContent())));
            } else if (node.getNodeName().equals(Elements.LINK.name())) {
                var link = result.createElement(Elements.LINK.name());
                var href = node.getAttributes().getNamedItem("KEY").getTextContent();
                link.setTextContent(node.getTextContent());

                var matcher = LINK_PATTERN.matcher(href);
                if (matcher.matches()) {
                    var key = matcher.group(1);
                    link.setAttribute("href", "#" + LINK_MAP.get(key));
                    contentElement.appendChild(link);
                }

            } else {
                System.out.println("Unknown element " + node.getNodeName());
            }
        }
        return contentElement;
    }

    private static void handleTerm(Document result, Element langElement, Node item, Map<String, List<Element>> terms) {
        Element termElement;
        if (langElement.getElementsByTagName(Elements.TE.name()).getLength() == 0) {
            termElement = result.createElement(Elements.TE.name());
        } else {
            termElement = result.createElement(Elements.DTE.name());
        }

        var term = result.createElement(Elements.TERM.name());
        term.setTextContent(item.getTextContent());
        termElement.appendChild(term);
        langElement.appendChild(termElement);

        String lang = langElement.getAttribute("value");
        var termList = terms.getOrDefault(lang, new ArrayList<>());
        termList.add(termElement);
        terms.put(lang, termList);
    }

    private static Element getLangElement(HashMap<String, Element> createdLanguages, Node item) {
        var att = item.getAttributes().getNamedItem("LANG");
        return att != null ? createdLanguages.get(att.getTextContent()) : null;
    }

    private static void handleLanguageElement(Document result, HashMap<String, Element> createdLanguages, Node item) {
        var attr = item.getAttributes().item(0);
        if (attr != null && attr.getNodeName().equals(Elements.LANG.name())) {
            String language = item.getAttributes().item(0).getTextContent();
            createdLanguages.computeIfAbsent(language, (var key) -> {
                var lang = result.createElement(Elements.LANG.name());
                lang.setAttribute("value", language);
                return lang;
            });
        }
    }

    private static String formatText(String text) {
        return text.replaceAll("\\s{2,}", "");
    }

    private static void writeXml(Document doc)
            throws TransformerException {

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(System.out);

        transformer.transform(source, result);

    }
    public static void main(String...a) throws Exception {
        var ntrf = new NtrfConverter();

        var builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        var data = builder.parse(ntrf.getClass().getResourceAsStream("/Hallintosanasto_20230216_A.xml"));
        ntrf.constructLinkMap(data);
        ntrf.convert(data);
    }


}
