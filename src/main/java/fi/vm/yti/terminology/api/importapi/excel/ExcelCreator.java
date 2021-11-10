package fi.vm.yti.terminology.api.importapi.excel;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.NotNull;

import java.util.List;
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

            dto.disableMultiColumnMode("Language");

            dto.addDataToCurrentRow("Code", terminology.getCode());
            this.addProperty("Name", "prefLabel", terminology, dto);
            this.addProperty("Language", "language", terminology, dto);
            this.addProperty("Priority name", "priority", terminology, dto);
            this.addPropertyOfReference("Information domain", "inGroup", "prefLabel", terminology, dto);
            dto.addDataToCurrentRow("Vocabulary type", terminology.getType());
            this.addProperty("Description", "description", terminology, dto);
            this.addProperty("Terminology status", "status", terminology, dto);
            this.addPropertyOfReference("Contributor", "contributor", "prefLabel", terminology, dto);
            this.addProperty("Contact", "contact", terminology, dto);
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
            dto.addDataToCurrentRow("Code", terminology.getCode());
            this.addProperty("Name", "prefLabel", terminology, dto);
            this.addProperty("Definition", "definition", terminology, dto);
            this.addCodeOfReference("Collection broader", "broader", terminology, dto);
            this.addCodeOfReference("Member", "member", terminology, dto);
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
            dto.addDataToCurrentRow("Code", terminology.getCode());
            this.addCodeOfReference("Preferred term", "prefLabelXl", terminology, dto);
            this.addCodeOfReference("Synonym", "altLabelXl", terminology, dto);
            this.addCodeOfReference("Non-recommended synonym", "notRecommendedSynonym", terminology, dto);
            this.addCodeOfReference("Hidden term", "hiddenTerm", terminology, dto);
            this.addProperty("Definition", "definition", terminology, dto);
            this.addProperty("Note", "note", terminology, dto);
            this.addProperty("Editorial note", "editorialNote", terminology, dto);
            this.addProperty("Example", "example", terminology, dto);
            this.addProperty("Concept scope", "conceptScope", terminology, dto);
            this.addProperty("Concept class", "conceptClass", terminology, dto);
            this.addProperty("Word class", "wordClass", terminology, dto);
            this.addProperty("Change note", "changeNote", terminology, dto);
            this.addProperty("History note", "historyNote", terminology, dto);
            this.addProperty("Concept status", "status", terminology, dto);
            this.addProperty("Notation", "notation", terminology, dto);
            this.addProperty("Source", "source", terminology, dto);
            this.addCodeOfReference("Broader concept", "broader", terminology, dto);
            this.addCodeOfReference("Narrower concept", "narrower", terminology, dto);
            this.addConceptLink("Close match in other vocabulary", "closeMatch", terminology, dto);
            this.addCodeOfReference("Related concept", "related", terminology, dto);
            this.addCodeOfReference("Is part of concept", "isPartOf", terminology, dto);
            this.addCodeOfReference("Has part concept", "hasPart", terminology, dto);
            this.addConceptLink("Related concept in other vocabulary", "relatedMatch", terminology, dto);
            this.addConceptLink("Matching concept in other vocabulary", "exactMatch", terminology, dto);
            this.addCodeOfReference("Search term", "searchTerm", terminology, dto);
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
            dto.addDataToCurrentRow("Code", terminology.getCode());
            this.addProperty("Term literal value", "prefLabel", terminology, dto);
            this.addProperty("Source", "source", terminology, dto);
            this.addProperty("Scope", "scope", terminology, dto);
            this.addProperty("Term style", "termStyle", terminology, dto);
            this.addProperty("Term family", "termFamily", terminology, dto);
            this.addProperty("Term conjugation", "termConjugation", terminology, dto);
            this.addProperty("Term equivalency", "termEquivalency", terminology, dto);
            this.addProperty("Term info", "termInfo", terminology, dto);
            this.addProperty("Word class", "wordClass", terminology, dto);
            this.addProperty("Homograph number", "termHomographNumber", terminology, dto);
            this.addProperty("Editorial note", "editorialNote", terminology, dto);
            this.addProperty("Draft comment", "draftComment", terminology, dto);
            this.addProperty("History note", "historyNote", terminology, dto);
            this.addProperty("Change note", "changeNote", terminology, dto);
            this.addProperty("Term status", "status", terminology, dto);
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
        dto.addDataToCurrentRow("Created at", wrapper.getCreatedDate());
        // todo: uuid is not probably what we want here
        dto.addDataToCurrentRow("Created by", wrapper.getCreatedBy());
        dto.addDataToCurrentRow("Modified at", wrapper.getLastModifiedDate());
        // todo: uuid is not probably what we want here
        dto.addDataToCurrentRow("Modified by", wrapper.getLastModifiedBy());
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
     * Map concept link as vocabularyName/prefLabel from JSON to SheetDTO.
     */
    private void addConceptLink(
            @NotNull String columnName,
            @NotNull String referenceName,
            @NotNull JSONWrapper wrapper,
            @NotNull SheetDTO dto
    ) {
        dto.addColumn(columnName);

        List<String> values = wrapper.getReference(referenceName).stream()
                .flatMap(reference -> Stream.of(String.format(
                        "%s/%s",
                        reference.getFirstPropertyValue("vocabularyLabel", "en"),
                        reference.getFirstPropertyValue("prefLabel", "en")
                )))
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
