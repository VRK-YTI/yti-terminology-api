package fi.vm.yti.terminology.api.index;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fi.vm.yti.terminology.api.util.IndexUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static fi.vm.yti.terminology.api.util.JsonUtils.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class Concept {

    private final UUID id;
    private final Vocabulary vocabulary;
    private final Map<String, List<String>> label;
    private final Map<String, List<String>> altLabel;
    private final Map<String, List<String>> definition;
    @Nullable
    private final String status;
    private final List<UUID> broaderIds;
    private final List<UUID> narrowerIds;
    @Nullable
    private final String createdDate;
    @Nullable
    private final String lastModifiedDate;
    @Nullable
    private final String uri;

    private Concept(UUID id,
                    Vocabulary vocabulary,
                    Map<String, List<String>> label,
                    Map<String, List<String>> altLabel,
                    Map<String, List<String>> definition,
                    @Nullable String status,
                    List<UUID> broaderIds,
                    List<UUID> narrowerIds,
                    @Nullable String createdDate,
                    @Nullable String lastModifiedDate,
                    @Nullable String uri) {

        this.id = id;
        this.vocabulary = vocabulary;
        this.label = label;
        this.altLabel = altLabel;
        this.definition = definition;
        this.status = status != null ? status : "DRAFT";
        this.broaderIds = broaderIds;
        this.narrowerIds = narrowerIds;
        this.createdDate = createdDate;
        this.lastModifiedDate = lastModifiedDate;
        this.uri = uri;
    }

    private static @NotNull Concept createFromTermedNodes(@NotNull JsonNode conceptJson,
                                                          @NotNull List<JsonNode> prefLabelXlReferences,
                                                          @NotNull List<JsonNode> altLabelXlReferences,
                                                          @NotNull Vocabulary vocabulary) {

        UUID id = UUID.fromString(conceptJson.get("id").textValue());
        String createdDate = conceptJson.get("createdDate").textValue();
        String lastModifiedDate = conceptJson.get("lastModifiedDate").textValue();

        JsonNode properties = conceptJson.get("properties");
        JsonNode references = conceptJson.get("references");
        JsonNode referrers = conceptJson.get("referrers");
        Map<String, List<String>> label =
                properties.has("prefLabel")
                        ? localizableFromTermedProperties(properties, "prefLabel")
                        : prefLabelXlReferences.size() > 0
                        ? localizableFromTermReferences(prefLabelXlReferences, "prefLabel")
                        : Collections.emptyMap();

        Map<String, List<String>> altLabel =
                properties.has("altLabel")
                        ? localizableFromTermedProperties(properties, "altLabel")
                        : altLabelXlReferences.size() > 0
                        ? localizableFromTermReferences(altLabelXlReferences, "prefLabel")
                        : Collections.emptyMap();

        Map<String, List<String>> definition = localizableFromTermedProperties(properties, "definition");

        String status = getSinglePropertyValue(properties, "status");

        List<UUID> broaderIds = getReferenceIdsFromTermedReferences(references, "broader", "Concept");
        List<UUID> narrowerIds = getReferenceIdsFromTermedReferences(referrers, "broader", "Concept");

        JsonNode uri = conceptJson.get("uri");

        return new Concept(id, vocabulary, label, altLabel, definition, status, broaderIds, narrowerIds, createdDate, lastModifiedDate, uri != null ? uri.asText() : null);
    }

    static @NotNull Concept createFromExtJson(@NotNull JsonNode json, @NotNull Vocabulary vocabulary) {

        JsonNode references = json.get("references");

        List<JsonNode> prefLabelXlReferences = references.has("prefLabelXl")
                ? asStream(references.get("prefLabelXl")).collect(toList())
                : Collections.emptyList();

        List<JsonNode> altLabelXlReferences = references.has("altLabelXl")
                ? asStream(references.get("altLabelXl")).collect(toList())
                : Collections.emptyList();

        return createFromTermedNodes(json, prefLabelXlReferences, altLabelXlReferences, vocabulary);
    }

    static @NotNull Concept createFromAllNodeResult(@NotNull UUID conceptId, @NotNull UUID vocabularyId, @NotNull AllNodesResult allNodesResult) {

        JsonNode conceptJson = requireNonNull(allNodesResult.getNode(conceptId, "Concept"));
        JsonNode vocabularyJson = requireNonNull(allNodesResult.getNode(vocabularyId));        
        Vocabulary vocabulary = Vocabulary.createFromExtJson(vocabularyJson);

        JsonNode references = conceptJson.get("references");

        List<JsonNode> prefLabelXLReferences =
                getReferenceIdsFromTermedReferences(references, "prefLabelXl", "Term").stream()
                        .map(refId -> {
                            JsonNode term = allNodesResult.getNode(refId, "Term");

                            if (term == null)
                                throw new BrokenTermedDataLinkException(vocabulary, refId);

                            return term;
                        })
                        .collect(toList());

        List<JsonNode> altLabelXLReferences =
                getReferenceIdsFromTermedReferences(references, "altLabelXl", "Term").stream()
                        .map(refId -> {
                            JsonNode term = allNodesResult.getNode(refId, "Term");

                            if (term == null)
                                throw new BrokenTermedDataLinkException(vocabulary, refId);

                            return term;
                        })
                        .collect(toList());

        return createFromTermedNodes(conceptJson, prefLabelXLReferences, altLabelXLReferences, vocabulary);
    }

    static @NotNull Concept createFromIndex(ObjectMapper mapper, @NotNull JsonNode json) {

        UUID id = UUID.fromString(json.get("id").textValue());
        List<UUID> broader = jsonToList(json.get("broader"));
        List<UUID> narrower = jsonToList(json.get("narrower"));
        Map<String, List<String>> definition = jsonToLocalizable(mapper, json.get("definition"));
        Map<String, List<String>> label = jsonToLocalizable(mapper, json.get("label"));
        Map<String, List<String>> altLabel = jsonToLocalizable(mapper, json.get("altLabel"));
        String createdDate = json.has("created") ? json.get("created").textValue() : null;
        String lastModifiedDate = json.has("modified")  ? json.get("modified").textValue() : null;
        String status = json.has("status") ? json.get("status").textValue() : null;
        Vocabulary vocabulary = Vocabulary.createFromIndex(mapper, json.get("vocabulary"));
        String uri = json.has("uri") ? json.get("uri").textValue() : null;

        return new Concept(id, vocabulary, label, altLabel, definition, status, broader, narrower, createdDate, lastModifiedDate, uri);
    }

    private static @NotNull List<UUID> getReferenceIdsFromTermedReferences(@NotNull JsonNode references, @NotNull String referenceName, @NotNull String typeRequirement) {
        if (references.has(referenceName)) {
            return asStream(references.get(referenceName))
                    .filter(node -> AllNodesResult.typeIs(node, typeRequirement))
                    .map(node -> UUID.fromString(node.get("id").textValue()))
                    .collect(toList());
        } else {
            return Collections.emptyList();
        }
    }

    @NotNull String getDocumentId() {
        return formDocumentId(vocabulary.getGraphId(), id);
    }

    static @NotNull String formDocumentId(@NotNull UUID graphId, @NotNull UUID conceptId) {
        return graphId + "/" + conceptId;
    }

    @NotNull List<UUID> getBroaderIds() {
        return broaderIds;
    }

    @NotNull List<UUID> getNarrowerIds() {
        return narrowerIds;
    }

    @NotNull JsonNode toElasticSearchDocument(ObjectMapper mapper) {

        ObjectNode output = mapper.createObjectNode();

        output.put("id", id.toString());
        output.set("broader", listToJson(mapper, broaderIds));
        output.set("narrower", listToJson(mapper, narrowerIds));
        output.set("definition", localizableToJson(mapper, definition));
        output.set("label", localizableToJson(mapper, label));
        output.set("altLabel", localizableToJson(mapper, altLabel));
        output.set("sortByLabel", localizableToJson(mapper, IndexUtil.createSortLabels(label)));

        if (createdDate != null) {
            output.put("created", createdDate);
        }

        if (lastModifiedDate != null) {
            output.put("modified", lastModifiedDate);
        }

        output.put("hasNarrower",narrowerIds.size() > 0);

        if (status != null) {
            output.put("status", status);
        }

        if (uri != null) {
            output.put("uri", uri);
        }

        output.set("vocabulary", vocabulary.toElasticSearchObject(mapper));

        return output;
    }
}
