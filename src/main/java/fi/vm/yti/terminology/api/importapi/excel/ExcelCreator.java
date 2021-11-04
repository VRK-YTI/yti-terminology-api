package fi.vm.yti.terminology.api.importapi.excel;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Create Excel from provided JSON data.
 */
public class ExcelCreator {
    private static final String TERMINOLOGICAL_VOCABULARY = "TerminologicalVocabulary";

    /**
     * JSON data used as input.
     */
    @NotNull
    private final List<JSONWrapper> wrappers;

    public ExcelCreator(@NotNull List<JSONWrapper> wrappers) {
        this.wrappers = wrappers;
    }

    /**
     * Create Excel workbook
     */
    public @NotNull Workbook createExcel() {
        Workbook workbook = new XSSFWorkbook();

        this.createTerminologyDetailsSheet(workbook);
        // todo: add other sheets

        return workbook;
    }

    /**
     * Create Terminology Details sheet in the workbook.
     *
     * It loops over all terminological vocabularies (even there should be only 1) and creates a row for each.
     * All the vocabulary basic details are mapped from JSON to Excel here.
     */
    private void createTerminologyDetailsSheet(@NotNull Workbook workbook) {
        SheetDTO dto = new SheetDTO();
        for (JSONWrapper terminology : this.wrappersOfType(TERMINOLOGICAL_VOCABULARY)) {
            dto.disableMultiColumnMode("Language");

            this.addProperty("Name", "prefLabel", terminology, dto);
            this.addProperty("Language", "language", terminology, dto);
            this.addProperty("Priority name", "priority", terminology, dto);
            // todo: add Information domain
            dto.addDataToCurrentRow("Vocabulary type", terminology.getType());
            this.addProperty("Description", "description", terminology, dto);
            this.addProperty("Terminology status", "status", terminology, dto);
            // todo: add Contributor
            this.addProperty("Contact", "contact", terminology, dto);
            // todo: add Namespace
            dto.addDataToCurrentRow("Created at", terminology.getCreatedDate());
            // todo: uuid is not probably what we want here
            dto.addDataToCurrentRow("Created by", terminology.getCreatedBy());
            dto.addDataToCurrentRow("Modified at", terminology.getLastModifiedDate());
            // todo: uuid is not probably what we want here
            dto.addDataToCurrentRow("Modified by", terminology.getLastModifiedBy());
            dto.addDataToCurrentRow("URI", terminology.getURI());

            dto.nextRow();
        }

        Sheet sheet = workbook.createSheet("Terminology details");
        dto.fillSheet(sheet);
    }

    /**
     * Maps data from JSON to SheetDTO.
     */
    private void addProperty(@NotNull String columnName, @NotNull String propertyName, @NotNull JSONWrapper wrapper, @NotNull SheetDTO dto) {
        wrapper.getProperty(propertyName).forEach((language, values) -> {
            dto.addDataToCurrentRow(columnName, language, values);
        });
    }

    /**
     * Filter JSON inputs by type.
     */
    private @NotNull List<JSONWrapper> wrappersOfType(@NotNull String type) {
        return this.wrappers.stream().filter(wrapper -> wrapper.getType().equals(type)).collect(Collectors.toList());
    }
}
