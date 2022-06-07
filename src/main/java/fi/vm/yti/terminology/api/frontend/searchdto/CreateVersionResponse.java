package fi.vm.yti.terminology.api.frontend.searchdto;

import java.util.UUID;

public class CreateVersionResponse {
    UUID newGraphId;
    String uri;

    public CreateVersionResponse(UUID newGraphId, String uri) {
        this.newGraphId = newGraphId;
        this.uri = uri;
    }

    public UUID getNewGraphId() {
        return newGraphId;
    }

    public void setNewGraphId(UUID newGraphId) {
        this.newGraphId = newGraphId;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}
