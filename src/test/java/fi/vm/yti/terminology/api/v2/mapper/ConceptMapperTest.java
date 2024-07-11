package fi.vm.yti.terminology.api.v2.mapper;

import fi.vm.yti.common.dto.LinkDTO;
import fi.vm.yti.common.enums.Status;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.terminology.api.v2.TestUtils;
import fi.vm.yti.terminology.api.v2.dto.ConceptDTO;
import fi.vm.yti.terminology.api.v2.dto.ConceptReferenceDTO;
import fi.vm.yti.terminology.api.v2.dto.LocalizedValueDTO;
import fi.vm.yti.terminology.api.v2.dto.TermDTO;
import fi.vm.yti.terminology.api.v2.enums.*;
import fi.vm.yti.terminology.api.v2.property.Term;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static fi.vm.yti.terminology.api.v2.TestUtils.getConceptData;
import static fi.vm.yti.terminology.api.v2.TestUtils.mockUser;
import static org.junit.jupiter.api.Assertions.*;

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
        assertEquals(concept.getSources(), getList(conceptResource, Term.source));

        assertEquals(concept.getStatus(), MapperUtils.getStatus(conceptResource));
        assertEquals(concept.getSubjectArea(), Map.of("en", "subject area"));

        var examples = getLocalizedList(conceptResource, SKOS.example);
        assertEquals(concept.getExamples().size(), examples.size());
        assertEquals(concept.getExamples().get(0), examples.get(0));

        var notes = getLocalizedList(conceptResource, SKOS.note);
        assertEquals(concept.getNotes().size(), notes.size());
        assertEquals(concept.getNotes().get(0), notes.get(0));

        var link = conceptResource.getProperty(RDFS.seeAlso).getObject().asResource();
        var expectedLink = concept.getLinks().get(0);
        assertEquals(expectedLink.getName(), MapperUtils.localizedPropertyToMap(link, DCTerms.title));
        assertEquals(expectedLink.getDescription(), MapperUtils.localizedPropertyToMap(link, DCTerms.description));
        assertEquals(expectedLink.getUri(), link.getProperty(FOAF.homepage).getString());

        ConceptMapper.internalRefProperties.forEach(prop -> assertEquals(graphURI + "concept-1000",
                conceptResource.getProperty(prop).getObject().toString()));

        ConceptMapper.externalRefProperties.forEach(prop ->
                assertEquals("https://iri.suomi.fi/terminology/external/concept-123",
                        conceptResource.getProperty(prop).getObject().toString()));
    }

    @Test
    void testMapTermToModel() {
        var graphURI = TerminologyURI.createTerminologyURI("test").getGraphURI();
        var model = TestUtils.getModelFromFile("/terminology-metadata.ttl", graphURI);

        var concept = getConceptData();

        ConceptMapper.dtoToModel(model, concept, mockUser);

        var conceptResource = model.getResourceById(concept.getIdentifier());

        var term = concept.getTerms().iterator().next();
        var termResource = conceptResource.getProperty(SKOS.prefLabel).getResource();

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

        var prefLabel = new TermDTO();
        prefLabel.setTermType(TermType.RECOMMENDED);
        prefLabel.setIdentifier("term-614007ae-5d84-45d8-b473-6359c3cbc5ca");
        prefLabel.setLabel("pref term label");
        prefLabel.setTermFamily(TermFamily.FEMININE);

        var altLabel = new TermDTO();
        altLabel.setTermType(TermType.SYNONYM);
        altLabel.setIdentifier("term-f04ce627-c799-4e9a-9b0c-71b65f69130b");
        altLabel.setStatus(Status.VALID);
        altLabel.setLabel("modified alt label");

        var searchTerm = new TermDTO();
        searchTerm.setTermType(TermType.SEARCH_TERM);
        searchTerm.setStatus(Status.DRAFT);
        searchTerm.setLanguage("en");
        searchTerm.setLabel("new search term");

        dto.setTerms(Set.of(prefLabel, altLabel, searchTerm));

        var ref1 = new ConceptReferenceDTO();
        ref1.setReferenceType(ReferenceType.RELATED);
        ref1.setConceptURI("https://iri.suomi.fi/terminology/test/concept-1000");

        var ref2 = new ConceptReferenceDTO();
        ref2.setReferenceType(ReferenceType.BROAD_MATCH);
        ref2.setConceptURI("https://iri.suomi.fi/terminology/external/concept-300");

        dto.setReferences(Set.of(ref1, ref2));

        ConceptMapper.dtoToUpdateModel(model, "concept-1", dto, mockUser);

        var updatedResource = model.getResourceById("concept-1");

        assertEquals(Status.VALID, MapperUtils.getStatus(updatedResource));
        assertEquals(Map.of("fi", "New definition"), MapperUtils.localizedPropertyToMap(updatedResource, SKOS.definition));
        assertFalse(updatedResource.hasProperty(Term.subjectArea));
        assertEquals("new history", MapperUtils.propertyToString(updatedResource, SKOS.historyNote));
        assertEquals("new change", MapperUtils.propertyToString(updatedResource, SKOS.changeNote));
        assertEquals("new class", MapperUtils.propertyToString(updatedResource, Term.conceptClass));

        var linkResource = updatedResource.getProperty(RDFS.seeAlso).getResource();
        assertEquals(Map.of("fi", "link 2"), MapperUtils.localizedPropertyToMap(linkResource, DCTerms.title));
        assertEquals("https://dvv.fi/updated", MapperUtils.propertyToString(linkResource, FOAF.homepage));

        var prefLabels = updatedResource.listProperties(SKOS.prefLabel).toList();
        assertEquals(1, prefLabels.size());
        checkLabel("pref term label", "fi", prefLabels.get(0));

        var altLabels = updatedResource.listProperties(SKOS.altLabel).toList();
        assertEquals(1, altLabels.size());
        checkLabel("modified alt label", "fi", altLabels.get(0));

        var hiddenLabels = updatedResource.listProperties(SKOS.hiddenLabel).toList();
        checkLabel("new search term", "en", hiddenLabels.get(0));

        // Other alt label, search term and not recommended synonym should not exists in the model
        assertFalse(model.getResourceById("term-ce6c2547-261f-46e1-9cba-662f886df7f6;").listProperties().hasNext());
        assertFalse(model.getResourceById("term-47d79614-d8f5-4c42-8967-a860c3451f5b").listProperties().hasNext());
        assertFalse(model.getResourceById("term-45f21a97-4bae-42ee-b7c5-c1d871ee9c2a").listProperties().hasNext());

        var related = updatedResource.listProperties(SKOS.related).toList();
        var broadMatch = updatedResource.listProperties(SKOS.broadMatch).toList();

        assertEquals(1, related.size());
        assertEquals(1, broadMatch.size());

        assertEquals(0, updatedResource.listProperties(SKOS.broader).toList().size());
        assertEquals("https://iri.suomi.fi/terminology/test/concept-1000", related.get(0).getObject().toString());
        assertEquals("https://iri.suomi.fi/terminology/external/concept-300", broadMatch.get(0).getObject().toString());
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
        assertEquals("subject area", dto.getSubjectArea().get("fi"));
        assertEquals("concept class", dto.getConceptClass());
        assertEquals(graphURI + "concept-1", dto.getUri());
        assertEquals(Map.of("fi", "def"), dto.getDefinition());
        assertEquals(new LocalizedValueDTO("fi", "note"), dto.getNotes().get(0));
        assertEquals(new LocalizedValueDTO("fi", "example"), dto.getExamples().get(0));

        var link = dto.getLinks().get(0);
        assertEquals("link 1", link.getName().get("fi"));
        assertEquals("description", link.getDescription().get("fi"));
        assertEquals("https://dvv.fi", link.getUri());

        var internalRef = dto.getReferences().stream()
                .filter(r -> r.getReferenceType().equals(ReferenceType.BROADER))
                .findFirst();
        assertTrue(internalRef.isPresent());
        assertEquals(graphURI + "concept-2", internalRef.get().getConceptURI());
        assertEquals("Suositettava termi", internalRef.get().getLabel().get("fi"));

        var externalRef = dto.getReferences().stream()
                .filter(r -> r.getReferenceType().equals(ReferenceType.NARROW_MATCH))
                .findFirst();
        assertTrue(externalRef.isPresent());
        assertEquals(TerminologyURI.createConceptURI("ext", "concept-1").getResourceURI(), externalRef.get().getConceptURI());
    }

    @Test
    void testMapTermToDTO() {
        var graphURI = TerminologyURI.createTerminologyURI("test").getGraphURI();
        var model = TestUtils.getModelFromFile("/terminology-with-concepts.ttl", graphURI);

        var dto = ConceptMapper.modelToDTO(model, "concept-1", TestUtils.mapUser);

        assertEquals(5, dto.getTerms().size());

        var termOpt = dto.getTerms().stream()
                .filter(t -> t.getIdentifier().equals("term-614007ae-5d84-45d8-b473-6359c3cbc5ca"))
                .findFirst();
        assertTrue(termOpt.isPresent());

        var term = termOpt.get();

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
        assertEquals(TermFamily.NEUTRAL, term.getTermFamily());
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
        assertEquals(List.of("synonyymi 2", "synonyymi 1"), dto.getAltLabel().get("fi"));
        assertEquals(List.of("ei suositettava"), dto.getNotRecommendedSynonym().get("fi"));
        assertEquals(List.of("hakutermi"), dto.getSearchTerm().get("fi"));
        assertEquals("2024-05-06T05:00:00.000Z", dto.getCreated());
        assertEquals("2024-05-07T04:00:00.000Z", dto.getModified());
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

    private static void checkLabel(String expectedValue, String expectedLanguage, Statement stmt) {
        assertEquals(expectedValue, stmt.getProperty(SKOSXL.literalForm).getString());
        assertEquals(expectedLanguage, stmt.getProperty(SKOSXL.literalForm).getLanguage());
    }
}
