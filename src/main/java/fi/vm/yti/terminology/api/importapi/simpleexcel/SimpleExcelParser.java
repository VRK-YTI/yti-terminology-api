package fi.vm.yti.terminology.api.importapi.simpleexcel;

import fi.vm.yti.terminology.api.exception.ExcelParseException;
import fi.vm.yti.terminology.api.frontend.Status;
import fi.vm.yti.terminology.api.importapi.excel.Fields;
import fi.vm.yti.terminology.api.model.termed.*;
import fi.vm.yti.terminology.api.validation.ValidationConstants;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;

public class SimpleExcelParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleExcelParser.class);

    private static final String HEADER_SEPARATOR = "_";
    private static final String CONCEPT_TYPE_URI = "http://www.w3.org/2004/02/skos/core#Concept";
    private static final String TERM_TYPE_URI = "http://www.w3.org/2008/05/skos-xl#Label";

    private static final String[] CONCEPT_PROPERTIES = new String[]{"changeNote", "conceptClass", "conceptScope", "definition", "editorialNotes", "example", "externalLink", "historyNote", "notation", "note", "source", "status", "subjectArea", "wordClass"};
    private static final String[] CONCEPT_NO_LANG_PROPERTIES = new String[]{"status"};

    private static final String[] TERM_PROPERTIES = new String[]{"changeNote", "draftComment", "editorialNote", "historyNote", "scope", "source", "termConjugation", "termEquivalency", "termEquivalencyRelation", "termFamily", "termHomographNumber", "termInfo", "termStyle", "wordClass"};
    private static final String[] CONCEPT_MULTILINE_PROPERTIES = new String[]{"note", "example"};

    private static final String[] TERM_TYPES = new String[]{"prefLabel", "altLabel", "searchTerm", "hiddenTerm", "notRecommendedSynonym"};


    public XSSFWorkbook getWorkbook(InputStream is) throws IOException {
        try {
            return new XSSFWorkbook(is);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Build nodes
     *
     * @param workbook      Workbook
     * @param terminologyId Terminology id
     * @param languages     Terminology languages
     * @return List of generic nodes to be saved
     */
    public List<GenericNode> buildNodes(XSSFWorkbook workbook, UUID terminologyId, List<String> languages) {
        XSSFSheet sheet = workbook.getSheetAt(0);
        var headers = mapColumnNames(sheet.getRow(0), languages);
        List<GenericNode> nodes = new ArrayList<>();

        for (var i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);

            //Make concept properties
            Map<String, List<Attribute>> conceptProperties = createConceptProperties(row, headers);

            //Get status from concept properties to set it term status same as concept status
            if (!conceptProperties.containsKey("status")) {
                throw new ExcelParseException("Status column missing for concept", row);
            }
            String status = conceptProperties.get("status").get(0).getValue();

            Map<String, List<GenericNode>> terms = createTerms(row, headers, terminologyId, status);

            Map<String, List<Identifier>> conceptReferences = new HashMap<>();
            for (var nodeset : terms.entrySet()) {
                String key = nodeset.getKey();
                //These to have special keyName that does not match the Excel, so we set them separately
                if (key.equals("prefLabel") || key.equals("altLabel")) {
                    key += "Xl";
                }
                //Get identifiers from List
                List<Identifier> identifiers = nodeset.getValue()
                        .stream()
                        .map(GenericNode::getIdentifier)
                        .collect(Collectors.toList());
                //Add references and add nodes to return list
                conceptReferences.put(key, identifiers);
                nodes.addAll(nodeset.getValue());
            }
            //Finally create concept node with correct properties and references
            nodes.add(new GenericNode(new TypeId(NodeType.Concept, new GraphId(terminologyId), CONCEPT_TYPE_URI), conceptProperties, conceptReferences));
        }
        return nodes;
    }

    /**
     * Add property to concept
     *
     * @param row     Row to get data from
     * @param headers Header map
     */
    private Map<String, List<Attribute>> createConceptProperties(Row row, Map<String, Integer> headers) {
        Map<String, List<Attribute>> properties = new HashMap<>();
        headers.entrySet().stream()
                //Only use properties that are concept properties
                .filter(entry -> cellHasText(row.getCell(headers.get(entry.getKey())))
                        && Arrays.asList(CONCEPT_PROPERTIES).contains(entry.getKey().split(HEADER_SEPARATOR)[0]))
                .forEach(entry -> {
                            String[] headerValues = entry.getKey().split(HEADER_SEPARATOR);
                            String propertyName = headerValues[0];
                            List<Attribute> attributes = properties.getOrDefault(propertyName, new ArrayList<>());

                            //Get language from header values can be empty
                            if (headerValues.length == 1 && !Arrays.asList(CONCEPT_NO_LANG_PROPERTIES).contains(propertyName)) {
                                throw new ExcelParseException("Property needs language: " + propertyName, row, entry.getValue());
                            }
                            String lang = headerValues.length > 1 ? headerValues[1] : "";

                            var cellValue = row.getCell(headers.get(entry.getKey())).getStringCellValue();

                            //Multiline properties can have multiple properties per cell
                            if (Arrays.asList(CONCEPT_MULTILINE_PROPERTIES).contains((propertyName))) {
                                cellValue.lines()
                                        //lines() can output empty lines but not null lines
                                        .filter(String::isEmpty)
                                        //Add all lines as a new attribute
                                        .forEach(value -> {
                                            if (isPropertyValid(propertyName, value)) {
                                                attributes.add(new Attribute(lang, value));
                                            } else {
                                                throw new ExcelParseException("Value is not valid for property: " + propertyName, row, entry.getValue());
                                            }
                                        });
                            } else {
                                //Add the whole string as attribute
                                if (isPropertyValid(propertyName, cellValue)) {
                                    attributes.add(new Attribute(lang, cellValue));
                                } else {
                                    throw new ExcelParseException("Value is not valid for property: " + propertyName, row, entry.getValue());
                                }
                            }
                            properties.put(propertyName, attributes);
                        }
                );
        return properties;
    }

    /**
     * Create all terms
     *
     * @param row           Row to check
     * @param headers       Headers
     * @param terminologyId Terminology id
     * @param status        Concept status
     * @return Map of reference names and their nodes
     */
    private Map<String, List<GenericNode>> createTerms(Row row, Map<String, Integer> headers, UUID terminologyId, String status) {
        Map<String, List<GenericNode>> terms = new HashMap<>();
        headers.entrySet().stream()
                .filter(entry -> cellHasText(row.getCell(headers.get(entry.getKey()))) &&
                        Arrays.asList(TERM_TYPES).contains(entry.getKey().split(HEADER_SEPARATOR)[0]))
                .forEach(entry -> {
                    String[] headerValues = entry.getKey().split(HEADER_SEPARATOR);
                    if (headerValues.length != 2) {
                        throw new ExcelParseException("Term name missing language suffix", row, entry.getValue());
                    }
                    List<GenericNode> termList = terms.getOrDefault(headerValues[0], new ArrayList<>());

                    row.getCell(headers.get(entry.getKey()))
                            .getStringCellValue().lines()
                            .filter(value -> value != null && !value.isEmpty())
                            .forEach(value -> {
                                Map<String, List<Attribute>> properties = new HashMap<>();
                                //headerValues[1] should always be language
                                properties.put("prefLabel", List.of(new Attribute(headerValues[1], value)));
                                properties.put("status", List.of(new Attribute("", status)));
                                for (String property : TERM_PROPERTIES) {
                                    properties.put(property, List.of(new Attribute("", "")));
                                }
                                var node = new GenericNode(new TypeId(NodeType.Term, new GraphId(terminologyId), TERM_TYPE_URI), properties, emptyMap());
                                termList.add(node);
                            });

                    terms.put(headerValues[0], termList);
                });

        return terms;
    }

    /**
     * Map column names and check if languages exist in terminology
     *
     * @param row       Header row
     * @param languages List of languages
     * @return Map of column names and their indexes
     */
    private Map<String, Integer> mapColumnNames(Row row, List<String> languages) {
        if (row == null) {
            return emptyMap();
        }
        HashMap<String, Integer> columnMap = new HashMap<>();
        row.forEach(cell -> {
            String[] separatedValue = cell.getStringCellValue().split(HEADER_SEPARATOR);
            if (separatedValue.length > 1 && languages.stream().noneMatch(language -> language.equals(separatedValue[1]))) {
                throw new ExcelParseException("Language does not exist in terminology", row, cell.getColumnIndex());
            }
            columnMap.put(cell.getStringCellValue(), cell.getColumnIndex());
        });
        return columnMap;
    }

    private boolean cellHasText(Cell cell) {
        return cell != null && cell.getCellType() != CellType.BLANK;
    }

    private boolean isPropertyValid(String propertyName, String propertyValue) {
        if (propertyName.equals("status")) {
            try {
                Status.valueOf(propertyValue);
                return true;
            } catch (IllegalArgumentException iae) {
                return false;
            }
        }
        if (propertyValue.length() > ValidationConstants.TEXT_AREA_MAX_LENGTH) {
            return false;
        }
        return true;
    }

}
