package fi.vm.yti.terminology.api.importapi.excel;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LocalizedColumnDTOTest {
    @Test
    public void testConstructor() {
        assertDoesNotThrow(LocalizedColumnDTO::new);
    }

    @Test
    public void testEmptyLocalizedColumnHasNoCells() {
        var localizedColumn = new LocalizedColumnDTO();
        assertTrue(localizedColumn.getCells().isEmpty());
    }

    @Test
    public void testLocalizedColumnWithTwoCellsHasTwoCells() {
        var localizedColum = new LocalizedColumnDTO();
        var cell1 = localizedColum.getOrCreateCell(0);
        var cell2 = localizedColum.getOrCreateCell(1);

        assertEquals(2, localizedColum.getCells().size());
        assertEquals(cell1, localizedColum.getCells().get(0));
        assertEquals(cell2, localizedColum.getCells().get(1));
    }

    @Test
    public void testGetOrCreateCellReturnsExistingCell() {
        var localizedColum = new LocalizedColumnDTO();
        var cell = localizedColum.getOrCreateCell(0);

        assertEquals(cell, localizedColum.getOrCreateCell(0));
    }

    @Test
    public void testRowCanBeSkipped() {
        var localizedColum = new LocalizedColumnDTO();
        localizedColum.getOrCreateCell(0);
        localizedColum.getOrCreateCell(2);

        assertNull(localizedColum.getCells().get(1));
    }

    @Test
    public void testEmptyLocalizedColumnConsumesOneColumn() {
        var localizedColumn = new LocalizedColumnDTO();
        assertEquals(1, localizedColumn.getColumnSpan());
    }

    @Test
    public void testLocalizedColumnWithTwoEmptyCellsConsumesOneColumn() {
        var localizedColum = new LocalizedColumnDTO();
        localizedColum.getOrCreateCell(0);
        localizedColum.getOrCreateCell(1);

        assertEquals(1, localizedColum.getColumnSpan());
    }

    @Test
    public void testLocalizedColumnWithMultiValueCellConsumesMultipleColumns() {
        var localizedColum = new LocalizedColumnDTO();
        var cell = localizedColum.getOrCreateCell(0);
        cell.addAll(List.of(new StringValueDTO("value 1"), new StringValueDTO("value 2")));

        assertEquals(2, localizedColum.getColumnSpan());
    }

    @Test
    public void testLocalizedColumnWithTwoMultiValueCellsConsumesMultipleColumns() {
        var localizedColum = new LocalizedColumnDTO();
        var cell1 = localizedColum.getOrCreateCell(0);
        var cell2 = localizedColum.getOrCreateCell(1);
        cell1.addAll(List.of(
                new StringValueDTO("value 1"),
                new StringValueDTO("value 2")
        ));
        cell2.addAll(List.of(
                new StringValueDTO("value 1"),
                new StringValueDTO("value 2"),
                new StringValueDTO("value 3")
        ));

        assertEquals(3, localizedColum.getColumnSpan());
    }
}
