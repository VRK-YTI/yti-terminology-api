package fi.vm.yti.terminology.api.importapi.excel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
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
        Workbook workbook = creator.createExcel(true);

        // check that all sheets exist
        assertNotNull(workbook);
        assertEquals(5, workbook.getNumberOfSheets());
        assertEquals("Terminology details", workbook.getSheetName(0));
        assertEquals("Collections", workbook.getSheetName(1));
        assertEquals("Concepts", workbook.getSheetName(2));
        assertEquals("Terms", workbook.getSheetName(3));
        assertEquals("Concept links", workbook.getSheetName(4));

        // Check filename
        assertEquals("Terminology", creator.getFilename());
    }

    @Test
    public void testCreateExcelWithData() {
        ExcelCreator creator = new ExcelCreator(wrappers);
        Workbook workbook = creator.createExcel(true);

        // Check that all sheets exist
        assertNotNull(workbook);
        assertEquals(5, workbook.getNumberOfSheets());
        assertEquals("Terminology details", workbook.getSheetName(0));
        assertEquals("Collections", workbook.getSheetName(1));
        assertEquals("Concepts", workbook.getSheetName(2));
        assertEquals("Terms", workbook.getSheetName(3));
        assertEquals("Concept links", workbook.getSheetName(4));

        // Check only that sheets are not empty
        Helpers.assertExcelCellHasStringValue(workbook.getSheet("Terminology details"), 0, 0, "IDENTIFIER");
        Helpers.assertExcelCellHasStringValue(workbook.getSheet("Collections"), 0, 0, "IDENTIFIER");
        Helpers.assertExcelCellHasStringValue(workbook.getSheet("Concepts"), 0, 0, "IDENTIFIER");
        Helpers.assertExcelCellHasStringValue(workbook.getSheet("Terms"), 0, 0, "IDENTIFIER");

        // Check IDs and namespace
        Helpers.assertExcelCellHasStringValue(workbook.getSheet("Terminology details"), 1, 14,
                "5346b891-1cb3-4b38-845c-fbf3b56bf626");
        Helpers.assertExcelCellHasStringValue(workbook.getSheet("Terminology details"), 1, 15,
                "8e99a762-92b6-4830-a49f-8519e7fc2e4d");
        Helpers.assertExcelCellHasStringValue(workbook.getSheet("Terminology details"), 1, 16,
                "test");

        // Check filename
        assertEquals("New terminology", creator.getFilename());
    }

    @Test
    public void testCreateTermLanguageVersion() {
        ExcelCreator creator = new ExcelCreator(wrappers);
        Workbook workbook = creator.createExcel(List.of("fi", "sv", "en"), true);

        Sheet concepts = workbook.getSheet("Concepts");
        Sheet terms = workbook.getSheet("Terms");

        Cell preferredTermCell = getHeaderCellByName(concepts, "PREFERREDTERM");

        // Preferred term should have three values in concepts sheet,
        // one existing term and two placeholders
        // FI exists -> no placeholder will be created for that
        CellRangeAddress mergedRegion = concepts.getMergedRegion(0);
        assertEquals(preferredTermCell.getColumnIndex(), mergedRegion.getFirstColumn());
        assertEquals(2, mergedRegion.getLastColumn() - mergedRegion.getFirstColumn());

        // All language versions should exist in terms sheet
        Cell preflabel_fi = getHeaderCellByName(terms, "PREFLABEL_FI");
        Cell preflabel_sv = getHeaderCellByName(terms, "PREFLABEL_SV");
        Cell preflabel_en = getHeaderCellByName(terms, "PREFLABEL_EN");

        assertNotNull(preflabel_fi);
        assertNotNull(preflabel_sv);
        assertNotNull(preflabel_en);

        // Get first concept row's preferred terms
        Row row = concepts.getRow(1);
        List<String> placeholderValues = new ArrayList<>();
        for (int i = mergedRegion.getFirstColumn(); i <= mergedRegion.getLastColumn(); i++) {
            placeholderValues.add(row.getCell(i).getStringCellValue());
        }

        // Check term data. Placeholder rows' preflabel_<language> should have text
        Cell uuidCell = getHeaderCellByName(terms, "UUID");
        Iterator<Row> iterator = terms.iterator();

        while(iterator.hasNext()) {
            Row termRow = iterator.next();
            String termId = termRow.getCell(uuidCell.getColumnIndex()).getStringCellValue();

            if (placeholderValues.contains(termId)) {
                if (!isEmptyCell(termRow, preflabel_fi.getColumnIndex())) {
                    assertEquals("uusi käsite", termRow.getCell(preflabel_fi.getColumnIndex()).getStringCellValue());
                } else if (!isEmptyCell(termRow, preflabel_sv.getColumnIndex())) {
                    assertEquals("uusi käsite (sv)", termRow.getCell(preflabel_sv.getColumnIndex()).getStringCellValue());
                } else if (!isEmptyCell(termRow, preflabel_en.getColumnIndex())) {
                    assertEquals("uusi käsite (en)", termRow.getCell(preflabel_en.getColumnIndex()).getStringCellValue());
                } else {
                    fail("Term should have pref label for one language");
                }
            }
        }
    }

    @Test
    public void testEditorialNote() {
        ExcelCreator creator = new ExcelCreator(wrappers);
        Workbook workbook = creator.createExcel(List.of("fi", "sv", "en"), false);

        Sheet concepts = workbook.getSheet("Concepts");

        Iterator<Row> iterator = concepts.iterator();

        assertNull(getHeaderCellByName(concepts, "EDITORIALNOTE"));
    }

    private boolean isEmptyCell(Row row, int index) {
        return row.getCell(index) == null || row.getCell(index).getCellType() == CellType.BLANK;
    }

    private Cell getHeaderCellByName(Sheet sheet, String name) {
        Cell cell = null;
        Iterator<Cell> cellIterator = sheet.getRow(0).cellIterator();
        while (cellIterator.hasNext()) {
            cell = cellIterator.next();
            if (cell.getStringCellValue().equals(name)) {
                return cell;
            }
        }
        return null;
    }
}
