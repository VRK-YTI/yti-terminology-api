package fi.vm.yti.terminology.api.v2.mapper;

import fi.vm.yti.common.dto.LinkDTO;
import fi.vm.yti.common.dto.ResourceCommonInfoDTO;
import fi.vm.yti.common.properties.SuomiMeta;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.common.util.ModelWrapper;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.v2.dto.*;
import fi.vm.yti.terminology.api.v2.enums.*;
import fi.vm.yti.terminology.api.v2.opensearch.IndexConcept;
import fi.vm.yti.terminology.api.v2.property.Term;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ConceptMapper {

    private ConceptMapper() {
        // only static methods
    }

    public static final List<Property> internalRefProperties = List.of(SKOS.broader, SKOS.narrower,
            SKOS.related, DCTerms.hasPart, DCTerms.isPartOf);

    public static final List<Property> externalRefProperties = List.of(SKOS.broadMatch, SKOS.narrowMatch,
            SKOS.closeMatch, SKOS.relatedMatch, SKOS.exactMatch);

    public static final List<Property> termProperties = List.of(SKOS.prefLabel, SKOS.altLabel,
            Term.notRecommendedSynonym, SKOS.hiddenLabel);

    public static void dtoToModel(ModelWrapper model, ConceptDTO dto, YtiUser user) {
        var modelResource = model.getModelResource();
        var languages = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.language);

        var conceptResource = model.createResourceWithId(dto.getIdentifier())
                .addProperty(SKOS.inScheme, ResourceFactory.createResource(modelResource.getURI()))
                .addProperty(RDF.type, SKOS.Concept);

        MapperUtils.addStatus(conceptResource, dto.getStatus());

        MapperUtils.addLocalizedProperty(languages, dto.getDefinition(), conceptResource, SKOS.definition);
        MapperUtils.addOptionalStringProperty(conceptResource, Term.subjectArea, dto.getSubjectArea());
        MapperUtils.addOptionalStringProperty(conceptResource, SKOS.changeNote, dto.getChangeNote());
        MapperUtils.addOptionalStringProperty(conceptResource, SKOS.historyNote, dto.getHistoryNote());
        MapperUtils.addOptionalStringProperty(conceptResource, Term.conceptClass, dto.getConceptClass());

        addLocalizedListProperty(conceptResource, SKOS.example, dto.getExamples());
        addLocalizedListProperty(conceptResource, SKOS.note, dto.getNotes());
        addListProperty(conceptResource, SKOS.editorialNote, dto.getEditorialNotes());
        addListProperty(conceptResource, Term.source, dto.getSources());

        handleTerms(model, dto, conceptResource);
        mapReferencesToResource(dto, conceptResource);
        dto.getLinks().forEach(link -> mapLinks(model, link, languages, conceptResource));

        MapperUtils.addCreationMetadata(conceptResource, user);
    }

    public static ConceptInfoDTO modelToDTO(ModelWrapper model, String conceptIdentifier, Consumer<ResourceCommonInfoDTO> mapUser) {
        var resource = model.getResourceById(conceptIdentifier);
        var dto = new ConceptInfoDTO();

        dto.setIdentifier(resource.getLocalName());
        dto.setUri(resource.getURI());
        dto.setStatus(MapperUtils.getStatus(resource));
        dto.setDefinition(MapperUtils.localizedPropertyToMap(resource, SKOS.definition));

        dto.setExamples(listToLocalizedValues(resource, SKOS.example));
        dto.setNotes(listToLocalizedValues(resource, SKOS.note));
        dto.setSubjectArea(MapperUtils.localizedPropertyToMap(resource, Term.subjectArea));

        dto.setReferences(mapConceptReferencesDTO(model, resource));

        var links = resource.listProperties(RDFS.seeAlso).mapWith(l -> {
            var link = new LinkDTO();
            link.setUri(MapperUtils.propertyToString(l.getResource(), FOAF.homepage));
            link.setName(MapperUtils.localizedPropertyToMap(l.getResource(), DCTerms.title));
            link.setDescription(MapperUtils.localizedPropertyToMap(l.getResource(), DCTerms.description));
            return link;
        });
        dto.setLinks(links.toList());
        dto.setSources(listToValues(resource, Term.source));

        dto.setChangeNote(MapperUtils.propertyToString(resource, SKOS.changeNote));
        dto.setHistoryNote(MapperUtils.propertyToString(resource, SKOS.historyNote));

        // if mapUser is provided, user has permissions to the model
        if (mapUser != null) {
            dto.setEditorialNotes(listToValues(resource, SKOS.editorialNote));
        }

        dto.setConceptClass(MapperUtils.propertyToString(resource, Term.conceptClass));

        dto.setRecommendedTerms(mapTermDTO(resource, SKOS.prefLabel));
        dto.setSynonyms(mapTermDTO(resource, SKOS.altLabel));
        dto.setNotRecommendedTerms(mapTermDTO(resource, Term.notRecommendedSynonym));
        dto.setSearchTerms(mapTermDTO(resource, SKOS.hiddenLabel));

        MapperUtils.mapCreationInfo(dto, resource, mapUser);
        return dto;
    }

    public static void dtoToUpdateModel(ModelWrapper model, String conceptIdentifier, ConceptDTO dto, YtiUser user) {
        var conceptResource = model.getResourceById(conceptIdentifier);
        var languages = MapperUtils.arrayPropertyToSet(model.getModelResource(), DCTerms.language);

        conceptResource.removeAll(SuomiMeta.publicationStatus);
        conceptResource.removeAll(RDFS.seeAlso);

        MapperUtils.addStatus(conceptResource, dto.getStatus());

        MapperUtils.updateLocalizedProperty(languages, dto.getDefinition(), conceptResource, SKOS.definition);
        MapperUtils.updateStringProperty(conceptResource, Term.subjectArea, dto.getSubjectArea());
        MapperUtils.updateStringProperty(conceptResource, SKOS.changeNote, dto.getChangeNote());
        MapperUtils.updateStringProperty(conceptResource, SKOS.historyNote, dto.getHistoryNote());
        MapperUtils.updateStringProperty(conceptResource, Term.conceptClass, dto.getConceptClass());

        addLocalizedListProperty(conceptResource, SKOS.example, dto.getExamples());
        addLocalizedListProperty(conceptResource, SKOS.note, dto.getNotes());
        addListProperty(conceptResource, SKOS.editorialNote, dto.getEditorialNotes());
        addListProperty(conceptResource, Term.source, dto.getSources());

        mapReferencesToResource(dto, conceptResource);

        // remove terms not included to payload
        var termURIs = dto.getTerms().stream()
                .filter(t -> t.getIdentifier() != null)
                .map(t -> model.getModelResource().getNameSpace() + t.getIdentifier())
                .toList();

        var removedTerms = getTermSubjects(conceptResource).stream()
                .filter(t -> !termURIs.contains(t.getURI()))
                .toList();

        removedTerms.forEach(removed -> {
            model.removeAll(conceptResource, null, removed);
            model.removeAll(removed.asResource(), null, null);
        });

        handleTerms(model, dto, conceptResource);
        dto.getLinks().forEach(link -> mapLinks(model, link, languages, conceptResource));

        MapperUtils.addUpdateMetadata(conceptResource, user);
    }

    public static IndexConcept toIndexDocument(ModelWrapper model, String identifier) {
        var resource = model.getResourceById(identifier);

        var prefLabels = getListProperty(resource, SKOS.prefLabel)
                .stream().collect(Collectors.toMap(
                        r -> r.getProperty(SKOSXL.literalForm).getLanguage(),
                        r -> r.getProperty(SKOSXL.literalForm).getString()));

        var indexConcept = new IndexConcept();
        indexConcept.setId(resource.getURI());
        indexConcept.setUri(resource.getURI());
        indexConcept.setStatus(MapperUtils.getStatus(resource));
        indexConcept.setNamespace(resource.getNameSpace());
        indexConcept.setLabel(prefLabels);
        indexConcept.setAltLabel(getIndexedTerm(resource, SKOS.altLabel));
        indexConcept.setNotRecommendedSynonym(getIndexedTerm(resource, Term.notRecommendedSynonym));
        indexConcept.setSearchTerm(getIndexedTerm(resource, SKOS.hiddenLabel));
        indexConcept.setDefinition(MapperUtils.localizedPropertyToMap(resource, SKOS.definition));
        indexConcept.setCreated(MapperUtils.getLiteral(resource, DCTerms.created, String.class));
        indexConcept.setModified(MapperUtils.getLiteral(resource, DCTerms.modified, String.class));

        return indexConcept;
    }

    public static void mapDeleteConcept(ModelWrapper model, String identifier) {
        var resource = model.getResourceById(identifier);

        getTermSubjects(resource).forEach(term -> model.removeAll(term, null, null));
        model.removeAll(null, null, resource);
        model.removeAll(resource, null, null);
    }

    public static List<Resource> getTermSubjects(Resource conceptResource) {
        return termProperties.stream()
                .flatMap(prop -> conceptResource.listProperties(prop)
                        .mapWith(Statement::getObject)
                        .toList().stream())
                .map(RDFNode::asResource)
                .toList();
    }

    private static Map<String, List<String>> getIndexedTerm(Resource resource, Property property) {
        return getListProperty(resource, property).stream()
                .filter(r -> r.hasProperty(SKOSXL.literalForm))
                .collect(Collectors.groupingBy(
                        r -> r.getProperty(SKOSXL.literalForm).getLanguage(),
                        Collectors.mapping(r -> r.getProperty(SKOSXL.literalForm).getString(), Collectors.toList())));
    }

    private static Set<ConceptReferenceInfoDTO> mapConceptReferencesDTO(ModelWrapper model, Resource resource) {
        var references = new HashSet<ConceptReferenceInfoDTO>();

        internalRefProperties.forEach(prop -> {
            var ref = MapperUtils.propertyToString(resource, prop);

            if (ref != null) {
                var dto = new ConceptReferenceInfoDTO();
                dto.setConceptURI(ref);

                var label = new HashMap<String, String>();
                getListProperty(model.getResource(ref), SKOS.prefLabel).forEach(r -> {
                    var value = r.getProperty(SKOSXL.literalForm);
                    label.put(value.getLanguage(), value.getString());
                });
                dto.setLabel(label);
                dto.setReferenceType(ReferenceType.getByPropertyName(prop.getLocalName()));
                references.add(dto);
            }
        });

        // at this point, populate URI and type for external references,
        // labels for them must be fetched from index / fuseki
        externalRefProperties.forEach(prop -> {
            var ref = MapperUtils.propertyToString(resource, prop);
            if (ref != null) {
                var dto = new ConceptReferenceInfoDTO();
                dto.setConceptURI(ref);
                dto.setReferenceType(ReferenceType.getByPropertyName(prop.getLocalName()));
                references.add(dto);
            }
        });
        return references;
    }

    private static void addLocalizedListProperty(Resource resource, Property property, List<LocalizedValueDTO> values) {
        resource.removeAll(property);
        if (values.isEmpty()) {
            return;
        }
        var list = resource.getModel().createList(values.stream()
                .map(e -> ResourceFactory.createLangLiteral(e.getValue(), e.getLanguage()))
                .iterator());
        resource.addProperty(property, list);
    }

    private static void addListProperty(Resource resource, Property property, List<String> values) {
        resource.removeAll(property);
        if (values.isEmpty()) {
            return;
        }
        var list = resource.getModel().createList(values.stream()
                .map(ResourceFactory::createStringLiteral)
                .iterator());
        resource.addProperty(property, list);
    }

    private static void addResourceListProperty(Resource resource, Property property, List<Resource> resources) {
        resource.removeAll(property);
        if (resources.isEmpty()) {
            return;
        }
        var list = resource.getModel().createList(resources.stream().iterator());
        resource.addProperty(property, list);
    }

    private static void mapReferencesToResource(ConceptDTO dto, Resource resource) {
        internalRefProperties.forEach(resource::removeAll);
        externalRefProperties.forEach(resource::removeAll);

        dto.getReferences().forEach(ref -> {
            switch (ref.getReferenceType()) {
                case BROADER -> MapperUtils.addOptionalUriProperty(resource, SKOS.broader, ref.getConceptURI());
                case NARROWER -> MapperUtils.addOptionalUriProperty(resource, SKOS.narrower, ref.getConceptURI());
                case RELATED -> MapperUtils.addOptionalUriProperty(resource, SKOS.related, ref.getConceptURI());
                case IS_PART_OF -> MapperUtils.addOptionalUriProperty(resource, DCTerms.isPartOf, ref.getConceptURI());
                case HAS_PART -> MapperUtils.addOptionalUriProperty(resource, DCTerms.hasPart, ref.getConceptURI());
                case BROAD_MATCH -> MapperUtils.addOptionalUriProperty(resource, SKOS.broadMatch, ref.getConceptURI());
                case NARROW_MATCH ->
                        MapperUtils.addOptionalUriProperty(resource, SKOS.narrowMatch, ref.getConceptURI());
                case CLOSE_MATCH -> MapperUtils.addOptionalUriProperty(resource, SKOS.closeMatch, ref.getConceptURI());
                case RELATED_MATCH ->
                        MapperUtils.addOptionalUriProperty(resource, SKOS.relatedMatch, ref.getConceptURI());
                case EXACT_MATCH -> MapperUtils.addOptionalUriProperty(resource, SKOS.exactMatch, ref.getConceptURI());
            }
        });
    }

    private static void mapLinks(ModelWrapper model, LinkDTO link, Set<String> languages, Resource resource) {
        var linkResource = model.createResource();
        MapperUtils.addLocalizedProperty(languages, link.getName(), linkResource, DCTerms.title);
        MapperUtils.addLocalizedProperty(languages, link.getDescription(), linkResource, DCTerms.description);
        linkResource.addProperty(FOAF.homepage, link.getUri());

        resource.addProperty(RDFS.seeAlso, linkResource);
    }

    private static void handleTerms(ModelWrapper model, ConceptDTO dto, Resource conceptResource) {
        addResourceListProperty(conceptResource, SKOS.prefLabel,
                dto.getRecommendedTerms().stream()
                        .map(term -> mapTerm(model, term))
                        .toList());

        addResourceListProperty(conceptResource, SKOS.altLabel,
                dto.getSynonyms().stream()
                        .map(term -> mapTerm(model, term))
                        .toList());

        addResourceListProperty(conceptResource, Term.notRecommendedSynonym,
                dto.getNotRecommendedTerms().stream()
                        .map(term -> mapTerm(model, term))
                        .toList());

        addResourceListProperty(conceptResource, SKOS.hiddenLabel,
                dto.getSearchTerms().stream()
                        .map(term -> mapTerm(model, term))
                        .toList());
    }

    private static Resource mapTerm(ModelWrapper model, TermDTO term) {
        var termResource = model.createResource();
        termResource.addProperty(RDF.type, SKOSXL.Label);
        termResource.addProperty(SKOSXL.literalForm, ResourceFactory.createLangLiteral(term.getLabel(), term.getLanguage()));
        MapperUtils.addLiteral(termResource, Term.homographNumber, term.getHomographNumber());
        MapperUtils.addStatus(termResource, term.getStatus());
        MapperUtils.addOptionalStringProperty(termResource, Term.termInfo, term.getTermInfo());
        MapperUtils.addOptionalStringProperty(termResource, Term.scope, term.getScope());
        MapperUtils.addOptionalStringProperty(termResource, SKOS.historyNote, term.getHistoryNote());
        MapperUtils.addOptionalStringProperty(termResource, SKOS.changeNote, term.getChangeNote());
        MapperUtils.addOptionalStringProperty(termResource, Term.termStyle, term.getTermStyle());
        addOptionalEnumProperty(termResource, Term.termFamily, term.getTermFamily());
        addOptionalEnumProperty(termResource, Term.termConjugation, term.getTermConjugation());
        addOptionalEnumProperty(termResource, Term.termEquivalency, term.getTermEquivalency());
        addOptionalEnumProperty(termResource, Term.wordClass, term.getWordClass());
        term.getEditorialNotes().forEach(e -> termResource.addProperty(SKOS.editorialNote, e));
        term.getSources().forEach(e -> termResource.addProperty(DCTerms.source, e));

        return termResource;
    }

    private static Set<TermDTO> mapTermDTO(Resource resource, Property property) {

        var terms = new LinkedHashSet<TermDTO>();

        var termResources = getListProperty(resource, property);

        termResources.forEach(termResource -> {
            var label = termResource.getProperty(SKOSXL.literalForm).getObject().asLiteral();

            var term = new TermDTO();
            term.setIdentifier(termResource.getLocalName());
            term.setStatus(MapperUtils.getStatus(termResource));
            term.setLanguage(label.getLanguage());
            term.setHomographNumber(MapperUtils.getLiteral(termResource, Term.homographNumber, Integer.class));

            term.setLabel(label.getString());
            term.setTermInfo(MapperUtils.propertyToString(termResource, Term.termInfo));
            term.setChangeNote(MapperUtils.propertyToString(termResource, SKOS.changeNote));
            term.setHistoryNote(MapperUtils.propertyToString(termResource, SKOS.historyNote));
            term.setScope(MapperUtils.propertyToString(termResource, Term.scope));

            term.setTermConjugation(getEnumValue(termResource, Term.termConjugation, TermConjugation.class));
            term.setTermEquivalency(getEnumValue(termResource, Term.termEquivalency, TermEquivalency.class));
            term.setWordClass(getEnumValue(termResource, Term.wordClass, WordClass.class));
            term.setTermStyle(MapperUtils.propertyToString(termResource, Term.termStyle));
            term.setTermFamily(getEnumValue(termResource, Term.termFamily, TermFamily.class));

            term.setSources(MapperUtils.arrayPropertyToList(termResource, DCTerms.source));
            term.setEditorialNotes(MapperUtils.arrayPropertyToList(termResource, SKOS.editorialNote));
            terms.add(term);
        });

        return terms;
    }

    private static List<Resource> getListProperty(Resource resource, Property property) {
        if (!resource.hasProperty(property)) {
            return new ArrayList<>();
        }
        return resource.getProperty(property)
                .getList()
                .asJavaList()
                .stream()
                .map(RDFNode::asResource)
                .toList();
    }

    private static void addOptionalEnumProperty(Resource resource, Property property, Enum<?> e) {
        resource.removeAll(property);
        if (e == null) {
            return;
        }
        MapperUtils.addOptionalStringProperty(resource, property, e.name());
    }

    private static List<LocalizedValueDTO> listToLocalizedValues(Resource resource, Property property) {
        if (!resource.hasProperty(property)) {
            return List.of();
        }
        return resource.getProperty(property)
                .getList().asJavaList().stream()
                .map(RDFNode::asLiteral)
                .map(o -> new LocalizedValueDTO(o.getLanguage(), o.getString()))
                .toList();
    }

    private static List<String> listToValues(Resource resource, Property property) {
        if (!resource.hasProperty(property)) {
            return List.of();
        }
        return resource.getProperty(property)
                .getList().asJavaList().stream()
                .map(s -> s.asLiteral().getString())
                .toList();
    }

    private static <E extends Enum<E>> E getEnumValue(Resource resource, Property property, Class<E> e) {
        var value = MapperUtils.propertyToString(resource, property);
        if (value == null) {
            return null;
        }
        return Enum.valueOf(e, value);
    }
}
