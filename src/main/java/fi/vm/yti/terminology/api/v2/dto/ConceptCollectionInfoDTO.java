package fi.vm.yti.terminology.api.v2.dto;

import fi.vm.yti.common.dto.ResourceCommonInfoDTO;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConceptCollectionInfoDTO extends ResourceCommonInfoDTO {
    class Concept {
        private String identifier;

        private Map<String, String> definition = Map.of();

        public String getIdentifier() {
            return identifier;
        }

        public void setIdentifier(String identifier) {
            this.identifier = identifier;
        }

        public Map<String, String> getDefinition() {
            return definition;
        }

        public void setDefinition(Map<String, String> definition) {
            this.definition = definition;
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

    public void addMember(String identifier, Map<String, String> definition) {
        Concept concept = new Concept();
        concept.identifier = identifier;
        concept.definition = definition;
        members.add(concept);
    }
}
