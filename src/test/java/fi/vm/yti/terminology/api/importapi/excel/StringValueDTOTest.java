package fi.vm.yti.terminology.api.importapi.excel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    public void testGetValueAsString() {
        var dto = new StringValueDTO("Test value");
        assertEquals("Test value", dto.getValueAsString());
    }

    @Test
    public void testGetValueAsInteger() {
        var dto = new StringValueDTO("123");
        assertEquals(123, dto.getValueAsInt());
    }

    @Test
    public void testIsIntegerWithNumericValue() {
        var dto = new StringValueDTO("123");
        assertTrue(dto.isInteger());
    }

    @Test
    public void testIsIntegerWithNonNumericValue() {
        var dto = new StringValueDTO("asdf");
        assertFalse(dto.isInteger());
    }
}
