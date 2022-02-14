package fi.vm.yti.terminology.api.importapi.excel;

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
     * # Data: Map<ColumnName:String, Column>
     * Data contains all columns, their localized variants and then cells for each row.
     *
     * # Column: Map<Language:String, LocalizedColumn>
     * Column represents column which can have localized variants and then cells for each row. Column is extracted from
     * the data with SheetDTO::getColumn.
     *
     * # LocalizedColumn: Map<RowIndex:Integer, Cell>
     * LocalizedColumn represents a column but in single language. It contains cells for each row. LocalizedColumn is
     * extracted from the data with SheetDTO::getLocalizedColumn.
     *
     * # Cell: List<Value:String>
     * Cell represents a single cell in table, but it can have multiple values. Cell is extracted from the data with
     * SheetDTO::getCell.
     */
    @NotNull
    private final Map<String, ColumnDTO> columns;

    /**
     * Column names in the order they will be rendered in Excel.
     */
    @NotNull
    private final List<String> columnNames;

    public SheetDTO() {
        this.columns = new HashMap<>();
        this.columnNames = new ArrayList<>();
    }

    public @NotNull Map<String, ColumnDTO> getColumns() {
        return columns;
    }

    public @NotNull List<String> getColumnNames() {
        return columnNames;
    }

    /**
     * Get/create the pointer of column from the internal data.
     */
    public @NotNull ColumnDTO getOrCreateColumn(@NotNull String name, boolean multiColumnModeDisabled) {
        this.addColumnName(name);

        if (!this.columns.containsKey(name)) {
            this.columns.put(name, new ColumnDTO(name, multiColumnModeDisabled));
        }

        return this.columns.get(name);
    }

    /**
     * Add column name to ordered column name list if not present.
     */
    private void addColumnName(@NotNull String columnName) {
        if (!this.columnNames.contains(columnName)) {
            this.columnNames.add(columnName);
        }
    }
}
