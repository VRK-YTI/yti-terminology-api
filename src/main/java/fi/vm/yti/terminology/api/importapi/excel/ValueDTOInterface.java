package fi.vm.yti.terminology.api.importapi.excel;

import org.jetbrains.annotations.NotNull;

public interface ValueDTOInterface<T> {
    public @NotNull T getValue();
    public @NotNull String getValueAsString();
}
