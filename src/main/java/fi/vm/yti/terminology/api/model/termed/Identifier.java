package fi.vm.yti.terminology.api.model.termed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import static java.util.UUID.randomUUID;

@JsonIgnoreProperties(value = { "properties", "references", "referrers" })
public final class Identifier implements Serializable {

    private final UUID id;
    private final TypeId type;

    // Jackson constructor
    private Identifier() {
        this(randomUUID(), TypeId.placeholder());
    }

    public Identifier(UUID id, TypeId type) {
        this.id = id;
        this.type = type;
    }

    public UUID getId() {
        return id;
    }

    public TypeId getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Identifier that = (Identifier) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type);
    }
}
