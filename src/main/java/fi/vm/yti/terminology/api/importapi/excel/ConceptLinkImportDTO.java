package fi.vm.yti.terminology.api.importapi.excel;

import fi.vm.yti.terminology.api.model.termed.GenericNode;

public class ConceptLinkImportDTO {

    private String referenceType;
    private GenericNode conceptLinkNode;

    public ConceptLinkImportDTO(String referenceType, GenericNode node) {
        this.referenceType = referenceType;
        this.conceptLinkNode = node;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public GenericNode getConceptLinkNode() {
        return conceptLinkNode;
    }

}
