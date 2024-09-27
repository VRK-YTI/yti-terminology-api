package fi.vm.yti.terminology.api.v2.migration.v1;

import com.fasterxml.jackson.databind.JsonNode;
import fi.vm.yti.terminology.api.v2.dto.LocalizedValueDTO;

import java.util.*;

public class TermedDataParser {

    private final JsonNode json;

    public TermedDataParser(JsonNode json) {
        this.json = json;
    }

    public String getProperty(String name) {
        if (!hasProperty(name)) {
            return null;
        }

        return this.json.get("properties").get(name).iterator().next().get("value").asText();
    }

    public List<String> getListProperty(String name) {
        if (!hasProperty(name)) {
            return new ArrayList<>();
        }
        var result = new ArrayList<String>();
        this.json.get("properties").get(name).iterator()
                .forEachRemaining(v -> result.add(v.get("value").asText()));

        return result;
    }

    public Map<String, String> getLocalizedProperty(String name) {
        if (!hasProperty(name)) {
            return new HashMap<>();
        }
        var result = new HashMap<String, String>();
        this.json.get("properties").get(name).iterator().forEachRemaining(prop ->
                result.put(prop.get("lang").asText(), prop.get("value").asText()));
        return result;
    }

    public List<LocalizedValueDTO> getLocalizedListValue(String name) {
        if (!hasProperty(name)) {
            return new ArrayList<>();
        }

        var result = new ArrayList<LocalizedValueDTO>();
        this.json.get("properties").get(name).iterator().forEachRemaining(prop ->
                result.add(new LocalizedValueDTO(prop.get("lang").asText(), prop.get("value").asText())));
        return result;
    }

    public Set<String> getReferences(String name) {
        if (!this.json.get("references").has(name)) {
            return new LinkedHashSet<>();
        }

        var result = new LinkedHashSet<String>();

        this.json.get("references").get(name).iterator()
                .forEachRemaining(ref -> result.add(TermedDataMapper.fixURI(ref.get("uri").asText())));

        return result;
    }

    public List<JsonNode> getReferenceNodes(String name) {
        if (!this.json.get("references").has(name)) {
            return new ArrayList<>();
        }
        var iterator = this.json.get("references").get(name).iterator();

        var result = new ArrayList<JsonNode>();
        while (iterator.hasNext()) {
            result.add(iterator.next());
        }
        return result;
    }

    public String getString(String name) {
        return this.json.has(name) ? this.json.get(name).asText() : null;
    }

    private boolean hasProperty(String name) {
        return this.json.has("properties") && this.json.get("properties").has(name);
    }
}
