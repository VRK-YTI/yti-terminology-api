package fi.vm.yti.terminology.api.v2.migration;

import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.terminology.api.v2.TestUtils;
import fi.vm.yti.terminology.api.v2.migration.task.V3_RDFListConversion;
import fi.vm.yti.terminology.api.v2.property.Term;
import fi.vm.yti.terminology.api.v2.repository.TerminologyRepository;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.apache.jena.vocabulary.SKOSXL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import({
        V3_RDFListConversion.class
})
class RDFListConversionTest {

    @MockBean
    TerminologyRepository repository;

    @Autowired
    private V3_RDFListConversion migration;

    @Test
    void testConvertRDFLists() {
        var uri = TerminologyURI.createTerminologyURI("order-test");
        var model = TestUtils.getModelFromFile("/rdf-list-conversion.ttl", uri.getGraphURI());

        var qs = mock(QuerySolution.class);

        when(qs.get(anyString())).thenReturn(ResourceFactory.createResource(uri.getGraphURI()));

        doAnswer(ans -> {
            Consumer<QuerySolution> cons = ans.getArgument(1, Consumer.class);
            cons.accept(qs);
            return null;
        }).when(repository).querySelect(anyString(), any(Consumer.class));

        when(repository.fetch(anyString())).thenReturn(model);

        migration.migrate();

        var modelCaptor = ArgumentCaptor.forClass(Model.class);
        verify(repository).put(eq(uri.getGraphURI()), modelCaptor.capture());

        var result = modelCaptor.getValue();

        result.listSubjectsWithProperty(RDF.type, SKOS.Concept).forEach(c -> {
            var prefLabels = c.listProperties(SKOS.prefLabel).toList();
            assertFalse(prefLabels.isEmpty());

            prefLabels.forEach(label -> {
                var labelResource = result.getResource(label.getResource().getURI());
                assertTrue(labelResource.hasProperty(SKOSXL.literalForm));
            });
        });

        var concept = result.getResource(uri.getGraphURI() + "order-1");

        var synonyms = concept.getProperty(Term.orderedSynonym)
                .getList().asJavaList().stream()
                .map(s -> s.asResource().getProperty(SKOSXL.literalForm).getString())
                .toList();

        List<String> expectedSynonyms = List.of("synonyymi1", "synonyymi2", "synonyymi3");

        assertTrue(expectedSynonyms.containsAll(concept.listProperties(SKOS.altLabel)
                .toList().stream()
                .map(s -> s.getResource().getProperty(SKOSXL.literalForm).getString())
                .toList()));

        assertEquals(expectedSynonyms, synonyms);

        var collection = result.getResource(uri.getGraphURI() + "collection-2000");

        var expectedMembers = List.of(
                "https://iri.suomi.fi/terminology/order-test/c340",
                "https://iri.suomi.fi/terminology/order-test/concept-3",
                "https://iri.suomi.fi/terminology/order-test/order-1");

        assertTrue(expectedMembers.containsAll(collection.listProperties(SKOS.member)
                .mapWith(s -> s.getResource().getURI()).toList()));
        assertEquals(expectedMembers, MapperUtils.getResourceList(collection, Term.orderedMember).stream()
                .map(Resource::getURI)
                .toList());
    }
}
