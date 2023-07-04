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
        VOCABULARY, NUMB, DEF, RECORD, TE, TERM, LANG, DTE, SY, CX, SOURF, ADD, LINK, EQUI,
        EXPLAN, EXAMP, NOTE, RCON, TYPT, TYPR, SOURC, GRAM, TIT, TBLE, POSI, STE, IMG
    }

    private static final Map<String, String> LINK_MAP = new HashMap<>();
    private static final Pattern LINK_PATTERN = Pattern.compile("@@(.*?)\\sWITH LANG(.*)$");
    private static final List<String> IGNORED = List.of(
            Elements.SOURC.name(),
            Elements.TIT.name(),
            Elements.POSI.name(),
            Elements.TBLE.name(),
            Elements.STE.name(),
            Elements.IMG.name());
    public static final String CONCEPT_ID_PREFIX = "concept-";
    public static final List<String> TERM_FAMILY_VALUES = List.of("m", "f", "n");
    public static final List<String> NOT_RECOMMENDED_TYPES = List.of("hylättävä", "vanhahtava", "vanhentunut");

    /**
     * Concepts are linked based on term's name, e.g. <LINK KEY='@@selvitys WITH LANG=fi'>selvitys</LINK>, where "selvitys"
     * is the name of the term. Construct link map with key of term's name and value of concept's id
     *
     * @param document document
     */
    private void constructLinkMap(Document document) {
        var records = document.getElementsByTagName(Elements.RECORD.name());

        for (var i = 0; i < records.getLength(); i++) {
            var childNodes = records.item(i).getChildNodes();
            String conceptId = null;
            for (var j = 0; j < childNodes.getLength(); j++) {
                var item = childNodes.item(j);
                if (item.getNodeName().equals(Elements.NUMB.name())) {
                    conceptId = CONCEPT_ID_PREFIX + item.getTextContent();
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

        var handledConcepts = new HashSet<String>();
        var languages = new HashSet<String>();

        var root = result.createElement(Elements.VOCABULARY.name());

        var records = document.getElementsByTagName(Elements.RECORD.name());
        for (var i = 0; i < records.getLength(); i++) {
            var createdLanguages = new LinkedHashMap<String, Element>();
            var terms = new HashMap<String, List<Element>>();
            var relatedConcepts = new HashSet<String>();
            Element currentTerm = null;

            var rec = result.createElement(Elements.RECORD.name());

            var childNodes = records.item(i).getChildNodes();
            for (var j = 0; j < childNodes.getLength(); j++) {
                var item = childNodes.item(j);
                String tagName = item.getNodeName();

                if (item.getNodeType() != Node.ELEMENT_NODE || IGNORED.contains(tagName)) {
                    continue;
                }

                var langElement = getLanguageElement(result, createdLanguages, item);

                if (tagName.equals(Elements.NUMB.name())) {
                    if (handledConcepts.contains(item.getTextContent())) {
                        System.out.println("Duplicate id found " + item.getTextContent());
                        continue;
                    }
                    handledConcepts.add(item.getTextContent());
                    rec.setAttribute("numb", CONCEPT_ID_PREFIX + item.getTextContent());
                } else if (tagName.equals(Elements.TE.name()) && langElement != null) {
                    currentTerm = handleTerm(result, langElement, item, terms);
                } else if (tagName.equals(Elements.DEF.name()) && langElement != null) {
                    var def = handleContentWithLinks(result, item, Elements.DEF);
                    langElement.appendChild(def);
                } else if (tagName.equals(Elements.SOURF.name()) && langElement != null) {
                    if (currentTerm != null) {
                        var sourf = result.createElement(Elements.SOURF.name());
                        sourf.setTextContent(formatText(item.getTextContent()));
                        currentTerm.appendChild(sourf);
                    }
                } else if (tagName.equals(Elements.ADD.name())
                        || tagName.equals(Elements.TYPT.name())) {
                    handleAdd(result, item, currentTerm);
                } else if ((tagName.equals(Elements.CX.name())
                        || tagName.equals(Elements.EXPLAN.name()))
                        && langElement != null) {
                    var note = handleContentWithLinks(result, item, Elements.NOTE);
                    langElement.appendChild(note);
                } else if (tagName.equals(Elements.EXAMP.name()) && langElement != null) {
                    var examp = handleContentWithLinks(result, item, Elements.EXAMP);
                    langElement.appendChild(examp);
                } else if (tagName.equals(Elements.RCON.name())) {
                    handleRcon(result, rec, item, relatedConcepts);
                } else if (tagName.equals(Elements.GRAM.name())) {
                    handleGram(result, item, currentTerm);
                } else if (tagName.equals(Elements.TYPR.name())) {
                    if (currentTerm != null) {
                        var content = item.getTextContent();
                        var equi = handleEqui(result, content);
                        if (equi != null) {
                            currentTerm.appendChild(equi);
                        }
                    }
                } else {
                    System.out.println("Unhandled element: " + tagName);
                }
            }

            createdLanguages.forEach((key, value) -> {
                languages.add(key);
                rec.appendChild(value);
            });

            // if no NUMB element not present
            if (!rec.getAttribute("numb").equals("")) {
                root.appendChild(rec);
            }
        }
        result.appendChild(root);

        System.out.println("Existing languages: " + languages);
        writeXml(result);
    }

    private static Element getLanguageElement(Document result, HashMap<String, Element> createdLanguages, Node item) {
        var attr = item.getAttributes().getNamedItem("LANG");
        if (attr != null) {
            String language = item.getAttributes().item(0).getTextContent();
            createdLanguages.computeIfAbsent(language, (var key) -> {
                var lang = result.createElement(Elements.LANG.name());
                lang.setAttribute("value", language);
                return lang;
            });
            return createdLanguages.get(language);
        }
        return null;
    }

    private static Element handleTerm(Document result, Element langElement, Node item, Map<String, List<Element>> terms) {
        Element termElement;
        if (langElement.getElementsByTagName(Elements.TE.name()).getLength() == 0) {
            termElement = result.createElement(Elements.TE.name());
        } else {
            // check if next sibling element is TYPT (type of the term)
            var sibling = item.getNextSibling();
            while (sibling.getNodeType() != Node.ELEMENT_NODE) {
                sibling = sibling.getNextSibling();
            }
            if (sibling.getNodeName().equals(Elements.TYPT.name()) && NOT_RECOMMENDED_TYPES.contains(sibling.getTextContent())) {
                termElement = result.createElement(Elements.DTE.name());
            } else {
                termElement = result.createElement(Elements.SY.name());
            }
        }

        var term = result.createElement(Elements.TERM.name());
        term.setTextContent(item.getTextContent());
        termElement.appendChild(term);
        langElement.appendChild(termElement);

        String lang = langElement.getAttribute("value");
        var termList = terms.getOrDefault(lang, new ArrayList<>());
        termList.add(termElement);
        terms.put(lang, termList);

        return termElement;
    }

    private static void handleAdd(Document result, Node item, Element currentTerm) {
        // assume that elements are in particular order, so append ADD element to the previously handled term
        if (currentTerm != null) {
            var add = handleContentWithLinks(result, item, Elements.ADD);
            Node existingAdd = findChildNode(currentTerm, Elements.ADD.name());
            if (existingAdd != null) {
                existingAdd.appendChild(result.createTextNode(", "));
                appendContent(result, add, existingAdd);
            } else {
                currentTerm.appendChild(add);
            }
        }
    }

    private static Node findChildNode(Element element, String name) {
        var childNodes = element.getChildNodes();
        for (var i = 0; i < childNodes.getLength(); i++) {
            if (childNodes.item(i).getNodeName().equals(name)) {
                return childNodes.item(i);
            }
        }
        return null;
    }

    private static Element handleEqui(Document result, String content) {
        var equi = result.createElement(Elements.EQUI.name());
        if (content.equalsIgnoreCase("Laajempi käsite")
                || content.equalsIgnoreCase("weiterer Begriff")) {
            equi.setAttribute("value", "broader");
        } else if (content.equalsIgnoreCase("Suppeampi käsite")
                || content.equalsIgnoreCase("понятие более низкого уровня")) {
            equi.setAttribute("value", "narrower");
        } else if (content.equalsIgnoreCase("близкое понятие")) {
            equi.setAttribute("value", "near-equivalent");
        } else {
            System.out.println("Unhandled TYPR value " + content);
            return null;
        }
        return equi;
    }

    private static void handleGram(Document result, Node item, Element currentTerm) {
        var gram = result.createElement(Elements.GRAM.name());
        var content = item.getTextContent();
        if (TERM_FAMILY_VALUES.contains(content)) {
            gram.setAttribute("gend", content);
        } else if ("pl".equals(content)) {
            gram.setAttribute("value", content);
        } else if (content.startsWith("verb")) {
            gram.setAttribute("posi", "verb");
        } else if (content.equals("npl")) {
            gram.setAttribute("value", "n pl");
        } else if (content.equals("fpl")) {
            gram.setAttribute("value", "f pl");
        } else {
            System.out.println("Unhandled GRAM value " + content);
            return;
        }
        var termNode = findChildNode(currentTerm, Elements.TERM.name());
        if (termNode != null) {
            termNode.appendChild(gram);
        }
    }

    private static void handleRcon(Document result, Element rec, Node item, Set<String> relatedConcepts) {
        var links = item.getChildNodes();
        for (var i = 0; i < links.getLength(); i++) {
            var link = links.item(i);
            if (link.getNodeType() == Node.ELEMENT_NODE && link.getNodeName().equals(Elements.LINK.name())) {
                var linkTarget = getLinkTarget(link);
                if (relatedConcepts.contains(linkTarget)) {
                    continue;
                }
                var rcon = result.createElement(Elements.RCON.name());
                rcon.setAttribute("href", linkTarget);
                rec.appendChild(rcon);
                relatedConcepts.add(linkTarget);
            }
        }
    }

    private static Element handleContentWithLinks(Document result, Node item, Elements nodeName) {
        var contentElement = result.createElement(nodeName.name());
        appendContent(result, item, contentElement);
        return contentElement;
    }

    private static String getLinkTarget(Node node) {
        var href = node.getAttributes().item(0).getTextContent();
        var matcher = LINK_PATTERN.matcher(href);
        if (matcher.matches()) {
            return "#" + LINK_MAP.getOrDefault(matcher.group(1), "");
        } else {
            return href;
        }
    }

    private static void appendContent(Document result, Node source, Node target) {
        var childNodes = source.getChildNodes();
        for (var x = 0; x < childNodes.getLength(); x++) {
            var node = childNodes.item(x);
            if (node.getNodeType() == Node.TEXT_NODE) {
                target.appendChild(result.createTextNode(formatText(node.getTextContent())));
            } else if (node.getNodeName().equals(Elements.LINK.name())) {
                var link = result.createElement(Elements.LINK.name());
                var href = getLinkTarget(node);
                link.setAttribute("href", href);
                link.setTextContent(node.getTextContent());
                target.appendChild(link);
            }
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

    public static void main(String... a) throws Exception {
        var ntrf = new NtrfConverter();

        var factory = DocumentBuilderFactory.newInstance();
        var data = factory.newDocumentBuilder()
                .parse(ntrf.getClass().getResourceAsStream("/simple.xml"));
        ntrf.constructLinkMap(data);
        ntrf.convert(data);
    }
}
