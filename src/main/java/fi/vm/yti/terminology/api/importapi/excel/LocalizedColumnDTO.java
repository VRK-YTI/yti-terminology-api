package fi.vm.yti.terminology.api.importapi.excel;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class LocalizedColumnDTO {
    @NotNull
    private final Map<Integer, CellDTO> cells;

    public LocalizedColumnDTO() {
        this.cells = new HashMap<>();
    }

    public @NotNull CellDTO getOrCreateCell(int currentRowIndex) {
        if (!this.cells.containsKey(currentRowIndex)) {
            this.cells.put(currentRowIndex, new CellDTO());
        }

        return this.cells.get(currentRowIndex);
    }

    public @NotNull Map<Integer, CellDTO> getCells() {
        return cells;
    }

    public int getColumnSpan() {
        return this.cells.values().stream()
                .mapToInt(CellDTO::getColumnSpan)
                .max()
                .orElse(1);
    }
}
