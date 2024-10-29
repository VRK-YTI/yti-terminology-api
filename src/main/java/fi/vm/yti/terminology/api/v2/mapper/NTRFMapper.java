package fi.vm.yti.terminology.api.v2.mapper;

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import fi.vm.yti.common.Constants;
import fi.vm.yti.common.enums.Status;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.common.util.ModelWrapper;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.v2.dto.ConceptCollectionDTO;
import fi.vm.yti.terminology.api.v2.dto.ConceptDTO;
import fi.vm.yti.terminology.api.v2.dto.LocalizedValueDTO;
import fi.vm.yti.terminology.api.v2.dto.TermDTO;
import fi.vm.yti.terminology.api.v2.enums.TermConjugation;
import fi.vm.yti.terminology.api.v2.enums.TermEquivalency;
import fi.vm.yti.terminology.api.v2.enums.TermFamily;
import fi.vm.yti.terminology.api.v2.enums.WordClass;
import fi.vm.yti.terminology.api.v2.ntrf.*;
import fi.vm.yti.terminology.api.v2.property.Term;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
import jakarta.xml.bind.JAXBElement;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.SKOS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class NTRFMapper {

    private NTRFMapper() {
        // only static methods
    }

    private static final Logger LOG = LoggerFactory.getLogger(NTRFMapper.class);

    // allowed formatting elements according to schema
    private static final List<String> HTML_ELEMENTS = List.of("br", "i", "b", "sup", "sub");

    public static void mapTerminology(VOCABULARY vocabulary, ModelWrapper model, YtiUser user) {
        var elements = vocabulary.getRECORDAndHEADAndDIAG();

        for (var elem : elements) {
            if (elem instanceof RECORD concept) {
                mapConcept(model, concept, user);
            } else if (elem instanceof DIAG collection) {
                mapCollection(model, collection, user);
            }
        }
    }
    private static void mapConcept(ModelWrapper model, RECORD concept, YtiUser user) {
        var resource = model.getResourceById(concept.getNumb());
        var conceptDTO = xmlToDTO(concept, model);

        if (conceptDTO == null) {
            return;
        }

        if (resource.listProperties().hasNext()) {
            ConceptMapper.dtoToUpdateModel(model, concept.getNumb(), conceptDTO, user);
        } else {
            ConceptMapper.dtoToModel(model, conceptDTO, user);
        }
    }

    private static void mapCollection(ModelWrapper model, DIAG collection, YtiUser user) {
        var dto = new ConceptCollectionDTO();
        var lang = collection.getLang() != null ? collection.getLang() : "fi";
        dto.setLabel(Map.of(lang, collection.getName()));
        dto.setIdentifier(collection.getNumb());

        var members = new LinkedHashSet<String>();
        collection.getLINK().forEach(member -> members.add(member.getHref().substring(1)));
        dto.setMembers(members);

        var collectionResource = model.getResourceById(collection.getNumb());
        if (collectionResource.listProperties().hasNext()) {
            ConceptCollectionMapper.dtoToUpdateModel(model, collection.getNumb(), dto, user);
        } else {
            ConceptCollectionMapper.dtoToModel(model, dto, user);
        }
    }

    private static ConceptDTO xmlToDTO(RECORD concept, ModelWrapper model) {
        var languages = MapperUtils.arrayPropertyToSet(model.getModelResource(), DCTerms.language);

        if ("ulottuvuus".equalsIgnoreCase(concept.getType())) {
            LOG.info("Drop concept {} with type=ulottuvuus", concept.getNumb());
            return null;
        }

        var dto = new ConceptDTO();

        dto.setIdentifier(concept.getNumb());
        dto.setStatus(getConceptStatus(concept));

        if (!concept.getCLAS().isEmpty()) {
            dto.setConceptClass(getContentWithTags(concept.getCLAS().get(0).getContent(), dto, model));
        }
        if (!concept.getSUBJ().isEmpty()) {
            dto.setSubjectArea(getContentWithTags(concept.getSUBJ().get(0).getContent(), dto, model));
        }
        handleEditorialNotes(concept, model, dto);
        concept.getSOURC().forEach(source -> dto.getSources().add(getContentWithTags(source.getContent(), dto, model)));

        concept.getLANG().forEach(lang -> {
            var langValue = lang.getValue().value();

            if (!languages.contains(langValue.toLowerCase())) {
                LOG.warn("Language {} in concept {} not added to terminology {}", langValue, concept.getNumb(), model.getPrefix());
                return;
            }

            lang.getDEF().forEach(def -> dto.getDefinition().computeIfAbsent(
                    langValue,
                    value -> getContentWithTags(def.getContent(), dto, model)
            ));

            // Notes and examples are reversed to preserve the order in xml file.
            // In the UI notes and examples are represented in latest first
            var notes = lang.getNOTE();
            Collections.reverse(notes);
            notes.forEach(note -> dto.getNotes().add(
                    new LocalizedValueDTO(langValue, getContentWithTags(note.getContent(), dto, model)))
            );
            var examples = lang.getEXAMP();
            Collections.reverse(examples);
            examples.forEach(example -> dto.getExamples().add(
                    new LocalizedValueDTO(langValue, getContentWithTags(example.getContent(), dto, model)))
            );

            handleTerm(model, dto, lang.getTE(), SKOS.prefLabel, langValue);
            lang.getSY().forEach(sy -> handleTerm(model, dto, sy, SKOS.altLabel, langValue));
            lang.getSTE().forEach(ste -> handleTerm(model, dto, ste, SKOS.hiddenLabel, langValue));
            lang.getDTE().forEach(dte -> handleTerm(model, dto, dte, Term.notRecommendedSynonym, langValue));
            lang.getDTEA().forEach(dtea -> handleTerm(model, dto, dtea, Term.notRecommendedSynonym, langValue));
            lang.getDTEB().forEach(dteb -> handleTerm(model, dto, dteb, Term.notRecommendedSynonym, langValue));
        });

        addConceptReferences(model.getPrefix(), dto, concept.getRCON());
        addConceptReferences(model.getPrefix(), dto, concept.getBCON());
        addConceptReferences(model.getPrefix(), dto, concept.getNCON());
        addConceptReferences(model.getPrefix(), dto, concept.getECON());
        addConceptReferences(model.getPrefix(), dto, concept.getRCONEXT());
        addConceptReferences(model.getPrefix(), dto, concept.getBCONEXT());
        addConceptReferences(model.getPrefix(), dto, concept.getNCONEXT());
        return dto;
    }

    private static void handleEditorialNotes(RECORD concept, ModelWrapper model, ConceptDTO dto) {
        concept.getREMK().forEach(remk -> dto.getEditorialNotes().add(getContentWithTags(remk.getContent(), dto, model)));
        var conceptType = concept.getType();
        var additionalNote = new ArrayList<String>();
        if ("aputermi".equalsIgnoreCase(conceptType)) {
            additionalNote.add(conceptType);
        }
        if (concept.getUpda() != null) {
            var upd = concept.getUpda().split(",");
            if (upd.length == 2) {
                DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                try {
                    var lastModifiedDate = LocalDate.parse(upd[1].trim(), df);

                    additionalNote.add("- Viimeksi muokattu, " + lastModifiedDate);
                } catch (DateTimeParseException dex) {
                    LOG.warn("");
                }
            }
        }
        if (!additionalNote.isEmpty()) {
            dto.getEditorialNotes().add(String.join(" ", additionalNote));
        }
    }

    private static Status getConceptStatus(RECORD concept) {
        try {
            if (concept.getStat() != null) {
                return getStatus(concept.getStat());
            } else if ("vanhentunut".equalsIgnoreCase(concept.getType())) {
                return Status.RETIRED;
            } else if (concept.getCHECK() != null) {
                return Status.DRAFT;
            }
        } catch (Exception e) {
            LOG.warn("Invalid status value for concept {}, {}", concept.getNumb(), concept.getStat());
        }
        return Status.DRAFT;
    }

    private static void addConceptReferences(String prefix, ConceptDTO dto, List<?> refs) {
        refs.forEach(ref -> addAndReturnConceptReference(ref, dto, prefix));
    }

    private static String addAndReturnConceptReference(Object ref, ConceptDTO dto, String prefix) {
        String referenceURI;
        if (ref instanceof RCON rcon) {
            referenceURI = fixURI(rcon.getHref(), prefix);
            dto.getRelated().add(referenceURI);
        } else if (ref instanceof BCON bcon) {
            referenceURI = fixURI(bcon.getHref(), prefix);
            if ("partitive".equalsIgnoreCase(bcon.getTypr())) {
                dto.getIsPartOf().add(referenceURI);
            } else {
                dto.getBroader().add(referenceURI);
            }
        } else if (ref instanceof NCON ncon) {
            referenceURI = fixURI(ncon.getHref(), prefix);
            if ("partitive".equalsIgnoreCase(ncon.getTypr())) {
                dto.getHasPart().add(referenceURI);
            } else {
                dto.getNarrower().add(referenceURI);
            }
        } else if (ref instanceof ECON econ) {
            referenceURI = fixURI(econ.getHref(), prefix);
            var type = econ.getTypr();
            if ("exactMatch".equalsIgnoreCase(type)) {
                dto.getExactMatch().add(referenceURI);
            } else if ("closeMatch".equalsIgnoreCase(type)) {
                dto.getCloseMatch().add(referenceURI);
            } else {
                return null;
            }
        } else if (ref instanceof RCONEXT rconext) {
            referenceURI = fixURI(rconext.getHref(), prefix);
            dto.getRelatedMatch().add(referenceURI);
        } else if (ref instanceof BCONEXT bconext) {
            referenceURI = fixURI(bconext.getHref(), prefix);
            dto.getBroadMatch().add(referenceURI);
        } else if (ref instanceof NCONEXT nconext) {
            referenceURI = fixURI(nconext.getHref(), prefix);
            dto.getNarrowMatch().add(referenceURI);
        } else {
            return null;
        }

        return referenceURI;
    }

    private static String fixURI(String uri, String prefix) {
        if (uri.startsWith("#")) {
            var identifier = uri.substring(1);
            return TerminologyURI.createConceptURI(prefix, identifier).getResourceURI();
        } else if (uri.contains("uri.suomi.fi/terminology")) {
            return uri.replaceAll("^https?://uri.suomi.fi/terminology/", Constants.TERMINOLOGY_NAMESPACE);
        } else {
            return uri;
        }
    }

    private static void handleTerm(
            ModelWrapper model,
            ConceptDTO conceptDTO,
            Termcontent term,
            Property termProperty,
            String lang) {
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
        termDTO.setLabel(termLabel.toString().trim());

        if (term.getSCOPE() != null) {
            termDTO.setScope(getContentWithTags(term.getSCOPE().getContent(), conceptDTO, model));
        }
        if (term.getHOGR() != null) {
            termDTO.setHomographNumber(Integer.parseInt(term.getHOGR()));
        }
        if (termProperty.equals(SKOS.prefLabel) && term.getSOURF() != null) {
            var termSource = getContentWithTags(term.getSOURF().getContent(), conceptDTO, model);
            conceptDTO.getSources().add(termSource);
        }
        termDTO.setTermEquivalency(handleEQUI(term.getEQUI()));
        termDTO.setTermInfo(term.getADD());
        termDTO.setLanguage(lang);

        try {
            var status = term.getClass().getMethod("getStat").invoke(term);
            termDTO.setStatus(status != null
                    ? getStatus(status.toString())
                    : Status.DRAFT);
        } catch (Exception e) {
            LOG.warn("Invalid status value for term in concept {}", conceptDTO.getIdentifier());
            termDTO.setStatus(Status.DRAFT);
        }

        if (termProperty.equals(SKOS.prefLabel)) {
            conceptDTO.getRecommendedTerms().add(termDTO);
        } else if (termProperty.equals(SKOS.altLabel)) {
            conceptDTO.getSynonyms().add(termDTO);
        } else if (termProperty.equals(SKOS.hiddenLabel)) {
            conceptDTO.getSearchTerms().add(termDTO);
        } else {
            conceptDTO.getNotRecommendedTerms().add(termDTO);
        }
    }

    private static TermEquivalency handleEQUI(EQUI equi) {
        if (equi == null) {
            return null;
        }

        if (equi.getValue().equalsIgnoreCase("broader")) {
            return TermEquivalency.BROADER;
        } else if (equi.getValue().equalsIgnoreCase("narrower")) {
            return TermEquivalency.NARROWER;
        } else if (equi.getValue().equalsIgnoreCase("near-equivalent")) {
            return TermEquivalency.CLOSE;
        }
        return null;
    }

    private static void handleGRAM(GRAM gt, TermDTO dto) {
        // term conjugation / family
        if ("pl".equalsIgnoreCase(gt.getValue())) {
            dto.setTermConjugation(TermConjugation.PLURAL);
        } else if ("n pl".equalsIgnoreCase(gt.getValue())) {
            dto.setTermConjugation(TermConjugation.PLURAL);
            dto.setTermFamily(TermFamily.NEUTER);
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
            dto.setTermFamily(TermFamily.NEUTER);
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

    private static Status getStatus(String stat) {
        if (stat == null) {
            return Status.DRAFT;
        } else if (stat.equalsIgnoreCase("vanhentunut")) {
            return Status.RETIRED;
        }

        return Status.valueOf(stat);
    }

    private static String getContentWithTags(List<?> contentElements, ConceptDTO conceptDTO, ModelWrapper model) {
        StringBuilder content = new StringBuilder();

        contentElements.forEach(c -> {
            if (c instanceof String s) {
                addStringContent(content, s);
            } else if (c instanceof LINK link) {
                addLink(content, link.getHref(), link.getContent(), model);
            } else if (c instanceof JAXBElement<?> el) {
                var name = el.getName().toString().toLowerCase();
                if (name.equalsIgnoreCase("HOGR")) {
                    addHOGR(content, el);
                } else if (HTML_ELEMENTS.contains(name)) {
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

            var reference = addAndReturnConceptReference(c, conceptDTO, model.getPrefix());
            if (reference != null) {
                try {
                    var linkContent = (List<?>) c.getClass().getMethod("getContent").invoke(c);
                    addLink(content, reference, linkContent, model);
                } catch (Exception e) {
                    LOG.warn("Invalid concept reference for {}", conceptDTO.getIdentifier());
                }
            }
        });

        return content.toString()
                .replace(" , ", ", ")
                .replaceAll("\\s\\.\\s?", ". ")
                .trim();
    }

    private static void addHOGR(StringBuilder content, JAXBElement<?> el) {
        var value = el.getValue().toString();
        if (value != null && !value.isBlank()) {
            content.append(" (")
                    .append(value.trim())
                    .append(")");
        }
    }

    private static void addStringContent(StringBuilder content, String strElement) {
        strElement = escapeStringContent(strElement);

        if (content.isEmpty()) {
            content.append(strElement);
        } else if (strElement.isEmpty() && !StringUtils.endsWith(content, " ")) {
            content.append(" ");
        } else if (!strElement.isEmpty()) {
            if (!strElement.startsWith(" ") && !StringUtils.endsWith(content, " ")) {
                content.append(" ");
            }
            content.append(strElement);
        }
    }

    private static void addLink(StringBuilder content, String href, List<?> text, ModelWrapper model) {
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
        for (var c : text) {
            if (c instanceof JAXBElement<?> el) {
                if (el.getName().toString().equalsIgnoreCase("HOGR")) {
                    addHOGR(linkText, el);
                }
            } else if (c instanceof String s) {
                linkText.append(s.trim());
            }
        }
        content.append("<a href=\"")
                .append(href)
                .append("\">")
                .append(escapeStringContent(linkText.toString()))
                .append("</a>");
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
}
