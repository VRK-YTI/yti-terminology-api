package fi.vm.yti.terminology.api.importapi.excel;

import com.fasterxml.jackson.databind.JsonNode;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Wrapper component with helper functions that allow to extract data from JSON.
 */
public class JSONWrapper {
    @NotNull
    private final JsonNode json;

    public JSONWrapper(@NotNull JsonNode json) {
        this.json = json;
    }

    public String getID() {
        return this.json.get("id").textValue();
    }

    public String getURI() {
        return this.json.get("uri").textValue();
    }

    public String getCreatedBy() {
        return this.json.get("createdBy").textValue();
    }

    public String getCreatedDate() {
        return this.json.get("createdDate").textValue();
    }

    public String getLastModifiedBy() {
        return this.json.get("lastModifiedBy").textValue();
    }

    public String getLastModifiedDate() {
        return this.json.get("lastModifiedDate").textValue();
    }

    public String getType() {
        return this.json.get("type").get("id").textValue();
    }

    /**
     * Extract given property from JSON. The result is a list of values grouped by language. If property is not
     * localized an empty string is used as language instead.
     */
    public @NotNull Map<String, List<String>> getProperty(@NotNull String name) {
        Map<String, List<String>> result = new HashMap<>();

        JsonNode property = this.json.get("properties").get(name);
        if (property != null) {
            property.forEach(node -> {
                String lang = node.get("lang").asText();
                result.putIfAbsent(lang, new ArrayList<>());
                result.get(lang).add(node.get("value").textValue());
            });
        }

        return result;
    }

    /**
     * Extract given reference(s) from JSON.
     */
    public @NotNull List<JSONWrapper> getReference(@NotNull String name) {
        List<JSONWrapper> result = new ArrayList<>();

        JsonNode reference = this.json.get("references").get(name);
        if (reference != null) {
            reference.forEach(node -> result.add(new JSONWrapper(node)));
        }

        return result;
    }

    /**
     * Extract given referrer(s) from JSON.
     */
    public @NotNull List<JSONWrapper> getReferrer(@NotNull String name) {
        List<JSONWrapper> result = new ArrayList<>();

        JsonNode referrer = this.json.get("refeffers").get(name);
        if (referrer != null) {
            referrer.forEach(node -> result.add(new JSONWrapper(node)));
        }

        return result;
    }
}
