package fi.vm.yti.terminology.api.importapi.excel;

import org.apache.poi.ss.formula.functions.Column;
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
        var builder = new DTOBuilder();
        for (JSONWrapper terminology : this.wrappersOfType(TERMINOLOGICAL_VOCABULARY)) {
            this.updateFilename(terminology);

            builder.addDataToCurrentRow("IDENTIFIER", terminology.getCode());
            this.addProperty("PREFLABEL", "prefLabel", terminology, builder);
            this.addProperty("LANGUAGE", "language", terminology, builder, ColumnDTO.MULTI_COLUMN_MODE_DISABLED);
            this.addPropertyOfReference(
                    "INFORMATIONDOMAIN",
                    "inGroup",
                    "notation",
                    terminology,
                    builder,
                    ColumnDTO.MULTI_COLUMN_MODE_DISABLED
            );
            builder.addDataToCurrentRow("VOCABULARYTYPE", terminology.getTypeAsText());
            this.addProperty("DESCRIPTION", "description", terminology, builder);
            this.addProperty("STATUS", "status", terminology, builder);
            this.addPropertyOfReference(
                    "CONTRIBUTOR",
                    "contributor",
                    "prefLabel",
                    terminology,
                    builder
            );
            this.addProperty("CONTACT", "contact", terminology, builder);
            this.addCommonProperties(builder, terminology);

            builder.nextRow();
        }

        Sheet sheet = workbook.createSheet("Terminology details");
        new ExcelBuilder().renderSheetDTO(sheet, builder.getSheet());
    }

    /**
     * Create Collections sheet in the workbook.
     * <p>
     * It loops over all collections and creates a row for each.
     */
    private void createCollectionsSheet(@NotNull Workbook workbook) {
        var builder = new DTOBuilder();
        for (JSONWrapper terminology : this.wrappersOfType(COLLECTION)) {
            builder.addDataToCurrentRow("IDENTIFIER", terminology.getCode());
            this.addProperty("PREFLABEL", "prefLabel", terminology, builder);
            this.addProperty("DEFINITION", "definition", terminology, builder);
            this.addCodeOfReference("COLLECTIONBROADER", "broader", terminology, builder);
            this.addCodeOfReference("MEMBER", "member", terminology, builder);
            this.addCommonProperties(builder, terminology);

            builder.nextRow();
        }

        Sheet sheet = workbook.createSheet("Collections");
        new ExcelBuilder().renderSheetDTO(sheet, builder.getSheet());
    }

    /**
     * Create Concepts sheet in the workbook.
     * <p>
     * It loops over all concepts and creates a row for each.
     */
    private void createConceptsSheet(@NotNull Workbook workbook) {
        var builder = new DTOBuilder();
        for (JSONWrapper terminology : this.wrappersOfType(CONCEPT)) {
            builder.addDataToCurrentRow("IDENTIFIER", terminology.getCode());
            this.addCodeOfReference("PREFERREDTERM", "prefLabelXl", terminology, builder);
            this.addCodeOfReference("SYNONYM", "altLabelXl", terminology, builder);
            this.addCodeOfReference("NONRECOMMENDEDSYNONYM", "notRecommendedSynonym", terminology, builder);
            this.addCodeOfReference("HIDDENTERM", "hiddenTerm", terminology, builder);
            this.addProperty("DEFINITION", "definition", terminology, builder);
            this.addProperty("NOTE", "note", terminology, builder);
            // TODO: EDITORIALNOTE should be visible if the user has write permissions to the terminology.
//            this.addProperty("EDITORIALNOTE", "editorialNote", terminology, builder);
            this.addProperty("EXAMPLE", "example", terminology, builder);
            // TODO: Add data to SUBJECTAREA when implemented. Currently only placeholder column is added.
            builder.ensureColumn("SUBJECTAREA", ColumnDTO.MULTI_COLUMN_MODE_ENABLED);
            this.addProperty("CONCEPTCLASS", "conceptClass", terminology, builder);
            this.addProperty("WORDCLASS", "wordClass", terminology, builder);
            this.addProperty("CHANGENOTE", "changeNote", terminology, builder);
            this.addProperty("HISTORYNOTE", "historyNote", terminology, builder);
            this.addProperty("STATUS", "status", terminology, builder);
            this.addProperty("NOTATION", "notation", terminology, builder);
            this.addProperty("SOURCE", "source", terminology, builder);
            this.addCodeOfReference("BROADERCONCEPT", "broader", terminology, builder);
            this.addCodeOfReference("NARROWERCONCEPT", "narrower", terminology, builder);
            this.addConceptLink("CLOSEMATCHINOTHERVOCABULARY", "closeMatch", terminology, builder);
            this.addCodeOfReference("RELATEDCONCEPT", "related", terminology, builder);
            this.addCodeOfReference("ISPARTOFCONCEPT", "isPartOf", terminology, builder);
            this.addCodeOfReference("HASPARTCONCEPT", "hasPart", terminology, builder);
            this.addConceptLink("RELATEDCONCEPTINOTHERVOCABULARY", "relatedMatch", terminology, builder);
            this.addConceptLink("MATCHINGCONCEPTINOTHERVOCABULARY", "exactMatch", terminology, builder);
            this.addCodeOfReference("SEARCHTERM", "searchTerm", terminology, builder);
            this.addCommonProperties(builder, terminology);

            builder.nextRow();
        }

        Sheet sheet = workbook.createSheet("Concepts");
        new ExcelBuilder().renderSheetDTO(sheet, builder.getSheet());
    }

    /**
     * Create Terms sheet in the workbook.
     * <p>
     * It loops over all terms and creates a row for each.
     */
    private void createTermsSheet(@NotNull Workbook workbook) {
        var builder = new DTOBuilder();
        for (JSONWrapper terminology : this.wrappersOfType(TERM)) {
            builder.addDataToCurrentRow("IDENTIFIER", terminology.getCode());
            this.addProperty("TERMLITERALVALUE", "prefLabel", terminology, builder);
            this.addProperty("SOURCE", "source", terminology, builder);
            this.addProperty("SCOPE", "scope", terminology, builder);
            this.addProperty("TERMSTYLE", "termStyle", terminology, builder);
            this.addProperty("TERMFAMILY", "termFamily", terminology, builder);
            this.addProperty("TERMCONJUGATION", "termConjugation", terminology, builder);
            this.addProperty("TERMEQUIVALENCY", "termEquivalency", terminology, builder);
            this.addProperty("TERMINFO", "termInfo", terminology, builder);
            this.addProperty("WORDCLASS", "wordClass", terminology, builder);
            this.addProperty("HOMOGRAPHNUMBER", "termHomographNumber", terminology, builder);
            this.addProperty("EDITORIALNOTE", "editorialNote", terminology, builder);
            this.addProperty("DRAFTCOMMENT", "draftComment", terminology, builder);
            this.addProperty("HISTORYNOTE", "historyNote", terminology, builder);
            this.addProperty("CHANGENOTE", "changeNote", terminology, builder);
            this.addProperty("STATUS", "status", terminology, builder);
            this.addCommonProperties(builder, terminology);

            builder.nextRow();
        }

        Sheet sheet = workbook.createSheet("Terms");
        new ExcelBuilder().renderSheetDTO(sheet, builder.getSheet());
    }

    /**
     * Add common details to the given sheet. This could be called last.
     */
    private void addCommonProperties(@NotNull DTOBuilder builder, @NotNull JSONWrapper wrapper) {
        builder.addDataToCurrentRow("CREATED", wrapper.getCreatedDate());
        builder.addDataToCurrentRow("MODIFIED", wrapper.getLastModifiedDate());
        builder.addDataToCurrentRow("URI", wrapper.getURI());
        builder.addDataToCurrentRow("OPERATION", "");
    }

    private void addProperty(
            @NotNull String columnName,
            @NotNull String propertyName,
            @NotNull JSONWrapper wrapper,
            @NotNull DTOBuilder builder) {
        this.addProperty(columnName, propertyName, wrapper, builder, ColumnDTO.MULTI_COLUMN_MODE_ENABLED);
    }

    /**
     * Map given property from JSON to SheetDTO.
     */
    private void addProperty(
            @NotNull String columnName,
            @NotNull String propertyName,
            @NotNull JSONWrapper wrapper,
            @NotNull DTOBuilder builder,
            boolean multiColumnModeDisabled) {
        builder.ensureColumn(columnName, multiColumnModeDisabled);
        wrapper.getProperty(propertyName).forEach((language, values) -> {
            builder.addDataToCurrentRow(columnName, language, values, multiColumnModeDisabled);
        });
    }

    private void addPropertyOfReference(
            @NotNull String columnName,
            @NotNull String referenceName,
            @NotNull String propertyName,
            @NotNull JSONWrapper wrapper,
            @NotNull DTOBuilder builder) {
        this.addPropertyOfReference(
                columnName,
                referenceName,
                propertyName,
                wrapper,
                builder,
                ColumnDTO.MULTI_COLUMN_MODE_ENABLED
        );
    }

    /**
     * Map given property of reference from JSON to SheetDTO.
     */
    private void addPropertyOfReference(
            @NotNull String columnName,
            @NotNull String referenceName,
            @NotNull String propertyName,
            @NotNull JSONWrapper wrapper,
            @NotNull DTOBuilder builder,
            boolean multiColumnModeDisabled) {
        List<String> values = wrapper.getReference(referenceName).stream()
                .flatMap(group -> Stream.of(group.getFirstPropertyValue(propertyName, "en")))
                .collect(Collectors.toList());

        builder.addDataToCurrentRow(columnName, "", values, multiColumnModeDisabled);
    }

    /**
     * Map code of reference from JSON to SheetDTO.
     */
    private void addCodeOfReference(
            @NotNull String columnName,
            @NotNull String referenceName,
            @NotNull JSONWrapper wrapper,
            @NotNull DTOBuilder builder) {
        List<String> values = wrapper.getReference(referenceName).stream()
                .flatMap(group -> Stream.of(group.getCode()))
                .collect(Collectors.toList());

        builder.addDataToCurrentRow(columnName, values);
    }

    /**
     * Map concept link as URI of the concept in other terminology from JSON to SheetDTO.
     */
    private void addConceptLink(
            @NotNull String columnName,
            @NotNull String referenceName,
            @NotNull JSONWrapper wrapper,
            @NotNull DTOBuilder builder) {
        List<String> values = wrapper.getReference(referenceName).stream()
                .flatMap(reference -> Stream.ofNullable(reference.getDefinition()))
                .map(JSONWrapper::getMemo)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        builder.addDataToCurrentRow(columnName, values);
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
