package fi.vm.yti.terminology.api.v2.mapper;

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import fi.vm.yti.common.enums.Status;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.common.util.ModelWrapper;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.v2.dto.*;
import fi.vm.yti.terminology.api.v2.enums.*;
import fi.vm.yti.terminology.api.v2.ntrf.*;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
import jakarta.xml.bind.JAXBElement;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.SKOS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class NTRFMapper {

    private NTRFMapper() {
        // only static methods
    }

    private static final Logger LOG = LoggerFactory.getLogger(NTRFMapper.class);

    // allowed formatting elements according to schema
    private static final List<String> HTML_ELEMENTS = List.of("br", "i", "b", "sup", "sub");

    public static void mapConcept(ModelWrapper model, RECORD concept, YtiUser user) {
        var resource = model.getResourceById(concept.getNumb());
        var conceptDTO = xmlToDTO(concept, model);

        if (resource.listProperties().hasNext()) {
            ConceptMapper.dtoToUpdateModel(model, concept.getNumb(), conceptDTO, user);
        } else {
            ConceptMapper.dtoToModel(model, conceptDTO, user);
        }
    }

    public static void mapCollection(ModelWrapper model, DIAG collection, YtiUser user) {
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

        var collectionResource = model.getResourceById(collection.getNumb());
        if (collectionResource.listProperties().hasNext()) {
            ConceptCollectionMapper.dtoToUpdateModel(model, collection.getNumb(), dto, user);
        } else {
            ConceptCollectionMapper.dtoToModel(model, dto, user);
        }
    }

    private static ConceptDTO xmlToDTO(RECORD concept, ModelWrapper model) {
        var languages = MapperUtils.arrayPropertyToSet(model.getModelResource(), DCTerms.language);

        var dto = new ConceptDTO();
        dto.setIdentifier(concept.getNumb());
        try {
            dto.setStatus(getStatus(concept.getStat()));
        } catch (Exception e) {
            LOG.warn("Invalid status value for concept {}, {}", concept.getNumb(), concept.getStat());
            dto.setStatus(Status.DRAFT);
        }

        concept.getSOURC().forEach(source -> getContentWithTags(source.getContent(), dto, model));
        dto.setConceptClass(getContentWithTags(concept.getCLAS(), dto, model));

        concept.getLANG().forEach(lang -> {
            var langValue = lang.getValue().value();

            if (!languages.contains(langValue.toLowerCase())) {
                LOG.warn("Language {} not added to terminology {}", langValue, model.getPrefix());
                return;
            }

            lang.getDEF().forEach(def -> dto.getDefinition().computeIfAbsent(
                    langValue,
                    value -> getContentWithTags(def.getContent(), dto, model)
            ));
            lang.getNOTE().forEach(note -> dto.getNotes().add(
                    new LocalizedValueDTO(langValue, getContentWithTags(note.getContent(), dto, model)))
            );
            lang.getEXAMP().forEach(example -> dto.getExamples().add(
                    new LocalizedValueDTO(langValue, getContentWithTags(example.getContent(), dto, model)))
            );

            handleTerm(model, dto, lang.getTE(), lang.getTE().getStat(), TermType.RECOMMENDED, langValue);
            lang.getSY().forEach(sy -> handleTerm(model, dto, sy, sy.getStat(), TermType.SYNONYM, langValue));
            lang.getSTE().forEach(ste -> handleTerm(model, dto, ste, ste.getStat(), TermType.SEARCH_TERM, langValue));
            lang.getDTE().forEach(dte -> handleTerm(model, dto, dte, dte.getStat(), TermType.NOT_RECOMMENDED, langValue));
            lang.getDTEA().forEach(dtea -> handleTerm(model, dto, dtea, dtea.getStat(), TermType.NOT_RECOMMENDED, langValue));
            lang.getDTEB().forEach(dteb -> handleTerm(model, dto, dteb, dteb.getStat(), TermType.NOT_RECOMMENDED, langValue));
        });
        addConceptReference(dto, concept.getRCON());
        addConceptReference(dto, concept.getBCON());
        addConceptReference(dto, concept.getNCON());
        addConceptReference(dto, concept.getECON());
        addConceptReference(dto, concept.getRCONEXT());
        addConceptReference(dto, concept.getBCONEXT());
        addConceptReference(dto, concept.getNCONEXT());
        return dto;
    }

    private static void addConceptReference(ConceptDTO conceptDTO, List<?> refs) {
        refs.forEach(ref -> {
            var reference = getConceptReference(ref);
            if (reference != null) {
                conceptDTO.getReferences().add(reference);
            }
        });
    }

    private static ConceptReferenceDTO getConceptReference(Object ref) {
        ConceptReferenceDTO dto = new ConceptReferenceDTO();
        if (ref instanceof RCON rcon) {
            dto.setReferenceType(ReferenceType.RELATED);
            dto.setConceptURI(rcon.getHref());
        } else if (ref instanceof BCON bcon) {
            dto = new ConceptReferenceDTO();
            if ("partitive".equalsIgnoreCase(bcon.getTypr())) {
                dto.setReferenceType(ReferenceType.IS_PART_OF);
            } else {
                dto.setReferenceType(ReferenceType.BROADER);
            }
            dto.setConceptURI(bcon.getHref());
        } else if (ref instanceof NCON ncon) {
            if ("partitive".equalsIgnoreCase(ncon.getTypr())) {
                dto.setReferenceType(ReferenceType.HAS_PART);
            } else {
                dto.setReferenceType(ReferenceType.NARROWER);
            }
            dto.setConceptURI(ncon.getHref());
        } else if (ref instanceof ECON econ) {
            var type = econ.getTypr();
            if ("exactMatch".equalsIgnoreCase(type)) {
                dto.setReferenceType(ReferenceType.EXACT_MATCH);
            } else if ("closeMatch".equalsIgnoreCase(type)) {
                dto.setReferenceType(ReferenceType.CLOSE_MATCH);
            } else {
                return null;
            }
            dto.setConceptURI(econ.getHref());
        } else if (ref instanceof RCONEXT rconext) {
            dto.setReferenceType(ReferenceType.RELATED_MATCH);
            dto.setConceptURI(rconext.getHref());
        } else if (ref instanceof BCONEXT bconext) {
            dto.setReferenceType(ReferenceType.BROAD_MATCH);
            dto.setConceptURI(bconext.getHref());
        } else if (ref instanceof NCONEXT nconext) {
            dto.setReferenceType(ReferenceType.NARROW_MATCH);
            dto.setConceptURI(nconext.getHref());
        } else {
            return null;
        }
        return dto;
    }

    private static void handleTerm(
                               ModelWrapper model,
                               ConceptDTO conceptDTO,
                               Termcontent term,
                               String status,
                               TermType termType,
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
        termDTO.setTermEquivalency(handleEQUI(term.getEQUI()));
        termDTO.setTermInfo(term.getADD());
        termDTO.setLanguage(lang);
        termDTO.setTermType(termType);

        try {
            termDTO.setStatus(getStatus(status));
        } catch (Exception e) {
            LOG.warn("Invalid status value for term in concept {}, {}", conceptDTO.getIdentifier(), status);
            termDTO.setStatus(Status.DRAFT);
        }
        conceptDTO.getTerms().add(termDTO);
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
            var reference = getConceptReference(c);

            if (c instanceof String s) {
                addStringContent(content, s);
            } else if (c instanceof LINK link) {
                addLink(content, link.getHref(), link.getContent(), model);
            } else if (reference != null) {
                try {
                    var linkContent = (List<?>)c.getClass().getMethod("getContent").invoke(c);
                    addLink(content, reference.getConceptURI(), linkContent, model);
                    conceptDTO.getReferences().add(reference);
                } catch (Exception e) {
                    LOG.warn("Invalid concept reference for {}", conceptDTO.getIdentifier());
                }
            } else if (c instanceof JAXBElement<?> el) {
                var name = el.getName().toString().toLowerCase();
                if (name.equalsIgnoreCase("HOGR")) {
                    content.append(" (").append(el.getValue().toString()).append(")");
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
        });

        return content.toString()
                .replace(" , ", ", ")
                .replace(" . ", ". ")
                .replaceAll("( )+", " ")
                .trim();
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
