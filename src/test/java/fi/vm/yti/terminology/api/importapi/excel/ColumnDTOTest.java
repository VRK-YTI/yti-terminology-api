package fi.vm.yti.terminology.api.importapi.excel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ColumnDTOTest {
    @Test
    public void testConstructor() {
        assertDoesNotThrow(() -> new ColumnDTO("", ColumnDTO.MULTI_COLUMN_MODE_ENABLED));
    }

    @Test
    public void testName() {
        var column = new ColumnDTO("Test column", ColumnDTO.MULTI_COLUMN_MODE_ENABLED);
        assertEquals("Test column", column.getName());
    }

    @Test
    public void testMultiColumnModeDisabled() {
        var column = new ColumnDTO("", ColumnDTO.MULTI_COLUMN_MODE_DISABLED);
        assertTrue(column.isMultiColumnModeDisabled());
    }

    @Test
    public void testMultiColumnModeEnabled() {
        var column = new ColumnDTO("", ColumnDTO.MULTI_COLUMN_MODE_ENABLED);
        assertFalse(column.isMultiColumnModeDisabled());
    }

    @Test
    public void testEmptyColumnHasNoLocalizedColumns() {
        var column = new ColumnDTO("", ColumnDTO.MULTI_COLUMN_MODE_ENABLED);
        assertTrue(column.getLocalizedColumns().isEmpty());
    }

    @Test
    public void testColumnCanHaveUnlocalizedColumn() {
        var column = new ColumnDTO("", ColumnDTO.MULTI_COLUMN_MODE_ENABLED);
        var localizedColumn = column.getOrCreateLocalizedColumn("");
        assertEquals(localizedColumn, column.getLocalizedColumns().get(""));
    }

    @Test
    public void testColumnCanHaveMultipleLocalizedColumns() {
        var column = new ColumnDTO("", ColumnDTO.MULTI_COLUMN_MODE_ENABLED);
        var localizedColumn1 = column.getOrCreateLocalizedColumn("FI");
        var localizedColumn2 = column.getOrCreateLocalizedColumn("EN");

        assertEquals(2, column.getLocalizedColumns().size());
        assertEquals(localizedColumn1, column.getLocalizedColumns().get("FI"));
        assertEquals(localizedColumn2, column.getLocalizedColumns().get("EN"));
    }

    @Test
    public void testGetOrCreateLocalizedColumnReturnsExistingLocalizedColumn() {
        var column = new ColumnDTO("", ColumnDTO.MULTI_COLUMN_MODE_ENABLED);
        var localizedColumn = column.getOrCreateLocalizedColumn("FI");

        assertEquals(localizedColumn, column.getOrCreateLocalizedColumn("FI"));
    }
}
