package fi.vm.yti.terminology.api.importapi.excel;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JSONWrapperTest {
    JsonNode json;

    @BeforeEach
    void setUp() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        json = mapper.readTree(new File("src/test/resources/importapi/excel/data.json"));
    }

    @Test
    public void testConstructor() {
        assertDoesNotThrow(() -> new JSONWrapper(json.get(0), new ArrayList<>()));
    }

    @Test
    public void testGetID() {
        JSONWrapper wrapper = new JSONWrapper(json.get(0), new ArrayList<>());
        assertEquals("d4070115-7932-4c85-a566-e35e2be625b3", wrapper.getID());
    }

    @Test
    public void testGetURI() {
        JSONWrapper wrapper = new JSONWrapper(json.get(0), new ArrayList<>());
        assertEquals("http://uri.suomi.fi/terminology/test/collection-0", wrapper.getURI());
    }

    @Test
    public void testGetCode() {
        JSONWrapper wrapper = new JSONWrapper(json.get(0), new ArrayList<>());
        assertEquals("collection-0", wrapper.getCode());
    }

    @Test
    public void testGetCreatedDate() {
        JSONWrapper wrapper = new JSONWrapper(json.get(0), new ArrayList<>());
        assertEquals(Instant.parse("2021-11-08T13:06:31Z"), wrapper.getCreatedDate());
    }

    @Test
    public void testGetLastModifiedDate() {
        JSONWrapper wrapper = new JSONWrapper(json.get(0), new ArrayList<>());
        assertEquals(Instant.parse("2021-11-09T10:48:54Z"), wrapper.getLastModifiedDate());
    }

    @Test
    public void testGetType() {
        JSONWrapper wrapper = new JSONWrapper(json.get(0), new ArrayList<>());
        assertEquals("Collection", wrapper.getType());
    }

    @Test
    public void testGetTypeAsText() {
        JSONWrapper wrapper = new JSONWrapper(json.get(0), new ArrayList<>());
        assertEquals("2", wrapper.getTypeAsText());
    }

    @Test
    public void testGetTypeAsTextOfTerminologicalVocabulary() {
        JSONWrapper wrapper = new JSONWrapper(json.get(4), new ArrayList<>());
        assertEquals("1", wrapper.getTypeAsText());
    }

    @Test
    public void testGetPropertyWithNonExistingProperty() {
        JSONWrapper wrapper = new JSONWrapper(json.get(0), new ArrayList<>());
        Map<String, List<String>> property = wrapper.getProperty("non-existing-property");
        assertEquals(0, property.size());
    }

    @Test
    public void testGetPropertyWithEmptyProperty() {
        JSONWrapper wrapper = new JSONWrapper(json.get(0), new ArrayList<>());
        Map<String, List<String>> property = wrapper.getProperty("empty-property");
        assertEquals(0, property.size());
    }

    @Test
    public void testGetPropertyWithOneValueAndNoLang() {
        JSONWrapper wrapper = new JSONWrapper(json.get(0), new ArrayList<>());
        Map<String, List<String>> property = wrapper.getProperty("one-value-no-lang");
        assertEquals(1, property.size());
        assertTrue(property.containsKey(""));
        assertEquals(1, property.get("").size());
        assertEquals("value of one-value-no-lang property", property.get("").get(0));
    }

    @Test
    public void testGetPropertyWithOneValueAndOneLang() {
        JSONWrapper wrapper = new JSONWrapper(json.get(0), new ArrayList<>());
        Map<String, List<String>> property = wrapper.getProperty("one-value-one-lang");
        assertEquals(1, property.size());
        assertTrue(property.containsKey("en"));
        assertEquals(1, property.get("en").size());
        assertEquals("value of one-value-one-lang property in English", property.get("en").get(0));
    }

    @Test
    public void testGetPropertyWithOneValueAndTwoLang() {
        JSONWrapper wrapper = new JSONWrapper(json.get(0), new ArrayList<>());
        Map<String, List<String>> property = wrapper.getProperty("one-value-two-lang");
        assertEquals(2, property.size());
        assertTrue(property.containsKey("en"));
        assertEquals(1, property.get("en").size());
        assertEquals("value of one-value-one-lang property in English", property.get("en").get(0));

        assertTrue(property.containsKey("fi"));
        assertEquals(1, property.get("fi").size());
        assertEquals("value of one-value-one-lang property in Finnish", property.get("fi").get(0));
    }

    @Test
    public void testGetPropertyWithTwoValuesAndOneLang() {
        JSONWrapper wrapper = new JSONWrapper(json.get(0), new ArrayList<>());
        Map<String, List<String>> property = wrapper.getProperty("two-values-one-lang");
        assertEquals(1, property.size());
        assertTrue(property.containsKey("en"));
        assertEquals(2, property.get("en").size());
        assertEquals("value #1 of simple-one-lang property in English", property.get("en").get(0));
        assertEquals("value #2 of simple-one-lang property in English", property.get("en").get(1));
    }

    @Test
    public void testGetFirstPropertyValueFromNonExistingProperty() {
        JSONWrapper wrapper = new JSONWrapper(json.get(0), new ArrayList<>());
        assertNull(wrapper.getFirstPropertyValue("non-existing-property", ""));
    }

    @Test
    public void testGetFirstPropertyValueFromEmptyProperty() {
        JSONWrapper wrapper = new JSONWrapper(json.get(0), new ArrayList<>());
        assertNull(wrapper.getFirstPropertyValue("empty-property", ""));
    }

    @Test
    public void testGetFirstPropertyValueFromPropertyHavingOneValueAndNoLang() {
        JSONWrapper wrapper = new JSONWrapper(json.get(0), new ArrayList<>());
        assertEquals(
                "value of one-value-no-lang property",
                wrapper.getFirstPropertyValue("one-value-no-lang", "")
        );
    }

    @Test
    public void testGetFirstPropertyValueFromPropertyHavingTwoValuesAndNoLang() {
        JSONWrapper wrapper = new JSONWrapper(json.get(0), new ArrayList<>());
        assertEquals(
                "value #1 of two-values-no-lang property",
                wrapper.getFirstPropertyValue("two-values-no-lang", "")
        );
    }

    @Test
    public void testGetFirstPropertyValueFromPropertyHavingOneValueAndTwoLang() {
        JSONWrapper wrapper = new JSONWrapper(json.get(0), new ArrayList<>());
        assertEquals(
                "value of one-value-one-lang property in English",
                wrapper.getFirstPropertyValue("one-value-two-lang", "en")
        );
        assertEquals(
                "value of one-value-one-lang property in Finnish",
                wrapper.getFirstPropertyValue("one-value-two-lang", "fi")
        );
    }

    @Test
    public void testGetFirstPropertyValueFromPropertyHavingOneValueAndTwoLangWithNonExistingLanguage() {
        JSONWrapper wrapper = new JSONWrapper(json.get(0), new ArrayList<>());
        // note: this is internally picking first item from hash map so order is not always guaranteed.
        assertEquals(
                "value of one-value-one-lang property in Finnish",
                wrapper.getFirstPropertyValue("one-value-two-lang", "sv")
        );
    }

    @Test
    public void testGetReferenceWithNonExistingReference() {
        JSONWrapper wrapper = new JSONWrapper(json.get(0), new ArrayList<>());
        assertEquals(0, wrapper.getReference("non-existing-reference").size());
    }

    @Test
    public void testGetReferenceWithEmptyReference() {
        JSONWrapper wrapper = new JSONWrapper(json.get(0), new ArrayList<>());
        assertEquals(0, wrapper.getReference("empty-reference").size());
    }

    @Test
    public void testGetReference() {
        JSONWrapper wrapper = new JSONWrapper(json.get(0), new ArrayList<>());
        List<JSONWrapper> reference = wrapper.getReference("broader");
        assertEquals(2, reference.size());
        assertEquals("http://uri.suomi.fi/terminology/test/concept-0", reference.get(0).getURI());
    }

    @Test
    public void testGetReferrerWithNonExistingReference() {
        JSONWrapper wrapper = new JSONWrapper(json.get(1), new ArrayList<>());
        assertEquals(0, wrapper.getReferrer("non-existing-referrer").size());
    }

    @Test
    public void testGetReferrerWithEmptyReference() {
        JSONWrapper wrapper = new JSONWrapper(json.get(1), new ArrayList<>());
        assertEquals(0, wrapper.getReferrer("empty-referrer").size());
    }

    @Test
    public void testGetReferrer() {
        JSONWrapper wrapper = new JSONWrapper(json.get(1), new ArrayList<>());
        List<JSONWrapper> referrer = wrapper.getReferrer("closeMatch");
        assertEquals(1, referrer.size());
        assertEquals("http://uri.suomi.fi/terminology/test/concept-0", referrer.get(0).getURI());
    }

    @Test
    public void testGetDefinition() {
        List<JSONWrapper> wrappers = new ArrayList<>();
        wrappers.add(new JSONWrapper(json.get(1), wrappers)); // starting point
        wrappers.add(new JSONWrapper(json.get(0), wrappers)); // unused extra item
        wrappers.add(new JSONWrapper(json.get(7), wrappers)); // definition

        JSONWrapper wrapper = wrappers.get(0);
        List<JSONWrapper> referrer = wrapper.getReferrer("closeMatch");
        assertEquals(1, referrer.size());
        assertSame(wrappers.get(2), referrer.get(0).getDefinition());
    }

    @Test
    public void testGetDefinitionWithoutDefinition() {
        List<JSONWrapper> wrappers = new ArrayList<>();
        wrappers.add(new JSONWrapper(json.get(1), wrappers)); // starting point
        wrappers.add(new JSONWrapper(json.get(0), wrappers)); // unused extra item (not the definition)

        JSONWrapper wrapper = wrappers.get(0);
        List<JSONWrapper> referrer = wrapper.getReferrer("closeMatch");
        assertEquals(1, referrer.size());
        assertNull(referrer.get(0).getDefinition());
    }
}
