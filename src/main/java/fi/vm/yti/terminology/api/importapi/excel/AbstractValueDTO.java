package fi.vm.yti.terminology.api.importapi.excel;

import org.jetbrains.annotations.NotNull;

public abstract class AbstractValueDTO<T> implements ValueDTOInterface<T> {
    @NotNull
    protected final T value;

    public AbstractValueDTO(@NotNull T value) {
        this.value = value;
    }

    public @NotNull T getValue() {
        return this.value;
    }
}
