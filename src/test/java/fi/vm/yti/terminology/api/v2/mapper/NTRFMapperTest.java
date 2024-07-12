package fi.vm.yti.terminology.api.v2.mapper;

import fi.vm.yti.common.enums.Status;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.terminology.api.v2.TestUtils;
import fi.vm.yti.terminology.api.v2.dto.ConceptCollectionDTO;
import fi.vm.yti.terminology.api.v2.enums.TermConjugation;
import fi.vm.yti.terminology.api.v2.enums.TermFamily;
import fi.vm.yti.terminology.api.v2.enums.WordClass;
import fi.vm.yti.terminology.api.v2.ntrf.VOCABULARY;
import fi.vm.yti.terminology.api.v2.property.Term;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.SKOS;
import org.apache.jena.vocabulary.SKOSXL;
import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NTRFMapperTest {

    @Test
    void testConceptAndTerm() {
        var model = TestUtils.getDefaultModel("test");
        var vocabulary = unmarshall("/ntrf/term-and-concept-info.xml");
        NTRFMapper.mapTerminology(vocabulary, model, TestUtils.mockUser);

        var concept = model.getResourceById("c100");

        var notesEN = getLocalizedList(concept, SKOS.note, "en");
        var notesFI = getLocalizedList(concept, SKOS.note, "fi");

        var examplesEN = getLocalizedList(concept, SKOS.example, "en");
        var examplesFI = getLocalizedList(concept, SKOS.example, "fi");

        assertEquals(Status.VALID, MapperUtils.getStatus(concept));
        assertEquals(List.of("Note 1", "Note 2"), notesEN);
        assertEquals(List.of("Note 1 FI", "Note 2 FI"), notesFI);
        assertEquals(List.of("Example 1", "Example 2"), examplesEN);
        assertEquals(List.of("Example 1 FI", "Example 2 FI"), examplesFI);
        assertEquals("Concept definition", MapperUtils.localizedPropertyToMap(concept, SKOS.definition).get("en"));
        assertEquals("Definition FI", MapperUtils.localizedPropertyToMap(concept, SKOS.definition).get("fi"));
        assertEquals("Concept class", MapperUtils.propertyToString(concept, Term.conceptClass));
        assertEquals(List.of("Editorial note 1", "Editorial note 2"), getListValues(concept, SKOS.editorialNote));
        assertEquals("Subject area", MapperUtils.propertyToString(concept, Term.subjectArea));
        assertEquals(List.of("Source 1", "Source 2", "Term source"), getListValues(concept, Term.source));

        var recommendedTermEN = getTerm(concept, SKOS.prefLabel, "en").get(0);
        var recommendedTermFI = getTerm(concept, SKOS.prefLabel, "fi").get(0);
        var synonymEN = getTerm(concept, SKOS.altLabel, "en");

        // prefLabel EN
        assertEquals(Status.VALID, MapperUtils.getStatus(recommendedTermEN));
        assertEquals("Test term recommended", MapperUtils.localizedPropertyToMap(recommendedTermEN, SKOSXL.literalForm).get("en"));
        assertEquals(2, MapperUtils.getLiteral(recommendedTermEN, Term.homographNumber, Integer.class));
        assertEquals(WordClass.VERB.name(), MapperUtils.propertyToString(recommendedTermEN, Term.wordClass));
        assertEquals(TermConjugation.PLURAL.name(), MapperUtils.propertyToString(recommendedTermEN, Term.termConjugation));
        assertEquals(TermFamily.FEMININE.name(), MapperUtils.propertyToString(recommendedTermEN, Term.termFamily));
        assertEquals("Term scope", MapperUtils.propertyToString(recommendedTermEN, Term.scope));
        assertEquals("Term additional info", MapperUtils.propertyToString(recommendedTermEN, Term.termInfo));

        // prefLabel FI
        assertEquals("Test term recommended FI", MapperUtils.localizedPropertyToMap(recommendedTermFI, SKOSXL.literalForm).get("fi"));

        // synonym EN
        assertEquals(2, synonymEN.size());
        // TODO: not working -> add terms to RDF list per type
        // assertEquals(TermEquivalency.NARROWER.name(), MapperUtils.propertyToString(synonymEN.get(0), Term.termEquivalency));

        assertEquals(1, getTerm(concept, SKOS.hiddenLabel, "en").size());
        assertEquals(2, getTerm(concept, Term.notRecommendedSynonym, "en").size());
    }

    @Test
    void testUpdateConcept() {
        var model = TestUtils.getDefaultModel("test");
        var concept = model.createResourceWithId("c1");
        concept.addProperty(Term.conceptClass, "Old concept class");
        MapperUtils.addLocalizedProperty(Set.of("en"), Map.of("en", "old definition"), concept, SKOS.definition);

        model.createResourceWithId("c888").addProperty(SKOS.prefLabel, "Remaining concept label");

        var vocabulary = unmarshall("/ntrf/ntrf-simple.xml");
        NTRFMapper.mapTerminology(vocabulary, model, TestUtils.mockUser);

        var updatedResource = model.getResourceById("c1");
        assertFalse(updatedResource.hasProperty(Term.conceptClass));
        assertEquals("Definition", MapperUtils.localizedPropertyToMap(updatedResource, SKOS.definition).get("en"));

        // other resources should not be affected
        assertEquals("Remaining concept label", model.getResourceById("c888").getProperty(SKOS.prefLabel).getString());
    }

    @Test
    void testConceptReferences() {
        var model = TestUtils.getDefaultModel("test");
        var vocabulary = unmarshall("/ntrf/concept-references.xml");
        NTRFMapper.mapTerminology(vocabulary, model, TestUtils.mockUser);

        var concept = model.getResourceById("c100");

        System.out.println(concept.listProperties(SKOS.related).toList());
    }

    @Test
    void testConceptFormattingAndLinks() {
        var model = TestUtils.getDefaultModel("test");
        var vocabulary = unmarshall("/ntrf/concept-content-formatting.xml");
        NTRFMapper.mapTerminology(vocabulary, model, TestUtils.mockUser);

        var concept = model.getResourceById("c1");

        var def = MapperUtils.localizedPropertyToMap(concept, SKOS.definition).get("en");
        var note = getLocalizedList(concept, SKOS.note, "en").get(0);

        assertEquals("Formatted content <i>italic</i> <b>bold</b> <sup>superscript</sup> and <sub>subscript</sub>.", def);
        assertEquals("<a href=\"https://iri.suomi.fi/terminology/test/c2\">Note (2)</a> with <a href=\"https://example.com\">link</a>", note);
    }

    @Test
    void testMapCollection() {
        var model = TestUtils.getDefaultModel("test");
        var vocabulary = unmarshall("/ntrf/concept-collection.xml");

        NTRFMapper.mapTerminology(vocabulary, model, TestUtils.mockUser);

        var collection = model.getResourceById("collection-1");

        assertEquals("Test collection", MapperUtils.localizedPropertyToMap(collection, SKOS.prefLabel).get("fi"));

        var members = collection.getProperty(SKOS.member).getList().asJavaList();
        assertEquals(2, members.size());
        assertEquals(List.of("c1", "c2"), members.stream().map(m -> m.asResource().getLocalName()).toList());
    }

    @Test
    void testMapUpdateCollection() {
        var model = TestUtils.getDefaultModel("test");
        var dto = new ConceptCollectionDTO();
        dto.setIdentifier("collection-1");
        dto.setLabel(Map.of("en", "Old label"));
        dto.setMembers(Set.of(
                "https://iri.suomi.fi/terminology/old-1",
                "https://iri.suomi.fi/terminology/old-2")
        );
        ConceptCollectionMapper.dtoToModel(model, dto, TestUtils.mockUser);
        var vocabulary = unmarshall("/ntrf/concept-collection.xml");

        NTRFMapper.mapTerminology(vocabulary, model, TestUtils.mockUser);

        var collection = model.getResourceById("collection-1");

        assertEquals("Test collection", MapperUtils.localizedPropertyToMap(collection, SKOS.prefLabel).get("fi"));

        var members = collection.getProperty(SKOS.member).getList().asJavaList();
        assertEquals(2, members.size());
        assertEquals(List.of("c1", "c2"), members.stream().map(m -> m.asResource().getLocalName()).toList());
    }

    private static List<Resource> getTerm(Resource concept, Property property, String language) {
        return concept.listProperties(property).toList().stream()
                .filter(term -> term.getObject().asResource()
                        .getProperty(SKOSXL.literalForm)
                        .getObject()
                        .asLiteral()
                        .getLanguage()
                        .equals(language))
                .map(term -> term.getObject().asResource())
                .toList();
    }

    private static List<String> getLocalizedList(Resource concept, Property property, String language) {
        if (!concept.hasProperty(property)) {
            return new ArrayList<>();
        }
        return concept.getProperty(property).getList().asJavaList().stream()
                .map(RDFNode::asLiteral)
                .filter(n -> n.getLanguage().equals(language))
                .map(Literal::getString)
                .toList();
    }

    private static List<String> getListValues(Resource resource, Property property) {
        if (!resource.hasProperty(property)) {
            return new ArrayList<>();
        }
        return resource.getProperty(property)
                .getList().asJavaList().stream()
                .map(s -> s.asLiteral().getString())
                .toList();
    }

    private VOCABULARY unmarshall(String file) {
        try {
            var data = NTRFMapperTest.class.getResourceAsStream(file);
            assertNotNull(data);

            JAXBContext context = JAXBContext.newInstance(VOCABULARY.class);
            XMLInputFactory factory = XMLInputFactory.newFactory();
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            XMLStreamReader reader = factory.createXMLStreamReader(data);
            Unmarshaller unmarshaller = context.createUnmarshaller();

            return (VOCABULARY) unmarshaller.unmarshal(reader);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
