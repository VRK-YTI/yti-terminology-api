package fi.vm.yti.terminology.api.importapi.excel;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SheetDTOTest {
    @Test
    public void testConstructor() {
        assertDoesNotThrow(SheetDTO::new);
    }

    @Test
    public void testEmptySheetHasNoColumns() {
        var sheet = new SheetDTO();

        assertTrue(sheet.getColumns().isEmpty());
        assertTrue(sheet.getColumnNames().isEmpty());
    }

    @Test
    public void testSheetWithTwoColumnsHasTwoColumns() {
        var sheet = new SheetDTO();
        var column1 = sheet.getOrCreateColumn("Column 1", false);
        var column2 = sheet.getOrCreateColumn("Column 2", false);

        assertEquals(2, sheet.getColumns().size());
        assertEquals(column1, sheet.getColumns().get("Column 1"));
        assertEquals(column2, sheet.getColumns().get("Column 2"));

        assertEquals(2, sheet.getColumnNames().size());
        assertTrue(sheet.getColumnNames().containsAll(List.of("Column 1", "Column 2")));
    }

    @Test
    public void testGetOrCreateColumnReturnsExistingColumn() {
        var sheet = new SheetDTO();
        var column = sheet.getOrCreateColumn("Column 1", false);

        assertEquals(column, sheet.getOrCreateColumn("Column 1", false));
    }

    @Test
    public void testColumnNamesAreAddedInOrder() {
        var sheet = new SheetDTO();
        sheet.getOrCreateColumn("Column 1", false);
        sheet.getOrCreateColumn("Column 2", false);
        sheet.getOrCreateColumn("Column 1", false);
        sheet.getOrCreateColumn("Column 3", false);

        assertEquals(3, sheet.getColumnNames().size());
        assertEquals("Column 1", sheet.getColumnNames().get(0));
        assertEquals("Column 2", sheet.getColumnNames().get(1));
        assertEquals("Column 3", sheet.getColumnNames().get(2));
    }
}
