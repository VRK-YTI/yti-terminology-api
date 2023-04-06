package fi.vm.yti.terminology.api.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class ResolvedResource {

    public enum Type {
        VOCABULARY,
        CONCEPT,
        COLLECTION
    }

    @NotNull
    private final UUID graphId;
    @NotNull
    private final Type type;
    @Nullable
    private final UUID id;

    ResolvedResource(@NotNull UUID graphId,
                     @NotNull Type type) {
        this(graphId, type, null);
    }

    public ResolvedResource(@NotNull UUID graphId,
                     @NotNull Type type,
                     @Nullable UUID id) {
        this.graphId = graphId;
        this.type = type;
        this.id = id;
    }

    @NotNull
    public UUID getGraphId() {
        return graphId;
    }

    @NotNull
    public Type getType() {
        return type;
    }

    @Nullable
    public UUID getId() {
        return id;
    }
}
