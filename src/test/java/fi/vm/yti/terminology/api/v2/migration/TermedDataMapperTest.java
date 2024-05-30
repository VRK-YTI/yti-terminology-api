package fi.vm.yti.terminology.api.v2.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.common.dto.ServiceCategoryDTO;
import fi.vm.yti.common.enums.GraphType;
import fi.vm.yti.common.enums.Status;
import fi.vm.yti.terminology.api.v2.TestUtils;
import fi.vm.yti.terminology.api.v2.enums.TermType;
import fi.vm.yti.terminology.api.v2.migration.v1.TermedDataMapper;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TermedDataMapperTest {

    @Test
    void testMapTerminology() {
        var oldData = getData();

        var serviceCategory = new ServiceCategoryDTO("http://urn.fi/URN:NBN:fi:au:ptvl/v1090",
                Map.of("fi", "Asuminen"), "P10");

        var dto = TermedDataMapper.mapTerminology(oldData.getResource("http://uri.suomi.fi/terminology/test"), List.of(serviceCategory));

        assertNotNull(dto);
        assertEquals("test", dto.getPrefix());
        assertEquals(Map.of(
                "fi", "test fi",
                "sv", "test sv",
                "en", "test en"
            ), dto.getLabel());
        assertEquals(Status.VALID, dto.getStatus());
        assertEquals(Set.of(UUID.fromString("7d3a3c00-5a6b-489b-a3ed-63bb58c26a63")), dto.getOrganizations());
        assertEquals("yhteentoimivuus@dvv.fi", dto.getContact());
        assertEquals(Set.of("P10"), dto.getGroups());
        assertEquals(GraphType.OTHER_VOCABULARY, dto.getGraphType());
    }

    @Test
    void testMapConcept() {
        var oldData = getData();

        var dto = TermedDataMapper.mapConcept(oldData, oldData.getResource("http://uri.suomi.fi/terminology/test/c340"),
                new ObjectMapper(), "fi");

        assertEquals(5, dto.getTerms().size());
        assertEquals(1, dto.getReferences().size());
        assertEquals(4, dto.getNotes().size());

        assertEquals("test definition", dto.getDefinition().get("fi"));

        var prefTerm = dto.getTerms().stream().filter(t -> t.getTermType().equals(TermType.RECOMMENDED)).findFirst();
        var synonym = dto.getTerms().stream().filter(t -> t.getTermType().equals(TermType.SYNONYM)).findFirst();
        var searchTerm = dto.getTerms().stream().filter(t -> t.getTermType().equals(TermType.SEARCH_TERM)).findFirst();
        var notRecommended = dto.getTerms().stream().filter(t -> t.getTermType().equals(TermType.NOT_RECOMMENDED)).toList();

        assertTrue(prefTerm.isPresent());
        assertTrue(synonym.isPresent());
        assertTrue(searchTerm.isPresent());
        assertEquals(2, notRecommended.size());

        assertEquals("pref term label", prefTerm.get().getLabel());
        assertEquals("fi", prefTerm.get().getLanguage());

        assertEquals(1, dto.getReferences().size());
    }

    private Model getData() {
        var model = ModelFactory.createDefaultModel();
        var stream = TestUtils.class.getResourceAsStream("/v1-migration/termed-data.ttl");
        assertNotNull(stream);
        RDFDataMgr.read(model, stream, RDFLanguages.TURTLE);

        return model;
    }
}
