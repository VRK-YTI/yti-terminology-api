package fi.vm.yti.terminology.api.importapi.excel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ExcelCreatorTest {
    List<JSONWrapper> wrappers;

    @BeforeEach
    void setUp() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(new File("src/test/resources/importapi/excel/data.json"));

        wrappers = new ArrayList<>();
        json.forEach(node -> wrappers.add(new JSONWrapper(node, wrappers)));
    }

    @Test
    public void testConstructor() {
        assertDoesNotThrow(() -> new ExcelCreator(List.of()));
    }

    @Test
    public void testCreateExcelWithoutData() {
        ExcelCreator creator = new ExcelCreator(List.of());
        Workbook workbook = creator.createExcel();

        // check that all sheets exist
        assertNotNull(workbook);
        assertEquals(4, workbook.getNumberOfSheets());
        assertEquals("Terminology details", workbook.getSheetName(0));
        assertEquals("Collections", workbook.getSheetName(1));
        assertEquals("Concepts", workbook.getSheetName(2));
        assertEquals("Terms", workbook.getSheetName(3));

        // Check filename
        assertEquals("Terminology", creator.getFilename());
    }

    @Test
    public void testCreateExcelWithData() {
        ExcelCreator creator = new ExcelCreator(wrappers);
        Workbook workbook = creator.createExcel();

        // Check that all sheets exist
        assertNotNull(workbook);
        assertEquals(4, workbook.getNumberOfSheets());
        assertEquals("Terminology details", workbook.getSheetName(0));
        assertEquals("Collections", workbook.getSheetName(1));
        assertEquals("Concepts", workbook.getSheetName(2));
        assertEquals("Terms", workbook.getSheetName(3));

        // Check only that sheets are not empty
        SheetHelpers.assertCellHasValue(workbook.getSheet("Terminology details"), 0, 0, "CODE");
        SheetHelpers.assertCellHasValue(workbook.getSheet("Collections"), 0, 0, "CODE");
        SheetHelpers.assertCellHasValue(workbook.getSheet("Concepts"), 0, 0, "CODE");
        SheetHelpers.assertCellHasValue(workbook.getSheet("Terms"), 0, 0, "CODE");

        // Check filename
        assertEquals("New terminology", creator.getFilename());
    }

}
