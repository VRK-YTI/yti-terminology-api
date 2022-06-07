package fi.vm.yti.terminology.api.importapi.excel;

import org.apache.poi.ss.usermodel.Sheet;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNull;

public class Helpers {
    public static void assertExcelCellHasDateValue(Sheet sheet, int rowIndex, int columnIndex, LocalDateTime value) {
        assertExcelCellExists(sheet, rowIndex, columnIndex);
        assertEquals(
                value,
                sheet.getRow(rowIndex).getCell(columnIndex).getLocalDateTimeCellValue(),
                String.format("Value of cell #%d at row #%d", columnIndex, rowIndex)
        );
    }

    public static void assertExcelCellHasNumericValue(Sheet sheet, int rowIndex, int columnIndex, Double value) {
        assertExcelCellExists(sheet, rowIndex, columnIndex);
        assertEquals(
                value,
                sheet.getRow(rowIndex).getCell(columnIndex).getNumericCellValue(),
                String.format("Value of cell #%d at row #%d", columnIndex, rowIndex)
        );
    }

    public static void assertExcelCellHasStringValue(Sheet sheet, int rowIndex, int columnIndex, String value) {
        assertExcelCellExists(sheet, rowIndex, columnIndex);
        assertEquals(
                value,
                sheet.getRow(rowIndex).getCell(columnIndex).getStringCellValue(),
                String.format("Value of cell #%d at row #%d", columnIndex, rowIndex)
        );
    }

    public static void assertExcelCellExists(Sheet sheet, int rowIndex, int columnIndex) {
        assertNotNull(
                sheet.getRow(rowIndex),
                String.format("Row #%d", rowIndex)
        );
        assertNotNull(
                sheet.getRow(rowIndex).getCell(columnIndex),
                String.format("Cell #%d at row #%d", columnIndex, rowIndex)
        );
    }

    public static void assertExcelCellDoesNotExist(Sheet sheet, int rowIndex, int columnIndex) {
        if (sheet.getRow(rowIndex) != null) {
            assertNull(
                    sheet.getRow(rowIndex).getCell(columnIndex),
                    String.format("Cell #%d at row #%d", columnIndex, rowIndex)
            );
        } else {
            assertNull(
                    sheet.getRow(rowIndex),
                    String.format("Row #%d", rowIndex)
            );
        }
    }

    public static void assertDTOHasValue(SheetDTO dto, String columnName, String lang, int rowNumber, Object value) {
        var column = dto.getOrCreateColumn(columnName, ColumnDTO.MULTI_COLUMN_MODE_DISABLED);
        var localizedColumn = column.getOrCreateLocalizedColumn(lang);
        var cell = localizedColumn.getOrCreateCell(rowNumber);
        var values = cell.getValues();
        var matchingValues = values.stream()
                .filter(stringValueDTO -> stringValueDTO.getValue().equals(value));

        assertEquals(
                1,
                matchingValues.count(),
                String.format(
                        "Value \"%s\" not found from column: \"%s\", language: \"%s\", row: %d",
                        value,
                        columnName,
                        lang,
                        rowNumber
                )
        );
    }
}
