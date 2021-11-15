package fi.vm.yti.terminology.api.importapi.excel;

import org.apache.poi.ss.usermodel.Sheet;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SheetHelpers {
    public static void assertCellHasValue(Sheet sheet, int rowIndex, int columnIndex, String value) {
        assertCellExists(sheet, rowIndex, columnIndex);
        assertEquals(
                value,
                sheet.getRow(rowIndex).getCell(columnIndex).getStringCellValue(),
                String.format("Value of cell #%d at row #%d", columnIndex, rowIndex)
        );
    }

    public static void assertCellExists(Sheet sheet, int rowIndex, int columnIndex) {
        assertNotNull(
                sheet.getRow(rowIndex),
                String.format("Row #%d", rowIndex)
        );
        assertNotNull(
                sheet.getRow(rowIndex).getCell(columnIndex),
                String.format("Cell #%d at row #%d", columnIndex, rowIndex)
        );
    }

    public static void assertCellDoesNotExist(Sheet sheet, int rowIndex, int columnIndex) {
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
}
