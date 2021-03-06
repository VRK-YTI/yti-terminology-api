package fi.vm.yti.terminology.api.model.termed;

import java.util.*;

public interface Node {

    UUID getId();
    TypeId getType();
    Map<String, List<Attribute>> getProperties();

    default String getCode() {
        return null;
    }

    default String getUri() {
        return null;
    }

    default Long getNumber() {
        return null;
    }

    default String getCreatedBy() {
        return null;
    }

    default Date getCreatedDate() {
        return null;
    }

    default String getLastModifiedBy() {
        return null;
    }

    default Date getLastModifiedDate() {
        return null;
    }
}
