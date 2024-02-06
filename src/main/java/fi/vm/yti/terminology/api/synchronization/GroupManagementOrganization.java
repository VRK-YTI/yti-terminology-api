package fi.vm.yti.terminology.api.synchronization;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyMap;

// Ignore unknown properties for easier deployment when groupmanagement
// introduces new changes
@JsonIgnoreProperties(ignoreUnknown = true)
public final class GroupManagementOrganization {

    private final UUID uuid;
    private final Map<String, String> prefLabel;
    private final Map<String, String> description;
    private final String url;
    private final boolean removed;
    private final UUID parentId;

    // Jackson constructor
    private GroupManagementOrganization() {
        this(UUID.randomUUID(), emptyMap(), emptyMap(), "", false, null);
    }

    GroupManagementOrganization(UUID uuid, Map<String, String> prefLabel, Map<String, String> description, String url, boolean removed, UUID parentId) {
        this.uuid = uuid;
        this.prefLabel = prefLabel;
        this.description = description;
        this.url = url;
        this.removed = removed;
        this.parentId = parentId;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Map<String, String> getPrefLabel() {
        return prefLabel;
    }

    public Map<String, String> getDescription() {
        return description;
    }

    public String getUrl() {
        return url;
    }

    public boolean isRemoved() {
        return removed;
    }

    public UUID getParentId() {
        return parentId;
    }
}
