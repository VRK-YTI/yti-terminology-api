package fi.vm.yti.terminology.api.v2.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.common.enums.GraphType;
import fi.vm.yti.common.enums.Status;
import fi.vm.yti.terminology.api.v2.enums.TermConjugation;
import fi.vm.yti.terminology.api.v2.enums.TermEquivalency;
import fi.vm.yti.terminology.api.v2.enums.TermFamily;
import fi.vm.yti.terminology.api.v2.enums.WordClass;
import fi.vm.yti.terminology.api.v2.migration.v1.TermedDataMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TermedDataMapperTest {

    @Test
    void testMapTerminology() {
        var oldData = getTermedData("termed-terminology.json");

        var dto = TermedDataMapper.mapTerminology(oldData);

        assertNotNull(dto);
        assertEquals("test", dto.getPrefix());
        assertEquals(Map.of(
                "fi", "test fi",
                "sv", "test sv",
                "en", "test en"
            ), dto.getLabel());
        assertEquals(Map.of(
                "fi", "desc fi",
                "sv", "desc sv",
                "en", "desc en"
        ), dto.getDescription());
        assertEquals(Status.VALID, dto.getStatus());
        assertEquals(Set.of(UUID.fromString("7d3a3c00-5a6b-489b-a3ed-63bb58c26a63")), dto.getOrganizations());
        assertEquals("yhteentoimivuus@dvv.fi", dto.getContact());
        assertEquals(Set.of("P10"), dto.getGroups());
        assertEquals(GraphType.OTHER_VOCABULARY, dto.getGraphType());
    }

    @Test
    void testMapConcept() {
        var oldData = getTermedData("termed-concept.json");

        var dto = TermedDataMapper.mapConcept(oldData,"fi");

        assertEquals(1, dto.getRecommendedTerms().size());
        assertEquals(1, dto.getSynonyms().size());
        assertEquals(2, dto.getNotRecommendedTerms().size());
        assertEquals(1, dto.getSearchTerms().size());
        assertEquals(1, dto.getBroader().size());
        assertEquals(4, dto.getNotes().size());

        assertEquals("test definition", dto.getDefinition().get("fi"));
        assertEquals("c340", dto.getIdentifier());
        assertEquals("muutos", dto.getChangeNote());
        assertEquals("luokkakäsite", dto.getConceptClass());
        assertEquals("käyttö", dto.getHistoryNote());
        assertEquals(List.of("muistiinpano"), dto.getEditorialNotes());
        assertEquals(Status.VALID, dto.getStatus());
        assertEquals("Test aihealue", dto.getSubjectArea());

        var term = dto.getRecommendedTerms().iterator().next();

        assertEquals("pref term label", term.getLabel());
        assertEquals("fi", term.getLanguage());
        assertEquals("lisätieto", term.getTermInfo());
        assertEquals("tyyli", term.getTermStyle());
        assertEquals("term käytönhistoria", term.getHistoryNote());
        assertEquals("skooppi", term.getScope());
        assertEquals(TermConjugation.SINGULAR, term.getTermConjugation());
        assertEquals(TermFamily.MASCULINE, term.getTermFamily());
        assertEquals(WordClass.ADJECTIVE, term.getWordClass());
        assertEquals(Status.VALID, term.getStatus());
        assertEquals(1, term.getHomographNumber());
        assertEquals("term muutoshistoria", term.getChangeNote());
        assertEquals(TermEquivalency.BROADER, term.getTermEquivalency());
        assertEquals(List.of("term muistiinpano"), term.getEditorialNotes());
        assertEquals(List.of("lähde 2", "termin lähde"), term.getSources());

        var ref = dto.getBroader().iterator().next();

        assertEquals("https://iri.suomi.fi/terminology/test/broader-test", ref);
    }

    @Test
    void testMapCollection() {
        var oldData = getTermedData("termed-collection.json");

        var dto = TermedDataMapper.mapCollection(oldData);

        assertEquals("Testikokoelma", dto.getLabel().get("fi"));
        assertEquals("Kuvaus", dto.getDescription().get("fi"));
        assertEquals(Set.of("c1", "c2"), dto.getMembers());
    }

    private JsonNode getTermedData(String file) {
        try {
            return new ObjectMapper().readTree(
                    TermedDataMapperTest.class.getResourceAsStream("/v1-migration/" + file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
