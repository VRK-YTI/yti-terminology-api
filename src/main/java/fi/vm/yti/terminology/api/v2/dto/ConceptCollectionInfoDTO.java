package fi.vm.yti.terminology.api.v2.dto;

import fi.vm.yti.common.dto.ResourceCommonInfoDTO;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConceptCollectionInfoDTO extends ResourceCommonInfoDTO {
    class Concept {
        private String identifier;

        private String uri;

        private Map<String, String> label = Map.of();

        public String getIdentifier() {
            return identifier;
        }

        public void setIdentifier(String identifier) {
            this.identifier = identifier;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public Map<String, String> getLabel() {
            return label;
        }

        public void setLabel(Map<String, String> label) {
            this.label = label;
        }
    }

    private String identifier;

    private Map<String, String> description = Map.of();

    private Set<Concept> members = new HashSet<>();

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Map<String, String> getDescription() {
        return description;
    }

    public void setDescription(Map<String, String> description) {
        this.description = description;
    }

    public Set<Concept> getMembers() {
        return members;
    }

    public void setMembers(Set<Concept> members) {
        this.members = members;
    }

    public void addMember(String identifier, String uri, Map<String, String> label) {
        Concept concept = new Concept();
        concept.identifier = identifier;
        concept.uri = uri;
        concept.label = label;
        members.add(concept);
    }
}
