package fi.vm.yti.terminology.api.importapi.excel;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class InstantValueDTO extends AbstractValueDTO<Instant> {
    public InstantValueDTO(@NotNull Instant value) {
        super(value);
    }

    public @NotNull String getValueAsString() {
        return DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(this.value);
    }
}
