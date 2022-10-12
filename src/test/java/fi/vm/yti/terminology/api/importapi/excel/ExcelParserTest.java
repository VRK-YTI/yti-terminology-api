package fi.vm.yti.terminology.api.importapi.excel;

import fi.vm.yti.terminology.api.frontend.Status;
import fi.vm.yti.terminology.api.model.termed.Attribute;
import fi.vm.yti.terminology.api.model.termed.GenericNode;
import fi.vm.yti.terminology.api.model.termed.NodeType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class ExcelParserTest {

    ExcelParser parser = new ExcelParser();

    @Test
    public void buildNewTerminologyNode() {
        XSSFWorkbook workbook = getWorkbook("excel_export.xlsx");

        Map<String, String> groupMap = Map.of(
                "P26", "74a41211-8c99-4835-a519-7a61612b1098"
        );
        TerminologyImportDTO dto = parser.buildTerminologyNode(workbook, groupMap,
                List.of("7d3a3c00-5a6b-489b-a3ed-63bb58c26a63"));

        assertNotNull(dto.getTerminologyNode().getId());
        assertNotNull(dto.getTerminologyNode().getType().getGraph());

        assertEquals(NodeType.TerminologicalVocabulary, dto.getTerminologyNode().getType().getId());
        assertEquals("terminological-vocabulary-0", dto.getTerminologyNode().getCode());
        assertEquals("testdev", dto.getNamespace());
        assertEquals("http://uri.suomi.fi/terminology/testdev/terminological-vocabulary-0",
                dto.getTerminologyNode().getUri());

        // properties
        var prefLabel_fi = getProperty(dto, "prefLabel").get(0);
        var prefLabel_sv = getProperty(dto, "prefLabel").get(1);
        var description = getProperty(dto, "description").get(0);
        var contact = getProperty(dto, "contact").get(0);

        assertEquals("Test terminology fi", prefLabel_fi.getValue());
        assertEquals("fi", prefLabel_fi.getLang());
        assertEquals("Svenska testisanasto", prefLabel_sv.getValue());
        assertEquals("sv", prefLabel_sv.getLang());

        assertEquals("Kuvaus", description.getValue());
        assertEquals("fi", description.getLang());

        assertEquals("", contact.getValue());

        // references
        assertEquals("7d3a3c00-5a6b-489b-a3ed-63bb58c26a63", dto.getTerminologyNode().getReferences()
                .get("contributor").get(0).getId().toString());
        assertEquals(1, dto.getTerminologyNode().getReferences().get("inGroup").size());
    }

    @Test
    public void buildTermNodes() {
        XSSFWorkbook workbook = getWorkbook("excel_export.xlsx");
        UUID terminologyId = UUID.randomUUID();
        ExcelParser parser = new ExcelParser();

        var genericNodes = parser.buildTermNodes(
                workbook,
                "testdev",
                terminologyId,
                Arrays.asList("fi", "en", "sv")
        );

        assertEquals(3, genericNodes.size());
        assertTrue(genericNodes.stream().allMatch(node -> node.getType().getGraph().getId().equals(terminologyId)));

        GenericNode node = genericNodes.get(0);
        // finnish term
        assertEquals("term name fi", getPropertyValue(node, "prefLabel"));
        // english term
        assertEquals("term name en", getProperty(genericNodes.get(1), "prefLabel").get(0).getValue());
        // placeholder term
        assertEquals("term placeholder", getProperty(genericNodes.get(2), "prefLabel").get(0).getValue());
        assertEquals("source", getPropertyValue(node, "source"));
        assertEquals("scope", getPropertyValue(node, "scope"));
        assertEquals("style", getPropertyValue(node, "termStyle"));
        assertEquals("family", getPropertyValue(node, "termFamily"));
        assertEquals("conjugation", getPropertyValue(node, "termConjugation"));
        assertEquals("equivalency", getPropertyValue(node, "termEquivalency"));
        assertEquals("info", getPropertyValue(node, "termInfo"));
        assertEquals("wordclass", getPropertyValue(node, "wordClass"));
        assertEquals("1", getPropertyValue(node, "termHomographNumber"));
        assertEquals("draft comment", getPropertyValue(node, "draftComment"));
        assertEquals("history note", getPropertyValue(node, "historyNote"));
        assertEquals("note", getPropertyValue(node, "changeNote"));
        assertEquals(Status.DRAFT.name(), getPropertyValue(node, "status"));

        assertEquals("term-0", node.getCode());
        assertEquals("http://uri.suomi.fi/terminology/testdev/term-0", node.getUri());

        assertNull(genericNodes.get(2).getUri());
        assertNull(genericNodes.get(2).getCode());
    }

    @Test
    public void buildConceptAndConceptLinkNodes() {
        XSSFWorkbook workbook = getWorkbook("excel_export.xlsx");
        UUID terminologyId = UUID.randomUUID();
        ExcelParser parser = new ExcelParser();

        List<GenericNode> nodes = parser.buildConceptNodes(workbook,
                "testdev",
                terminologyId,
                List.of("fi", "en")
        );
        var conceptNode = nodes.stream()
                .filter(n -> n.getType().getId() == NodeType.Concept)
                .findFirst()
                .get();

        // one concept and two concept links
        assertEquals(3, nodes.size());
        assertEquals("concept-2", conceptNode.getCode());
        assertEquals("http://uri.suomi.fi/terminology/testdev/concept-2", conceptNode.getUri());
        assertEquals(Status.DRAFT.name(), getPropertyValue(conceptNode, "status"));

        assertEquals(2, conceptNode.getProperties().get("definition").stream().filter(p -> p.getLang().equals("fi")).count());
        assertEquals(1, conceptNode.getProperties().get("definition").stream().filter(p -> p.getLang().equals("en")).count());

        assertEquals(1, conceptNode.getProperties().get("note").stream().filter(p -> p.getLang().equals("fi")).count());
        assertEquals(2, conceptNode.getProperties().get("note").stream().filter(p -> p.getLang().equals("en")).count());

        assertEquals("Definition FI 1", getPropertyValue(conceptNode, "definition"));
        assertEquals("Definition FI 2", getPropertyValue(conceptNode, "definition", 1));
        assertEquals("Definition EN", getPropertyValue(conceptNode, "definition", 2));
        assertEquals("Note FI", getPropertyValue(conceptNode, "note"));
        assertEquals("Note EN 1", getPropertyValue(conceptNode, "note", 1));
        assertEquals("Note EN 2", getPropertyValue(conceptNode, "note", 2));
        assertEquals("Example", getPropertyValue(conceptNode, "example"));
        assertEquals("Subjectarea", getPropertyValue(conceptNode, "subjectArea"));
        assertEquals("Class", getPropertyValue(conceptNode, "conceptClass"));
        assertEquals("Wordclass", getPropertyValue(conceptNode, "wordClass"));
        assertEquals("Change note", getPropertyValue(conceptNode, "changeNote"));
        assertEquals("History note", getPropertyValue(conceptNode, "historyNote"));
        assertEquals("Notation", getPropertyValue(conceptNode, "notation"));
        assertEquals("Source", getPropertyValue(conceptNode, "source"));

        assertEquals("b8f88a28-4425-4b24-8d4a-98603789f40a", conceptNode.getReferences().get("broader").get(0).getId().toString());

        assertEquals("9f991927-6fd8-49e1-b102-7a8963af3305", conceptNode.getReferences().get("exactMatch").get(0).getId().toString());
        assertEquals("8f991927-6fd8-49e1-b102-7a8963af3305", conceptNode.getReferences().get("closeMatch").get(0).getId().toString());

        var conceptLinkNode = nodes.stream()
                .filter(n -> n.getCode().equals("concept-link-1"))
                .findFirst()
                .get();

        assertEquals("18df7b1f-08aa-405b-9043-20b3acd4795e", getPropertyValue(conceptLinkNode, "targetId"));
        assertEquals("d02d106f-2155-484d-b05b-fbbc83e1b268", getPropertyValue(conceptLinkNode, "targetGraph"));
        assertEquals("Terminology name", getPropertyValue(conceptLinkNode, "vocabularyLabel"));
        assertEquals("Exact concept name", getPropertyValue(conceptLinkNode, "prefLabel"));
    }

    @Test
    public void buildCollectionNodes() {
        XSSFWorkbook workbook = getWorkbook("excel_export.xlsx");
        UUID terminologyId = UUID.randomUUID();
        ExcelParser parser = new ExcelParser();

        List<GenericNode> nodes = parser.buildCollectionNodes(workbook, "testdev", terminologyId, List.of("fi"));

        var node = nodes.get(0);

        assertEquals("Testivalikoima", getPropertyValue(node, "prefLabel"));
        assertEquals("Testivalikoiman kuvaus", getPropertyValue(node, "definition"));

        assertEquals(2, node.getReferences().get("member").size());

        assertEquals("http://uri.suomi.fi/terminology/testdev/collection-0", node.getUri());
        assertEquals("collection-0", node.getCode());
    }

    private List<Attribute> getProperty(TerminologyImportDTO dto, String name) {
        return getProperty(dto.getTerminologyNode(), name);
    }

    private String getPropertyValue(GenericNode node, String name) {
        return getProperty(node, name).get(0).getValue();
    }

    private String getPropertyValue(GenericNode node, String name, int index) {
        return getProperty(node, name).get(index).getValue();
    }

    private List<Attribute> getProperty(GenericNode node, String name) {
        return node.getProperties().get(name);
    }

    private XSSFWorkbook getWorkbook(String filename) {
        InputStream is = this.getClass().getResourceAsStream("/importapi/excel/" + filename);
        try {
            return parser.getWorkbook(is);
        } catch (Exception e) {
            return null;
        }
    }

}
