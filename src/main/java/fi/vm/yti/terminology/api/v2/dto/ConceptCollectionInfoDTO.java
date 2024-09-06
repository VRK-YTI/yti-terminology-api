package fi.vm.yti.terminology.api.v2.dto;

import fi.vm.yti.common.dto.ResourceCommonInfoDTO;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class ConceptCollectionInfoDTO extends ResourceCommonInfoDTO {

    private String identifier;

    private Map<String, String> description = Map.of();

    private Set<ConceptReferenceInfoDTO> members = new LinkedHashSet<>();

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

    public Set<ConceptReferenceInfoDTO> getMembers() {
        return members;
    }

    public void setMembers(Set<ConceptReferenceInfoDTO> members) {
        this.members = members;
    }

    public void addMember(String identifier, String uri, Map<String, String> label, String prefix) {
        ConceptReferenceInfoDTO concept = new ConceptReferenceInfoDTO();
        concept.setIdentifier(identifier);
        concept.setReferenceURI(uri);
        concept.setLabel(label);
        concept.setPrefix(prefix);
        members.add(concept);
    }
}
