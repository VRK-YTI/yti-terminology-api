package fi.vm.yti.terminology.api.importapi.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ExcelBuilder {
    private int currentColumnIndex;

    /**
     * Render internal data to the given Excel sheet.
     */
    public void renderSheetDTO(@NotNull Sheet sheet, @NotNull SheetDTO dto) {
        this.currentColumnIndex = 0;

        dto.getColumnNames().forEach(columnName -> {
            this.renderColumnDTO(sheet, Objects.requireNonNullElse(
                    dto.getColumns().get(columnName),
                    new ColumnDTO(columnName, ColumnDTO.MULTI_COLUMN_MODE_ENABLED)
            ));
        });
    }

    private void renderColumnDTO(@NotNull Sheet sheet, @NotNull ColumnDTO dto) {
        dto.getLocalizedColumns().forEach((language, localizedColumn) -> {
            this.renderLocalizedColumnDTO(
                    sheet,
                    localizedColumn,
                    this.makeColumnName(dto.getName(), language),
                    dto.isMultiColumnModeDisabled()
            );
        });

        if (dto.getLocalizedColumns().isEmpty()) {
            this.renderLocalizedColumnDTO(
                    sheet,
                    new LocalizedColumnDTO(),
                    this.makeColumnName(dto.getName(), ""),
                    dto.isMultiColumnModeDisabled()
            );
        }
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
    private void renderLocalizedColumnDTO(
            @NotNull Sheet sheet,
            @NotNull LocalizedColumnDTO dto,
            @NotNull String columnName,
            boolean disableMultiColumnMode) {
        this.renderCellDTOs(sheet, dto, disableMultiColumnMode);
        this.renderColumnHeader(sheet, dto, columnName, disableMultiColumnMode);
    }

    private void renderCellDTOs(
            @NotNull Sheet sheet,
            @NotNull LocalizedColumnDTO dto,
            boolean disableMultiColumnMode) {
        dto.getCells().forEach((i, cell) -> {
            this.renderCellDTO(
                    sheet,
                    cell,
                    i + 1,
                    this.currentColumnIndex,
                    disableMultiColumnMode
            );
        });
    }

    /**
     * Render header of the column.
     */
    private void renderColumnHeader(
            @NotNull Sheet sheet,
            @NotNull LocalizedColumnDTO localizedColumn,
            @NotNull String columnName,
            boolean disableMultiColumnMode) {
        // 1. Write column header
        this.renderStringValueDTO(
                sheet,
                new StringValueDTO(columnName),
                0,
                this.currentColumnIndex
        );

        // 2. Merge header cells if needed.
        int usedColumnCount = 1;
        if (!disableMultiColumnMode) {
            usedColumnCount = localizedColumn.getColumnSpan();
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
     * Render a list of values to the given point of Excel sheet. If there are multiple values and multi-column mode is
     * enabled, it will use multiple actual Excel cells. Otherwise, values are joined with semicolon (;) and rendered in
     * a single Excel cell.
     */
    private void renderCellDTO(
            @NotNull Sheet sheet,
            @NotNull CellDTO dto,
            int rowIndex,
            int columnIndex,
            boolean disableMultiColumnMode) {
        if (disableMultiColumnMode) {
            var groupedValue = new StringValueDTO(dto.joinValues());
            this.renderStringValueDTO(sheet, groupedValue, rowIndex, columnIndex);
        } else {
            var values = dto.getValues();
            for (int i = 0; i < values.size(); i++) {
                var value = values.get(i);
                this.renderStringValueDTO(sheet, value, rowIndex, columnIndex + i);
            }
        }
    }

    /**
     * Render a single value to the given point of Excel sheet.
     */
    private void renderStringValueDTO(
            @NotNull Sheet sheet,
            @NotNull StringValueDTO value,
            int rowIndex,
            int columnIndex) {
        var row = sheet.getRow(rowIndex);
        if (row == null) {
            row = sheet.createRow(rowIndex);
        }

        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(value.getValue());
    }
}
