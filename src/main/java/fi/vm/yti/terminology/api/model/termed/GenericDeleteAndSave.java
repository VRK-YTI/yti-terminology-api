package fi.vm.yti.terminology.api.model.termed;

import java.io.Serializable;
import java.util.List;

import static java.util.Collections.emptyList;

public final class GenericDeleteAndSave implements DeleteAndSave, Serializable {

    private final List<Identifier> delete;
    private final List<GenericNode> save;

    // Jackson constructor
    private GenericDeleteAndSave() {
        this(emptyList(), emptyList());
    }

    public GenericDeleteAndSave(List<Identifier> delete, List<GenericNode> save) {
        this.delete = delete;
        this.save = save;
    }

    public List<Identifier> getDelete() {
        return delete;
    }

    public List<GenericNode> getSave() {
        return save;
    }
}
