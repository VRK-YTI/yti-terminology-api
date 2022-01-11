package fi.vm.yti.terminology.api.frontend.searchdto;

import java.util.Collections;
import java.util.Map;

public class CountDTO {

    public CategoriesCountDTO getCategories() {
        return categories;
    }

    public void setCategories(CategoriesCountDTO categories) {
        this.categories = categories;
    }

    class CategoriesCountDTO {
        private long terminologicalVocabulary;
        private long otherVocabulary;
        private long concept;

        public CategoriesCountDTO() {
            this.terminologicalVocabulary = 0;
            this.otherVocabulary = 0;
            this.concept = 0;
        }

        public CategoriesCountDTO(Map<String, Long> categories) {
            this.terminologicalVocabulary = categories.getOrDefault(
                    "TerminologicalVocabulary", 0L);
            this.otherVocabulary = categories.getOrDefault(
                    "OtherVocabulary", 0L);
            this.concept = categories.getOrDefault(
                    "Concept", 0L);
        }

        public long getTerminologicalVocabulary() {
            return terminologicalVocabulary;
        }

        public void setTerminologicalVocabulary(int terminologicalVocabulary) {
            this.terminologicalVocabulary = terminologicalVocabulary;
        }

        public long getOtherVocabulary() {
            return otherVocabulary;
        }

        public void setOtherVocabulary(int otherVocabulary) {
            this.otherVocabulary = otherVocabulary;
        }

        public long getConcept() {
            return concept;
        }

        public void setConcept(int concept) {
            this.concept = concept;
        }
    }

    private CategoriesCountDTO categories;
    private Map<String, Long> statuses;
    private Map<String, Long> groups;

    public CountDTO() {
        this.categories = new CategoriesCountDTO();
        this.statuses = Collections.emptyMap();
        this.groups = Collections.emptyMap();
    }

    public CountDTO(
            final Map<String, Long> categories,
            final Map<String, Long> statuses,
            final Map<String, Long> groups) {
        this.categories = new CategoriesCountDTO(categories);
        this.statuses = statuses;
        this.groups = groups;
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
}
