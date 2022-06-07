package fi.vm.yti.terminology.api.importapi.excel;

import com.fasterxml.jackson.databind.JsonNode;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wrapper component with helper functions that allow to extract data from JSON.
 */
public class JSONWrapper {
    /**
     * Original JSON data of this wrapper.
     */
    @NotNull
    private final JsonNode json;

    /**
     * Other wrappers in the same list. This is used for finding other wrapper that defines some references/referrers.
     */
    @NotNull
    private final List<JSONWrapper> others;

    /**
     * Internal memo. This is used by concept links when fetching uri of the linked concept in other vocabulary.
     */
    private String memo;

    public JSONWrapper(@NotNull JsonNode json, @NotNull List<JSONWrapper> others) {
        this.json = json;
        this.others = others;
    }

    public String getID() {
        return this.json.get("id").textValue();
    }

    public String getURI() {
        return this.json.get("uri").textValue();
    }

    public String getCode() {
        return this.json.get("code").textValue();
    }

    public Instant getCreatedDate() {
        return OffsetDateTime.parse(this.json.get("createdDate").textValue()).toInstant();
    }

    public Instant getLastModifiedDate() {
        return OffsetDateTime.parse(this.json.get("lastModifiedDate").textValue()).toInstant();
    }

    public String getType() {
        return this.json.get("type").get("id").textValue();
    }

    public String getGraphId() {
        return this.json.get("type").get("graph").get("id").textValue();
    }

    public String getNamespace() {
        String uri = this.json.get("uri").textValue();
        Pattern p = Pattern.compile("uri.suomi.fi/terminology/(\\w+)/");
        Matcher m = p.matcher(uri);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

    /**
     * https://koodistot.suomi.fi/codescheme;registryCode=interoperabilityplatform;schemeCode=yterm
     */
    public String getTypeAsText() {
        String type = this.getType();

        if (type.equals("TerminologicalVocabulary")) {
            return "1";
        }

        return "2";
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
     * Extract given property from JSON and take first value of it. If the property is localized, try to use version in
     * the given language first.
     */
    public String getFirstPropertyValue(@NotNull String name, @NotNull String preferredLanguage) {
        Map<String, List<String>> property = this.getProperty(name);

        if (property.containsKey(preferredLanguage)) {
            return property.get(preferredLanguage).get(0);
        } else {
            return property.values().stream().flatMap(Collection::stream).findFirst().orElse(null);
        }
    }

    /**
     * Extract given reference(s) from JSON.
     */
    public @NotNull List<JSONWrapper> getReference(@NotNull String name) {
        List<JSONWrapper> result = new ArrayList<>();

        JsonNode reference = this.json.get("references").get(name);
        if (reference != null) {
            reference.forEach(node -> result.add(new JSONWrapper(node, this.others)));
        }

        return result;
    }

    /**
     * Extract given referrer(s) from JSON.
     */
    public @NotNull List<JSONWrapper> getReferrer(@NotNull String name) {
        List<JSONWrapper> result = new ArrayList<>();

        JsonNode referrer = this.json.get("referrers").get(name);
        if (referrer != null) {
            referrer.forEach(node -> result.add(new JSONWrapper(node, this.others)));
        }

        return result;
    }

    /**
     * Get definition of this wrapper.
     * <p>
     * Warning: definition.getDefinition() == definition.
     * <p>
     * Instead, this should be used only with getReference or getReferrer like:
     * wrapper.getReference(...).getDefinition().
     */
    public JSONWrapper getDefinition() {
        return this.others.stream()
                .filter(other -> other.getCode().equals(this.getCode()))
                .findFirst()
                .orElse(null);
    }

    public String getMemo() {
        return this.memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public List<String> getReferrerTypes() {
        List<String> fieldNames = new ArrayList<>();
        this.json.get("referrers").fieldNames().forEachRemaining(name -> fieldNames.add(name));
        return fieldNames;
    }
}
