package fi.vm.yti.terminology.api.v2.mapper;

import fi.vm.yti.common.dto.LinkDTO;
import fi.vm.yti.common.enums.Status;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.terminology.api.v2.TestUtils;
import fi.vm.yti.terminology.api.v2.dto.ConceptDTO;
import fi.vm.yti.terminology.api.v2.dto.LocalizedValueDTO;
import fi.vm.yti.terminology.api.v2.dto.TermDTO;
import fi.vm.yti.terminology.api.v2.enums.TermConjugation;
import fi.vm.yti.terminology.api.v2.enums.TermEquivalency;
import fi.vm.yti.terminology.api.v2.enums.TermFamily;
import fi.vm.yti.terminology.api.v2.enums.WordClass;
import fi.vm.yti.terminology.api.v2.property.Term;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static fi.vm.yti.terminology.api.v2.TestUtils.getConceptData;
import static fi.vm.yti.terminology.api.v2.TestUtils.mockUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ConceptMapperTest {
    @Test
    void testMapConceptToModel() {
        var graphURI = TerminologyURI.createTerminologyURI("test").getGraphURI();
        var model = TestUtils.getModelFromFile("/terminology-metadata.ttl", graphURI);

        var concept = getConceptData();

        ConceptMapper.dtoToModel(model, concept, mockUser);

        var conceptResource = model.getResourceById(concept.getIdentifier());

        assertEquals(graphURI + concept.getIdentifier(), conceptResource.getURI());
        assertEquals(concept.getConceptClass(), conceptResource.getProperty(Term.conceptClass).getString());
        assertEquals(concept.getDefinition(), Map.of("en", "definition"));
        assertEquals(concept.getChangeNote(), conceptResource.getProperty(SKOS.changeNote).getString());
        assertEquals(concept.getHistoryNote(), conceptResource.getProperty(SKOS.historyNote).getString());

        assertEquals(concept.getEditorialNotes(), getList(conceptResource, SKOS.editorialNote));
        assertEquals(concept.getSources(), getList(conceptResource, DCTerms.source));

        assertEquals(concept.getStatus(), MapperUtils.getStatus(conceptResource));
        assertEquals(concept.getSubjectArea(), conceptResource.getProperty(Term.subjectArea).getString());

        var examples = getLocalizedList(conceptResource, SKOS.example);
        assertEquals(concept.getExamples().size(), examples.size());
        assertEquals(concept.getExamples().get(0), examples.get(0));

        var notes = getLocalizedList(conceptResource, SKOS.note);
        assertEquals(concept.getNotes().size(), notes.size());
        assertEquals(concept.getNotes().get(0), notes.get(0));

        var link = MapperUtils.getResourceList(conceptResource, RDFS.seeAlso).get(0);
        var expectedLink = concept.getLinks().get(0);
        assertEquals(expectedLink.getName(), MapperUtils.localizedPropertyToMap(link, DCTerms.title));
        assertEquals(expectedLink.getDescription(), MapperUtils.localizedPropertyToMap(link, DCTerms.description));
        assertEquals(expectedLink.getUri(), link.getProperty(FOAF.homepage).getString());
    }

    @Test
    void testMapConceptReferences() {
        var graphURI = TerminologyURI.createTerminologyURI("test").getGraphURI();
        var model = TestUtils.getModelFromFile("/terminology-metadata.ttl", graphURI);

        var concept = getConceptData();

        ConceptMapper.dtoToModel(model, concept, mockUser);

        var conceptResource = model.getResourceById(concept.getIdentifier());

        assertEquals(List.of("broader-1", "broader-2", "broader-3"),
                MapperUtils.getResourceList(conceptResource, SKOS.broader).stream()
                        .map(Resource::getLocalName)
                        .toList());

        assertEquals("narrower", MapperUtils.getResourceList(conceptResource, SKOS.narrower).get(0).getLocalName());
        assertEquals("isPartOf", MapperUtils.getResourceList(conceptResource, DCTerms.isPartOf).get(0).getLocalName());
        assertEquals("hasPart", MapperUtils.getResourceList(conceptResource, DCTerms.hasPart).get(0).getLocalName());
        assertEquals("related", MapperUtils.getResourceList(conceptResource, SKOS.related).get(0).getLocalName());
        assertEquals("broadMatch", MapperUtils.getResourceList(conceptResource, SKOS.broadMatch).get(0).getLocalName());
        assertEquals("narrowMatch", MapperUtils.getResourceList(conceptResource, SKOS.narrowMatch).get(0).getLocalName());
        assertEquals("exactMatch", MapperUtils.getResourceList(conceptResource, SKOS.exactMatch).get(0).getLocalName());
        assertEquals("closeMatch", MapperUtils.getResourceList(conceptResource, SKOS.closeMatch).get(0).getLocalName());
        assertEquals("relatedMatch", MapperUtils.getResourceList(conceptResource, SKOS.relatedMatch).get(0).getLocalName());
    }

    @Test
    void testMapTermToModel() {
        var graphURI = TerminologyURI.createTerminologyURI("test").getGraphURI();
        var model = TestUtils.getModelFromFile("/terminology-metadata.ttl", graphURI);

        var concept = getConceptData();

        ConceptMapper.dtoToModel(model, concept, mockUser);

        var conceptResource = model.getResourceById(concept.getIdentifier());

        var term = concept.getRecommendedTerms().iterator().next();
        var termResource = conceptResource.getProperty(SKOS.prefLabel).getList().get(0).asResource();

        assertEquals(SKOSXL.Label, termResource.getProperty(RDF.type).getObject().asResource());

        var label = termResource.getProperty(SKOSXL.literalForm);
        assertEquals(term.getLabel(), label.getString());
        assertEquals(term.getLanguage(), label.getLanguage());

        assertEquals(term.getHomographNumber(), MapperUtils.getLiteral(termResource, Term.homographNumber, Integer.class));
        assertEquals(term.getChangeNote(), MapperUtils.propertyToString(termResource, SKOS.changeNote));
        assertEquals(term.getHistoryNote(), MapperUtils.propertyToString(termResource, SKOS.historyNote));
        assertEquals(term.getScope(), MapperUtils.propertyToString(termResource, Term.scope));
        assertEquals(term.getTermInfo(), MapperUtils.propertyToString(termResource, Term.termInfo));
        assertEquals(term.getTermStyle(), MapperUtils.propertyToString(termResource, Term.termStyle));
        assertEquals(term.getStatus(), MapperUtils.getStatus(termResource));

        assertEquals(term.getTermConjugation(), TermConjugation.valueOf(MapperUtils.propertyToString(termResource, Term.termConjugation)));
        assertEquals(term.getTermEquivalency(), TermEquivalency.valueOf(MapperUtils.propertyToString(termResource, Term.termEquivalency)));
        assertEquals(term.getTermFamily(), TermFamily.valueOf(MapperUtils.propertyToString(termResource, Term.termFamily)));
        assertEquals(term.getWordClass(), WordClass.valueOf(MapperUtils.propertyToString(termResource, Term.wordClass)));
    }

    @Test
    void testMapUpdateConcept() {
        var graphURI = TerminologyURI.createTerminologyURI("test").getGraphURI();
        var model = TestUtils.getModelFromFile("/terminology-with-concepts.ttl", graphURI);

        var dto = new ConceptDTO();
        dto.setStatus(Status.VALID);
        dto.setDefinition(Map.of("fi", "New definition"));
        dto.setNotes(List.of(
                new LocalizedValueDTO("fi", "note fi"),
                new LocalizedValueDTO("en", "note en")
        ));
        dto.setExamples(List.of());
        dto.setConceptClass("new class");
        dto.setChangeNote("new change");
        dto.setHistoryNote("new history");

        var link = new LinkDTO();
        link.setName(Map.of("fi", "link 2"));
        link.setUri("https://dvv.fi/updated");
        dto.setLinks(List.of(link));

        var recommendedTerms = new ArrayList<TermDTO>();
        var prefLabel = new TermDTO();
        prefLabel.setLabel("pref term label");
        prefLabel.setTermFamily(TermFamily.FEMININE);
        prefLabel.setLanguage("fi");
        recommendedTerms.add(prefLabel);

        var synonyms = new ArrayList<TermDTO>();
        var altLabel = new TermDTO();
        altLabel.setStatus(Status.VALID);
        altLabel.setLabel("modified alt label");
        altLabel.setLanguage("fi");
        synonyms.add(altLabel);

        var searchTerms = new ArrayList<TermDTO>();
        var searchTerm = new TermDTO();
        searchTerm.setStatus(Status.DRAFT);
        searchTerm.setLanguage("en");
        searchTerm.setLabel("new search term");
        searchTerms.add(searchTerm);

        dto.setRecommendedTerms(recommendedTerms);
        dto.setSynonyms(synonyms);
        dto.setSearchTerms(searchTerms);

        dto.setRelated(Set.of(
                TerminologyURI.createConceptURI("test", "concept-1000").getResourceURI())
        );
        dto.setBroadMatch(Set.of(
                TerminologyURI.createConceptURI("external", "concept-300").getResourceURI())
        );

        ConceptMapper.dtoToUpdateModel(model, "concept-1", dto, mockUser);

        var updatedResource = model.getResourceById("concept-1");

        assertEquals(Status.VALID, MapperUtils.getStatus(updatedResource));
        assertEquals(Map.of("fi", "New definition"), MapperUtils.localizedPropertyToMap(updatedResource, SKOS.definition));
        assertFalse(updatedResource.hasProperty(Term.subjectArea));
        assertEquals("new history", MapperUtils.propertyToString(updatedResource, SKOS.historyNote));
        assertEquals("new change", MapperUtils.propertyToString(updatedResource, SKOS.changeNote));
        assertEquals("new class", MapperUtils.propertyToString(updatedResource, Term.conceptClass));


        var linkResource = MapperUtils.getResourceList(updatedResource, RDFS.seeAlso).get(0);
        assertEquals(Map.of("fi", "link 2"), MapperUtils.localizedPropertyToMap(linkResource, DCTerms.title));
        assertEquals("https://dvv.fi/updated", MapperUtils.propertyToString(linkResource, FOAF.homepage));

        var prefLabels = updatedResource.getProperty(SKOS.prefLabel).getList();
        assertEquals(1, prefLabels.size());
        checkLabel("pref term label", "fi", prefLabels.get(0).asResource());

        var altLabels = updatedResource.getProperty(SKOS.altLabel).getList();
        assertEquals(1, altLabels.size());
        checkLabel("modified alt label", "fi", altLabels.get(0).asResource());

        var hiddenLabels = updatedResource.getProperty(SKOS.hiddenLabel).getList();
        checkLabel("new search term", "en", hiddenLabels.get(0).asResource());

        var related = MapperUtils.getList(updatedResource, SKOS.related);
        var broadMatch = MapperUtils.getList(updatedResource, SKOS.broadMatch);

        assertEquals(1, related.size());
        assertEquals(1, broadMatch.size());

        assertEquals(0, updatedResource.listProperties(SKOS.broader).toList().size());
        assertEquals("https://iri.suomi.fi/terminology/test/concept-1000", related.get(0).asResource().getURI());
        assertEquals("https://iri.suomi.fi/terminology/external/concept-300", broadMatch.get(0).asResource().getURI());
    }

    @Test
    void testMapConceptToDTO() {
        var graphURI = TerminologyURI.createTerminologyURI("test").getGraphURI();
        var model = TestUtils.getModelFromFile("/terminology-with-concepts.ttl", graphURI);

        var dto = ConceptMapper.modelToDTO(model, "concept-1", TestUtils.mapUser);

        assertEquals("concept-1", dto.getIdentifier());
        assertEquals("change", dto.getChangeNote());
        assertEquals("history", dto.getHistoryNote());
        assertEquals(Status.DRAFT, dto.getStatus());
        assertEquals("subject area", dto.getSubjectArea());
        assertEquals("concept class", dto.getConceptClass());
        assertEquals(graphURI + "concept-1", dto.getUri());
        assertEquals(Map.of("fi", "def"), dto.getDefinition());
        assertEquals(new LocalizedValueDTO("fi", "note"), dto.getNotes().get(0));
        assertEquals(new LocalizedValueDTO("fi", "example"), dto.getExamples().get(0));

        var link = dto.getLinks().get(0);
        assertEquals("link 1", link.getName().get("fi"));
        assertEquals("description", link.getDescription().get("fi"));
        assertEquals("https://dvv.fi", link.getUri());

        var internalRef = dto.getBroader().iterator().next();
        assertEquals(graphURI + "concept-2", internalRef.getReferenceURI());
        assertEquals("Suositettava termi", internalRef.getLabel().get("fi"));

        var externalRef = dto.getNarrowMatch().iterator().next();
        assertEquals(TerminologyURI.createConceptURI("ext", "concept-1").getResourceURI(), externalRef.getReferenceURI());
    }

    @Test
    void testMapTermToDTO() {
        var graphURI = TerminologyURI.createTerminologyURI("test").getGraphURI();
        var model = TestUtils.getModelFromFile("/terminology-with-concepts.ttl", graphURI);

        var dto = ConceptMapper.modelToDTO(model, "concept-1", TestUtils.mapUser);

        assertEquals(1, dto.getRecommendedTerms().size());
        assertEquals(2, dto.getSynonyms().size());
        assertEquals(1, dto.getNotRecommendedTerms().size());
        assertEquals(1, dto.getSearchTerms().size());

        var term = dto.getRecommendedTerms().iterator().next();

        assertEquals("Suositettava termi", term.getLabel());
        assertEquals("info", term.getTermInfo());
        assertEquals("term style", term.getTermStyle());
        assertEquals("term change", term.getChangeNote());
        assertEquals("term history", term.getHistoryNote());
        assertEquals("fi", term.getLanguage());
        assertEquals("scope", term.getScope());
        assertEquals(2, term.getHomographNumber());
        assertEquals(Status.DRAFT, term.getStatus());
        assertEquals(TermConjugation.SINGULAR, term.getTermConjugation());
        assertEquals(TermFamily.NEUTER, term.getTermFamily());
        assertEquals(WordClass.ADJECTIVE, term.getWordClass());
        assertEquals(TermEquivalency.BROADER, term.getTermEquivalency());
    }

    @Test
    void mapToIndexDocument() {
        var conceptURI = TerminologyURI.createConceptURI("test", "concept-1");
        var model = TestUtils.getModelFromFile("/terminology-with-concepts.ttl", conceptURI.getGraphURI());

        var dto = ConceptMapper.toIndexDocument(model, "concept-1");

        assertEquals(conceptURI.getResourceURI(), dto.getUri());
        assertEquals(conceptURI.getResourceURI(), dto.getId());
        assertEquals(Status.DRAFT, dto.getStatus());
        assertEquals("Suositettava termi", dto.getLabel().get("fi"));
        assertEquals("def", dto.getDefinition().get("fi"));
        assertEquals(List.of("synonyymi 1", "synonyymi 2"), dto.getAltLabel());
        assertEquals(List.of("ei suositettava"), dto.getNotRecommendedSynonym());
        assertEquals(List.of("hakutermi"), dto.getSearchTerm());
        assertEquals("2024-05-06T05:00:00.000Z", dto.getCreated());
        assertEquals("2024-05-07T04:00:00.000Z", dto.getModified());
    }

    @Test
    void testMapRemoveConcept() {
        var graphURI = TerminologyURI.createTerminologyURI("test").getGraphURI();
        var model = TestUtils.getModelFromFile("/terminology-metadata.ttl", graphURI);
        model.createResourceWithId("concept-test-123").addProperty(SKOS.note, "Test note");

        var initialSize = model.size();

        var dto = getConceptData();
        dto.setRelated(
                Set.of(TerminologyURI.createConceptURI("test", "concept-test-123").getResourceURI())
        );

        ConceptMapper.dtoToModel(model, dto, mockUser);
        ConceptMapper.mapDeleteConcept(model, dto.getIdentifier());

        // all triples related to removed concept should be removed
        assertEquals(initialSize, model.size());
    }

    private static List<String> getList(Resource conceptResource, Property property) {
        return conceptResource.getProperty(property).getList()
                .asJavaList().stream()
                .map(r -> r.asLiteral().getString())
                .toList();
    }

    private static List<LocalizedValueDTO> getLocalizedList(Resource conceptResource, Property property) {
        return conceptResource.getProperty(property).getList()
                .asJavaList().stream()
                .map(r -> new LocalizedValueDTO(r.asLiteral().getLanguage(), r.asLiteral().getString()))
                .toList();
    }

    private static void checkLabel(String expectedValue, String expectedLanguage, Resource resource) {
        assertEquals(expectedValue, resource.getProperty(SKOSXL.literalForm).getString());
        assertEquals(expectedLanguage, resource.getProperty(SKOSXL.literalForm).getLanguage());
    }
}
