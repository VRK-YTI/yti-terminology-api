package fi.vm.yti.terminology.api.importapi.excel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringValueDTOTest {
    @Test
    public void testConstructor() {
        assertDoesNotThrow(() -> new StringValueDTO(""));
    }

    @Test
    public void testGetValue() {
        var dto = new StringValueDTO("Test value");
        assertEquals("Test value", dto.getValue());
    }
}
