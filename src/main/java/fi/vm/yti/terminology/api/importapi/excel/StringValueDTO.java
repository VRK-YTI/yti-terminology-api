package fi.vm.yti.terminology.api.importapi.excel;

import org.jetbrains.annotations.NotNull;

public class StringValueDTO extends AbstractValueDTO<String> {
    public StringValueDTO(@NotNull String value) {
        super(value);
    }

    public @NotNull String getValueAsString() {
        return this.value;
    }

    public int getValueAsInt() {
        try {
            return Integer.parseInt(this.value);
        } catch(NumberFormatException e) {
            return 0;
        }
    }

    public boolean isInteger() {
        try {
            Integer.parseInt(this.value);
        } catch(NumberFormatException e) {
            return false;
        }

        return true;
    }
}
