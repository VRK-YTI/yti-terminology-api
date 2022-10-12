package fi.vm.yti.terminology.api.importapi.excel;

import fi.vm.yti.terminology.api.exception.ExcelParseException;
import fi.vm.yti.terminology.api.frontend.Status;
import fi.vm.yti.terminology.api.migration.DomainIndex;
import fi.vm.yti.terminology.api.model.termed.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.*;

public class ExcelParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelParser.class);

    private static final String SEPARATOR = ";";
    private static final String URI_PREFIX = "http://uri.suomi.fi/terminology";

    public static final String SHEET_TERMINOLOGY = "Terminology details";
    public static final String SHEET_CONCEPTS = "Concepts";
    public static final String SHEET_TERMS = "Terms";
    public static final String SHEET_COLLECTIONS = "Collections";
    public static final String SHEET_CONCEPT_LINKS = "Concept links";

    public XSSFWorkbook getWorkbook(InputStream is) throws IOException {
        try {
            return new XSSFWorkbook(is);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw e;
        }
    }

    public TerminologyImportDTO buildTerminologyNode(XSSFWorkbook workbook, Map<String, String> groupMap,
                                                     List<String> defaultOrganizations) throws ExcelParseException {
        XSSFSheet sheet = getSheet(workbook, SHEET_TERMINOLOGY);
        XSSFRow row = getRow(sheet, 1);
        XSSFRow headerRow = getRow(sheet, 0);
        var columnMap = mapColumnNames(headerRow);

        XSSFCell languageCell = row.getCell(columnMap.get(Fields.LANGUAGE));

        if (isEmptyCell(languageCell)) {
            throw new ExcelParseException("Missing value", row, Fields.LANGUAGE);
        }

        var languages = getSplittedCellValues(row, columnMap.get(Fields.LANGUAGE));

        String namespace = getCellValue(row, columnMap, Fields.NAMESPACE, true);

        try {
            Map<String, List<Attribute>> properties = Map.of(
                    "prefLabel", getLocalizedProperty(row, columnMap, Fields.PREFLABEL, languages, true),
                    "status", getStatus(row, columnMap),
                    "description", getLocalizedProperty(row, columnMap, "description", languages),
                    "contact", getProperty(row, columnMap.get(Fields.CONTACT)),
                    "language", languages.stream()
                            .map(lang -> new Attribute("", lang))
                            .collect(Collectors.toList())
            );

            // find uuids of information domains
            List<String> informationDomains =
                    getSplittedCellValues(row, columnMap.get(Fields.INFORMATIONDOMAIN))
                            .stream()
                            .map(domain -> {
                                String value = groupMap.get(domain);
                                if (value == null) {
                                    throw new ExcelParseException("Invalid value", row, Fields.INFORMATIONDOMAIN);
                                }
                                return value;
                            })
                            .collect(Collectors.toList());

            // If no organizations specified use user's organization
            List<String> organizations = getMergedCellValues(row, columnMap.get(Fields.CONTRIBUTOR));

            Map<String, List<Identifier>> references = Map.of(
                    "contributor", getIdentifiers(
                            organizations.isEmpty() ? defaultOrganizations : organizations,
                            DomainIndex.ORGANIZATION_DOMAIN
                    ),
                    "inGroup", getIdentifiers(informationDomains, DomainIndex.GROUP_DOMAIN)
            );

            var node = getNode(row,
                    new TypeId(
                        NodeType.TerminologicalVocabulary,
                        new GraphId(getUUID(row, columnMap.get(Fields.GRAPH_ID), true))
                    ),
                    columnMap,
                    namespace,
                    properties,
                    references,
                    emptyMap()
            );

            return new TerminologyImportDTO(namespace, node, languages);
        } catch (ExcelParseException ee) {
            throw ee;
        } catch (Exception e) {
            throw new ExcelParseException(e.getMessage(), row);
        }
    }

    public List<GenericNode> buildTermNodes(XSSFWorkbook workbook, String namespace, UUID terminologyId, List<String> languages) {
        var sheet = getSheet(workbook, SHEET_TERMS);

        var columnMap = mapColumnNames(getRow(sheet, 0));

        List<GenericNode> nodes = new ArrayList<>();
        Row row = null;
        try {
            for (var i = 1; i <= sheet.getLastRowNum(); i++) {
                row = sheet.getRow(i);

                Map<String, List<Attribute>> properties = new HashMap<>();

                properties.put("prefLabel", getLocalizedProperty(row, columnMap, Fields.PREFLABEL, languages, true));
                properties.put("source", getMergedCellAttributes(row, columnMap.get(Fields.SOURCE)));
                properties.put("scope", getProperty(row, columnMap.get(Fields.SCOPE)));
                properties.put("termStyle", getProperty(row, columnMap.get(Fields.TERMSTYLE)));
                properties.put("termFamily", getProperty(row, columnMap.get(Fields.TERMFAMILY)));
                properties.put("termConjugation", getProperty(row, columnMap.get(Fields.TERMCONJUGATION)));
                properties.put("termEquivalency", getProperty(row, columnMap.get(Fields.TERMEQUIVALENCY)));
                properties.put("termInfo", getProperty(row, columnMap.get(Fields.TERMINFO)));
                properties.put("wordClass", getProperty(row, columnMap.get(Fields.WORDCLASS)));
                properties.put("termHomographNumber", getProperty(row, columnMap.get(Fields.HOMOGRAPHNUMBER)));
                properties.put("draftComment", getProperty(row, columnMap.get(Fields.DRAFTCOMMENT)));
                properties.put("historyNote", getProperty(row, columnMap.get(Fields.HISTORYNOTE)));
                properties.put("changeNote", getProperty(row, columnMap.get(Fields.CHANGENOTE)));
                properties.put("status", getStatus(row, columnMap));

                nodes.add(
                        getNode(row,
                            new TypeId(
                                    NodeType.Term,
                                    new GraphId(terminologyId)
                            ),
                            columnMap,
                            namespace,
                            properties,
                            emptyMap(),
                            emptyMap()
                    )
                );
            }
        } catch (ExcelParseException ee) {
            throw ee;
        } catch (Exception e) {
            throw new ExcelParseException("Invalid term data: " + e.getMessage(), row);
        }

        return nodes;
    }

    public List<GenericNode> buildConceptNodes(XSSFWorkbook workbook, String namespace, UUID terminologyId, List<String> languages) {
        var sheet = getSheet(workbook, SHEET_CONCEPTS);

        var columnMap = mapColumnNames(getRow(sheet, 0));

        List<GenericNode> nodes = new ArrayList<>();
        Row row = null;

        // get concept link for references
        var conceptLinks = buildConceptLinkNodes(workbook, namespace, terminologyId, languages);

        try {
            for (var i = 1; i <= sheet.getLastRowNum(); i++) {
                row = sheet.getRow(i);

                Map<String, List<Attribute>> properties = new HashMap<>();

                properties.put("definition", getLocalizedProperty(row, columnMap, Fields.DEFINITION, languages));
                properties.put("note", getLocalizedProperty(row, columnMap, Fields.NOTE, languages));
                properties.put("example", getLocalizedProperty(row, columnMap, Fields.EXAMPLE, languages));
                properties.put("conceptClass", getProperty(row, columnMap.get(Fields.CONCEPTCLASS)));
                properties.put("wordClass", getProperty(row, columnMap.get(Fields.WORDCLASS)));
                properties.put("changeNote", getProperty(row, columnMap.get(Fields.CHANGENOTE)));
                properties.put("historyNote", getProperty(row, columnMap.get(Fields.HISTORYNOTE)));
                properties.put("status", getStatus(row, columnMap));
                properties.put("notation", getProperty(row, columnMap.get(Fields.NOTATION)));
                properties.put("source", getMergedCellAttributes(row, columnMap.get(Fields.SOURCE)));
                properties.put("externalLink", getMergedCellAttributes(row, columnMap.get(Fields.EXTERNALLINK)));
                properties.put("subjectArea", getProperty(row, columnMap.get(Fields.SUBJECTAREA)));

                Map<String, List<Identifier>> references = new HashMap<>();

                TypeId typeId = new TypeId(NodeType.Concept, new GraphId(terminologyId));

                // Term references
                references.put("prefLabelXl", getMergedCellIdentifiers(
                        row, columnMap.get(Fields.PREFERREDTERM), typeId, true));
                references.put("altLabelXl", getMergedCellIdentifiers(
                        row, columnMap.get(Fields.SYNONYM), typeId, false));
                references.put("notRecommendedSynonym", getMergedCellIdentifiers(
                        row, columnMap.get(Fields.NONRECOMMENDEDSYNONYM), typeId, false));
                references.put("hiddenTerm", getMergedCellIdentifiers(
                        row, columnMap.get(Fields.HIDDENTERM), typeId, false));
                references.put("searchTerm", getMergedCellIdentifiers(
                        row, columnMap.get(Fields.SEARCHTERM), typeId, false));

                // Concept references
                references.put("broader", getMergedCellIdentifiers(
                        row, columnMap.get(Fields.BROADERCONCEPT), typeId, false));
                references.put("narrower", getMergedCellIdentifiers(
                        row, columnMap.get(Fields.NARROWERCONCEPT), typeId, false));
                references.put("related", getMergedCellIdentifiers(
                        row, columnMap.get(Fields.RELATEDCONCEPT), typeId, false));
                references.put("isPartOf", getMergedCellIdentifiers(
                        row, columnMap.get(Fields.ISPARTOFCONCEPT), typeId, false));
                references.put("hasPart", getMergedCellIdentifiers(
                        row, columnMap.get(Fields.HASPARTCONCEPT), typeId, false));

                conceptLinks.getOrDefault(getUUID(row, columnMap.get(Fields.UUID)), new HashSet<>())
                        .forEach(link -> {
                            references.put(
                                    link.getReferenceType(),
                                    List.of(getIdentifier(
                                                    link.getConceptLinkNode().getId(),
                                                    new TypeId(
                                                            NodeType.ConceptLink,
                                                            new GraphId(terminologyId)
                                                    )
                                            )
                                    )
                            );
                            nodes.add(link.getConceptLinkNode());
                        }
                );

                nodes.add(
                        getNode(
                                row,
                                typeId,
                                columnMap,
                                namespace,
                                properties,
                                references,
                                emptyMap()
                        )
                );
            }
        } catch (ExcelParseException ee) {
            throw ee;
        } catch (Exception e) {
            throw new ExcelParseException("Invalid concept data: " + e.getMessage(), row);
        }

        return nodes;
    }

    public Map<UUID, Set<ConceptLinkImportDTO>> buildConceptLinkNodes(XSSFWorkbook workbook, String namespace, UUID terminologyId, List<String> languages) {
        var sheet = workbook.getSheet(SHEET_CONCEPT_LINKS);
        var columnMap = mapColumnNames(getRow(sheet, 0, false));

        if (columnMap.isEmpty()) {
            return emptyMap();
        }

        Map<UUID, Set<ConceptLinkImportDTO>> conceptLinkMap = new HashMap<>();
        Row row = null;

        try {
            for (var i = 1; i <= sheet.getLastRowNum(); i++) {
                row = sheet.getRow(i);

                Map<String, List<Attribute>> properties = Map.of(
                        "prefLabel", getLocalizedProperty(row, columnMap, Fields.PREFLABEL, languages),
                        "vocabularyLabel", getLocalizedProperty(row, columnMap, Fields.VOCABULARY_LABEL, languages),
                        "targetId", List.of(new Attribute("", getUUID(
                                row, columnMap.get(Fields.TARGET_ID)
                        ).toString())),
                        "targetGraph", List.of(new Attribute("", getUUID(
                                row, columnMap.get(Fields.TARGET_GRAPH)
                        ).toString()))
                );

                GenericNode node = getNode(
                        row,
                        new TypeId(
                                NodeType.ConceptLink,
                                new GraphId(terminologyId)
                        ),
                        columnMap,
                        namespace,
                        properties,
                        emptyMap(),
                        emptyMap()
                );


                UUID conceptId = getUUID(row, columnMap.get(Fields.CONCEPT_ID));
                Set<ConceptLinkImportDTO> conceptLinks = conceptLinkMap.getOrDefault(conceptId, new HashSet<>());
                conceptLinks.add(new ConceptLinkImportDTO(
                        getCellValue(row, columnMap, Fields.LINK_TYPE, true),
                        node
                ));
                conceptLinkMap.putIfAbsent(conceptId, conceptLinks);
            }
        } catch (ExcelParseException e) {
            throw e;
        } catch (Exception e) {
            throw new ExcelParseException("Invalid concept link data: " + e.getMessage(), row);
        }

        return conceptLinkMap;
    }

    public List<GenericNode> buildCollectionNodes(XSSFWorkbook workbook, String namespace, UUID terminologyId, List<String> languages) {
        var sheet = getSheet(workbook, SHEET_COLLECTIONS);

        var columnMap = mapColumnNames(getRow(sheet, 0, false));

        if (columnMap.isEmpty()) {
            return emptyList();
        }

        List<GenericNode> nodes = new ArrayList<>();
        Row row = null;

        try {
            for (var i = 1; i <= sheet.getLastRowNum(); i++) {
                row = sheet.getRow(i);

                Map<String, List<Attribute>> properties = Map.of(
                        "prefLabel", getLocalizedProperty(row, columnMap, Fields.PREFLABEL, languages),
                        "definition", getLocalizedProperty(row, columnMap, Fields.DESCRIPTION, languages)
                );

                Map<String, List<Identifier>> references = Map.of(
                       "member", getIdentifiers(
                               getMergedCellValues(row, columnMap.get(Fields.MEMBER)),
                               new TypeId(NodeType.Concept, new GraphId(terminologyId))
                        )
                );

                nodes.add(
                        getNode(
                                row,
                                new TypeId(
                                        NodeType.Collection,
                                        new GraphId(terminologyId)
                                ),
                                columnMap,
                                namespace,
                                properties,
                                references,
                                emptyMap()
                        )
                );
            }
        } catch (ExcelParseException ee) {
            throw ee;
        } catch (Exception e) {
            throw new ExcelParseException("Invalid collection data: " + e.getMessage(), row);
        }
        return nodes;
    }

    private GenericNode getNode(Row row,
                                TypeId typeId,
                                Map<String, Integer> columnMap,
                                String namespace,
                                Map<String, List<Attribute>> properties,
                                Map<String, List<Identifier>> references,
                                Map<String, List<Identifier>> referrers) throws ExcelParseException {
        String identifier;
        String uri = null;

        // IDENTIFIER column is not mandatory for terms (placeholder terms does not have that information)
        if (typeId.getId().equals(NodeType.Term)) {
            identifier = getCellValue(row, columnMap, Fields.IDENTIFIER);

            if (identifier != null && !identifier.trim().isEmpty()) {
                uri = getURI(identifier, namespace);
            } else {
                identifier = null;
            }
        } else {
            identifier = getCellValue(row, columnMap, Fields.IDENTIFIER, true);
            uri = getURI(identifier, namespace);
        }

        return new GenericNode(
                getUUID(row, columnMap.get(Fields.UUID), true),
                identifier,
                uri,
                0L, null, null, null, null,
                typeId,
                properties,
                references,
                referrers
        );
    }

    private String getURI(String identifier, String namespace) {
        return String.format("%s/%s/%s",
                URI_PREFIX,
                namespace,
                identifier
        );
    }

    private UUID getUUID(Row row, int uuidColumn, boolean createNew) throws ExcelParseException {
        Cell uuidCell = row.getCell(uuidColumn);
        try {
            if (isEmptyCell(uuidCell) && !createNew) {
                throw new ExcelParseException("Missing value", row, uuidColumn);
            } else if (isEmptyCell(uuidCell) && createNew) {
                return UUID.randomUUID();
            } else {
                return UUID.fromString(uuidCell.getStringCellValue());
            }
        } catch (Exception e) {
            throw new ExcelParseException("Invalid UUID", row, uuidColumn);
        }
    }

    private UUID getUUID(Row row, int uuidColumn) throws ExcelParseException {
        return getUUID(row, uuidColumn, false);
    }

    private Identifier getIdentifier(UUID id, TypeId typeId) {
        return new Identifier(id, typeId);
    }

    private List<Identifier> getIdentifiers(List<String> uuids, TypeId typeId) {
        return uuids.stream()
                .map(id -> getIdentifier(UUID.fromString(id), typeId))
                .collect(Collectors.toList());
    }

    private List<Attribute> getLocalizedProperty(Row row,
                                        Map<String, Integer> columnMap,
                                        String columnName,
                                        List<String> languages,
                                        boolean isMandatory) {
        List<Attribute> attributes = new ArrayList<>();
        languages.forEach(lang -> {
            String headerName = columnName + "_" + lang.trim();
            Integer columnIndex = columnMap.get(headerName.toUpperCase());

            if (columnIndex != null) {
                getMergedCellValues(row, columnIndex)
                    .stream()
                    .forEach(value -> attributes.add(new Attribute(lang, value)));
            }
        });

        if (isMandatory && attributes.size() == 0) {
            throw new ExcelParseException("Missing value", row, columnName);
        }
        return attributes;
    }

    private List<Attribute> getLocalizedProperty(Row row,
                                                 Map<String, Integer> columnMap,
                                                 String columnName,
                                                 List<String> languages
                                                 ) {
        return getLocalizedProperty(row, columnMap, columnName, languages, false);
    }

    private List<Attribute> getProperty(Row row, Integer index, String defaultValue) {

        if (index == null) {
            throw new ExcelParseException("Missing column", row);
        }

        var cell = row.getCell(index);

        if (isEmptyCell(cell)) {
            return List.of(new Attribute("", defaultValue));
        } else {
            String value;
            switch (cell.getCellType()) {
                case STRING:
                    value = cell.getStringCellValue();
                    break;
                case NUMERIC:
                    value = String.valueOf((int)cell.getNumericCellValue());
                    break;
                default:
                    value = "";
                    break;
            }
            return List.of(new Attribute("", value));
        }
    }

    private List<Attribute> getProperty(Row row, Integer index) {
        return getProperty(row, index, "");
    }

    private List<String> getMergedCellValues(Row row, Integer columnIndex, boolean isMandatory) throws ExcelParseException {

        if (columnIndex == null) {
            if (isMandatory) {
                throw new ExcelParseException("Missing value", row);
            } else {
                return emptyList();
            }
        }

        CellRangeAddress range = row.getSheet().getMergedRegions().stream()
                .filter(r -> r.getFirstRow() == 0 && r.getFirstColumn() == columnIndex)
                .findFirst()
                .orElse(null);

        List<String> values = new ArrayList<>();
        int lastColumnIndex = range != null ? range.getLastColumn() : columnIndex;

        for (int i = columnIndex; i <= lastColumnIndex; i++) {
            Cell cell = row.getCell(i);
            if (!isEmptyCell(cell)) {
                values.add(cell.getStringCellValue());
            }
        }
        if (isMandatory && values.size() == 0) {
            throw new ExcelParseException("Missing value", row, columnIndex);
        }
        return values;
    }

    private List<String> getMergedCellValues(Row row, Integer columnIndex) throws ExcelParseException {
        return getMergedCellValues(row, columnIndex, false);
    }

    private List<Attribute> getMergedCellAttributes(Row row, Integer columnIndex) throws ExcelParseException {
        return getMergedCellValues(row, columnIndex, false)
                .stream()
                .map(v -> new Attribute("", v))
                .collect(Collectors.toList());
    }

    private List<Identifier> getMergedCellIdentifiers(Row row, Integer columnIndex, TypeId typeId, boolean isMandatory) {
        return getMergedCellValues(row, columnIndex, isMandatory)
                .stream()
                .map(i -> new Identifier(UUID.fromString(i), typeId))
                .collect(Collectors.toList());
    }

    private List<String> getSplittedCellValues(Row row, Integer columnIndex) {
        Cell cell = row.getCell(columnIndex);

        if (isEmptyCell(cell)) {
            throw new ExcelParseException("Missing value", row, columnIndex);
        }

        return Arrays.stream(cell.getStringCellValue().split(SEPARATOR))
                        .collect(Collectors.toList());
    }

    private List<Attribute> getStatus(Row row, Map<String, Integer> columnMap) {
        List<Attribute> statusAttribute = getProperty(row, columnMap.get(Fields.STATUS), Status.DRAFT.name());
        String value = statusAttribute.get(0).getValue();
        try {
            Status.valueOf(value);
            return statusAttribute;
        } catch(IllegalArgumentException iae) {
            throw new ExcelParseException(String.format("Invalid value %s", value), row, Fields.STATUS);
        }
    }

    private Map<String, Integer> mapColumnNames(Row row) {
        if (row == null) {
            return emptyMap();
        }
        Map<String, Integer> columnMap = new HashMap<>();
        Iterator<Cell> iterator = row.iterator();
        while(iterator.hasNext()) {
            Cell cell = iterator.next();
            columnMap.put(cell.getStringCellValue(), cell.getColumnIndex());
        }
        return columnMap;
    }

    private XSSFSheet getSheet(XSSFWorkbook workbook, String name) {
        XSSFSheet sheet = workbook.getSheet(name);
        if (sheet == null) {
            throw new ExcelParseException("Sheet doesn't exist: " + name);
        }
        return sheet;
    }

    private XSSFRow getRow(XSSFSheet sheet, int rowNumber, boolean isMandatory) {
        XSSFRow row = sheet.getRow(rowNumber);
        if (row == null && isMandatory) {
            throw new ExcelParseException(String.format("Row %d doesn't exist, sheet %s", rowNumber, sheet.getSheetName()));
        }
        return row;
    }

    private XSSFRow getRow(XSSFSheet sheet, int rowNumber) {
        return getRow(sheet, rowNumber, true);
    }

    private String getCellValue(Row row, Map<String, Integer> columnMap, String fieldName, boolean isMandatory) {
        Cell cell = row.getCell(columnMap.get(fieldName));

        if (isEmptyCell(cell) && isMandatory) {
            throw new ExcelParseException("Missing value", row, fieldName);
        } else if (isEmptyCell(cell) && !isMandatory) {
            return null;
        } else {
            return cell.getStringCellValue();
        }
    }

    private String getCellValue(Row row, Map<String, Integer> columnMap, String fieldName) {
        return getCellValue(row, columnMap, fieldName, false);
    }

    private boolean isEmptyCell(Cell cell) {
        return cell == null || cell.getCellType() == CellType.BLANK;
    }
}
