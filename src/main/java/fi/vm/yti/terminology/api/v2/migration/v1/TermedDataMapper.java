package fi.vm.yti.terminology.api.v2.migration.v1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import fi.vm.yti.common.dto.LinkDTO;
import fi.vm.yti.common.enums.GraphType;
import fi.vm.yti.common.enums.Status;
import fi.vm.yti.terminology.api.v2.dto.ConceptCollectionDTO;
import fi.vm.yti.terminology.api.v2.dto.ConceptDTO;
import fi.vm.yti.terminology.api.v2.dto.TermDTO;
import fi.vm.yti.terminology.api.v2.dto.TerminologyDTO;
import fi.vm.yti.terminology.api.v2.enums.TermConjugation;
import fi.vm.yti.terminology.api.v2.enums.TermEquivalency;
import fi.vm.yti.terminology.api.v2.enums.TermFamily;
import fi.vm.yti.terminology.api.v2.enums.WordClass;
import fi.vm.yti.terminology.api.v2.mapper.ConceptMapper;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.SKOS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class TermedDataMapper {

    private static final Logger LOG = LoggerFactory.getLogger(TermedDataMapper.class);

    public static final String URI_SUOMI_FI = "http://uri.suomi.fi";

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final Map<String, Enum<?>> enumValueMap = Map.ofEntries(
            Map.entry("maskuliini", TermFamily.MASCULINE),
            Map.entry("feminiini", TermFamily.FEMININE),
            Map.entry("neutri", TermFamily.NEUTRAL),
            Map.entry("monikko", TermConjugation.PLURAL),
            Map.entry("yksikkö", TermConjugation.SINGULAR),
            Map.entry(">", TermEquivalency.BROADER),
            Map.entry("<", TermEquivalency.NARROWER),
            Map.entry("~", TermEquivalency.CLOSE),
            Map.entry("near-equivalent", TermEquivalency.CLOSE),
            Map.entry("adj.", WordClass.ADJECTIVE),
            Map.entry("verbi", WordClass.VERB),
            Map.entry("verb/verbi", WordClass.VERB)
    );

    private TermedDataMapper() {
    }

    public static TerminologyDTO mapTerminology(JsonNode json) {
        var data = new TermedDataParser(json);

        var terminologyDTO = new TerminologyDTO();

        var uri = data.getString(Termed.uri.getLocalName());
        var uriParts = Arrays.stream(uri.split("/"))
                .filter(p -> !p.isEmpty())
                .toList();

        var prefix = Iterables.getLast(uriParts, null);
        if (Character.isDigit(prefix.charAt(0))) {
            prefix = "a" + prefix;
        }
        terminologyDTO.setPrefix(prefix);
        terminologyDTO.setLanguages(new HashSet<>(data.getListProperty(DCTerms.language.getLocalName())));
        terminologyDTO.setLabel(data.getLocalizedProperty(SKOS.prefLabel.getLocalName()));
        terminologyDTO.setDescription(data.getLocalizedProperty(DCTerms.description.getLocalName()));
        var type = getEnumValue(data, Termed.terminologyType.getLocalName(), GraphType.class);
        terminologyDTO.setStatus(getStatus(data));
        terminologyDTO.setGraphType(type != null ? type : GraphType.TERMINOLOGICAL_VOCABULARY);
        terminologyDTO.setContact(data.getProperty(Termed.contact.getLocalName()));

        var groups = data.getReferenceNodes("inGroup").stream()
                .map(group -> new TermedDataParser(group).getProperty(SKOS.notation.getLocalName()))
                .collect(Collectors.toSet());
        var organizations = data.getReferenceNodes("contributor").stream()
                .map(org -> UUID.fromString(org.get("id").asText()))
                .collect(Collectors.toSet());
        terminologyDTO.setGroups(groups);
        terminologyDTO.setOrganizations(organizations);
        return terminologyDTO;
    }

    public static ConceptDTO mapConcept(JsonNode termedData, String defaultLanguage) {
        var data = new TermedDataParser(termedData);
        var concept = new ConceptDTO();
        concept.setIdentifier(data.getString("code"));
        concept.setStatus(getStatus(data));
        concept.setDefinition(data.getLocalizedProperty(SKOS.definition.getLocalName()));
        concept.setExamples(data.getLocalizedListValue(SKOS.example.getLocalName()));
        concept.setNotes(data.getLocalizedListValue(SKOS.note.getLocalName()));
        concept.setConceptClass(data.getProperty(Termed.conceptClass.getLocalName()));
        concept.setHistoryNote(data.getProperty(SKOS.historyNote.getLocalName()));
        concept.setChangeNote(data.getProperty(SKOS.changeNote.getLocalName()));
        concept.setSubjectArea(data.getProperty(Termed.subjectArea.getLocalName()));
        concept.setEditorialNotes(data.getListProperty(SKOS.editorialNote.getLocalName()));
        concept.setSources(data.getListProperty(DCTerms.source.getLocalName()));

        var links = data.getListProperty(Termed.link.getLocalName()).stream().map(l -> {
            try {
                l = l.replace("\\\"", "\"");
                l = l.replace("\"{", "{");
                l = l.replace("}\"", "}");
                var json = mapper.readTree(l);

                var dto = new LinkDTO();
                dto.setName(Map.of(defaultLanguage, Optional.ofNullable(json.get("name")).map(JsonNode::asText).orElse("")));
                dto.setDescription(Map.of(defaultLanguage, Optional.ofNullable(json.get("description")).map(JsonNode::asText).orElse("")));
                dto.setUri(Optional.ofNullable(json.get("url")).map(JsonNode::asText).orElse(""));
                return dto;
            } catch (JsonProcessingException e) {
                LOG.error("Error parsing links for concept {}, {}", concept.getIdentifier(), l);
                return null;
            }
        }).filter(Objects::nonNull).toList();

        concept.setLinks(links);

        data.getReferenceNodes("prefLabelXl").stream()
                .map(TermedDataMapper::getTerm)
                .filter(Objects::nonNull)
                .forEach(term -> concept.getRecommendedTerms().add(term));
        data.getReferenceNodes("altLabelXl").stream()
                .map(TermedDataMapper::getTerm)
                .filter(Objects::nonNull)
                .forEach(term -> concept.getSynonyms().add(term));
        data.getReferenceNodes("notRecommendedSynonym").stream()
                .map(TermedDataMapper::getTerm)
                .filter(Objects::nonNull)
                .forEach(term -> concept.getNotRecommendedTerms().add(term));
        data.getReferenceNodes("searchTerm").stream()
                .map(TermedDataMapper::getTerm)
                .filter(Objects::nonNull)
                .forEach(term -> concept.getSearchTerms().add(term));

        ConceptMapper.internalRefProperties.forEach(prop -> {
            var references = data.getReferences(prop.getLocalName());
            if (prop.equals(SKOS.broader)) {
                concept.getBroader().addAll(references);
            } else if (prop.equals(SKOS.narrower)) {
                concept.getNarrower().addAll(references);
            } else if (prop.equals(SKOS.related)) {
                concept.getRelated().addAll(references);
            } else if (prop.equals(DCTerms.isPartOf)) {
                concept.getIsPartOf().addAll(references);
            } else if (prop.equals(DCTerms.hasPart)) {
                concept.getHasPart().addAll(references);
            }
        });
        return concept;
    }

    public static ConceptCollectionDTO mapCollection(JsonNode termedData) {
        var data = new TermedDataParser(termedData);

        var dto = new ConceptCollectionDTO();

        var label = data.getLocalizedProperty(SKOS.prefLabel.getLocalName());
        var description = data.getLocalizedProperty(SKOS.definition.getLocalName());

        dto.setIdentifier(data.getString("code"));
        dto.setLabel(label);
        dto.setDescription(description);
        data.getReferences("member").forEach(member ->
                dto.addMember(NodeFactory.createURI(member).getLocalName()));

        return dto;
    }

    public static String fixURI(String uri) {
        if (uri == null) {
            return null;
        }
        uri = uri.replace(URI_SUOMI_FI, "https://iri.suomi.fi");

        // add 'a' to prefix if the old prefix starts with number
        var terminologyURI = TerminologyURI.fromUri(uri);
        if (terminologyURI.getPrefix() != null && Character.isDigit(terminologyURI.getPrefix().charAt(0))) {
            uri = uri.replace("/terminology/", "/terminology/a");
        }
        return uri;
    }

    public static TermDTO getTerm(JsonNode json) {
        var dto = new TermDTO();
        var data = new TermedDataParser(json);
        var label = data.getLocalizedProperty(SKOS.prefLabel.getLocalName());
        if (label == null || label.isEmpty()) {
            var uri = data.getString("uri");
            LOG.warn("Skip term with empty label {}", uri);
            return null;
        }
        var language = label.keySet().iterator().next();
        dto.setLabel(label.get(language));
        dto.setLanguage(language);
        dto.setStatus(getStatus(data));
        dto.setHistoryNote(data.getProperty(SKOS.historyNote.getLocalName()));
        dto.setChangeNote(data.getProperty(SKOS.changeNote.getLocalName()));
        dto.setTermFamily(getEnumValue(data, Termed.termFamily.getLocalName(), TermFamily.class));
        dto.setTermConjugation(getEnumValue(data, Termed.termConjugation.getLocalName(), TermConjugation.class));
        dto.setTermEquivalency(getEnumValue(data, Termed.termEquivalency.getLocalName(), TermEquivalency.class));
        dto.setScope(data.getProperty(Termed.scope.getLocalName()));
        dto.setTermInfo(data.getProperty(Termed.termInfo.getLocalName()));
        dto.setTermStyle(data.getProperty(Termed.termStyle.getLocalName()));

        var homograph = data.getProperty(Termed.homographNumber.getLocalName());
        try {
            if (homograph != null) {
                dto.setHomographNumber(Integer.valueOf(homograph));
            }
        } catch (NumberFormatException e) {
            LOG.warn("Invalid term homograph number {}", homograph);
        }

        dto.setWordClass(getEnumValue(data, "wordClass", WordClass.class));
        dto.setEditorialNotes(data.getListProperty(SKOS.editorialNote.getLocalName()));
        dto.setSources(data.getListProperty(DCTerms.source.getLocalName()));
        return dto;
    }

    private static Status getStatus(TermedDataParser data) {
        var status = getEnumValue(data, "status", Status.class);
        return status != null ? status : Status.DRAFT;
    }

    private static <E extends Enum<E>> E getEnumValue(TermedDataParser parser, String property, Class<E> e) {
        var value = parser.getProperty(property);

        if (value == null) {
            return null;
        }

        try {
            if (enumValueMap.containsKey(value.toLowerCase())) {
                return (E) enumValueMap.get(value.toLowerCase());
            }
            return Enum.valueOf(e, value.toUpperCase());
        } catch (Exception ex) {
            LOG.error("Invalid enum value {}, {}", value, e);
            return null;
        }
    }
}
