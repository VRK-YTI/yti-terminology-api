package fi.vm.yti.terminology.api.v2.mapper;

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import fi.vm.yti.common.enums.Status;
import fi.vm.yti.common.properties.SuomiMeta;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.common.util.ModelWrapper;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.v2.dto.ConceptCollectionDTO;
import fi.vm.yti.terminology.api.v2.dto.ConceptDTO;
import fi.vm.yti.terminology.api.v2.dto.LocalizedValueDTO;
import fi.vm.yti.terminology.api.v2.dto.TermDTO;
import fi.vm.yti.terminology.api.v2.enums.TermConjugation;
import fi.vm.yti.terminology.api.v2.enums.TermFamily;
import fi.vm.yti.terminology.api.v2.enums.TermType;
import fi.vm.yti.terminology.api.v2.enums.WordClass;
import fi.vm.yti.terminology.api.v2.ntrf.*;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
import jakarta.xml.bind.JAXBElement;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.SKOS;

import java.io.Serializable;
import java.util.*;

public class NTRFMapper {

    private NTRFMapper() {
        // only static methods
    }

    // allowed formatting elements according to schema
    private static final List<String> HTML_ELEMENTS = List.of("br", "i", "b", "sup", "sub");

    public static void mapConcept(ModelWrapper model, RECORD concept, YtiUser user) {
        var resource = model.getResourceById(concept.getNumb());
        var properties = resource.listProperties();
        var conceptDTO = xmlToDTO(concept, model);

        if (properties.hasNext()) {
            // remove all properties but creation info
            var removedStatements = properties.filterDrop(s -> s.getPredicate().equals(SuomiMeta.creator)
                || s.getPredicate().equals(DCTerms.created))
                    .toList();
            removedStatements.forEach(s -> resource.removeAll(s.getPredicate()));

            // remove all terms
            ConceptMapper.getTermSubjects(resource).forEach(removed -> {
                model.removeAll(resource, null, removed);
                model.removeAll(removed.asResource(), null, null);
            });

            ConceptMapper.dtoToUpdateModel(model, concept.getNumb(), conceptDTO, user);
        } else {
            ConceptMapper.dtoToModel(model, conceptDTO, user);
        }
    }

    private static ConceptDTO xmlToDTO(RECORD concept, ModelWrapper model) {
        var languages = MapperUtils.arrayPropertyToSet(model.getModelResource(), DCTerms.language);

        var dto = new ConceptDTO();
        dto.setIdentifier(concept.getNumb());
        try {
            dto.setStatus(getStatus(concept.getStat()));
        } catch (Exception e) {
            // TODO: add error
            dto.setStatus(Status.DRAFT);
        }
        dto.getSources().addAll(getListContentValue(concept.getSOURC(), model));
        dto.setConceptClass(getSingleContentValue(concept.getCLAS()));

        concept.getLANG().forEach(lang -> {
            var langValue = lang.getValue().value();

            if (!languages.contains(langValue.toLowerCase())) {
                // TODO: add error
                // return;
            }

            dto.getDefinition().computeIfAbsent(langValue, val -> getSingleContentValue(lang.getDEF()));
            dto.getNotes().addAll(getLocalizedList(langValue, lang.getNOTE(), model));
            dto.getExamples().addAll(getLocalizedList(langValue, lang.getEXAMP(), model));

            handleTerm(dto, lang.getTE(), lang.getTE().getStat(), TermType.RECOMMENDED, langValue);
            lang.getSY().forEach(sy -> handleTerm(dto, sy, sy.getStat(), TermType.SYNONYM, langValue));
            lang.getSTE().forEach(ste -> handleTerm(dto, ste, ste.getStat(), TermType.SEARCH_TERM, langValue));
            lang.getDTE().forEach(dte -> handleTerm(dto, dte, dte.getStat(), TermType.NOT_RECOMMENDED, langValue));
            lang.getDTEA().forEach(dtea -> handleTerm(dto, dtea, dtea.getStat(), TermType.NOT_RECOMMENDED, langValue));
            lang.getDTEB().forEach(dteb -> handleTerm(dto, dteb, dteb.getStat(), TermType.NOT_RECOMMENDED, langValue));
        });
        return dto;
    }

    private static void handleTerm(ConceptDTO conceptDTO, Termcontent term, String status, TermType termType, String lang) {
        var termDTO = new TermDTO();

        StringBuilder termLabel = new StringBuilder();
        term.getTERM().getContent().forEach(c -> {
            if (c instanceof String s) {
                addStringContent(termLabel, s);
            } else if (c instanceof GRAM g) {
                handleGRAM(g, termDTO);
                addStringContent(termLabel, g.getContent());
            }
        });
        termDTO.setScope(getContentValue(term.getSCOPE()));

        if (term.getHOGR() != null) {
            termDTO.setHomographNumber(Integer.parseInt(term.getHOGR()));
        }
        // termDTO.setTermEquivalency(null); // TODO
        termDTO.setTermInfo(term.getADD());
        termDTO.setLabel(termLabel.toString().trim());
        termDTO.setLanguage(lang);
        termDTO.setTermType(termType);

        try {
            termDTO.setStatus(getStatus(status));
        } catch (Exception e) {
            termDTO.setStatus(Status.DRAFT);
        }
        conceptDTO.getTerms().add(termDTO);
    }

    private static Status getStatus(String stat) {
        if (stat == null) {
            return Status.DRAFT;
        }
        return Status.valueOf(stat);
    }

    private static String getContentValue(Object obj) {
        try {
            return obj.getClass().getMethod("getContent").invoke(obj).toString();
        } catch (Exception e) {
            return null;
        }
    }
    private static String getSingleContentValue(List<?> elements) {
        if (elements.isEmpty()) {
            return null;
        }

        try {
            return getContentValue(elements.get(0));
        } catch (Exception e) {
            return null;
        }

    }

    private static List<LocalizedValueDTO> getLocalizedList(String lang, List<?> elements, ModelWrapper model) {
        return getListContentValue(elements, model)
                .stream()
                .map(value -> new LocalizedValueDTO(lang, value))
                .toList();
    }

    private static List<String> getListContentValue(List<?> elements, ModelWrapper model) {
        var result = new ArrayList<String>();
        if (elements.isEmpty()) {
            return result;
        }

        elements.forEach(elem -> {
            try {
                var contentElements = (List<?>)elem.getClass().getMethod("getContent").invoke(elem);
                result.add(getContentWithTags(contentElements, model));
            } catch (Exception e) {
                //
            }
        });

        return result;
    }

    private static String getContentWithTags(List<?> contentElements, ModelWrapper model) {
        StringBuilder content = new StringBuilder();

        contentElements.forEach(c -> {
            if (c instanceof String s) {
                addStringContent(content, s);
            } else if (c instanceof LINK link) {
                addLink(content, link.getHref(), link.getContent(), model);
            } else if (c instanceof JAXBElement<?> el) {
                if (el.getName().toString().equalsIgnoreCase("HOGR")) {
                    content.append(" (").append(el.getValue().toString()).append(")");
                }

                appendHtmlTag(content, el);
            }
        });

        return content.toString()
                .replace(" , ", ", ")
                .replace(" . ", ". ")
                .replaceAll("( )+", " ")
                .trim();
    }

    private static void appendHtmlTag(StringBuilder content, JAXBElement<?> el) {
        var name = el.getName().toString().toLowerCase();
        if (HTML_ELEMENTS.contains(name)) {
            content.append("<")
                    .append(name);

            if (name.equals("br")) {
                content.append(" />");
            } else {
                content.append(">")
                    .append(el.getValue().toString())
                    .append("</")
                    .append(name)
                    .append(">");
            }

        }
    }

    private static void addStringContent(StringBuilder content, String strElement) {
        strElement = escapeStringContent(strElement).trim();

        // Add space after and before if not the first content or element is not empty
        if (content.isEmpty()) {
            content.append(strElement)
                    .append(" ");
        } else if (strElement.isEmpty() && !StringUtils.endsWith(content, " ")) {
            content.append(" ");
        } else if (!strElement.isEmpty()) {
            content.append(" ")
                    .append(strElement)
                    .append(" ");
        }
    }

    private static void addLink(StringBuilder content, String href, List<Serializable> text, ModelWrapper model) {
        href = href.replaceAll("^href:", "");
        if (href.startsWith("#")) {
            var resourceId = href.substring(1);
            var res = model.getResourceById(resourceId);
            if (MapperUtils.hasType(res, SKOS.Collection)) {
                href = TerminologyURI.createConceptCollectionURI(model.getPrefix(), resourceId).getResourceURI();
            } else {
                href = TerminologyURI.createConceptURI(model.getPrefix(), resourceId).getResourceURI();
            }
        }

        StringBuilder linkText = new StringBuilder();
        for (Serializable c : text) {
            if (c instanceof JAXBElement<?> el) {
                if (el.getName().toString().equalsIgnoreCase("HOGR")) {
                    linkText.append(" (")
                            .append(el.getValue().toString())
                            .append(")");
                }
            } else if (c instanceof String s) {
                linkText.append(s.trim());
            }
        }

        content.append("<a href=\"")
                .append(href)
                .append("\">")
                .append(linkText.toString().replace("\n", ""))
                .append("</a>");
    }

    private static String escapeStringContent(String s) {
        Escaper escaper = Escapers.builder()
                .addEscape('&', "&amp;")
                .addEscape('\"', "&quot;")
                .addEscape('\'', "&apos;")
                .addEscape('<', "&lt;")
                .addEscape('>', "&gt;")
                .build();

        return escaper.escape(s);
    }

    private static void handleGRAM(GRAM gt, TermDTO dto) {
        // term conjugation / family
        if ("pl".equalsIgnoreCase(gt.getValue())) {
            dto.setTermConjugation(TermConjugation.PLURAL);
        } else if ("n pl".equalsIgnoreCase(gt.getValue())) {
            dto.setTermConjugation(TermConjugation.PLURAL);
            dto.setTermFamily(TermFamily.NEUTRAL);
        } else if ("f pl".equalsIgnoreCase(gt.getValue())) {
            dto.setTermConjugation(TermConjugation.PLURAL);
            dto.setTermFamily(TermFamily.FEMININE);
        }
        // termFamily
        if ("f".equalsIgnoreCase(gt.getGend())) {
            dto.setTermFamily(TermFamily.FEMININE);
        } else if ("m".equalsIgnoreCase(gt.getGend())) {
            dto.setTermFamily(TermFamily.MASCULINE);
        } else if ("n".equalsIgnoreCase(gt.getGend())) {
            dto.setTermFamily(TermFamily.NEUTRAL);
        }
        // wordClass
        if (gt.getPos() != null && !gt.getPos().isEmpty()) {
            if (gt.getPos().toLowerCase().startsWith("verb")) {
                dto.setWordClass(WordClass.VERB);
            } else if (gt.getPos().toLowerCase().startsWith("adj")) {
                dto.setWordClass(WordClass.ADJECTIVE);
            }
        }
    }

    public static void mapCollection(ModelWrapper model, DIAG collection, YtiUser user) {
        var collectionResource = model.getResourceById(collection.getNumb());

        var dto = new ConceptCollectionDTO();
        var lang = collection.getLang() != null ? collection.getLang() : "fi";
        dto.setLabel(Map.of(lang, collection.getName()));
        dto.setIdentifier(collection.getNumb());

        var members = new HashSet<String>();
        collection.getLINK().forEach(member -> {
            var conceptId = member.getHref().substring(1);
            members.add(TerminologyURI.createConceptURI(model.getPrefix(), conceptId).getResourceURI());
        });
        dto.setMembers(members);

        if (collectionResource.listProperties().hasNext()) {
            ConceptCollectionMapper.dtoToUpdateModel(model, collection.getNumb(), dto, user);
        } else {
            ConceptCollectionMapper.dtoToModel(model, dto, user);
        }
    }
}
