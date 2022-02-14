package fi.vm.yti.terminology.api.importapi.excel;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class ExcelBuilderTest {
    @Test
    public void testConstructor() {
        assertDoesNotThrow(ExcelBuilder::new);
    }

    @Test
    public void testEmpty() {
        var sheet = new XSSFWorkbook().createSheet();
        var builder = new ExcelBuilder();
        builder.renderSheetDTO(sheet, new SheetDTO());

        Helpers.assertExcelCellDoesNotExist(sheet, 0, 0);
    }

    @Test
    public void testWithTwoColumnHeader() {
        var dto = new SheetDTO();
        dto.getOrCreateColumn("Column A", ColumnDTO.MULTI_COLUMN_MODE_ENABLED);
        dto.getOrCreateColumn("Column B", ColumnDTO.MULTI_COLUMN_MODE_ENABLED);

        var sheet = new XSSFWorkbook().createSheet();
        var builder = new ExcelBuilder();
        builder.renderSheetDTO(sheet, dto);

        // Expected sheet layout
        // Column A | Column B
        Helpers.assertExcelCellHasValue(sheet, 0, 0, "Column A");
        Helpers.assertExcelCellHasValue(sheet, 0, 1, "Column B");
    }

    @Test
    public void testSimpleColumn() {
        var dto = new DTOBuilder();
        dto.addDataToCurrentRow("Column A", "Value 1");
        dto.nextRow();
        dto.addDataToCurrentRow("Column A", "Value 2");
        dto.nextRow();
        dto.addDataToCurrentRow("Column A", "Value 3");

        var sheet = new XSSFWorkbook().createSheet();
        var builder = new ExcelBuilder();
        builder.renderSheetDTO(sheet, dto.getSheet());

        // Expected sheet layout
        // Column A
        // Value 1
        // Value 2
        // Value 3
        Helpers.assertExcelCellHasValue(sheet, 0, 0, "Column A");
        Helpers.assertExcelCellHasValue(sheet, 1, 0, "Value 1");
        Helpers.assertExcelCellHasValue(sheet, 2, 0, "Value 2");
        Helpers.assertExcelCellHasValue(sheet, 3, 0, "Value 3");
    }

    @Test
    public void testMultipleColumns() {
        var dto = new DTOBuilder();
        dto.addDataToCurrentRow("Column A", "Value 1");
        dto.addDataToCurrentRow("Column B", "Value 2");

        var sheet = new XSSFWorkbook().createSheet();
        var builder = new ExcelBuilder();
        builder.renderSheetDTO(sheet, dto.getSheet());

        // Expected sheet layout
        // Column A | Column B
        // Value 1  | Value 2
        Helpers.assertExcelCellHasValue(sheet, 0, 0, "Column A");
        Helpers.assertExcelCellHasValue(sheet, 1, 0, "Value 1");
        Helpers.assertExcelCellHasValue(sheet, 0, 1, "Column B");
        Helpers.assertExcelCellHasValue(sheet, 1, 1, "Value 2");
    }

    @Test
    public void testMultipleValuesInColumn() {
        var dto = new DTOBuilder();
        dto.addDataToCurrentRow("Column A", List.of("Value 1.1", "Value 1.2"));
        dto.addDataToCurrentRow("Column B", "Value 2");

        var sheet = new XSSFWorkbook().createSheet();
        var builder = new ExcelBuilder();
        builder.renderSheetDTO(sheet, dto.getSheet());

        // Expected sheet layout
        // Column A              | Column B
        // Value 1.1 | Value 1.2 | Value 2
        Helpers.assertExcelCellHasValue(sheet, 0, 0, "Column A");
        Helpers.assertExcelCellHasValue(sheet, 1, 0, "Value 1.1");
        Helpers.assertExcelCellHasValue(sheet, 1, 1, "Value 1.2");
        Helpers.assertExcelCellHasValue(sheet, 0, 2, "Column B");
        Helpers.assertExcelCellHasValue(sheet, 1, 2, "Value 2");
    }

    @Test
    public void testMultipleValuesInColumnWithMultiColumnModeDisabled() {
        var dto = new DTOBuilder();
        dto.addDataToCurrentRow("Column A", "", List.of("Value 1.1", "Value 1.2"), ColumnDTO.MULTI_COLUMN_MODE_DISABLED);
        dto.addDataToCurrentRow("Column B", "Value 2");

        var sheet = new XSSFWorkbook().createSheet();
        var builder = new ExcelBuilder();
        builder.renderSheetDTO(sheet, dto.getSheet());

        // Expected sheet layout
        // Column A            | Column B
        // Value 1.1;Value 1.2 | Value 2
        Helpers.assertExcelCellHasValue(sheet, 1, 0, "Value 1.1;Value 1.2");
        Helpers.assertExcelCellHasValue(sheet, 0, 0, "Column A");
        Helpers.assertExcelCellHasValue(sheet, 0, 1, "Column B");
        Helpers.assertExcelCellHasValue(sheet, 1, 1, "Value 2");
    }

    @Test
    public void testMultilingualColumn() {
        var dto = new DTOBuilder();
        dto.addDataToCurrentRow("Column A", "fi", List.of("Value 1 (fi)"), false);
        dto.addDataToCurrentRow("Column A", "en", List.of("Value 1 (en)"), false);
        dto.addDataToCurrentRow("Column B", "Value 2");

        var sheet = new XSSFWorkbook().createSheet();
        var builder = new ExcelBuilder();
        builder.renderSheetDTO(sheet, dto.getSheet());

        // Expected sheet layout
        // Column A_FI | Column A_EN | Column B
        // Value 1 (fi) | Value 1 (en) | Value 2
        Helpers.assertExcelCellHasValue(sheet, 0, 0, "Column A_FI");
        Helpers.assertExcelCellHasValue(sheet, 1, 0, "Value 1 (fi)");
        Helpers.assertExcelCellHasValue(sheet, 0, 1, "Column A_EN");
        Helpers.assertExcelCellHasValue(sheet, 1, 1, "Value 1 (en)");
        Helpers.assertExcelCellHasValue(sheet, 0, 2, "Column B");
        Helpers.assertExcelCellHasValue(sheet, 1, 2, "Value 2");
    }

    @Test
    public void testMultipleValuesInMultilingualColumn() {
        var dto = new DTOBuilder();
        dto.addDataToCurrentRow("Column A", "fi", List.of("Value 1.1 (fi)", "Value 1.2 (fi)"), false);
        dto.addDataToCurrentRow("Column A", "en", List.of("Value 1 (en)"), false);
        dto.addDataToCurrentRow("Column B", "Value 2");

        var sheet = new XSSFWorkbook().createSheet();
        var builder = new ExcelBuilder();
        builder.renderSheetDTO(sheet, dto.getSheet());

        // Expected sheet layout
        // Column A_FI                     | Column A_EN  | Column B
        // Value 1.1 (fi) | Value 1.2 (fi) | Value 1 (en) | Value 2
        Helpers.assertExcelCellHasValue(sheet, 0, 0, "Column A_FI");
        Helpers.assertExcelCellHasValue(sheet, 1, 0, "Value 1.1 (fi)");
        Helpers.assertExcelCellHasValue(sheet, 1, 1, "Value 1.2 (fi)");
        Helpers.assertExcelCellHasValue(sheet, 0, 2, "Column A_EN");
        Helpers.assertExcelCellHasValue(sheet, 1, 2, "Value 1 (en)");
        Helpers.assertExcelCellHasValue(sheet, 0, 3, "Column B");
        Helpers.assertExcelCellHasValue(sheet, 1, 3, "Value 2");
    }

    @Test
    public void testMultipleValuesInMultilingualColumnWithMultiColumnModeDisabled() {
        var dto = new DTOBuilder();
        dto.addDataToCurrentRow("Column A", "fi", List.of("Value 1.1 (fi)", "Value 1.2 (fi)"), ColumnDTO.MULTI_COLUMN_MODE_DISABLED);
        dto.addDataToCurrentRow("Column A", "en", List.of("Value 1 (en)"), ColumnDTO.MULTI_COLUMN_MODE_DISABLED);
        dto.addDataToCurrentRow("Column B", "Value 2");

        var sheet = new XSSFWorkbook().createSheet();
        var builder = new ExcelBuilder();
        builder.renderSheetDTO(sheet, dto.getSheet());

        // Expected sheet layout
        // Column A_FI                   | Column A_EN  | Column B
        // Value 1.1 (fi);Value 1.2 (fi) | Value 1 (en) | Value 2
        Helpers.assertExcelCellHasValue(sheet, 0, 0, "Column A_FI");
        Helpers.assertExcelCellHasValue(sheet, 1, 0, "Value 1.1 (fi);Value 1.2 (fi)");
        Helpers.assertExcelCellHasValue(sheet, 0, 1, "Column A_EN");
        Helpers.assertExcelCellHasValue(sheet, 1, 1, "Value 1 (en)");
        Helpers.assertExcelCellHasValue(sheet, 0, 2, "Column B");
        Helpers.assertExcelCellHasValue(sheet, 1, 2, "Value 2");
    }
}
