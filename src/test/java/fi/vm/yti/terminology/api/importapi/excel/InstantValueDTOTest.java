package fi.vm.yti.terminology.api.importapi.excel;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class InstantValueDTOTest {
    @Test
    public void testConstructor() {
        assertDoesNotThrow(() -> new InstantValueDTO(Instant.now()));
    }

    @Test
    public void testGetValue() {
        var instant = Instant.now();
        var dto = new InstantValueDTO(instant);
        assertEquals(instant, dto.getValue());
    }

    @Test
    public void testGetValueAsString() {
        var instant = Instant.ofEpochMilli(1644927671637L);
        var dto = new InstantValueDTO(instant);
        assertEquals("2022-02-15 14:21:11", dto.getValueAsString());
    }
}
