package fi.vm.yti.terminology.api.importapi.excel;

import fi.vm.yti.terminology.api.frontend.Status;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.NotNull;

import java.text.Normalizer;
import java.util.*;
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
    private static final String CONCEPT_LINK = "ConceptLink";

    /**
     * JSON data used as input.
     */
    @NotNull
    private final List<JSONWrapper> wrappers;

    private String filename;

    private static final Map<String, List<TermPlaceHolderDTO>> placeHolderTerms = new HashMap<>();

    private static final Map<String, JSONWrapper> wrapperNodeMap = new HashMap<>();

    public ExcelCreator(@NotNull List<JSONWrapper> wrappers) {
        this.wrappers = wrappers;

        // create id map to make it easier to check if placeholder term needs to be generated
        wrappers.forEach(wrapper -> wrapperNodeMap.put(wrapper.getID(), wrapper));
    }

    /**
     * Create Excel workbook,
     *
     * @param placeHolderLanguages list of languages to create placeholder terms for
     */
    public @NotNull Workbook createExcel(List<String> placeHolderLanguages, boolean isInOrganization) {
        Workbook workbook = new XSSFWorkbook();

        this.createTerminologyDetailsSheet(workbook);
        this.createCollectionsSheet(workbook);
        this.createConceptsSheet(workbook, placeHolderLanguages, isInOrganization);
        this.createTermsSheet(workbook, isInOrganization);
        this.createConceptLinksSheet(workbook);

        return workbook;
    }

    /**
     * Create Excel workbook
     */
    public @NotNull Workbook createExcel(boolean isInOrganization) {
        return createExcel(List.of(), isInOrganization);
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
            this.addUuidOfReference(
                    "CONTRIBUTOR",
                    "contributor",
                    terminology,
                    builder
            );
            this.addProperty("CONTACT", "contact", terminology, builder);
            this.addCommonProperties(builder, terminology);
            builder.addDataToCurrentRow("GRAPH_ID", terminology.getGraphId());
            builder.addDataToCurrentRow("UUID", terminology.getID());
            builder.addDataToCurrentRow("NAMESPACE", terminology.getNamespace());

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
            // description is incorrectly named as definition in the database
            this.addProperty("DESCRIPTION", "definition", terminology, builder);
            this.addCodeOfReference("MEMBER", "member", terminology, builder);
            this.addCommonProperties(builder, terminology);
            builder.addDataToCurrentRow("UUID", terminology.getID());
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
    private void createConceptsSheet(@NotNull Workbook workbook,
                                     List<String> placeHolderLanguages,
                                     boolean isInOrganization) {
        var builder = new DTOBuilder();
        for (JSONWrapper terminology : this.wrappersOfType(CONCEPT)) {
            builder.addDataToCurrentRow("IDENTIFIER", terminology.getCode());
            // add preferred term name to concept sheet to make it easier to understand which concept
            // information is displayed
            Map<String, List<String>> prefLabel = terminology.getReference("prefLabelXl").get(0).getProperty("prefLabel");
            builder.addDataToCurrentRow("PREFERRED_TERM_LABEL", prefLabel
                    .get(prefLabel.keySet().iterator().next())
                    .get(0));
            this.addProperty("DEFINITION", "definition", terminology, builder);
            this.addProperty("NOTE", "note", terminology, builder);
            if (isInOrganization) {
                this.addProperty("EDITORIALNOTE", "editorialNote", terminology, builder);
            }
            this.addProperty("EXAMPLE", "example", terminology, builder);
            this.addProperty("SUBJECTAREA", "subjectArea", terminology, builder);
            this.addProperty("CONCEPTCLASS", "conceptClass", terminology, builder);
            this.addProperty("WORDCLASS", "wordClass", terminology, builder);
            this.addProperty("CHANGENOTE", "changeNote", terminology, builder);
            this.addProperty("HISTORYNOTE", "historyNote", terminology, builder);
            this.addProperty("STATUS", "status", terminology, builder);
            this.addProperty("NOTATION", "notation", terminology, builder);
            this.addProperty("SOURCE", "source", terminology, builder);
            this.addProperty("EXTERNALLINK", "externalLink", terminology, builder);

            this.addCommonProperties(builder, terminology);

            if (placeHolderLanguages.isEmpty()) {
                this.addCodeOfReference("PREFERREDTERM", "prefLabelXl", terminology, builder);
            } else {
                this.addCodeOfReferenceWithPlaceHolder("PREFERREDTERM", "prefLabelXl", terminology, builder, placeHolderLanguages);
            }
            this.addCodeOfReference("SYNONYM", "altLabelXl", terminology, builder);
            this.addCodeOfReference("NONRECOMMENDEDSYNONYM", "notRecommendedSynonym", terminology, builder);
            this.addCodeOfReference("HIDDENTERM", "hiddenTerm", terminology, builder);
            this.addCodeOfReference("SEARCHTERM", "searchTerm", terminology, builder);

            this.addCodeOfReference("BROADERCONCEPT", "broader", terminology, builder);
            this.addCodeOfReference("NARROWERCONCEPT", "narrower", terminology, builder);
            this.addCodeOfReference("RELATEDCONCEPT", "related", terminology, builder);
            this.addCodeOfReference("ISPARTOFCONCEPT", "isPartOf", terminology, builder);
            this.addCodeOfReference("HASPARTCONCEPT", "hasPart", terminology, builder);

            builder.addDataToCurrentRow("UUID", terminology.getID());

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
    private void createTermsSheet(@NotNull Workbook workbook, boolean isInOrganization) {
        var builder = new DTOBuilder();
        for (JSONWrapper terminology : this.wrappersOfType(TERM)) {

            String uuid = terminology.getID();
            List<TermPlaceHolderDTO> placeHolders = placeHolderTerms.getOrDefault(uuid, Collections.emptyList());

            builder.addDataToCurrentRow("IDENTIFIER", terminology.getCode());
            this.addProperty("PREFLABEL", "prefLabel", terminology, builder);
            this.addPropertyIgnoreLanguage("SOURCE", "source", terminology, builder);
            this.addPropertyIgnoreLanguage("SCOPE", "scope", terminology, builder);
            this.addPropertyIgnoreLanguage("TERMSTYLE", "termStyle", terminology, builder);
            this.addPropertyIgnoreLanguage("TERMFAMILY", "termFamily", terminology, builder);
            this.addPropertyIgnoreLanguage("TERMCONJUGATION", "termConjugation", terminology, builder);
            this.addPropertyIgnoreLanguage("TERMEQUIVALENCY", "termEquivalency", terminology, builder);
            this.addPropertyIgnoreLanguage("TERMINFO", "termInfo", terminology, builder);
            this.addPropertyIgnoreLanguage("WORDCLASS", "wordClass", terminology, builder);
            this.addPropertyIgnoreLanguage("HOMOGRAPHNUMBER", "termHomographNumber", terminology, builder);
            if (isInOrganization) {
                this.addProperty("EDITORIALNOTE", "editorialNote", terminology, builder);
            }
            this.addPropertyIgnoreLanguage("DRAFTCOMMENT", "draftComment", terminology, builder);
            this.addPropertyIgnoreLanguage("HISTORYNOTE", "historyNote", terminology, builder);
            this.addPropertyIgnoreLanguage("CHANGENOTE", "changeNote", terminology, builder);
            this.addPropertyIgnoreLanguage("STATUS", "status", terminology, builder);
            builder.addDataToCurrentRow("URI", terminology.getURI());
            builder.addDataToCurrentRow("UUID", uuid);
            builder.addDataToCurrentRow("OPERATION", "");
            builder.nextRow();

            String prefLabelValue = terminology.getFirstPropertyValue("prefLabel", "fi");
            for(TermPlaceHolderDTO placeHolder : placeHolders) {
                createTermPlaceholder(builder, placeHolder.getUuid().toString(), placeHolder.getLanguage(), prefLabelValue);
                placeHolderTerms.remove(uuid);
                builder.nextRow();
            }
        }

        Sheet sheet = workbook.createSheet("Terms");
        new ExcelBuilder().renderSheetDTO(sheet, builder.getSheet());
    }

    private void createTermPlaceholder(DTOBuilder builder, String uuid, String lang, String defaultValue) {
        builder.addDataToCurrentRow("IDENTIFIER", "");
        builder.addDataToCurrentRow("PREFLABEL", String.format("%s (%s)", defaultValue, lang), lang);
        builder.addDataToCurrentRow("SOURCE", "");
        builder.addDataToCurrentRow("SCOPE", "");
        builder.addDataToCurrentRow("TERMSTYLE", "");
        builder.addDataToCurrentRow("TERMFAMILY", "");
        builder.addDataToCurrentRow("TERMCONJUGATION", "");
        builder.addDataToCurrentRow("TERMEQUIVALENCY", "");
        builder.addDataToCurrentRow("TERMINFO", "");
        builder.addDataToCurrentRow("WORDCLASS", "");
        builder.addDataToCurrentRow("HOMOGRAPHNUMBER", "");
        // TODO: EDITORIALNOTE should be visible if the user has write permissions to the terminology.
//            this.addProperty("EDITORIALNOTE", "editorialNote", terminology, builder);
        builder.addDataToCurrentRow("DRAFTCOMMENT", "");
        builder.addDataToCurrentRow("HISTORYNOTE", "");
        builder.addDataToCurrentRow("CHANGENOTE", "");
        builder.addDataToCurrentRow("STATUS", Status.DRAFT.name());
        builder.addDataToCurrentRow("URI", "");
        builder.addDataToCurrentRow("UUID", uuid);
        builder.addDataToCurrentRow("OPERATION", "");
    }

    private void createConceptLinksSheet(@NotNull Workbook workbook) {
        var builder = new DTOBuilder();
        for (JSONWrapper conceptLink : this.wrappersOfType(CONCEPT_LINK)) {
            List<String> referrerTypes = conceptLink.getReferrerTypes();

            // In some cases there are concept links without referrer
            if (referrerTypes.isEmpty()) {
                continue;
            }
            String referrerType = referrerTypes.get(0);
            List<JSONWrapper> referrers = conceptLink.getReferrer(referrerType);
            if (referrers.isEmpty()) {
                continue;
            }

            builder.addDataToCurrentRow("UUID", conceptLink.getID());
            builder.addDataToCurrentRow("IDENTIFIER", conceptLink.getCode());
            builder.addDataToCurrentRow("LINK_TYPE", referrerType);
            builder.addDataToCurrentRow("CONCEPT_ID", referrers.get(0).getID());
            this.addProperty("TARGET_GRAPH", "targetGraph", conceptLink, builder);
            this.addProperty("TARGET_ID", "targetId", conceptLink, builder);
            this.addProperty("PREFLABEL", "prefLabel", conceptLink, builder);
            this.addProperty("VOCABULARY_LABEL", "vocabularyLabel", conceptLink, builder);
            builder.addDataToCurrentRow("URI", conceptLink.getURI());
            builder.addDataToCurrentRow("OPERATION", "");
            builder.nextRow();
        }
        Sheet sheet = workbook.createSheet("Concept links");
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

    private void addPropertyIgnoreLanguage(
            @NotNull String columnName,
            @NotNull String propertyName,
            @NotNull JSONWrapper wrapper,
            @NotNull DTOBuilder builder
            ) {
        builder.ensureColumn(columnName, ColumnDTO.MULTI_COLUMN_MODE_ENABLED);
        wrapper.getProperty(propertyName).forEach((language, values) -> {
            builder.addDataToCurrentRow(columnName, "", values, ColumnDTO.MULTI_COLUMN_MODE_ENABLED);
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

    private void addUuidOfReference(
            @NotNull String columnName,
            @NotNull String referenceName,
            @NotNull JSONWrapper wrapper,
            @NotNull DTOBuilder builder) {
        this.addUuidOfReference(
                columnName,
                referenceName,
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

    private void addUuidOfReference(
            @NotNull String columnName,
            @NotNull String referenceName,
            @NotNull JSONWrapper wrapper,
            @NotNull DTOBuilder builder,
            boolean multiColumnModeDisabled) {
        List<String> values = wrapper.getReference(referenceName).stream()
                .map(group -> group.getID())
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
                .flatMap(group -> Stream.of(group.getID()))
                .collect(Collectors.toList());

        builder.addDataToCurrentRow(columnName, values);
    }

    /**
     * Creates term reference with placeholder in given languages.
     * Placeholder is not created if term with specified language already exists.
     */
    private void addCodeOfReferenceWithPlaceHolder(
            @NotNull String columnName,
            @NotNull String referenceName,
            @NotNull JSONWrapper wrapper,
            @NotNull DTOBuilder builder,
            @NotNull List<String> languages) {
        List<String> values = wrapper.getReference(referenceName).stream()
                .flatMap(group -> Stream.of(group.getID()))
                .collect(Collectors.toList());

        String key = values.get(0);
        if (key == null) {
            return;
        }

        // check if language version with particular language already exist
        List<String> existingLanguages = new ArrayList<>();
        for (String value : values) {
            JSONWrapper jsonWrapper = wrapperNodeMap.get(value);
            for (String lang : languages) {
                if (jsonWrapper.getProperty("prefLabel").keySet().contains(lang)) {
                    existingLanguages.add(lang);
                }
            }
        }

        List<TermPlaceHolderDTO> placeHolders = new ArrayList<>();
        for (String language : languages) {

            if (existingLanguages.contains(language)) {
                continue;
            }

            UUID placeHolderId = UUID.randomUUID();
            values.add(placeHolderId.toString());
            placeHolders.add(new TermPlaceHolderDTO(placeHolderId, language));
        }
        placeHolderTerms.put(key, placeHolders);
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
            this.filename = Normalizer
                    .normalize(terminology.getFirstPropertyValue("prefLabel", "en"), Normalizer.Form.NFD)
                    .replaceAll("[^\\p{ASCII}]", "")
                    .replaceAll("[^a-zA-Z0-9-\\s]", "_");
        }
    }

    /**
     * Get filename set by updateFilename. If filename is not set, use "Terminology" as a default filename.
     */
    public String getFilename() {
        return this.filename != null ? this.filename : "Terminology";
    }
}
