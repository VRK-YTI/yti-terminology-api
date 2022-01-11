package fi.vm.yti.terminology.api.importapi.excel;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class SheetDTOTest {
    @Test
    public void testConstructor() {
        assertDoesNotThrow(SheetDTO::new);
    }

    @Test
    public void testEmpty() {
        Sheet sheet = new XSSFWorkbook().createSheet();
        SheetDTO dto = new SheetDTO();
        dto.fillSheet(sheet);

        SheetHelpers.assertCellDoesNotExist(sheet, 0, 0);
    }

    @Test
    public void testWithTwoColumnHeader() {
        Sheet sheet = new XSSFWorkbook().createSheet();
        SheetDTO dto = new SheetDTO();
        dto.addColumn("Column A");
        dto.addColumn("Column B");
        dto.fillSheet(sheet);

        // Expected sheet layout
        // Column A | Column B
        SheetHelpers.assertCellHasValue(sheet, 0, 0, "Column A");
        SheetHelpers.assertCellHasValue(sheet, 0, 1, "Column B");
    }

    @Test
    public void testSimpleColumn() {
        Sheet sheet = new XSSFWorkbook().createSheet();
        SheetDTO dto = new SheetDTO();
        dto.addDataToCurrentRow("Column A", "Value 1");
        dto.nextRow();
        dto.addDataToCurrentRow("Column A", "Value 2");
        dto.nextRow();
        dto.addDataToCurrentRow("Column A", "Value 3");
        dto.fillSheet(sheet);

        // Expected sheet layout
        // Column A
        // Value 1
        // Value 2
        // Value 3
        SheetHelpers.assertCellHasValue(sheet, 0, 0, "Column A");
        SheetHelpers.assertCellHasValue(sheet, 1, 0, "Value 1");
        SheetHelpers.assertCellHasValue(sheet, 2, 0, "Value 2");
        SheetHelpers.assertCellHasValue(sheet, 3, 0, "Value 3");
    }

    @Test
    public void testMultipleColumns() {
        Sheet sheet = new XSSFWorkbook().createSheet();
        SheetDTO dto = new SheetDTO();
        dto.addDataToCurrentRow("Column A", "Value 1");
        dto.addDataToCurrentRow("Column B", "Value 2");
        dto.fillSheet(sheet);

        // Expected sheet layout
        // Column A | Column B
        // Value 1  | Value 2
        SheetHelpers.assertCellHasValue(sheet, 0, 0, "Column A");
        SheetHelpers.assertCellHasValue(sheet, 1, 0, "Value 1");
        SheetHelpers.assertCellHasValue(sheet, 0, 1, "Column B");
        SheetHelpers.assertCellHasValue(sheet, 1, 1, "Value 2");
    }

    @Test
    public void testMultipleValuesInColumn() {
        Sheet sheet = new XSSFWorkbook().createSheet();
        SheetDTO dto = new SheetDTO();
        dto.addDataToCurrentRow("Column A", List.of("Value 1.1", "Value 1.2"));
        dto.addDataToCurrentRow("Column B", "Value 2");
        dto.fillSheet(sheet);

        // Expected sheet layout
        // Column A              | Column B
        // Value 1.1 | Value 1.2 | Value 2
        SheetHelpers.assertCellHasValue(sheet, 0, 0, "Column A");
        SheetHelpers.assertCellHasValue(sheet, 1, 0, "Value 1.1");
        SheetHelpers.assertCellHasValue(sheet, 1, 1, "Value 1.2");
        SheetHelpers.assertCellHasValue(sheet, 0, 2, "Column B");
        SheetHelpers.assertCellHasValue(sheet, 1, 2, "Value 2");
    }

    @Test
    public void testMultipleValuesInColumnWithMultiColumnModeDisabled() {
        Sheet sheet = new XSSFWorkbook().createSheet();
        SheetDTO dto = new SheetDTO();
        dto.addDataToCurrentRow("Column A", List.of("Value 1.1", "Value 1.2"));
        dto.addDataToCurrentRow("Column B", "Value 2");
        dto.disableMultiColumnMode("Column A");
        dto.fillSheet(sheet);

        // Expected sheet layout
        // Column A            | Column B
        // Value 1.1;Value 1.2 | Value 2
        SheetHelpers.assertCellHasValue(sheet, 1, 0, "Value 1.1;Value 1.2");
        SheetHelpers.assertCellHasValue(sheet, 0, 0, "Column A");
        SheetHelpers.assertCellHasValue(sheet, 0, 1, "Column B");
        SheetHelpers.assertCellHasValue(sheet, 1, 1, "Value 2");
    }

    @Test
    public void testMultilingualColumn() {
        Sheet sheet = new XSSFWorkbook().createSheet();
        SheetDTO dto = new SheetDTO();
        dto.addDataToCurrentRow("Column A", "fi", List.of("Value 1 (fi)"));
        dto.addDataToCurrentRow("Column A", "en", List.of("Value 1 (en)"));
        dto.addDataToCurrentRow("Column B", "Value 2");
        dto.fillSheet(sheet);

        // Expected sheet layout
        // Column A_FI | Column A_EN | Column B
        // Value 1 (fi) | Value 1 (en) | Value 2
        SheetHelpers.assertCellHasValue(sheet, 0, 0, "Column A_FI");
        SheetHelpers.assertCellHasValue(sheet, 1, 0, "Value 1 (fi)");
        SheetHelpers.assertCellHasValue(sheet, 0, 1, "Column A_EN");
        SheetHelpers.assertCellHasValue(sheet, 1, 1, "Value 1 (en)");
        SheetHelpers.assertCellHasValue(sheet, 0, 2, "Column B");
        SheetHelpers.assertCellHasValue(sheet, 1, 2, "Value 2");
    }

    @Test
    public void testMultipleValuesInMultilingualColumn() {
        Sheet sheet = new XSSFWorkbook().createSheet();
        SheetDTO dto = new SheetDTO();
        dto.addDataToCurrentRow("Column A", "fi", List.of("Value 1.1 (fi)", "Value 1.2 (fi)"));
        dto.addDataToCurrentRow("Column A", "en", List.of("Value 1 (en)"));
        dto.addDataToCurrentRow("Column B", "Value 2");
        dto.fillSheet(sheet);

        // Expected sheet layout
        // Column A_FI                     | Column A_EN  | Column B
        // Value 1.1 (fi) | Value 1.2 (fi) | Value 1 (en) | Value 2
        SheetHelpers.assertCellHasValue(sheet, 0, 0, "Column A_FI");
        SheetHelpers.assertCellHasValue(sheet, 1, 0, "Value 1.1 (fi)");
        SheetHelpers.assertCellHasValue(sheet, 1, 1, "Value 1.2 (fi)");
        SheetHelpers.assertCellHasValue(sheet, 0, 2, "Column A_EN");
        SheetHelpers.assertCellHasValue(sheet, 1, 2, "Value 1 (en)");
        SheetHelpers.assertCellHasValue(sheet, 0, 3, "Column B");
        SheetHelpers.assertCellHasValue(sheet, 1, 3, "Value 2");
    }

    @Test
    public void testMultipleValuesInMultilingualColumnWithMultiColumnModeDisabled() {
        Sheet sheet = new XSSFWorkbook().createSheet();
        SheetDTO dto = new SheetDTO();
        dto.addDataToCurrentRow("Column A", "fi", List.of("Value 1.1 (fi)", "Value 1.2 (fi)"));
        dto.addDataToCurrentRow("Column A", "en", List.of("Value 1 (en)"));
        dto.addDataToCurrentRow("Column B", "Value 2");
        dto.disableMultiColumnMode("Column A");
        dto.fillSheet(sheet);

        // Expected sheet layout
        // Column A_FI                   | Column A_EN  | Column B
        // Value 1.1 (fi);Value 1.2 (fi) | Value 1 (en) | Value 2
        SheetHelpers.assertCellHasValue(sheet, 0, 0, "Column A_FI");
        SheetHelpers.assertCellHasValue(sheet, 1, 0, "Value 1.1 (fi);Value 1.2 (fi)");
        SheetHelpers.assertCellHasValue(sheet, 0, 1, "Column A_EN");
        SheetHelpers.assertCellHasValue(sheet, 1, 1, "Value 1 (en)");
        SheetHelpers.assertCellHasValue(sheet, 0, 2, "Column B");
        SheetHelpers.assertCellHasValue(sheet, 1, 2, "Value 2");
    }
}
