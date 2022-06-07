package fi.vm.yti.terminology.api.importapi;

import com.google.common.collect.Iterables;
import fi.vm.yti.terminology.api.model.termed.GenericNode;
import fi.vm.yti.terminology.api.model.termed.Identifier;
import fi.vm.yti.terminology.api.model.termed.NodeType;

import java.util.*;
import java.util.stream.Collectors;

public class ImportUtil {

    static List<String> termReferenceProperties = List.of("prefLabelXl", "altLabelXl", "notRecommendedSynonym", "hiddenTerm", "searchTerm");
    static List<String> conceptReferenceProperties = List.of("broader", "narrower", "hasPart", "isPartOf", "related");
    static List<String> externalReferenceProperties = List.of("closeMatch", "exactMatch", "relatedMatch");

    /**
     * Split node list to smaller batches. Eventual batch size might be greater than defined one,
     * because all referred nodes must reside in the same batch
     *
     * @param allNodes
     * @param batchSize
     * @return
     */
    public static List<List<GenericNode>> getBatches(List<GenericNode> allNodes, int batchSize) {
        Set<UUID> allIds = new LinkedHashSet<>();
        List<Integer> batchSizes = new ArrayList<>();

        int currentBatchSize = 0;

        for (GenericNode node : allNodes) {
            if (allIds.contains(node.getId())) {
                continue;
            }

            if (node.getType().getId() == NodeType.Concept) {
                Set<UUID> references = getReferencesRecursive(
                        allNodes,
                        node.getId(),
                        new HashSet<>()
                );
                allIds.addAll(references);
                currentBatchSize += references.size();
            } else if (node.getType().getId() == NodeType.Collection) {
                allIds.add(node.getId());
                currentBatchSize++;
            } else if (node.getType().getId() == NodeType.TerminologicalVocabulary) {
                allIds.add(node.getId());
                currentBatchSize++;
            }

            if (currentBatchSize > batchSize) {
                batchSizes.add(currentBatchSize);
                currentBatchSize = 0;
            }
        }

        if (currentBatchSize > 0) {
            batchSizes.add(currentBatchSize);
        }

        List<List<GenericNode>> batches = new ArrayList<>();

        int total = 0;
        for (Integer size : batchSizes) {

            batches.add(
                    allIds.stream()
                            .skip(total)
                            .limit(size)
                            .map(id -> Iterables.find(allNodes, (n) -> n.getId().equals(id)))
                            .collect(Collectors.toList())
            );
            total += size;
        }

        return batches;
    }

    private static Set<UUID> getReferencesRecursive(List<GenericNode> allNodes, UUID current, Set<UUID> result) {

        // add current node to result set
        result.add(current);

        // find node from list
        GenericNode node = Iterables.find(allNodes, (n) -> n.getId().equals(current));

        // add term and external concept references
        result.addAll(findReferenceNodes(node, allNodes, termReferenceProperties));
        result.addAll(findReferenceNodes(node, allNodes, externalReferenceProperties));

        // recursively add concept references
        Set<UUID> conceptRefs = findReferenceNodes(node, allNodes, conceptReferenceProperties);

        for (UUID refId : conceptRefs) {
            boolean added = result.add(refId);
            if (added) {
                getReferencesRecursive(allNodes, refId, result);
            }
        }

        return result;
    }

    private static Set<UUID> findReferencesByType(String type, GenericNode node) {
        return node.getReferences().getOrDefault(type, Collections.emptyList())
                .stream()
                .map(Identifier::getId)
                .collect(Collectors.toSet());
    }

    private static Set<UUID> findReferenceNodes(GenericNode node, List<GenericNode> allNodes, List<String> propertyNames) {
        Set<UUID> result = new HashSet<>();
        for (String type : propertyNames) {
            Set<UUID> refs = findReferencesByType(type, node);
            result.addAll(allNodes.stream()
                    .map(GenericNode::getId)
                    .filter(refs::contains)
                    .collect(Collectors.toSet())
            );
        }
        return result;
    }

}
