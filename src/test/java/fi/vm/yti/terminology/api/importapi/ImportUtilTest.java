package fi.vm.yti.terminology.api.importapi;

import fi.vm.yti.terminology.api.model.termed.*;
import org.junit.jupiter.api.Test;

import static java.util.Collections.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ImportUtilTest {

    UUID graphId = UUID.randomUUID();

    @Test
    public void createBatchesConceptsAndTerms() {
        var terminology_id = UUID.randomUUID();

        var concept1_id = UUID.randomUUID();
        var concept2_id = UUID.randomUUID();

        var term1_id = UUID.randomUUID();
        var term2_id = UUID.randomUUID();

        Map<String, List<Identifier>> references_1 = Map.of(
                "prefLabelXl", List.of(new Identifier(term1_id, new TypeId(NodeType.Term, new GraphId(graphId))))
        );

        Map<String, List<Identifier>> references_2 = Map.of(
                "prefLabelXl", List.of(new Identifier(term2_id, new TypeId(NodeType.Term, new GraphId(graphId))))
        );

        GenericNode terminology = getNode(terminology_id, "terminological-vocabulary-1",
                NodeType.TerminologicalVocabulary, emptyMap());

        GenericNode concept1 = getNode(concept1_id, "concept-1", NodeType.Concept, references_1);
        GenericNode concept2 = getNode(concept2_id, "concept-2", NodeType.Concept, references_2);

        GenericNode term1 = getNode(term1_id, "term-1", NodeType.Term, emptyMap());
        GenericNode term2 = getNode(term2_id, "term-2", NodeType.Term, emptyMap());

        List<List<GenericNode>> batches = ImportUtil.getBatches(List.of(
                terminology, concept1, concept2, term1, term2
        ), 2);

        assertEquals(2, batches.size());

        assertTrue(batches.get(0).stream().anyMatch(n -> n.getId().equals(terminology_id)));
        assertTrue(batches.get(0).stream().anyMatch(n -> n.getId().equals(concept1_id)));
        assertTrue(batches.get(0).stream().anyMatch(n -> n.getId().equals(term1_id)));

        assertTrue(batches.get(1).stream().anyMatch(n -> n.getId().equals(concept2_id)));
        assertTrue(batches.get(1).stream().anyMatch(n -> n.getId().equals(term2_id)));
    }

    @Test
    /**
     * concept1 links to concept2 (broader)
     * concept2 links to concept3 (broader)
     * concept3 has external reference (closeMatch)
     * each concept has one term
     * all related nodes must be included in the same batch, even if the batch size is exceeded
     */
    public void createBatchesWithConceptReferences() {
        var concept1_id = UUID.randomUUID();
        var concept2_id = UUID.randomUUID();
        var concept3_id = UUID.randomUUID();
        var concept4_id = UUID.randomUUID();

        var term1_id = UUID.randomUUID();
        var term2_id = UUID.randomUUID();
        var term3_id = UUID.randomUUID();
        var term4_id = UUID.randomUUID();

        var conceptLinkId = UUID.randomUUID();

        GenericNode term1 = getNode(term1_id, "term-1", NodeType.Term, emptyMap());
        GenericNode term2 = getNode(term2_id, "term-2", NodeType.Term, emptyMap());
        GenericNode term3 = getNode(term3_id, "term-3", NodeType.Term, emptyMap());
        GenericNode term4 = getNode(term4_id, "term-4", NodeType.Term, emptyMap());

        GenericNode conceptLink = getNode(conceptLinkId, "concept-link-1", NodeType.ConceptLink, emptyMap());

        Map<String, List<Identifier>> references_1 = Map.of(
                "prefLabelXl", List.of(new Identifier(term1_id, new TypeId(NodeType.Term, new GraphId(graphId)))),
                "broader", List.of(new Identifier(concept2_id, new TypeId(NodeType.Concept, new GraphId(graphId))))
        );

        Map<String, List<Identifier>> references_2 = Map.of(
                "prefLabelXl", List.of(new Identifier(term2_id, new TypeId(NodeType.Term, new GraphId(graphId)))),
                "broader", List.of(new Identifier(concept3_id, new TypeId(NodeType.Concept, new GraphId(graphId))))
        );

        Map<String, List<Identifier>> references_3 = Map.of(
                "prefLabelXl", List.of(new Identifier(term3_id, new TypeId(NodeType.Term, new GraphId(graphId)))),
                "closeMatch", List.of(new Identifier(conceptLinkId, new TypeId(NodeType.ConceptLink, new GraphId(graphId))))
        );

        Map<String, List<Identifier>> references_4 = Map.of(
                "prefLabelXl", List.of(new Identifier(term4_id, new TypeId(NodeType.Term, new GraphId(graphId))))
        );

        GenericNode concept1 = getNode(concept1_id, "concept-1", NodeType.Concept, references_1);
        GenericNode concept2 = getNode(concept2_id, "concept-2", NodeType.Concept, references_2);
        GenericNode concept3 = getNode(concept3_id, "concept-3", NodeType.Concept, references_3);
        GenericNode concept4 = getNode(concept4_id, "concept-4", NodeType.Concept, references_4);

        List<GenericNode> allNodes = List.of(
                concept1,
                concept2,
                concept3,
                concept4,
                term1,
                term2,
                term3,
                term4,
                conceptLink
        );

        List<List<GenericNode>> batches = ImportUtil.getBatches(allNodes, 2);

        assertEquals(2, batches.size());
        assertEquals(9, batches.stream().mapToInt(List::size).sum());

        // First batch should include concepts 1, 2 and 3 with their references
        List.of(concept1, concept2, concept3, term1, term2, term3, conceptLink).forEach(node -> assertTrue(
                batches.get(0).stream().anyMatch(n -> n.getId().equals(node.getId())),
                String.format("Node %s not found from batch", node.getId()))
        );

        List.of(concept4, term4).forEach(node -> assertTrue(
                batches.get(1).stream().anyMatch(n -> n.getId().equals(node.getId())),
                String.format("Node %s not found from batch", node.getId()))
        );
    }

    private GenericNode getNode(UUID concept1_id, String code, NodeType type, Map<String, List<Identifier>> references) {
        return new GenericNode(concept1_id,
                code, "uri", 0L,
                null, null, null, null,
                new TypeId(type, new GraphId(graphId)),
                emptyMap(), references, emptyMap());
    }
}
