package fi.vm.yti.terminology.api.v2.mapper;

import fi.vm.yti.common.dto.LinkDTO;
import fi.vm.yti.common.dto.ResourceCommonInfoDTO;
import fi.vm.yti.common.exception.ResourceNotFoundException;
import fi.vm.yti.common.properties.SuomiMeta;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.common.util.ModelWrapper;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.v2.dto.*;
import fi.vm.yti.terminology.api.v2.enums.TermConjugation;
import fi.vm.yti.terminology.api.v2.enums.TermEquivalency;
import fi.vm.yti.terminology.api.v2.enums.TermFamily;
import fi.vm.yti.terminology.api.v2.enums.WordClass;
import fi.vm.yti.terminology.api.v2.opensearch.IndexConcept;
import fi.vm.yti.terminology.api.v2.property.Term;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
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

    public static final Map<String, Property> orderProperties = Map.ofEntries(
            Map.entry(SKOS.broader.getLocalName(), Term.orderedBroader),
            Map.entry(SKOS.narrower.getLocalName(), Term.orderedNarrower),
            Map.entry(SKOS.related.getLocalName(), Term.orderedRelated),
            Map.entry(DCTerms.isPartOf.getLocalName(), Term.orderedIsPartOf),
            Map.entry(DCTerms.hasPart.getLocalName(), Term.orderedHasPart),
            Map.entry(SKOS.broadMatch.getLocalName(), Term.orderedBroadMatch),
            Map.entry(SKOS.narrowMatch.getLocalName(), Term.orderedNarrowMatch),
            Map.entry(SKOS.relatedMatch.getLocalName(), Term.orderedRelatedMatch),
            Map.entry(SKOS.exactMatch.getLocalName(), Term.orderedExactMatch),
            Map.entry(SKOS.closeMatch.getLocalName(), Term.orderedCloseMatch),
            Map.entry(SKOS.altLabel.getLocalName(), Term.orderedSynonym),
            Map.entry(Term.notRecommendedSynonym.getLocalName(), Term.orderedNotRecommendedSynonym),
            Map.entry(SKOS.member.getLocalName(), Term.orderedMember)
    );

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
        addLiteralListProperty(conceptResource, SKOS.editorialNote, dto.getEditorialNotes());
        addLiteralListProperty(conceptResource, DCTerms.source, dto.getSources());

        handleTerms(model, dto, conceptResource);
        mapReferencesToResource(model, dto, conceptResource);
        mapLinks(model, dto, languages, conceptResource);

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
        dto.setSubjectArea(MapperUtils.propertyToString(resource, Term.subjectArea));

        dto.setBroader(mapConceptReferencesDTO(model, resource, SKOS.broader));
        dto.setNarrower(mapConceptReferencesDTO(model, resource, SKOS.narrower));
        dto.setHasPart(mapConceptReferencesDTO(model, resource, DCTerms.hasPart));
        dto.setIsPartOf(mapConceptReferencesDTO(model, resource, DCTerms.isPartOf));
        dto.setRelated(mapConceptReferencesDTO(model, resource, SKOS.related));
        dto.setBroadMatch(mapConceptReferencesDTO(model, resource, SKOS.broadMatch));
        dto.setNarrowMatch(mapConceptReferencesDTO(model, resource, SKOS.narrowMatch));
        dto.setExactMatch(mapConceptReferencesDTO(model, resource, SKOS.exactMatch));
        dto.setCloseMatch(mapConceptReferencesDTO(model, resource, SKOS.closeMatch));
        dto.setRelatedMatch(mapConceptReferencesDTO(model, resource, SKOS.relatedMatch));

        var links = MapperUtils.getResourceList(resource, RDFS.seeAlso).stream()
                .map(l -> {
                    var link = new LinkDTO();
                    link.setUri(MapperUtils.propertyToString(l, FOAF.homepage));
                    link.setName(MapperUtils.localizedPropertyToMap(l, DCTerms.title));
                    link.setDescription(MapperUtils.localizedPropertyToMap(l, DCTerms.description));
                    return link;
                });
        dto.setLinks(links.toList());
        dto.setSources(listToValues(resource, DCTerms.source));

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
        addLiteralListProperty(conceptResource, SKOS.editorialNote, dto.getEditorialNotes());
        addLiteralListProperty(conceptResource, DCTerms.source, dto.getSources());

        mapReferencesToResource(model, dto, conceptResource);
        handleTerms(model, dto, conceptResource);
        mapLinks(model, dto, languages, conceptResource);

        MapperUtils.addUpdateMetadata(conceptResource, user);
    }

    public static IndexConcept toIndexDocument(ModelWrapper model, String identifier) {
        var resource = model.getResourceById(identifier);

        var prefLabels = resource.listProperties(SKOS.prefLabel).toList()
                .stream().collect(Collectors.toMap(
                        r -> r.getProperty(SKOSXL.literalForm).getLanguage(),
                        r -> r.getProperty(SKOSXL.literalForm).getString()));

        var indexConcept = new IndexConcept();
        indexConcept.setId(resource.getURI());
        indexConcept.setUri(resource.getURI());
        indexConcept.setIdentifier(resource.getLocalName());
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

        if (!resource.listProperties().hasNext()) {
            throw new ResourceNotFoundException(identifier);
        }

        // remove all term resources
        termProperties.forEach(term ->
                resource.listProperties(term)
                        .mapWith(Statement::getResource)
                        .forEach(termResource -> {
                            MapperUtils.removeAllLists(termResource);
                            model.removeAll(termResource, null, null);
                        }));

        MapperUtils.removeAllLists(resource);
        model.removeAll(null, null, resource);
        model.removeAll(resource, null, null);
    }

    private static List<String> getIndexedTerm(Resource resource, Property property) {
        return resource.listProperties(property)
                .mapWith(Statement::getResource)
                .toList().stream()
                    .filter(r -> r.hasProperty(SKOSXL.literalForm))
                    .map(r -> r.getProperty(SKOSXL.literalForm).getString())
                    .toList();
    }

    private static Set<ConceptReferenceInfoDTO> mapConceptReferencesDTO(Model model, Resource resource, Property property) {
        var result = new LinkedHashSet<ConceptReferenceInfoDTO>();

        var orderProperty = orderProperties.get(property.getLocalName());

        var referenceList = orderProperty != null
                ? MapperUtils.getResourceList(resource, orderProperty)
                : resource.listProperties(property).mapWith(Statement::getResource).toList();

        referenceList.forEach(ref -> {
            var refDTO = new ConceptReferenceInfoDTO();
            var terminologyURI = TerminologyURI.fromUri(ref.getURI());
            refDTO.setReferenceURI(terminologyURI.getResourceURI());
            refDTO.setIdentifier(terminologyURI.getResourceId());
            refDTO.setPrefix(terminologyURI.getPrefix());
            var label = new HashMap<String, String>();
            model.getResource(ref.getURI()).listProperties(SKOS.prefLabel).forEach(r -> {
                var value = r.getProperty(SKOSXL.literalForm);
                label.put(value.getLanguage(), value.getString());
            });
            refDTO.setLabel(label);
            result.add(refDTO);
        });
        return result;
    }

    private static void addLiteralListProperty(Resource resource, Property property, Collection<String> values) {
        var literalValues = values.stream()
                .map(ResourceFactory::createStringLiteral)
                .toList();

        MapperUtils.addListProperty(resource, property, literalValues);
    }

    private static void addLocalizedListProperty(Resource resource, Property property, Collection<LocalizedValueDTO> values) {
        var literalValues = values.stream()
                .map(e -> ResourceFactory.createLangLiteral(e.getValue(), e.getLanguage()))
                .toList();
        MapperUtils.addListProperty(resource, property, literalValues);
    }

    private static void addResourceListProperty(Model model, Resource resource, Property property, Collection<String> values) {
        resource.removeAll(property);

        var resourceValues = values.stream()
                .map(model::getResource)
                .toList();

        resourceValues.forEach(r -> resource.addProperty(property, r));

        var orderProperty = orderProperties.get(property.getLocalName());
        if (orderProperty != null) {
            MapperUtils.addListProperty(resource, orderProperty, resourceValues);
        }
    }

    private static void mapReferencesToResource(Model model, ConceptDTO dto, Resource resource) {
        addResourceListProperty(model, resource, SKOS.broader, dto.getBroader());
        addResourceListProperty(model, resource, SKOS.narrower, dto.getNarrower());
        addResourceListProperty(model, resource, DCTerms.hasPart, dto.getHasPart());
        addResourceListProperty(model, resource, DCTerms.isPartOf, dto.getIsPartOf());
        addResourceListProperty(model, resource, SKOS.related, dto.getRelated());
        addResourceListProperty(model, resource, SKOS.broadMatch, dto.getBroadMatch());
        addResourceListProperty(model, resource, SKOS.narrowMatch, dto.getNarrowMatch());
        addResourceListProperty(model, resource, SKOS.exactMatch, dto.getExactMatch());
        addResourceListProperty(model, resource, SKOS.closeMatch, dto.getCloseMatch());
        addResourceListProperty(model, resource, SKOS.relatedMatch, dto.getRelatedMatch());
    }

    private static void mapLinks(Model model, ConceptDTO dto, Set<String> languages, Resource conceptResource) {
        var links = dto.getLinks().stream().map(link -> {
            var linkResource = model.createResource();
            MapperUtils.addLocalizedProperty(languages, link.getName(), linkResource, DCTerms.title);
            MapperUtils.addLocalizedProperty(languages, link.getDescription(), linkResource, DCTerms.description);
            linkResource.addProperty(FOAF.homepage, link.getUri());
            return linkResource;
        }).toList();

        MapperUtils.addListProperty(conceptResource, RDFS.seeAlso, links);
    }

    private static void handleTerms(ModelWrapper model, ConceptDTO dto, Resource conceptResource) {
        conceptResource.removeAll(SKOS.prefLabel);
        conceptResource.removeAll(SKOS.altLabel);
        conceptResource.removeAll(Term.notRecommendedSynonym);
        conceptResource.removeAll(SKOS.hiddenLabel);

        // Preferred terms don't need to be ordered, since there's only one preferred term / language
        dto.getRecommendedTerms().stream()
                .map(term -> mapTerm(model, term))
                .forEach(term -> conceptResource.addProperty(SKOS.prefLabel, term));

        // Add synonyms in ordered list
        var synonyms = dto.getSynonyms().stream()
                .map(term -> mapTerm(model, term))
                .toList();
        synonyms.forEach(s -> conceptResource.addProperty(SKOS.altLabel, s));
        MapperUtils.addListProperty(conceptResource, Term.orderedSynonym, synonyms);

        // Add not recommended synonyms in ordered list
        var notRecommendedSynonyms = dto.getNotRecommendedTerms().stream()
                .map(term -> mapTerm(model, term))
                .toList();
        notRecommendedSynonyms.forEach(s -> conceptResource.addProperty(Term.notRecommendedSynonym,s));
        MapperUtils.addListProperty(conceptResource, Term.orderedNotRecommendedSynonym, notRecommendedSynonyms);

        // hidden terms don't need to be ordered, because they are not visible in the UI
        dto.getSearchTerms().stream()
                .map(term -> mapTerm(model, term))
                .forEach(t -> conceptResource.addProperty(SKOS.hiddenLabel, t));
    }

    private static Resource mapTerm(ModelWrapper model, TermDTO term) {
        var termResource = model.createResource(UUID.randomUUID().toString());
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

    private static List<TermDTO> mapTermDTO(Resource resource, Property property) {

        var terms = new ArrayList<TermDTO>();

        var termResources = termProperties.contains(property)
            ? resource.listProperties(property).mapWith(Statement::getResource).toList()
            : MapperUtils.getResourceList(resource, property);

        termResources.forEach(termResource -> {
            var label = termResource.getProperty(SKOSXL.literalForm).getObject().asLiteral();

            var term = new TermDTO();
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
