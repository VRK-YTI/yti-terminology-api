package fi.vm.yti.terminology.api.v2.migration.v1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.common.dto.LinkDTO;
import fi.vm.yti.common.dto.ServiceCategoryDTO;
import fi.vm.yti.common.enums.GraphType;
import fi.vm.yti.common.enums.Status;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.terminology.api.v2.dto.*;
import fi.vm.yti.terminology.api.v2.enums.*;
import fi.vm.yti.terminology.api.v2.mapper.ConceptMapper;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.SKOS;
import org.apache.jena.vocabulary.SKOSXL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class TermedDataMapper {

    private static final Logger LOG = LoggerFactory.getLogger(TermedDataMapper.class);

    public static final String URI_SUOMI_FI = "http://uri.suomi.fi";

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

    public static TerminologyDTO mapTerminology(Resource metaResource, List<ServiceCategoryDTO> allCategories) {
        var terminologyDTO = new TerminologyDTO();

        var terminologyURI = NodeFactory.createURI(MapperUtils.propertyToString(metaResource, Termed.uri));

        terminologyDTO.setPrefix(terminologyURI.getLocalName());
        terminologyDTO.setLanguages(MapperUtils.arrayPropertyToSet(metaResource, DCTerms.language));
        terminologyDTO.setLabel(MapperUtils.localizedPropertyToMap(metaResource, SKOS.prefLabel));
        terminologyDTO.setDescription(MapperUtils.localizedPropertyToMap(metaResource, DCTerms.description));
        terminologyDTO.setStatus(Status.valueOf(MapperUtils.propertyToString(metaResource, Termed.status)));

        var organizations = MapperUtils.arrayPropertyToSet(metaResource, DCTerms.contributor).stream()
                .map(o -> {
                    var parts = o.split("/");
                    return UUID.fromString(parts[parts.length - 1]);
                })
                .collect(Collectors.toSet());
        terminologyDTO.setOrganizations(organizations);

        // fix group URIs stored in Termed :ptvl/v1000 -> :ptvl:v1000
        var groups = MapperUtils.arrayPropertyToSet(metaResource, DCTerms.isPartOf)
                .stream().map(g -> g.replace(":ptvl/", ":ptvl:"))
                .collect(Collectors.toSet());

        var newGroups = new HashSet<String>();
        groups.forEach(group ->
                allCategories.stream()
                        .filter(grp -> grp.getId().equals(group))
                        .findFirst()
                        .ifPresent(g -> newGroups.add(g.getIdentifier()))
        );
        terminologyDTO.setGroups(newGroups);

        var type = GraphType.TERMINOLOGICAL_VOCABULARY;
        if (metaResource.hasProperty(Termed.terminologyType)) {
            type = GraphType.valueOf(MapperUtils.propertyToString(metaResource, Termed.terminologyType));
        }
        terminologyDTO.setGraphType(type);
        terminologyDTO.setContact(MapperUtils.propertyToString(metaResource, Termed.contact));
        return terminologyDTO;
    }

    public static ConceptDTO mapConcept(Model oldData, Resource c, ObjectMapper mapper, String defaultLanguage) {
        var concept = new ConceptDTO();
        concept.setIdentifier(c.getLocalName());
        concept.setStatus(Status.valueOf(MapperUtils.propertyToString(c, Termed.status)));
        concept.setDefinition(MapperUtils.localizedPropertyToMap(c, SKOS.definition));

        var examples = c.listProperties(SKOS.example)
                .mapWith(e -> new LocalizedValueDTO(e.getLanguage(), e.getString()))
                .toList();
        var notes = c.listProperties(SKOS.note)
                .mapWith(e -> new LocalizedValueDTO(e.getLanguage(), e.getString()))
                .toList();
        concept.setExamples(examples);
        concept.setNotes(notes);
        concept.setConceptClass(MapperUtils.propertyToString(c, Termed.conceptClass));
        concept.setHistoryNote(MapperUtils.propertyToString(c, SKOS.historyNote));
        concept.setChangeNote(MapperUtils.propertyToString(c, SKOS.changeNote));
        concept.setSubjectArea(MapperUtils.propertyToString(c, Termed.subjectArea));
        concept.setEditorialNotes(MapperUtils.arrayPropertyToList(c, SKOS.editorialNote));
        concept.setSources(MapperUtils.arrayPropertyToList(c, DCTerms.source));

        var links = MapperUtils.arrayPropertyToList(c, Termed.link).stream().map(l -> {
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

        MapperUtils.arrayPropertyToList(c, SKOSXL.prefLabel)
                .forEach(t -> concept.getRecommendedTerms().add(getTerm(oldData.getResource(t))));

        MapperUtils.arrayPropertyToList(c, Termed.synonym)
                .forEach(t -> concept.getSynonyms().add(getTerm(oldData.getResource(t))));

        MapperUtils.arrayPropertyToList(c, Termed.notRecommended)
                .forEach(t -> concept.getNotRecommendedTerms().add(getTerm(oldData.getResource(t))));

        MapperUtils.arrayPropertyToList(c, Termed.searchTerm)
                .forEach(t -> concept.getSearchTerms().add(getTerm(oldData.getResource(t))));

        ConceptMapper.internalRefProperties.forEach(prop ->
                MapperUtils.arrayPropertyToList(c, prop).forEach(ref -> {
                    ref = fixURI(ref);
                    if (prop.equals(SKOS.broader)) {
                        concept.getBroader().add(ref);
                    } else if (prop.equals(SKOS.narrower)) {
                        concept.getNarrower().add(ref);
                    } else if (prop.equals(SKOS.related)) {
                        concept.getRelated().add(ref);
                    } else if (prop.equals(DCTerms.isPartOf)) {
                        concept.getIsPartOf().add(ref);
                    } else if (prop.equals(DCTerms.hasPart)) {
                        concept.getHasPart().add(ref);
                    }
                }));
        return concept;
    }

    public static String fixURI(String uri) {
        if (uri == null) {
            return null;
        }
        return uri.replace(URI_SUOMI_FI, "https://iri.suomi.fi");
    }

    private static TermDTO getTerm(Resource resource) {
        var dto = new TermDTO();
        var label = resource.getProperty(SKOSXL.literalForm);
        dto.setLanguage(label.getLanguage());
        dto.setLabel(label.getString());
        dto.setStatus(Status.valueOf(MapperUtils.propertyToString(resource, Termed.status)));
        dto.setHistoryNote(MapperUtils.propertyToString(resource, SKOS.historyNote));
        dto.setChangeNote(MapperUtils.propertyToString(resource, SKOS.changeNote));
        dto.setTermFamily(getEnumValue(resource, Termed.termFamily, TermFamily.class));
        dto.setTermConjugation(getEnumValue(resource, Termed.termConjugation, TermConjugation.class));
        dto.setTermEquivalency(getEnumValue(resource, Termed.termEquivalency, TermEquivalency.class));
        dto.setScope(MapperUtils.propertyToString(resource, Termed.scope));
        dto.setTermInfo(MapperUtils.propertyToString(resource, Termed.termInfo));
        dto.setTermStyle(MapperUtils.propertyToString(resource, Termed.termStyle));
        dto.setHomographNumber(MapperUtils.getLiteral(resource, Termed.homographNumber, Integer.class));
        dto.setWordClass(getEnumValue(resource, Termed.wordClass, WordClass.class));
        dto.setEditorialNotes(MapperUtils.arrayPropertyToList(resource, SKOS.editorialNote));
        dto.setSources(MapperUtils.arrayPropertyToList(resource, DCTerms.source));

        return dto;
    }

    private static <E extends Enum<E>> E getEnumValue(Resource resource, Property property, Class<E> e) {
        var obj = resource.getProperty(property);
        String value;
        if (obj != null) {
            value = obj.getString();
        } else {
            return null;
        }

        if (value == null) {
            return null;
        }

        try {
            if (enumValueMap.containsKey(value.toLowerCase())) {
                return (E) enumValueMap.get(value.toLowerCase());
            }
            return Enum.valueOf(e, value.toUpperCase());
        } catch (Exception ex) {
            LOG.error("Invalid enum value {}, {}", obj, e);
            return null;
        }
    }

}
