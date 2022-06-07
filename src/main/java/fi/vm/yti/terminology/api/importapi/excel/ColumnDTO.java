package fi.vm.yti.terminology.api.importapi.excel;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ColumnDTO {
    public static final boolean MULTI_COLUMN_MODE_DISABLED = true;

    public static final boolean MULTI_COLUMN_MODE_ENABLED = false;

    @NotNull
    private final Map<String, LocalizedColumnDTO> localizedColumns;

    @NotNull
    private final String name;

    private final boolean multiColumnModeDisabled;

    public ColumnDTO(@NotNull String name, boolean multiColumnModeDisabled) {
        this.name = name;
        this.multiColumnModeDisabled = multiColumnModeDisabled;
        this.localizedColumns = new HashMap<>();
    }

    public @NotNull LocalizedColumnDTO getOrCreateLocalizedColumn(@NotNull String lang) {
        if (!this.localizedColumns.containsKey(lang)) {
            this.localizedColumns.put(lang, new LocalizedColumnDTO());
        }

        return this.localizedColumns.get(lang);
    }

    public @NotNull String getName() {
        return name;
    }

    public boolean isMultiColumnModeDisabled() {
        return multiColumnModeDisabled;
    }

    public @NotNull Map<String, LocalizedColumnDTO> getLocalizedColumns() {
        return localizedColumns;
    }
}
