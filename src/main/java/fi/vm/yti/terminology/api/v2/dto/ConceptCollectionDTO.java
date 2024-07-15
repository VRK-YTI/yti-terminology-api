package fi.vm.yti.terminology.api.v2.dto;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class ConceptCollectionDTO {
    private String identifier;

    private Map<String, String> label;

    private Map<String, String> description = Map.of();

    private Set<String> members = new LinkedHashSet<>();

    public String getIdentifier() {
        return identifier;
    }

    public Map<String, String> getLabel() {
        return label;
    }

    public void setLabel(Map<String, String> label) {
        this.label = label;
    }

    public Map<String, String> getDescription() {
        return description;
    }

    public void setDescription(Map<String, String> description) {
        this.description = description;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Set<String> getMembers() {
        return members;
    }

    public void setMembers(Set<String> members) {
        this.members = members;
    }

    public void addMember(String identifier) {
        members.add(identifier);
    }
}
