package fi.vm.yti.terminology.api.frontend.searchdto;

import java.util.UUID;

public class CreateVersionDTO {
    UUID graphId;
    String newCode;

    public UUID getGraphId() {
        return graphId;
    }

    public CreateVersionDTO() {
        this(UUID.randomUUID(), "");
    }

    public CreateVersionDTO(UUID graphId, String newCode) {
        this.graphId = graphId;
        this.newCode = newCode;
    }

    public void setGraphId(UUID graphId) {
        this.graphId = graphId;
    }

    public String getNewCode() {
        return newCode;
    }

    public void setNewCode(String newCode) {
        this.newCode = newCode;
    }
}
