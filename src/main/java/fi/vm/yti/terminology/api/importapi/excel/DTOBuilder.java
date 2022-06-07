package fi.vm.yti.terminology.api.importapi.excel;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class DTOBuilder {
    @NotNull
    private final SheetDTO sheet;

    private int currentRowIndex = 0;

    public DTOBuilder() {
        this.sheet = new SheetDTO();
    }

    public @NotNull SheetDTO getSheet() {
        return sheet;
    }

    /**
     * Increase row cursor. Used in loop after adding a row and before adding the next.
     */
    public void nextRow() {
        this.currentRowIndex++;
    }

    public void addDataToCurrentRow(@NotNull String columnName, @NotNull String value, String language) {
        this.addDataToCurrentRow(columnName, language, List.of(value), ColumnDTO.MULTI_COLUMN_MODE_ENABLED);
    }

    public void addDataToCurrentRow(@NotNull String columnName, @NotNull String value) {
        this.addDataToCurrentRow(columnName, "", List.of(value), ColumnDTO.MULTI_COLUMN_MODE_ENABLED);
    }

    public void addDataToCurrentRow(@NotNull String columnName, @NotNull Instant value) {
        this.addDataToCurrentRow(columnName, "", List.of(value), ColumnDTO.MULTI_COLUMN_MODE_ENABLED);
    }

    public void addDataToCurrentRow(@NotNull String columnName, @NotNull List<?> values) {
        this.addDataToCurrentRow(columnName, "", values, ColumnDTO.MULTI_COLUMN_MODE_ENABLED);
    }

    /**
     * Add a list of values to the given localized column of the current row. This should be called in the same order as
     * columns should be rendered in Excel as this also stores the order of columns.
     * <p>
     * This works with non-localized columns too, as empty string is used as a locale for non-localized columns.
     */
    public void addDataToCurrentRow(
            @NotNull String columnName,
            @NotNull String lang,
            @NotNull List<?> values,
            boolean multiColumnModeDisabled) {
        var column = sheet.getOrCreateColumn(columnName, multiColumnModeDisabled);
        var localizedColumn = column.getOrCreateLocalizedColumn(lang);
        var cell = localizedColumn.getOrCreateCell(this.currentRowIndex);
        cell.addAll(values.stream().map(value -> {
            if (value instanceof String) {
                return new StringValueDTO((String) value);
            } else if (value instanceof Instant) {
                return new InstantValueDTO((Instant) value);
            } else {
                throw new UnsupportedOperationException("Only String and Instant are supported");
            }
        }).collect(Collectors.toList()));
    }

    /**
     * Ensure that column exists. Column are rendered to the Excel in the same order as this is called.
     */
    public void ensureColumn(@NotNull String columnName, boolean multiColumnModeDisabled) {
        sheet.getOrCreateColumn(columnName, multiColumnModeDisabled);
    }
}
