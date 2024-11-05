package fi.vm.yti.terminology.api.v2.mapper;

import fi.vm.yti.common.enums.Status;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.terminology.api.v2.TestUtils;
import fi.vm.yti.terminology.api.v2.exception.ExcelParseException;
import fi.vm.yti.terminology.api.v2.property.Term;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.apache.jena.vocabulary.SKOSXL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExcelMapperTest {

    @Test
    void mapSimpleExcel() throws IOException {
        var data = ExcelMapperTest.class.getResourceAsStream("/excel/excel_import_simple.xlsx");
        assertNotNull(data);
        var terminology = TestUtils.getModelFromFile(
                "/terminology-metadata.ttl",
                "https://iri.suomi.fi/terminology/test/");

        terminology.createResourceWithId("concept-0").addProperty(RDF.type, SKOS.Concept);
        terminology.createResourceWithId("concept-1").addProperty(RDF.type, SKOS.Concept);

        ExcelMapper.mapSimpleExcel(terminology, data, TestUtils.mockUser);

        // should auto generate identifier 'concept-2', because concept-0 and concept-1 already exist
        var concept = terminology.getResourceById("concept-2");

        assertEquals(Map.of(
                        "fi", "Määritelmä suomeksi",
                        "sv", "Määritelmä på svenska"
                ),
                MapperUtils.localizedPropertyToMap(concept, SKOS.definition));

        assertEquals(
                List.of("Huomautus 2", "Huomautus 1", "Note sv"),
                MapperUtils.getList(concept, Term.orderedNote)
                        .asJavaList().stream()
                        .map(r -> r.asLiteral().getString())
                        .toList());

        assertEquals(Status.VALID, MapperUtils.getStatus(concept));

        var examplesFI = concept.listProperties(SKOS.example, "fi").mapWith(Statement::getString).toList();
        var examplesSV = concept.listProperties(SKOS.example, "sv").mapWith(Statement::getString).toList();
        assertEquals("esimerkki fi", examplesFI.get(0));
        assertEquals("esimerkki sv", examplesSV.get(0));

        var expectedPrefLabels = Map.of("fi", "kissa", "sv", "katt");
        var prefLabels = concept.listProperties(SKOS.prefLabel)
                .mapWith(s -> s.getObject().asResource())
                .toList();
        assertEquals(expectedPrefLabels.size(), prefLabels.size());
        prefLabels.forEach(p -> {
            var value = p.getProperty(SKOSXL.literalForm);
            assertEquals(expectedPrefLabels.get(value.getLanguage()), value.getString());
        });

        var synonyms = getTerms(concept, Term.orderedSynonym);
        var notRecommendedSynonyms = getTerms(concept, Term.orderedNotRecommendedSynonym);
        assertEquals(List.of("kisu", "katti"), synonyms);
        assertEquals(List.of("mirri", "kisumisu"), notRecommendedSynonyms);

        assertEquals(List.of(
                "https://iri.suomi.fi/terminology/test/related-2",
                "https://iri.suomi.fi/terminology/test/related-1"
        ), MapperUtils.getList(concept, Term.orderedRelated).asJavaList().stream()
                .map(r -> r.asResource().getURI())
                .toList());
    }

    @Test
    void testTerminologyLanguageMissing() throws IOException {
        var data = ExcelMapperTest.class.getResourceAsStream("/excel/excel_import_simple.xlsx");
        assertNotNull(data);
        var terminology = TestUtils.getModelFromFile(
                "/terminology-metadata.ttl",
                "https://iri.suomi.fi/terminology/test/");

        terminology.getModelResource().removeAll(DCTerms.language);
        terminology.getModelResource().addProperty(DCTerms.language, "fi");

        var exception = assertThrows(ExcelParseException.class,
                () -> ExcelMapper.mapSimpleExcel(terminology, data, TestUtils.mockUser));
        assertEquals("terminology-missing-language", exception.getKey());
    }

    @CsvSource({
            "excel_import_pref_label_column_missing",
            "excel_import_pref_label_row_missing",
            "excel_import_duplicate_column",
            "excel_import_term_missing_lang"
    })
    @ParameterizedTest
    void testDataValidation(String fileName) {
        var errorMessages = Map.of(
                "excel_import_pref_label_column_missing", "pref-label-column-missing",
                "excel_import_pref_label_row_missing", "pref-label-row-missing",
                "excel_import_duplicate_column", "duplicate-key-value",
                "excel_import_term_missing_lang", "column-missing-language"
        );

        var data = ExcelMapperTest.class.getResourceAsStream(String.format("/excel/%s.xlsx", fileName));
        assertNotNull(data);
        var terminology = TestUtils.getModelFromFile(
                "/terminology-metadata.ttl",
                "https://iri.suomi.fi/terminology/test/");

        var exception = assertThrows(ExcelParseException.class,
                () -> ExcelMapper.mapSimpleExcel(terminology, data, TestUtils.mockUser));
        assertEquals(errorMessages.get(fileName), exception.getKey());
    }

    private static List<String> getTerms(Resource concept, Property property) {
        return MapperUtils.getResourceList(concept, property).stream()
                .filter(s -> s.getProperty(SKOSXL.literalForm).getLanguage().equals("fi"))
                .map(s -> s.getProperty(SKOSXL.literalForm).getString())
                .toList();
    }
}
