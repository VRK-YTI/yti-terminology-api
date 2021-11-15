package fi.vm.yti.terminology.api.importapi.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * DTO that stores information in format that can be rendered as Excel sheet.
 * <p>
 * Features:
 * - First row is a header row with column names.
 * - Localized columns render as multiple columns with language name as a suffix in column header.
 * - Cells can have multiple values.
 *   - If multi-column mode is disabled the values are joined by semicolon (;) and rendered in a single cell.
 *   - If multi-column mode is enabled the values are rendered in multiple cells. This is done by using next cells in
 *     the same row and then the column header will be merged to cover all used columns. This way single column can
 *     use multiple actual columns in Excel.
 */
public class SheetDTO {
    /**
     * All the data.
     */
    @NotNull
    private final Map<String, Map<String, Map<Integer, List<String>>>> data;

    /**
     * Column names in the order they will be rendered in Excel.
     */
    @NotNull
    private final List<String> columns;

    /**
     * Column names where multi-column mode is disabled. It is enabled by default.
     */
    @NotNull
    private final Set<String> multiColumnModeDisabled;

    // Internal cursors. These are pretty hacky, but I couldn't figure out better solutions.
    private int currentRowIndex = 0;
    private int currentColumnIndex = 0;

    public SheetDTO() {
        this.data = new HashMap<>();
        this.columns = new ArrayList<>();
        this.multiColumnModeDisabled = new HashSet<>();
    }

    /**
     * Increase row cursor. Used in loop after adding a row and before adding the next.
     */
    public void nextRow() {
        this.currentRowIndex++;
    }

    /**
     * Disable the multi-column mode for a given column.
     */
    public void disableMultiColumnMode(@NotNull String columnName) {
        this.multiColumnModeDisabled.add(columnName);
    }

    /**
     * Add single value to the given non-localized column of the current row.
     */
    public void addDataToCurrentRow(@NotNull String columnName, @NotNull String value) {
        this.addDataToCurrentRow(columnName, "", List.of(value));
    }

    /**
     * Add a lit of values to the given non-localized column of the current row.
     */
    public void addDataToCurrentRow(@NotNull String columnName, @NotNull List<String> values) {
        this.addDataToCurrentRow(columnName, "", values);
    }

    /**
     * Add a list of values to the given localized column of the current row. This should be called in the same order as
     * columns should be rendered in Excel as this also stores the order of columns.
     * <p>
     * This works with non-localized columns too, as empty string is used as a locale for non-localized columns.
     */
    public void addDataToCurrentRow(@NotNull String columnName, @NotNull String lang, @NotNull List<String> values) {
        this.getCell(columnName, lang).addAll(values);
        this.addColumn(columnName);
    }

    /**
     * Add column name to ordered column name list if not present.
     */
    public void addColumn(@NotNull String columnName) {
        if (!this.columns.contains(columnName)) {
            this.columns.add(columnName);
        }
    }

    /**
     * Get/create the pointer of cell from the internal data for a given localized column and current row.
     */
    private @NotNull List<String> getCell(@NotNull String columnName, @NotNull String lang) {
        Map<Integer, List<String>> localizedColumn = this.getLocalizedColumn(columnName, lang);
        if (!localizedColumn.containsKey(this.currentRowIndex)) {
            localizedColumn.put(this.currentRowIndex, new ArrayList<>());
        }

        return localizedColumn.get(this.currentRowIndex);
    }

    /**
     * Get/create the pointer of localized column from the internal data.
     */
    private @NotNull Map<Integer, List<String>> getLocalizedColumn(@NotNull String columnName, @NotNull String lang) {
        Map<String, Map<Integer, List<String>>> column = this.getColumn(columnName);
        if (!column.containsKey(lang)) {
            column.put(lang, new HashMap<>());
        }

        return column.get(lang);
    }

    /**
     * Get/create the pointer of column from the internal data.
     */
    private @NotNull Map<String, Map<Integer, List<String>>> getColumn(@NotNull String name) {
        if (!this.data.containsKey(name)) {
            this.data.put(name, new HashMap<>());
        }

        return this.data.get(name);
    }

    /**
     * Render internal data to the given Excel sheet.
     */
    public void fillSheet(@NotNull Sheet sheet) {
        this.columns.forEach(column -> {
            Map<String, Map<Integer, List<String>>> col = Objects.requireNonNullElse(this.data.get(column), new HashMap<>());
            col.forEach((language, cells) -> {
                this.fillColumn(
                        sheet,
                        cells,
                        this.makeColumnName(column, language),
                        this.multiColumnModeDisabled.contains(column)
                );
            });

            if (col.isEmpty()) {
                this.fillColumn(
                        sheet,
                        Map.of(),
                        this.makeColumnName(column, ""),
                        this.multiColumnModeDisabled.contains(column)
                );
            }
        });
    }

    /**
     * Join column name with optional language suffix.
     */
    private @NotNull String makeColumnName(@NotNull String columnName, @NotNull String language) {
        return language.equals("") ? columnName : columnName + "_" + language.toUpperCase();
    }

    /**
     * Render single localized column in Excel sheet. In the multi-column mode this can use multiple actual columns in
     * the Excel sheet.
     * <p>
     * Column header (with optional language suffix) is rendered in the first row. In multi-column mode the header cell
     * spans over multiple actual Excel columns if the content needs multiple columns.
     */
    private void fillColumn(
            @NotNull Sheet sheet,
            @NotNull Map<Integer, List<String>> cells,
            @NotNull String columnName,
            boolean disableMultiColumnMode
    ) {
        this.fillCells(sheet, cells, disableMultiColumnMode);
        this.fillColumnHeader(sheet, cells, columnName, disableMultiColumnMode);
    }

    /**
     * Render header of the column.
     */
    private void fillColumnHeader(
            @NotNull Sheet sheet,
            @NotNull Map<Integer, List<String>> cells,
            @NotNull String columnName,
            boolean disableMultiColumnMode
    ) {
        // 1. Write column header
        this.fillCell(sheet, 0, this.currentColumnIndex, columnName);

        // 2. Merge header cells if needed.
        int usedColumnCount = 1;
        if (!disableMultiColumnMode) {
            usedColumnCount = Math.max(1, cells.values().stream().mapToInt(List::size).max().orElse(0));
            if (usedColumnCount > 1) {
                sheet.addMergedRegion(new CellRangeAddress(
                        0,
                        0,
                        this.currentColumnIndex,
                        this.currentColumnIndex + usedColumnCount - 1
                ));
            }
        }

        // 3. Update index based on how many columns were used.
        this.currentColumnIndex += usedColumnCount;
    }

    /**
     * Render values of the column.
     */
    private void fillCells(
            @NotNull Sheet sheet,
            @NotNull Map<Integer, List<String>> cells,
            boolean disableMultiColumnMode
    ) {
        cells.forEach((i, values) -> {
            this.fillCell(sheet, i + 1, this.currentColumnIndex, values, disableMultiColumnMode);
        });
    }

    /**
     * Render a list of values to the given point of Excel sheet. If there are multiple values and multi-column mode is
     * enabled, it will use multiple actual Excel cells. Otherwise, values are joined with semicolon (;) and rendered in
     * a single Excel cell.
     */
    private void fillCell(
            @NotNull Sheet sheet,
            int rowIndex,
            int columnIndex,
            List<String> values,
            boolean disableMultiColumnMode
    ) {
        if (disableMultiColumnMode) {
            this.fillCell(sheet, rowIndex, columnIndex, String.join(";", values));
        } else {
            for (int i = 0; i < values.size(); i++) {
                this.fillCell(sheet, rowIndex, columnIndex + i, values.get(i));
            }
        }
    }

    /**
     * Render a single value to the given point of Excel sheet.
     */
    private void fillCell(@NotNull Sheet sheet, int rowIndex, int columnIndex, String value) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            row = sheet.createRow(rowIndex);
        }

        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(value);
    }
}
