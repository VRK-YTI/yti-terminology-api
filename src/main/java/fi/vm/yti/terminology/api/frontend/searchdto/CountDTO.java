package fi.vm.yti.terminology.api.frontend.searchdto;

import java.util.Collections;
import java.util.Map;

public class CountDTO {

    public enum Category {

        TERMINOLOGICAL_VOCABULARY("TerminologicalVocabulary"),
        OTHER_VOCABULARY("OtherVocabulary"),
        CONCEPT("Concept"),
        COLLECTION("Collection");

        private final String name;

        Category(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private Map<String, Long> categories;
    private Map<String, Long> statuses;
    private Map<String, Long> groups;
    private Map<String, Long> languages;

    public CountDTO() {
        this.categories = Collections.emptyMap();
        this.statuses = Collections.emptyMap();
        this.groups = Collections.emptyMap();
        this.languages = Collections.emptyMap();
    }

    public CountDTO(
            final Map<String, Long> categories,
            final Map<String, Long> statuses,
            final Map<String, Long> groups,
            final Map<String, Long> languages) {
        this.categories = categories;
        this.statuses = statuses;
        this.groups = groups;
        this.languages = languages;
    }

    public Map<String, Long> getCategories() {
        return categories;
    }

    public void setCategories(Map<String, Long> categories) {
        this.categories = categories;
    }

    public Map<String, Long> getStatuses() {
        return statuses;
    }

    public void setStatuses(Map<String, Long> statuses) {
        this.statuses = statuses;
    }

    public Map<String, Long> getGroups() {
        return groups;
    }

    public void setGroups(Map<String, Long> groups) {
        this.groups = groups;
    }

    public Map<String, Long> getLanguages() {
        return languages;
    }

    public void setLanguages(Map<String, Long> languages) {
        this.languages = languages;
    }
}
