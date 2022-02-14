package fi.vm.yti.terminology.api.importapi.excel;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class CellDTO {
    @NotNull
    private final List<StringValueDTO> values;

    public CellDTO() {
        this.values = new ArrayList<>();
    }

    public void addAll(@NotNull Collection<StringValueDTO> values) {
        this.values.addAll(values);
    }

    public @NotNull List<StringValueDTO> getValues() {
        return values;
    }

    public int getColumnSpan() {
        if (this.values.isEmpty()) {
            return 1;
        }

        return this.values.size();
    }

    public @NotNull String joinValues() {
        return this.values.stream()
                .map(StringValueDTO::getValue)
                .collect(Collectors.joining(";"));
    }
}
