package fi.vm.yti.terminology.api.importapi.excel;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CellDTOTest {
    @Test
    public void testConstructor() {
        assertDoesNotThrow(CellDTO::new);
    }

    @Test
    public void testEmptyCellHasNoValues() {
        var cell = new CellDTO();
        assertTrue(cell.getValues().isEmpty());
    }

    @Test
    public void testCellWithTwoValuesHasTwoValues() {
        var cell = new CellDTO();
        var value1 = new StringValueDTO("value 1");
        var value2 = new StringValueDTO("value 2");
        cell.addAll(List.of(value1, value2));

        assertEquals(2, cell.getValues().size());
        assertTrue(cell.getValues().containsAll(List.of(value1, value2)));
    }

    @Test
    public void testValuesCanBeAddedMultipleTimes() {
        var cell = new CellDTO();
        var value1 = new StringValueDTO("value 1");
        var value2 = new StringValueDTO("value 2");
        cell.addAll(List.of(value1));
        cell.addAll(List.of(value2));

        assertEquals(2, cell.getValues().size());
    }

    @Test
    public void testEmptyCellConsumesOneColumn() {
        var cell = new CellDTO();
        assertEquals(1, cell.getColumnSpan());
    }

    @Test
    public void testCellWithOneValueConsumesOneColumn() {
        var cell = new CellDTO();
        var value1 = new StringValueDTO("value 1");
        cell.addAll(List.of(value1));
        assertEquals(1, cell.getColumnSpan());
    }

    @Test
    public void testCellWithTwoValuesConsumesTwoColumns() {
        var cell = new CellDTO();
        var value1 = new StringValueDTO("value 1");
        var value2 = new StringValueDTO("value 2");
        cell.addAll(List.of(value1, value2));
        assertEquals(2, cell.getColumnSpan());
    }

    @Test
    public void testEmptyCellJoinsToEmptyString() {
        var cell = new CellDTO();
        assertEquals("", cell.joinValues());
    }

    @Test
    public void testCellWithOneValueJoinsToOriginalValue() {
        var cell = new CellDTO();
        var value1 = new StringValueDTO("value 1");
        cell.addAll(List.of(value1));
        assertEquals("value 1", cell.joinValues());
    }

    @Test
    public void testCellWithTwoValuesAreJoinedWithSemicolon() {
        var cell = new CellDTO();
        var value1 = new StringValueDTO("value 1");
        var value2 = new StringValueDTO("value 2");
        cell.addAll(List.of(value1, value2));
        assertEquals("value 1;value 2", cell.joinValues());
    }
}
