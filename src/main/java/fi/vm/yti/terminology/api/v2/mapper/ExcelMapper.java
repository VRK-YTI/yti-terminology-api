package fi.vm.yti.terminology.api.v2.mapper;

import fi.vm.yti.common.Constants;
import fi.vm.yti.common.enums.Status;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.common.util.ModelWrapper;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.v2.dto.ConceptDTO;
import fi.vm.yti.terminology.api.v2.dto.LocalizedValueDTO;
import fi.vm.yti.terminology.api.v2.dto.TermDTO;
import fi.vm.yti.terminology.api.v2.exception.ExcelParseException;
import fi.vm.yti.terminology.api.v2.util.TerminologyURI;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class ExcelMapper {

    private static final Logger LOG = LoggerFactory.getLogger(ExcelMapper.class);

    enum HeaderColumn {
        PREF_LABEL("prefLabel", true),
        ALT_LABEL("altLabel", true),
        NOT_RECOMMENDED_SYNONYM("notRecommendedSynonym", true),
        STATUS("status", false),
        DEFINITION("definition", true),
        NOTE("note", true),
        EXAMPLE("example", true),
        RELATED("related", false);

        private final String columnName;
        private final Boolean isLocalized;

        private static final Map<String, HeaderColumn> headerMap = new HashMap<>();

        static {
            for (var headerName : values()) {
                headerMap.put(headerName.columnName, headerName);
            }
        }

        HeaderColumn(String columnName, boolean isLocalized) {
            this.columnName = columnName;
            this.isLocalized = isLocalized;
        }

        public static HeaderColumn get(String name) {
            return headerMap.get(name);
        }
    }

    public static void mapSimpleExcel(ModelWrapper model, InputStream data, YtiUser user) throws IOException {
        var workbook = new XSSFWorkbook(data);
        var languages = MapperUtils.arrayPropertyToList(model.getModelResource(), DCTerms.language);
        var sheet = workbook.getSheetAt(0);

        var columnNames = mapColumnNames(sheet.getRow(0), languages);

        var identifierSuffix = 0;
        for (var i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (isRowEmpty(row)) {
                continue;
            }

            // find available identifier
            var conceptId = "concept-" + identifierSuffix;
            while (model.containsId(conceptId)) {
                conceptId = "concept-" + ++identifierSuffix;
            }

            var dto = new ConceptDTO();
            dto.setIdentifier(conceptId);
            dto.setStatus(Status.DRAFT);

            mapConceptDto(row, dto, model.getPrefix(), columnNames);

            ConceptMapper.dtoToModel(model, dto, user);
        }
    }

    private static void mapConceptDto(Row row, ConceptDTO dto, String prefix, List<Header> headers) {

        if (headers.stream()
                .filter(h -> h.column.equals(HeaderColumn.PREF_LABEL))
                .noneMatch(h -> cellHasText(row.getCell(h.ordinal)))) {
            throw new ExcelParseException("pref-label-row-missing", row);
        }

        headers.stream()
                .filter(header -> cellHasText(row.getCell(header.ordinal)))
                .forEach(header -> {
                    var cellValue = row.getCell(header.ordinal).getStringCellValue();
                    switch (header.column) {
                        case PREF_LABEL: {
                            dto.getRecommendedTerms().add(mapTerm(header, cellValue));
                            break;
                        }
                        case ALT_LABEL: {
                            getMultiLineValue(cellValue)
                                    .forEach(line -> dto.getSynonyms().add(mapTerm(header, line)));
                            break;
                        }
                        case NOT_RECOMMENDED_SYNONYM: {
                            getMultiLineValue(cellValue)
                                    .forEach(line -> dto.getNotRecommendedTerms().add(mapTerm(header, line)));
                            break;
                        }
                        case STATUS: {
                            dto.setStatus(getStatus(cellValue));
                            break;
                        }
                        case DEFINITION: {
                            dto.getDefinition().put(header.language, cellValue);
                            break;
                        }
                        case NOTE: {
                            var notes = getMultiLineValue(cellValue).stream()
                                    .map(line -> new LocalizedValueDTO(header.language, line))
                                    .toList();
                            dto.getNotes().addAll(notes);
                            break;
                        }
                        case EXAMPLE: {
                            var examples = getMultiLineValue(cellValue).stream()
                                    .map(line -> new LocalizedValueDTO(header.language, line))
                                    .toList();
                            dto.getExamples().addAll(examples);
                            break;
                        }
                        case RELATED: {
                            getMultiLineValue(cellValue).forEach(r -> {
                                if (!r.startsWith(Constants.TERMINOLOGY_NAMESPACE)) {
                                    r = TerminologyURI.createConceptURI(prefix, r).getResourceURI();
                                }
                                dto.getRelated().add(r);
                            });
                            break;
                        }
                        default: {
                            LOG.warn("Invalid header column {}", header.column);
                        }
                    }
                });

        dto.getRecommendedTerms().forEach(t -> t.setStatus(dto.getStatus()));
        dto.getSynonyms().forEach(t -> t.setStatus(dto.getStatus()));
        dto.getNotRecommendedTerms().forEach(t -> t.setStatus(dto.getStatus()));
    }

    private static List<String> getMultiLineValue(String cellValue) {
        var lines = cellValue.lines()
                .filter(line -> !line.trim().isEmpty())
                .collect(Collectors.toList());
        // reverse order so the first one in the cell will be on top of the list on the site
        Collections.reverse(lines);
        return lines;
    }

    private static TermDTO mapTerm(Header header, String cellValue) {
        var term = new TermDTO();
        term.setLanguage(header.language);
        term.setLabel(cellValue);
        return term;
    }

    private static Status getStatus(String cellValue) {
        try {
            return Status.valueOf(cellValue.toUpperCase());
        } catch (Exception e) {
            return Status.DRAFT;
        }
    }

    /**
     * Construct and validate header row
     *
     * @param row header row
     * @param languages available languages defined in terminology's metadata
     * @return header list
     */
    private static List<Header> mapColumnNames(Row row, List<String> languages) {
        if (row == null) {
            return new ArrayList<>();
        }
        var headers = new ArrayList<Header>();
        row.forEach(cell -> {
            var value = cell.getStringCellValue()
                    .replace("\u00a0", "")
                    .trim();
            var separatedValue = value.split("_");
            var headerColumn = HeaderColumn.get(separatedValue[0]);

            final String language = separatedValue.length == 2
                ? separatedValue[1]
                : null;

            if (headerColumn == null) {
                throw new ExcelParseException("header-column-not-supported", row, cell.getColumnIndex());
            }

            if (language != null && !languages.contains(language)) {
                throw new ExcelParseException("terminology-missing-language", row, cell.getColumnIndex());
            }

            if (language == null && headerColumn.isLocalized) {
                throw new ExcelParseException("column-missing-language", row, cell.getColumnIndex());
            }

            headers.stream()
                    .filter(h -> h.column.columnName.equals(headerColumn.columnName)
                                 && h.language.equals(language))
                    .findFirst()
                    .ifPresent(h -> {
                        throw new ExcelParseException("duplicate-key-value", row, cell.getColumnIndex());
                    });

            headers.add(new Header(headerColumn, language, cell.getColumnIndex()));
        });

        if (headers.stream().noneMatch(key -> key.column.equals(HeaderColumn.PREF_LABEL))) {
            throw new ExcelParseException("pref-label-column-missing", row);
        }

        return headers;
    }

    /**
     * Check that at least one value has been set in the row
     *
     * @param row Row
     * @return true if row is empty
     */
    private static boolean isRowEmpty(Row row) {
        var iterator = row.cellIterator();
        while (iterator.hasNext()) {
            if (cellHasText(iterator.next())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if cell is not empty
     *
     * @param cell Cell
     * @return true if cell has text
     */
    private static boolean cellHasText(Cell cell) {
        return cell != null
               && cell.getCellType() != CellType.BLANK
               && !cell.getStringCellValue().replace("\u00a0", "").trim().isEmpty();
    }

    record Header(HeaderColumn column, String language, Integer ordinal) { }
}
