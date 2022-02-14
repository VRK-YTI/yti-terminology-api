package fi.vm.yti.terminology.api.importapi.excel;

import org.jetbrains.annotations.NotNull;

public class StringValueDTO {
    @NotNull
    private final String value;

    public StringValueDTO(@NotNull String value) {
        this.value = value;
    }

    public @NotNull String getValue() {
        return value;
    }
}
