package fi.vm.yti.terminology.api.importapi.excel;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Create Excel from provided JSON data.
 */
public class ExcelCreator {
    private static final String TERMINOLOGICAL_VOCABULARY = "TerminologicalVocabulary";
    private static final String COLLECTION = "Collection";
    private static final String CONCEPT = "Concept";
    private static final String TERM = "Term";

    /**
     * JSON data used as input.
     */
    @NotNull
    private final List<JSONWrapper> wrappers;

    private String filename;

    public ExcelCreator(@NotNull List<JSONWrapper> wrappers) {
        this.wrappers = wrappers;
    }

    /**
     * Create Excel workbook
     */
    public @NotNull Workbook createExcel() {
        Workbook workbook = new XSSFWorkbook();

        this.createTerminologyDetailsSheet(workbook);
        this.createCollectionsSheet(workbook);
        this.createConceptsSheet(workbook);
        this.createTermsSheet(workbook);

        return workbook;
    }

    /**
     * Create Terminology Details sheet in the workbook.
     * <p>
     * It loops over all terminological vocabularies (even there should be only 1) and creates a row for each.
     * All the vocabulary basic details are mapped from JSON to Excel here.
     */
    private void createTerminologyDetailsSheet(@NotNull Workbook workbook) {
        SheetDTO dto = new SheetDTO();
        for (JSONWrapper terminology : this.wrappersOfType(TERMINOLOGICAL_VOCABULARY)) {
            this.updateFilename(terminology);

            dto.disableMultiColumnMode("LANGUAGE");

            dto.addDataToCurrentRow("CODE", terminology.getCode());
            this.addProperty("NAME", "prefLabel", terminology, dto);
            this.addProperty("LANGUAGE", "language", terminology, dto);
            this.addPropertyOfReference("INFORMATIONDOMAIN", "inGroup", "prefLabel", terminology, dto);
            dto.addDataToCurrentRow("VOCABULARYTYPE", terminology.getTypeAsText());
            this.addProperty("DESCRIPTION", "description", terminology, dto);
            this.addProperty("STATUS", "status", terminology, dto);
            this.addPropertyOfReference("CONTRIBUTOR", "contributor", "prefLabel", terminology, dto);
            this.addProperty("CONTACT", "contact", terminology, dto);
            this.addCommonProperties(dto, terminology);

            dto.nextRow();
        }

        Sheet sheet = workbook.createSheet("Terminology details");
        dto.fillSheet(sheet);
    }

    /**
     * Create Collections sheet in the workbook.
     * <p>
     * It loops over all collections and creates a row for each.
     */
    private void createCollectionsSheet(@NotNull Workbook workbook) {
        SheetDTO dto = new SheetDTO();
        for (JSONWrapper terminology : this.wrappersOfType(COLLECTION)) {
            dto.addDataToCurrentRow("CODE", terminology.getCode());
            this.addProperty("NAME", "prefLabel", terminology, dto);
            this.addProperty("DEFINITION", "definition", terminology, dto);
            this.addCodeOfReference("COLLECTIONBROADER", "broader", terminology, dto);
            this.addCodeOfReference("MEMBER", "member", terminology, dto);
            this.addCommonProperties(dto, terminology);

            dto.nextRow();
        }

        Sheet sheet = workbook.createSheet("Collections");
        dto.fillSheet(sheet);
    }

    /**
     * Create Concepts sheet in the workbook.
     * <p>
     * It loops over all concepts and creates a row for each.
     */
    private void createConceptsSheet(@NotNull Workbook workbook) {
        SheetDTO dto = new SheetDTO();
        for (JSONWrapper terminology : this.wrappersOfType(CONCEPT)) {
            dto.addDataToCurrentRow("CODE", terminology.getCode());
            this.addCodeOfReference("PREFERREDTERM", "prefLabelXl", terminology, dto);
            this.addCodeOfReference("SYNONYM", "altLabelXl", terminology, dto);
            this.addCodeOfReference("NONRECOMMENDEDSYNONYM", "notRecommendedSynonym", terminology, dto);
            this.addCodeOfReference("HIDDENTERM", "hiddenTerm", terminology, dto);
            this.addProperty("DEFINITION", "definition", terminology, dto);
            this.addProperty("NOTE", "note", terminology, dto);
            this.addProperty("EDITORIALNOTE", "editorialNote", terminology, dto);
            this.addProperty("EXAMPLE", "example", terminology, dto);
            this.addProperty("CONCEPTSCOPE", "conceptScope", terminology, dto);
            this.addProperty("CONCEPTCLASS", "conceptClass", terminology, dto);
            this.addProperty("WORDCLASS", "wordClass", terminology, dto);
            this.addProperty("CHANGENOTE", "changeNote", terminology, dto);
            this.addProperty("HISTORYNOTE", "historyNote", terminology, dto);
            this.addProperty("STATUS", "status", terminology, dto);
            this.addProperty("NOTATION", "notation", terminology, dto);
            this.addProperty("SOURCE", "source", terminology, dto);
            this.addCodeOfReference("BROADERCONCEPT", "broader", terminology, dto);
            this.addCodeOfReference("NARROWERCONCEPT", "narrower", terminology, dto);
            this.addConceptLink("CLOSEMATCHINOTHERVOCABULARY", "closeMatch", terminology, dto);
            this.addCodeOfReference("RELATEDCONCEPT", "related", terminology, dto);
            this.addCodeOfReference("ISPARTOFCONCEPT", "isPartOf", terminology, dto);
            this.addCodeOfReference("HASPARTCONCEPT", "hasPart", terminology, dto);
            this.addConceptLink("RELATEDCONCEPTINOTHERVOCABULARY", "relatedMatch", terminology, dto);
            this.addConceptLink("MATCHINGCONCEPTINOTHERVOCABULARY", "exactMatch", terminology, dto);
            this.addCodeOfReference("SEARCHTERM", "searchTerm", terminology, dto);
            this.addCommonProperties(dto, terminology);

            dto.nextRow();
        }

        Sheet sheet = workbook.createSheet("Concepts");
        dto.fillSheet(sheet);
    }

    /**
     * Create Terms sheet in the workbook.
     * <p>
     * It loops over all terms and creates a row for each.
     */
    private void createTermsSheet(@NotNull Workbook workbook) {
        SheetDTO dto = new SheetDTO();
        for (JSONWrapper terminology : this.wrappersOfType(TERM)) {
            dto.addDataToCurrentRow("CODE", terminology.getCode());
            this.addProperty("TERMLITERALVALUE", "prefLabel", terminology, dto);
            this.addProperty("SOURCE", "source", terminology, dto);
            this.addProperty("SCOPE", "scope", terminology, dto);
            this.addProperty("TERMSTYLE", "termStyle", terminology, dto);
            this.addProperty("TERMFAMILY", "termFamily", terminology, dto);
            this.addProperty("TERMCONJUGATION", "termConjugation", terminology, dto);
            this.addProperty("TERMEQUIVALENCY", "termEquivalency", terminology, dto);
            this.addProperty("TERMINFO", "termInfo", terminology, dto);
            this.addProperty("WORDCLASS", "wordClass", terminology, dto);
            this.addProperty("HOMOGRAPHNUMBER", "termHomographNumber", terminology, dto);
            this.addProperty("EDITORIALNOTE", "editorialNote", terminology, dto);
            this.addProperty("DRAFTCOMMENT", "draftComment", terminology, dto);
            this.addProperty("HISTORYNOTE", "historyNote", terminology, dto);
            this.addProperty("CHANGENOTE", "changeNote", terminology, dto);
            this.addProperty("STATUS", "status", terminology, dto);
            this.addCommonProperties(dto, terminology);

            dto.nextRow();
        }

        Sheet sheet = workbook.createSheet("Terms");
        dto.fillSheet(sheet);
    }

    /**
     * Add common details to the given sheet. This could be called last.
     */
    private void addCommonProperties(@NotNull SheetDTO dto, @NotNull JSONWrapper wrapper) {
        dto.addDataToCurrentRow("CREATED", wrapper.getCreatedDate());
        dto.addDataToCurrentRow("MODIFIED", wrapper.getLastModifiedDate());
        dto.addDataToCurrentRow("URI", wrapper.getURI());
    }

    /**
     * Map given property from JSON to SheetDTO.
     */
    private void addProperty(
            @NotNull String columnName,
            @NotNull String propertyName,
            @NotNull JSONWrapper wrapper,
            @NotNull SheetDTO dto
    ) {
        dto.addColumn(columnName);
        wrapper.getProperty(propertyName).forEach((language, values) -> {
            dto.addDataToCurrentRow(columnName, language, values);
        });
    }

    /**
     * Map given property of reference from JSON to SheetDTO.
     */
    private void addPropertyOfReference(
            @NotNull String columnName,
            @NotNull String referenceName,
            @NotNull String propertyName,
            @NotNull JSONWrapper wrapper,
            @NotNull SheetDTO dto
    ) {
        dto.addColumn(columnName);

        List<String> values = wrapper.getReference(referenceName).stream()
                .flatMap(group -> Stream.of(group.getFirstPropertyValue(propertyName, "en")))
                .collect(Collectors.toList());

        dto.addDataToCurrentRow(columnName, values);
    }

    /**
     * Map code of reference from JSON to SheetDTO.
     */
    private void addCodeOfReference(
            @NotNull String columnName,
            @NotNull String referenceName,
            @NotNull JSONWrapper wrapper,
            @NotNull SheetDTO dto
    ) {
        dto.addColumn(columnName);

        List<String> values = wrapper.getReference(referenceName).stream()
                .flatMap(group -> Stream.of(group.getCode()))
                .collect(Collectors.toList());

        dto.addDataToCurrentRow(columnName, values);
    }

    /**
     * Map concept link as URI of the concept in other terminology from JSON to SheetDTO.
     */
    private void addConceptLink(
            @NotNull String columnName,
            @NotNull String referenceName,
            @NotNull JSONWrapper wrapper,
            @NotNull SheetDTO dto
    ) {
        dto.addColumn(columnName);

        List<String> values = wrapper.getReference(referenceName).stream()
                .flatMap(reference -> Stream.ofNullable(reference.getDefinition()))
                .map(JSONWrapper::getMemo)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        dto.addDataToCurrentRow(columnName, values);
    }

    /**
     * Filter JSON inputs by type.
     */
    private @NotNull List<JSONWrapper> wrappersOfType(@NotNull String type) {
        return this.wrappers.stream().filter(wrapper -> wrapper.getType().equals(type)).collect(Collectors.toList());
    }

    /**
     * Set the first terminology's name as the filename.
     */
    private void updateFilename(@NotNull JSONWrapper terminology) {
        if (this.filename == null) {
            this.filename = terminology.getFirstPropertyValue("prefLabel", "en");
        }
    }

    /**
     * Get filename set by updateFilename. If filename is not set, use "Terminology" as a default filename.
     */
    public String getFilename() {
        return this.filename != null ? this.filename : "Terminology";
    }
}
