package fi.vm.yti.terminology.elasticsearch;

import fi.vm.yti.terminology.api.util.IndexUtil;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class IndexUtilTest {

    @Test
    public void sortLabelDefaultLanguage() {
        HashMap<String, List<String>> labels = new HashMap<>();
        String title = "Suomeksi";
        labels.put("fi", List.of(title));

        Map<String, List<String>> sortLabels = IndexUtil.createSortLabels(labels);

        assertEquals(3, sortLabels.keySet().size());
        checkSortLabels(title, sortLabels, "fi", "sv", "en");
    }

    @Test
    public void sortLabelMultipleLanguages() {
        HashMap<String, List<String>> labels = new HashMap<>();
        String titleFI = "Suomeksi";
        String titleSV = "På svenska";
        labels.put("fi", List.of(titleFI));
        labels.put("sv", List.of(titleSV));

        Map<String, List<String>> sortLabels = IndexUtil.createSortLabels(labels);

        assertEquals(3, sortLabels.keySet().size());
        checkSortLabels(titleFI, sortLabels, "fi", "en");
        checkSortLabels(titleSV, sortLabels, "sv");
    }

    @Test
    public void sortLabelNonDefaultLanguage() {
        HashMap<String, List<String>> labels = new HashMap<>();
        String title = "Auf Deutsch";
        labels.put("de", List.of(title));

        Map<String, List<String>> sortLabels = IndexUtil.createSortLabels(labels);

        assertEquals(4, sortLabels.keySet().size());
        checkSortLabels(title, sortLabels, "fi", "sv", "en", "de");
    }

    private void checkSortLabels(String label, Map<String, List<String>> sortLabels, String...languages) {
        Arrays.asList(languages).forEach(lang -> {
            assertEquals(label.toLowerCase(), sortLabels.get(lang).get(0));
        });
    }

}
