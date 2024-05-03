package fi.vm.yti.terminology.api.v2.mapper;

import fi.vm.yti.common.dto.LinkDTO;
import fi.vm.yti.common.dto.ResourceCommonInfoDTO;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.common.util.ModelWrapper;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.v2.dto.*;
import fi.vm.yti.terminology.api.v2.enums.ReferenceType;
import fi.vm.yti.terminology.api.v2.opensearch.IndexConcept;
import fi.vm.yti.terminology.api.v2.property.Term;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.*;

import java.util.*;
import java.util.function.Consumer;

public class ConceptMapper {

    private ConceptMapper() {
        // only static methods
    }

    public static void dtoToModel(ModelWrapper model, ConceptDTO dto, String identifier, YtiUser user) {
        var modelResource = model.getModelResource();
        var languages = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.language);

        var conceptResource = model.createResourceWithId(identifier)
                .addProperty(SKOS.inScheme, ResourceFactory.createResource(modelResource.getURI()));

        MapperUtils.addStatus(conceptResource, dto.getStatus());

        MapperUtils.addLocalizedProperty(languages, dto.getDefinition(), conceptResource, SKOS.definition);
        MapperUtils.addLocalizedProperty(languages, dto.getSubjectArea(), conceptResource, Term.subjectArea);

        MapperUtils.addOptionalStringProperty(conceptResource, SKOS.changeNote, dto.getChangeNote());
        MapperUtils.addOptionalStringProperty(conceptResource, SKOS.historyNote, dto.getHistoryNote());
        MapperUtils.addOptionalStringProperty(conceptResource, Term.conceptClass, dto.getConceptClass());

        addLocalizedListProperty(conceptResource, SKOS.example, dto.getExamples());
        addLocalizedListProperty(conceptResource, SKOS.note, dto.getNotes());
        addListProperty(conceptResource, SKOS.editorialNote, dto.getEditorialNotes());
        addListProperty(conceptResource, Term.source, dto.getSources());

        dto.getTerms().forEach(term -> mapTerm(model, term, conceptResource));
        dto.getReferences().forEach(ref -> mapReferences(ref, conceptResource));
        dto.getLinks().forEach(link -> mapLinks(model, link, languages, conceptResource));

        MapperUtils.addCreationMetadata(conceptResource, user);
    }

    public static ConceptInfoDTO modelToDTO(ModelWrapper model, String conceptIdentifier, Consumer<ResourceCommonInfoDTO> mapUser) {
        var resource = model.getResourceById(conceptIdentifier);
        var dto = new ConceptInfoDTO();

        dto.setIdentifier(resource.getLocalName());
        dto.setDefinition(MapperUtils.localizedPropertyToMap(resource, SKOS.definition));

        dto.setExamples(List.of());
        dto.setNotes(List.of());
        dto.setSubjectArea(MapperUtils.localizedPropertyToMap(resource, Term.subjectArea));

        dto.setReferences(mapReferences(model, resource));

        dto.setLinks(List.of());
        dto.setSources(List.of());

        dto.setChangeNote(null);
        dto.setHistoryNote(null);
        dto.setEditorialNotes(List.of());

        dto.setConceptClass(MapperUtils.propertyToString(resource, Term.conceptClass));

        if (mapUser != null) {
            mapUser.accept(dto);
        }
        return dto;
    }

    private static Set<ConceptReferenceInfoDTO> mapReferences(ModelWrapper model, Resource resource) {
        var references = new HashSet<ConceptReferenceInfoDTO>();

        var refProperties = List.of(SKOS.broader, SKOS.narrower, SKOS.related, DCTerms.hasPart, DCTerms.isPartOf);

        refProperties.forEach(prop -> {
            var ref = MapperUtils.propertyToString(resource, prop);

            if (ref != null) {
                var dto = new ConceptReferenceInfoDTO();
                dto.setConceptURI(ref);

                var label = new HashMap<String, String>();
                model.listStatements(ResourceFactory.createResource(ref), SKOS.prefLabel, (RDFNode) null)
                        .forEach(s -> {
                            var r = s.getObject().asResource().getProperty(SKOSXL.literalForm);
                            label.put(r.getLanguage(), r.getString());
                        });
                dto.setReferenceType(ReferenceType.getByPropertyName(prop.getLocalName()));
                dto.setLabel(label);
                references.add(dto);
            }
        });



        return references;
    }

    public static IndexConcept toIndexDocument(ModelWrapper model, String identifier) {
        var resource = model.getResourceById(identifier);

        var indexConcept = new IndexConcept();
        indexConcept.setId(resource.getURI());
        return indexConcept;
    }

    private static void addLocalizedListProperty(Resource resource, Property property, List<LocalizedValueDTO> values) {
        if (values.isEmpty()) {
            return;
        }
        var list = resource.getModel().createList(values.stream()
                .map(e -> ResourceFactory.createLangLiteral(e.getValue(), e.getLanguage()))
                .iterator());
        resource.addProperty(property, list);
    }

    private static void addListProperty(Resource resource, Property property, List<String> values) {
        if (values.isEmpty()) {
            return;
        }
        var list = resource.getModel().createList(values.stream()
                .map(ResourceFactory::createStringLiteral)
                .iterator());
        resource.addProperty(property, list);
    }

    private static void mapReferences(ConceptReferenceDTO ref, Resource resource) {
        switch (ref.getReferenceType()) {
            case BROADER -> MapperUtils.addOptionalUriProperty(resource, SKOS.broader, ref.getConceptURI());
            case NARROWER -> MapperUtils.addOptionalUriProperty(resource, SKOS.narrower, ref.getConceptURI());
            case RELATED -> MapperUtils.addOptionalUriProperty(resource, SKOS.related, ref.getConceptURI());
            case IS_PART_OF -> MapperUtils.addOptionalUriProperty(resource, DCTerms.isPartOf, ref.getConceptURI());
            case HAS_PART -> MapperUtils.addOptionalUriProperty(resource, DCTerms.hasPart, ref.getConceptURI());
            case BROAD_MATCH -> MapperUtils.addOptionalUriProperty(resource, SKOS.broadMatch, ref.getConceptURI());
            case NARROW_MATCH -> MapperUtils.addOptionalUriProperty(resource, SKOS.narrowMatch, ref.getConceptURI());
            case CLOSE_MATCH -> MapperUtils.addOptionalUriProperty(resource, SKOS.closeMatch, ref.getConceptURI());
            case RELATED_MATCH -> MapperUtils.addOptionalUriProperty(resource, SKOS.relatedMatch, ref.getConceptURI());
            case EXACT_MATCH -> MapperUtils.addOptionalUriProperty(resource, SKOS.exactMatch, ref.getConceptURI());
        }
    }

    private static void mapLinks(ModelWrapper model, LinkDTO link, Set<String> languages, Resource resource) {
        var linkResource = model.createResource();
        MapperUtils.addLocalizedProperty(languages, link.getName(), linkResource, DCTerms.title);
        MapperUtils.addLocalizedProperty(languages, link.getDescription(), linkResource, DCTerms.description);
        linkResource.addProperty(FOAF.homepage, link.getUri());

        resource.addProperty(RDFS.seeAlso, linkResource);
    }

    private static void mapTerm(ModelWrapper model, TermDTO term, Resource resource) {
        var termIdentifier = "term-" + UUID.randomUUID();
        var termResource = model.createResourceWithId(termIdentifier);
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

        switch (term.getTermType()) {
            case RECOMMENDED -> MapperUtils.addOptionalUriProperty(resource, SKOS.prefLabel, termResource.getURI());
            case SYNONYM -> MapperUtils.addOptionalUriProperty(resource, SKOS.altLabel, termResource.getURI());
            case NOT_RECOMMENDED -> MapperUtils.addOptionalUriProperty(resource, Term.notRecommendedSynonym, termResource.getURI());
            case SEARCH_TERM -> MapperUtils.addOptionalUriProperty(resource, SKOS.hiddenLabel, termResource.getURI());
        }
    }

    private static void addOptionalEnumProperty(Resource resource, Property property, Enum<?> e) {
        if (e == null) {
            return;
        }
        MapperUtils.addOptionalStringProperty(resource, property, e.name());
    }
}
