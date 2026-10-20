package fi.vm.yti.terminology.api.v2.util;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LabelSortingTest {

    private record Item(String id, Map<String, String> label) {}

    private static List<String> sortedIds(String lang, List<Item> items) {
        return items.stream()
                .sorted(LabelSorting.byLabel(lang, Item::label, Item::id))
                .map(Item::id)
                .toList();
    }

    @Test
    void finnishSortsScandinavianLettersAfterZ() {
        var items = List.of(
                new Item("o", Map.of("fi", "Öljy")),
                new Item("a-umlaut", Map.of("fi", "Äyriäinen")),
                new Item("z", Map.of("fi", "Zeta")),
                new Item("a-ring", Map.of("fi", "Ångström")),
                new Item("a", Map.of("fi", "Aakkonen")));

        assertEquals(List.of("a", "z", "a-ring", "a-umlaut", "o"), sortedIds("fi", items));
    }

    @Test
    void englishSortsAccentedLettersWithTheirBaseLetter() {
        var items = List.of(
                new Item("z", Map.of("en", "Zeta")),
                new Item("o", Map.of("en", "Öljy")),
                new Item("p", Map.of("en", "Pear")),
                new Item("a", Map.of("en", "Apple")));

        assertEquals(List.of("a", "o", "p", "z"), sortedIds("en", items));
    }

    @Test
    void comparisonIsCaseInsensitive() {
        var items = List.of(
                new Item("upper", Map.of("fi", "Banaani")),
                new Item("lower", Map.of("fi", "apina")));

        assertEquals(List.of("lower", "upper"), sortedIds("fi", items));
    }

    @Test
    void equalLabelsAreOrderedByTieBreaker() {
        var items = List.of(
                new Item("concept-2", Map.of("fi", "Sama")),
                new Item("concept-1", Map.of("fi", "sama")));

        assertEquals(List.of("concept-1", "concept-2"), sortedIds("fi", items));
    }

    @Test
    void itemsWithoutLabelSortLast() {
        var items = List.of(
                new Item("empty", Map.of()),
                new Item("null", null),
                new Item("labelled", Map.of("fi", "Öljy")));

        assertEquals(List.of("labelled", "empty", "null"), sortedIds("fi", items));
    }

    @Test
    void sortsByRequestedLanguageNotDefault() {
        var items = List.of(
                new Item("1", Map.of("fi", "Aamu", "en", "Morning")),
                new Item("2", Map.of("fi", "Ilta", "en", "Evening")));

        assertEquals(List.of("1", "2"), sortedIds("fi", items));
        assertEquals(List.of("2", "1"), sortedIds("en", items));
    }

    @Test
    void nullOrBlankLanguageUsesDefaultLanguage() {
        var items = List.of(
                new Item("1", Map.of("fi", "Ilta", "en", "Apple")),
                new Item("2", Map.of("fi", "Aamu", "en", "Zebra")));

        assertEquals(List.of("2¨", "1"), sortedIds(null, items));
        assertEquals(List.of("2", "1"), sortedIds(" ", items));
    }

    @Test
    void pickLabelFallsBackToDefaultLanguageThenEnglishThenFirstEntry() {
        assertEquals("sv", LabelSorting.pickLabel(Map.of("sv", "sv", "fi", "fi", "en", "en"), "sv"));
        assertEquals("fi", LabelSorting.pickLabel(Map.of("fi", "fi", "en", "en"), "sv"));
        assertEquals("en", LabelSorting.pickLabel(Map.of("en", "en", "se", "se"), "sv"));

        var noKnownLanguage = new LinkedHashMap<String, String>();
        noKnownLanguage.put("se", "first");
        noKnownLanguage.put("de", "second");
        assertEquals("first", LabelSorting.pickLabel(noKnownLanguage, "sv"));
    }

    @Test
    void pickLabelReturnsNullWithoutLabels() {
        assertNull(LabelSorting.pickLabel(null, "fi"));
        assertNull(LabelSorting.pickLabel(Map.of(), "fi"));
    }
}
