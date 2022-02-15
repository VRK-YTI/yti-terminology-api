package fi.vm.yti.terminology.api.importapi.excel;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DTOBuilderTest {
    @Test
    public void testConstructor() {
        assertDoesNotThrow(DTOBuilder::new);
    }

    @Test
    public void testEmpty() {
        var builder = new DTOBuilder();

        assertTrue(builder.getSheet().getColumns().isEmpty());
        assertTrue(builder.getSheet().getColumnNames().isEmpty());
    }

    @Test
    public void testEnsureColumn() {
        var builder = new DTOBuilder();
        builder.ensureColumn("Column A", false);
        builder.ensureColumn("Column A", false);
        builder.ensureColumn("Column B", false);

        assertEquals(2, builder.getSheet().getColumnNames().size());
        assertTrue(builder.getSheet().getColumnNames().containsAll(List.of("Column A", "Column B")));
    }

    @Test
    public void testAddDataToTheColumnWithStringValue() {
        var builder = new DTOBuilder();
        builder.addDataToCurrentRow("Column A", "Value");

        Helpers.assertDTOHasValue(builder.getSheet(), "Column A", "", 0, "Value");
    }

    @Test
    public void testAddDataToTheColumnWithInstantValue() {
        var instant = Instant.now();
        var builder = new DTOBuilder();
        builder.addDataToCurrentRow("Column A", instant);

        Helpers.assertDTOHasValue(builder.getSheet(), "Column A", "", 0, instant);
    }

    @Test
    public void testAddDataToTheColumnWithUnsupportedValue() {
        var builder = new DTOBuilder();
        assertThrows(
                UnsupportedOperationException.class,
                () -> builder.addDataToCurrentRow(
                        "Column A",
                        "",
                        List.of(Integer.MAX_VALUE),
                        false
                )
        );
    }

    @Test
    public void testSimpleColumn() {
        var builder = new DTOBuilder();
        builder.addDataToCurrentRow("Column A", "Value 1");
        builder.nextRow();
        builder.addDataToCurrentRow("Column A", "Value 2");
        builder.nextRow();
        builder.addDataToCurrentRow("Column A", "Value 3");

        // Expected sheet layout
        // Column A
        // Value 1
        // Value 2
        // Value 3
        Helpers.assertDTOHasValue(builder.getSheet(), "Column A", "", 0, "Value 1");
        Helpers.assertDTOHasValue(builder.getSheet(), "Column A", "", 1, "Value 2");
        Helpers.assertDTOHasValue(builder.getSheet(), "Column A", "", 2, "Value 3");
    }

    @Test
    public void testMultipleColumns() {
        var builder = new DTOBuilder();
        builder.addDataToCurrentRow("Column A", "Value 1");
        builder.addDataToCurrentRow("Column B", "Value 2");

        // Expected sheet layout
        // Column A | Column B
        // Value 1  | Value 2
        Helpers.assertDTOHasValue(builder.getSheet(), "Column A", "", 0, "Value 1");
        Helpers.assertDTOHasValue(builder.getSheet(), "Column B", "", 0, "Value 2");
    }

    @Test
    public void testMultipleValuesInColumn() {
        var builder = new DTOBuilder();
        builder.addDataToCurrentRow("Column A", List.of("Value 1.1", "Value 1.2"));
        builder.addDataToCurrentRow("Column B", "Value 2");

        // Expected sheet layout
        // Column A              | Column B
        // Value 1.1 | Value 1.2 | Value 2
        Helpers.assertDTOHasValue(builder.getSheet(), "Column A", "", 0, "Value 1.1");
        Helpers.assertDTOHasValue(builder.getSheet(), "Column A", "", 0, "Value 1.2");
        Helpers.assertDTOHasValue(builder.getSheet(), "Column B", "", 0, "Value 2");
    }

    @Test
    public void testMultilingualColumn() {
        var builder = new DTOBuilder();
        builder.addDataToCurrentRow("Column A", "fi", List.of("Value 1 (fi)"), true);
        builder.addDataToCurrentRow("Column A", "en", List.of("Value 1 (en)"), true);
        builder.addDataToCurrentRow("Column B", "Value 2");

        // Expected sheet layout
        // Column A_FI | Column A_EN | Column B
        // Value 1 (fi) | Value 1 (en) | Value 2
        Helpers.assertDTOHasValue(builder.getSheet(), "Column A", "fi", 0, "Value 1 (fi)");
        Helpers.assertDTOHasValue(builder.getSheet(), "Column A", "en", 0, "Value 1 (en)");
        Helpers.assertDTOHasValue(builder.getSheet(), "Column B", "", 0, "Value 2");
    }

    @Test
    public void testMultipleValuesInMultilingualColumn() {
        var builder = new DTOBuilder();
        builder.addDataToCurrentRow("Column A", "fi", List.of("Value 1.1 (fi)", "Value 1.2 (fi)"), true);
        builder.addDataToCurrentRow("Column A", "en", List.of("Value 1 (en)"), true);
        builder.addDataToCurrentRow("Column B", "Value 2");

        // Expected sheet layout
        // Column A_FI                     | Column A_EN  | Column B
        // Value 1.1 (fi) | Value 1.2 (fi) | Value 1 (en) | Value 2
        Helpers.assertDTOHasValue(builder.getSheet(), "Column A", "fi", 0, "Value 1.1 (fi)");
        Helpers.assertDTOHasValue(builder.getSheet(), "Column A", "fi", 0, "Value 1.2 (fi)");
        Helpers.assertDTOHasValue(builder.getSheet(), "Column A", "en", 0, "Value 1 (en)");
        Helpers.assertDTOHasValue(builder.getSheet(), "Column B", "", 0, "Value 2");
    }
}
